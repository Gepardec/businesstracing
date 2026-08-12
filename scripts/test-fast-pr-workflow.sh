#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
WORKFLOW="$ROOT/.github/workflows/verify.yml"
FAST="$ROOT/scripts/verify-pr.sh"

require_text() {
  text=$1
  file=$2
  grep -F -q "$text" "$file" || {
    echo "FAST_PR_WORKFLOW_FAILURE: missing '$text' in ${file#"$ROOT/"}" >&2
    exit 1
  }
}

reject_text() {
  text=$1
  file=$2
  if grep -F -q "$text" "$file"; then
    echo "FAST_PR_WORKFLOW_FAILURE: forbidden '$text' in ${file#"$ROOT/"}" >&2
    exit 1
  fi
}

require_text "schedule:" "$WORKFLOW"
require_text "tags: ['v*']" "$WORKFLOW"
require_text 'group: verify-${{ github.workflow }}-${{ github.ref }}' "$WORKFLOW"
require_text "cancel-in-progress: \${{ github.event_name == 'pull_request' }}" "$WORKFLOW"
require_text "pr-gate:" "$WORKFLOW"
require_text "mega:" "$WORKFLOW"
require_text "petclinic:" "$WORKFLOW"
require_text "postgres:" "$WORKFLOW"
require_text "cache: maven" "$WORKFLOW"
require_text "key: mega-backend-\${{ runner.os }}-782cdec8dfe5b4062eb5c1859e6a9e53afe02770" "$WORKFLOW"
require_text "key: spring-petclinic-\${{ runner.os }}-88e37c15cf6fc8490b01bc3e8e2c800cec1ac272" "$WORKFLOW"
require_text "run: ./scripts/verify.sh" "$WORKFLOW"
require_text "run: ./scripts/verify-mega-backend.sh" "$WORKFLOW"
require_text "run: ./scripts/verify-spring-petclinic.sh" "$WORKFLOW"
require_text "run: ./scripts/verify-postgres.sh" "$WORKFLOW"
require_text "FACHTRACING_SKIP_PROJECT_BUILD: 'true'" "$WORKFLOW"
reject_text "release-gate:" "$WORKFLOW"
reject_text "run: ./scripts/verify-release.sh" "$WORKFLOW"
reject_text "run: ./scripts/verify-pr.sh" "$WORKFLOW"

test -x "$FAST" || {
  echo "FAST_PR_WORKFLOW_FAILURE: scripts/verify-pr.sh is missing or not executable" >&2
  exit 1
}
require_text "./scripts/verify.sh" "$FAST"
require_text "FACHTRACING_SKIP_PROJECT_BUILD=true" "$FAST"
require_text "./scripts/verify-mega-backend.sh" "$FAST"
require_text "./scripts/verify-spring-petclinic.sh" "$FAST"
reject_text "verify-release.sh" "$FAST"

echo "FAST_PR_WORKFLOW_OK"
