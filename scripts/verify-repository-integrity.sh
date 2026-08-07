#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

fail() {
  echo "REPOSITORY_INTEGRITY_FAILURE: $*" >&2
  exit 1
}

require_tracked() {
  git ls-files --error-unmatch "$1" >/dev/null 2>&1 || fail "required tracked file is missing: $1"
}

require_spec() {
  test -f ".specops/$1/spec.json" || fail "referenced SpecOps specification is missing: $1"
}

require_tracked AGENTS.md
require_tracked README.md
require_tracked scripts/verify.sh
require_tracked scripts/capture-gate-output.sh
require_tracked scripts/maven-repository-path.sh
require_tracked scripts/test-capture-gate-output.sh
require_tracked scripts/test-maven-repository-path.sh
require_tracked scripts/test-release-workflow-budget.sh
require_tracked scripts/test-fast-pr-workflow.sh
require_tracked scripts/verify-pr.sh
require_tracked scripts/verify-release.sh
require_tracked scripts/verify-release-gates.sh
require_tracked scripts/verify-mega-backend.sh
require_tracked scripts/verify-postgres.sh
require_tracked .github/workflows/verify.yml
require_tracked conformance/mega-backend/README.md
require_tracked conformance/mega-backend/selection.md
require_tracked conformance/mega-backend/annotation-overlay.patch
require_tracked conformance/mega-backend/conformance-report.md
require_tracked conformance/mega-backend/src/test/java/at/gepardec/fachtracing/conformance/ForbiddenReferenceTest.java
require_tracked conformance/mega-backend/src/test/java/at/gepardec/fachtracing/conformance/MegaBackendConformanceTest.java
require_tracked conformance/mega-backend/src/test/resources/oracles/README.md

for oracle in \
  authorize-clarification-resolution \
  detect-overlapping-time-entries \
  determine-journey-warnings \
  determine-project-activity-in-month \
  validate-journey-direction
do
  require_tracked "conformance/mega-backend/src/test/resources/oracles/$oracle.txt"
done

check_hash() {
  expected=$1
  file=$2
  actual=$(shasum -a 256 "$file" | awk '{print $1}')
  test "$actual" = "$expected" || fail "reviewed oracle hash changed: $file"
}

check_hash 192cd18116ed3522a7bd4ab07dc6cd4000cfbc4b42afaf830e4ed620ca3f3848 \
  conformance/mega-backend/src/test/resources/oracles/authorize-clarification-resolution.txt
check_hash cccbb57b3ac143b86565c50ffeefb536d17ed6d2671feb7cf1ff2d553ee54198 \
  conformance/mega-backend/src/test/resources/oracles/detect-overlapping-time-entries.txt
check_hash 9c5bf68c9e9949eaca187c6675748b3cbe43dd170aff7c5ed3b32f1fb0b8affe \
  conformance/mega-backend/src/test/resources/oracles/determine-journey-warnings.txt
check_hash 0d4a30c9cc47e99913852f9546c7e3bc849b54c5b866490fda2cbfc3b1f11e38 \
  conformance/mega-backend/src/test/resources/oracles/determine-project-activity-in-month.txt
check_hash 1684955f4aa81930040a8d9df919be77df0f953f1fd4a419d70b6621a9f6c36e \
  conformance/mega-backend/src/test/resources/oracles/validate-journey-direction.txt

for spec_file in .specops/*/spec.json
do
  spec_id=$(sed -n 's/^[[:space:]]*"id":[[:space:]]*"\([^"]*\)".*/\1/p' "$spec_file" | head -1)
  test -n "$spec_id" || fail "specification has no id: $spec_file"
  grep -E -q '"id"[[:space:]]*:[[:space:]]*"'"$spec_id"'"' .specops/index.json \
    || fail "specification is absent from index.json: $spec_id"
done

for indexed_id in $(sed -n 's/^[[:space:]]*"id":[[:space:]]*"\([^"]*\)".*/\1/p' .specops/index.json)
do
  require_spec "$indexed_id"
done

for dependency_id in $(grep -E -h -o '"specId"[[:space:]]*:[[:space:]]*"[^"]+"' .specops/*/spec.json \
  | sed 's/.*"specId"[[:space:]]*:[[:space:]]*"\([^"]*\)"/\1/')
do
  require_spec "$dependency_id"
done

for initiative in .specops/initiatives/*.json
do
  awk 'collecting && /]/{exit} collecting{print} /"specs"[[:space:]]*:/{collecting=1}' "$initiative" \
    | sed -n 's/.*"\([a-zA-Z0-9._-]*\)".*/\1/p' \
    | while read -r initiative_spec
      do
        require_spec "$initiative_spec"
      done
done

for relative_link in $(grep -E -o '\]\([^)]+\)' README.md \
  | sed 's/^](//;s/)$//' \
  | grep -E -v '^(https?://|#)')
do
  link_path=${relative_link%%#*}
  test -e "$link_path" || fail "README link target is missing: $relative_link"
done

echo "REPOSITORY_INTEGRITY_OK"
