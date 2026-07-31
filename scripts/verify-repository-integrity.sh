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
require_tracked scripts/verify-mega-backend.sh
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

check_hash 18a7047cba3dd024c4058cd2f20411705c3e01b63502e04c5cc6a99c661cc384 \
  conformance/mega-backend/src/test/resources/oracles/authorize-clarification-resolution.txt
check_hash 04da12e820de5c3e640af192734261ddbb49f385ef99a573149331068e607ace \
  conformance/mega-backend/src/test/resources/oracles/detect-overlapping-time-entries.txt
check_hash 31c59b66a9a7887add4f88c850837cbdbfabdc6e9cfc90561e07103d80398205 \
  conformance/mega-backend/src/test/resources/oracles/determine-journey-warnings.txt
check_hash 53dba8048f35e02c15f6a67b03d0a7e7fb93dc08e980762af01899c262d70993 \
  conformance/mega-backend/src/test/resources/oracles/determine-project-activity-in-month.txt
check_hash 56ef5ac56f8a462569d00cbfd81157407e51c71e7c071866cb45be92a02c82ae \
  conformance/mega-backend/src/test/resources/oracles/validate-journey-direction.txt

for spec_file in .specops/*/spec.json
do
  spec_id=$(sed -n 's/^[[:space:]]*"id":[[:space:]]*"\([^"]*\)".*/\1/p' "$spec_file" | head -1)
  test -n "$spec_id" || fail "specification has no id: $spec_file"
  rg -q '"id"[[:space:]]*:[[:space:]]*"'"$spec_id"'"' .specops/index.json \
    || fail "specification is absent from index.json: $spec_id"
done

for indexed_id in $(sed -n 's/^[[:space:]]*"id":[[:space:]]*"\([^"]*\)".*/\1/p' .specops/index.json)
do
  require_spec "$indexed_id"
done

for dependency_id in $(rg -o '"specId"[[:space:]]*:[[:space:]]*"[^"]+"' .specops/*/spec.json \
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

for relative_link in $(rg -o '\]\([^)]+\)' README.md \
  | sed 's/^](//;s/)$//' \
  | rg -v '^(https?://|#)')
do
  link_path=${relative_link%%#*}
  test -e "$link_path" || fail "README link target is missing: $relative_link"
done

echo "REPOSITORY_INTEGRITY_OK"
