#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
WORK=$(mktemp -d "${TMPDIR:-/tmp}/fachtracing-gate-test.XXXXXX")
EVIDENCE="$WORK/evidence.txt"
TERMINAL="$WORK/terminal.txt"
STREAM_EVIDENCE="$WORK/stream-evidence.txt"
STREAM_TERMINAL="$WORK/stream-terminal.txt"
STREAM_READY="$WORK/stream-ready"
STREAM_RELEASE="$WORK/stream-release"
cleanup() { rm -rf "$WORK"; }
trap cleanup EXIT INT TERM

set +e
"$ROOT/scripts/capture-gate-output.sh" "$EVIDENCE" sh -c \
  'echo RELEASE_FIXTURE_FAILURE; exit 23' > "$TERMINAL" 2>&1
STATUS=$?
set -e

test "$STATUS" -eq 23
grep -q 'RELEASE_FIXTURE_FAILURE' "$EVIDENCE"
grep -q 'RELEASE_FIXTURE_FAILURE' "$TERMINAL"
if grep -q 'RELEASE_GATE_OK' "$EVIDENCE" "$TERMINAL"; then
  echo "release output helper emitted a false success marker" >&2
  exit 1
fi

"$ROOT/scripts/capture-gate-output.sh" "$STREAM_EVIDENCE" sh -c '
  echo RELEASE_STREAM_STARTED
  touch "$1"
  while [ ! -f "$2" ]; do sleep 1; done
  echo RELEASE_STREAM_DONE
' sh "$STREAM_READY" "$STREAM_RELEASE" > "$STREAM_TERMINAL" 2>&1 &
CAPTURE_PID=$!

while [ ! -f "$STREAM_READY" ]; do
  kill -0 "$CAPTURE_PID" 2>/dev/null || {
    echo "release output helper ended before the streaming fixture was ready" >&2
    wait "$CAPTURE_PID" || true
    exit 1
  }
  sleep 1
done

if ! grep -q 'RELEASE_STREAM_STARTED' "$STREAM_EVIDENCE" 2>/dev/null \
    || ! grep -q 'RELEASE_STREAM_STARTED' "$STREAM_TERMINAL" 2>/dev/null; then
  touch "$STREAM_RELEASE"
  wait "$CAPTURE_PID" || true
  echo "release output helper buffered output until command completion" >&2
  exit 1
fi

touch "$STREAM_RELEASE"
wait "$CAPTURE_PID"
grep -q 'RELEASE_STREAM_DONE' "$STREAM_EVIDENCE"
grep -q 'RELEASE_STREAM_DONE' "$STREAM_TERMINAL"

echo RELEASE_FAILURE_PROPAGATION_OK
