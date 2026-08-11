#!/usr/bin/env sh
set -eu

if [ "$#" -lt 2 ]; then
  echo "usage: capture-gate-output.sh OUTPUT COMMAND [ARGUMENT ...]" >&2
  exit 2
fi

OUTPUT=$1
shift
WORK=$(mktemp -d "${TMPDIR:-/tmp}/fachtracing-gate-output.XXXXXX")
PIPE="$WORK/output"
cleanup() { rm -rf "$WORK"; }
trap cleanup EXIT INT TERM

mkdir -p "$(dirname "$OUTPUT")"
mkfifo "$PIPE"
tee "$OUTPUT" < "$PIPE" &
TEE_PID=$!

set +e
"$@" > "$PIPE" 2>&1
STATUS=$?
wait "$TEE_PID"
TEE_STATUS=$?
set -e

if [ "$STATUS" -ne 0 ]; then
  exit "$STATUS"
fi
exit "$TEE_STATUS"
