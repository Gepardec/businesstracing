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
require_tracked scripts/verify-self-tracing.sh
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
require_tracked scripts/verify-spring-petclinic.sh
require_tracked scripts/verify-postgres.sh
require_tracked .github/workflows/verify.yml
require_tracked conformance/mega-backend/README.md
require_tracked conformance/mega-backend/selection.md
require_tracked conformance/mega-backend/annotation-overlay.patch
require_tracked conformance/mega-backend/conformance-report.md
require_tracked conformance/mega-backend/src/test/java/at/gepardec/fachtracing/conformance/ForbiddenReferenceTest.java
require_tracked conformance/mega-backend/src/test/java/at/gepardec/fachtracing/conformance/MegaBackendConformanceTest.java
require_tracked conformance/mega-backend/src/test/resources/oracles/README.md
require_tracked conformance/spring-petclinic/README.md
require_tracked conformance/spring-petclinic/selection.md
require_tracked conformance/spring-petclinic/annotation-overlay.patch
require_tracked conformance/spring-petclinic/conformance-report.md
require_tracked conformance/spring-petclinic/src/test/java/at/gepardec/fachtracing/conformance/SpringPetClinicConformanceTest.java
require_tracked conformance/spring-petclinic/src/test/java/at/gepardec/fachtracing/conformance/SpringPetClinicIsolationTest.java
require_tracked conformance/spring-petclinic/src/test/resources/oracles/README.md
require_tracked docs/self-tracing.md

test -z "$(git ls-files conformance/mega-backend/generated)" \
  || fail "generated Mega artifacts must not be tracked: use conformance/mega-backend/target/generated"
test -z "$(git ls-files conformance/spring-petclinic/generated)" \
  || fail "generated PetClinic artifacts must not be tracked: use conformance/spring-petclinic/target/generated"

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
check_hash 368391bfdb748f2d453d78439386083b83b2be4363201915589963aa878ede0a \
  conformance/mega-backend/src/test/resources/oracles/determine-journey-warnings.txt
check_hash 0d4a30c9cc47e99913852f9546c7e3bc849b54c5b866490fda2cbfc3b1f11e38 \
  conformance/mega-backend/src/test/resources/oracles/determine-project-activity-in-month.txt
check_hash 1684955f4aa81930040a8d9df919be77df0f953f1fd4a419d70b6621a9f6c36e \
  conformance/mega-backend/src/test/resources/oracles/validate-journey-direction.txt

for oracle in \
  determine-whether-an-entity-is-new \
  find-an-eligible-pet-by-name \
  register-a-new-pet
do
  require_tracked "conformance/spring-petclinic/src/test/resources/oracles/$oracle.txt"
done

check_hash 72c426152fd2e4aea025d4758da7d85faf3b7a1ac47824ba9f56d83df247b5b0 \
  conformance/spring-petclinic/src/test/resources/oracles/determine-whether-an-entity-is-new.txt
check_hash ccb613cd380454d5aaf939c210adab10d40e89fdbda312fe985a436bc13bed9d \
  conformance/spring-petclinic/src/test/resources/oracles/find-an-eligible-pet-by-name.txt
check_hash 888019a73253cb98abad87b79867da763bc833abe002e386fb6dad572c8e9a85 \
  conformance/spring-petclinic/src/test/resources/oracles/register-a-new-pet.txt

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
