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
require_tracked scripts/verify-keycloak.sh
require_tracked scripts/verify-spring-petclinic.sh
require_tracked scripts/verify-jakartaee-rest.sh
require_tracked scripts/verify-postgres.sh
require_tracked .github/workflows/verify.yml
require_tracked conformance/mega-backend/README.md
require_tracked conformance/mega-backend/selection.md
require_tracked conformance/mega-backend/conformance-report.md
require_tracked conformance/mega-backend/src/test/java/at/gepardec/fachtracing/conformance/ForbiddenReferenceTest.java
require_tracked conformance/mega-backend/src/test/java/at/gepardec/fachtracing/conformance/MegaBackendConformanceTest.java
require_tracked conformance/mega-backend/src/test/resources/oracles/README.md
require_tracked conformance/keycloak/README.md
require_tracked conformance/keycloak/selection.md
require_tracked conformance/keycloak/src/test/java/at/gepardec/fachtracing/conformance/KeycloakConformanceTest.java
require_tracked conformance/spring-petclinic/README.md
require_tracked conformance/spring-petclinic/selection.md
require_tracked conformance/spring-petclinic/annotation-overlay.patch
require_tracked conformance/spring-petclinic/conformance-report.md
require_tracked conformance/spring-petclinic/src/test/java/at/gepardec/fachtracing/conformance/BusinessJsonSchemaConformance.java
require_tracked conformance/spring-petclinic/src/test/java/at/gepardec/fachtracing/conformance/SpringPetClinicConformanceTest.java
require_tracked conformance/spring-petclinic/src/test/java/at/gepardec/fachtracing/conformance/SpringPetClinicIsolationTest.java
require_tracked conformance/spring-petclinic/src/test/resources/oracles/README.md
require_tracked conformance/jakartaee-rest/README.md
require_tracked conformance/jakartaee-rest/selection.md
require_tracked conformance/jakartaee-rest/src/test/java/at/gepardec/fachtracing/conformance/JakartaEeRestConformanceTest.java
require_tracked docs/self-tracing.md
require_tracked fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisSourceSelector.java
require_tracked fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphAudit.java
require_tracked fachtracing-engine/src/main/java/at/gepardec/fachtracing/developer/DecisionAuditMermaidRenderer.java
require_tracked fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/SourceUnavailableCallClassifier.java
require_tracked fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/ObservedBusinessSegmentConnector.java
require_tracked fachtracing-engine/src/test/resources/fixtures/analysis/SourceBoundaryBinaryRules.java
require_tracked fachtracing-engine/src/test/resources/fixtures/analysis/SourceBoundaryPolicy.java

test -z "$(git ls-files conformance/mega-backend/generated)" \
  || fail "generated Mega artifacts must not be tracked: use conformance/mega-backend/target/generated"
test -z "$(git ls-files conformance/keycloak/target)" \
  || fail "generated Keycloak artifacts must not be tracked: use conformance/keycloak/target/generated"
test -z "$(git ls-files conformance/spring-petclinic/generated)" \
  || fail "generated PetClinic artifacts must not be tracked: use conformance/spring-petclinic/target/generated"

KEYCLOAK_HARNESS=conformance/keycloak/src/test/java/at/gepardec/fachtracing/conformance/KeycloakConformanceTest.java
if grep -E -q 'reviewedOverview|overviewNode|overviewEdge|new BusinessLogicGraph' "$KEYCLOAK_HARNESS"
then
  fail "the Keycloak diagram must not use a manually constructed graph"
fi
if grep -E -q '^[[:space:]]*flowchart[[:space:]]' conformance/keycloak/README.md
then
  fail "the Keycloak guide must not embed a fixed Mermaid flowchart"
fi
if grep -E -q '^[[:space:]]*flowchart[[:space:]]' docs/self-tracing.md
then
  fail "the self-tracing guide must not embed a fixed Mermaid flowchart"
fi

PRODUCTION_ROOTS="fachtracing-api/src/main fachtracing-engine/src/main fachtracing-agent/src/main fachtracing-maven-plugin/src/main fachtracing-spring/src/main fachtracing-jakartaee/src/main fachtracing-storage-jdbc/src/main"
if grep -E -i -r -q 'keycloak|usersresource|org\.keycloak|com\.gepardec\.mega|mega[-_. ]backend' $PRODUCTION_ROOTS
then
  fail "production code must not contain reference-application identity"
fi
if grep -E -i -r -q 'search query is absent|admin permissions (enabled|disabled) for realm|determine journey warnings|warningcalculatorsmanager' $PRODUCTION_ROOTS
then
  fail "production code must not contain reviewed reference-application labels or methods"
fi

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
check_hash d907cbcc27f9dcb115d1f82b0ecbddc0497ed5cc95e8fb6c22104e81227f29e7 \
  conformance/mega-backend/src/test/resources/oracles/determine-journey-warnings.txt
check_hash 0d4a30c9cc47e99913852f9546c7e3bc849b54c5b866490fda2cbfc3b1f11e38 \
  conformance/mega-backend/src/test/resources/oracles/determine-project-activity-in-month.txt
check_hash 7a30ee2b6912fb8727ae1b6d686fcc8e1af3be4e78c4b26b9dc2c2f48b0f8148 \
  conformance/mega-backend/src/test/resources/oracles/validate-journey-direction.txt

for oracle in \
  owner-search \
  pet-registration \
  visit-booking
do
  require_tracked "conformance/spring-petclinic/src/test/resources/oracles/$oracle-business.json"
done

check_hash 85ef2d1f851f0c9df88bfe0f980752efcccd81359a00b14ec3955b86796df9a7 \
  conformance/spring-petclinic/src/test/resources/oracles/owner-search-business.json
check_hash 330c722131c8aebeeaa538483a5af5f799e133370fea5799b780cba001462d7b \
  conformance/spring-petclinic/src/test/resources/oracles/pet-registration-business.json
check_hash e0148a8cf3ec8f42210b0df765310c3e58d88cbf7bf6e428a5343822c2c587b3 \
  conformance/spring-petclinic/src/test/resources/oracles/visit-booking-business.json

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
