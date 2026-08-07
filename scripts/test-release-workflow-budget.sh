#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
WORKFLOW="$ROOT/.github/workflows/verify.yml"
MINIMUM_TIMEOUT_MINUTES=50

timeout_minutes=$(awk '
  /^  release-gate:/ { in_release_gate = 1; next }
  in_release_gate && /^  [[:alnum:]_-]+:/ { exit }
  in_release_gate && /timeout-minutes:/ { print $2; exit }
' "$WORKFLOW")

case "$timeout_minutes" in
  ''|*[!0-9]*)
    echo "RELEASE_WORKFLOW_BUDGET_FAILURE: release-gate timeout is missing or invalid" >&2
    exit 1
    ;;
esac

if [ "$timeout_minutes" -lt "$MINIMUM_TIMEOUT_MINUTES" ]; then
  echo "RELEASE_WORKFLOW_BUDGET_FAILURE: release-gate timeout must be at least $MINIMUM_TIMEOUT_MINUTES minutes" >&2
  exit 1
fi

echo "RELEASE_WORKFLOW_BUDGET_OK timeout_minutes=$timeout_minutes"
