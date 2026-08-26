#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
WORKFLOW="$ROOT/.github/workflows/verify.yml"
MAXIMUM_TIMEOUT_MINUTES=5
REQUIRED_JOBS="pr-gate mega petclinic jakartaee-rest viewer postgres"

for required_job in $REQUIRED_JOBS
do
  timeout_minutes=$(awk -v job="$required_job" '
    $0 == "  " job ":" { in_job = 1; next }
    in_job && /^  [[:alnum:]_-]+:/ { exit }
    in_job && /timeout-minutes:/ { print $2; exit }
  ' "$WORKFLOW")

  case "$timeout_minutes" in
    ''|*[!0-9]*)
      echo "RELEASE_WORKFLOW_BUDGET_FAILURE: $required_job timeout is missing or invalid" >&2
      exit 1
      ;;
  esac

  if [ "$timeout_minutes" -gt "$MAXIMUM_TIMEOUT_MINUTES" ]; then
    echo "RELEASE_WORKFLOW_BUDGET_FAILURE: $required_job timeout must not exceed $MAXIMUM_TIMEOUT_MINUTES minutes" >&2
    exit 1
  fi

  expected_timeout=3
  if [ "$required_job" = "postgres" ]; then expected_timeout=5; fi
  if [ "$timeout_minutes" -ne "$expected_timeout" ]; then
    echo "RELEASE_WORKFLOW_BUDGET_FAILURE: $required_job timeout must be $expected_timeout minutes" >&2
    exit 1
  fi
done

job_count=$(awk '
  /^jobs:/ { in_jobs = 1; next }
  in_jobs && /^  [[:alnum:]_-]+:$/ { count++ }
  END { print count + 0 }
' "$WORKFLOW")

if [ "$job_count" -ne 6 ]; then
  echo "RELEASE_WORKFLOW_BUDGET_FAILURE: expected 6 required jobs, found $job_count" >&2
  exit 1
fi

echo "RELEASE_WORKFLOW_BUDGET_OK maximum_timeout_minutes=$MAXIMUM_TIMEOUT_MINUTES jobs=$job_count"
