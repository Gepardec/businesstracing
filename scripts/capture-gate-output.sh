#!/usr/bin/env sh
set -eu

if [ "$#" -lt 2 ]; then
  echo "usage: capture-gate-output.sh OUTPUT COMMAND [ARGUMENT ...]" >&2
  exit 2
fi

OUTPUT=$1
shift
TEMPORARY=$(mktemp "${TMPDIR:-/tmp}/fachtracing-gate-output.XXXXXX")
cleanup() { rm -f "$TEMPORARY"; }
trap cleanup EXIT INT TERM

set +e
"$@" > "$TEMPORARY" 2>&1
STATUS=$?
set -e

mkdir -p "$(dirname "$OUTPUT")"
tee "$OUTPUT" < "$TEMPORARY"
exit "$STATUS"
