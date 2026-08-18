#!/usr/bin/env sh
set -eu

MATRIX="docs/java-capabilities.json"
DOC="docs/supported-java-constructs.md"

python3 -m json.tool "$MATRIX" >/dev/null
grep -q 'fachtracing-java-capabilities/v1' "$MATRIX"
grep -q 'Machine-readable capability IDs' "$DOC"

contracts=$(sed -n 's/.*"contract":"\([^"]*\)".*/\1/p' "$MATRIX")
test -n "$contracts"
for contract in $contracts; do
  class=${contract%%.*}
  method=${contract#*.}
  case "$class" in
    StaticDecisionAnalyzerTest)
      file="fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java" ;;
    RuntimeCollectorTest)
      file="fachtracing-engine/src/test/java/at/gepardec/fachtracing/runtime/RuntimeCollectorTest.java" ;;
    FachtracingTransformerTest)
      file="fachtracing-agent/src/test/java/at/gepardec/fachtracing/agent/FachtracingTransformerTest.java" ;;
    ApiModelTest)
      file="fachtracing-engine/src/test/java/at/gepardec/fachtracing/model/ApiModelTest.java" ;;
    BusinessGraphProjectionTest)
      file="fachtracing-engine/src/test/java/at/gepardec/fachtracing/business/BusinessGraphProjectionTest.java" ;;
    *)
      echo "Unknown capability contract class: $class" >&2
      exit 1 ;;
  esac
  grep -E -q "[[:space:]]$method\\(" "$file" || {
    echo "Missing capability contract: $contract" >&2
    exit 1
  }
done

ids=$(sed -n 's/.*"id":"\([^"]*\)".*/\1/p' "$MATRIX")
for id in $ids; do
  grep -F -q "\`$id\`" "$DOC" || {
    echo "Capability is missing from documentation: $id" >&2
    exit 1
  }
done

echo JAVA_CAPABILITIES_OK
