#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
WORK=$(mktemp -d "${TMPDIR:-/tmp}/fachtracing-gate-test.XXXXXX")
EVIDENCE="$WORK/evidence.txt"
TERMINAL="$WORK/terminal.txt"
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

echo RELEASE_FAILURE_PROPAGATION_OK
