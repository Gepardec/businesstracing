#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

if [ "${FACHTRACING_SKIP_PROJECT_BUILD:-false}" != "true" ]; then
  mvn -q install
fi

SELF_REPOSITORY_URL=${FACHTRACING_SELF_REPOSITORY_URL:-https://github.com/Gepardec/businesstracing}
SELF_SOURCE_URL_TEMPLATE=${FACHTRACING_SELF_SOURCE_URL_TEMPLATE:-}
if [ -z "$SELF_SOURCE_URL_TEMPLATE" ]; then
  SELF_SOURCE_URL_TEMPLATE='https://github.com/Gepardec/businesstracing/blob/{commit}/{path}#L{line}'
fi

run_analysis() {
  if [ "${FACHTRACING_SELF_DEVELOPER_JSON:-false}" = "true" ]; then
    mvn -q -Pself-tracing -f "$ROOT/pom.xml" compile \
      -Dfachtracing.repositoryUrl="$SELF_REPOSITORY_URL" \
      -Dfachtracing.sourceUrlTemplate="$SELF_SOURCE_URL_TEMPLATE" \
      at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-rc.1:analyze-reactor
  else
    mvn -q -Pself-tracing -f "$ROOT/pom.xml" compile \
      at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-rc.1:analyze-reactor
  fi
}

run_analysis

OUTPUT="$ROOT/target/fachtracing"
NODE_BASE="$OUTPUT/include-exact-node-in-business-graph"
SOURCE_BASE="$OUTPUT/select-source-inputs-for-graph-analysis"

for base in "$NODE_BASE" "$SOURCE_BASE"
do
  test -f "$base-structure.mmd"
  test -f "$base-business.mmd"
  test -f "$base-business.json"
  test -f "$base-analysis-audit.mmd"
  test -f "$base-projection-audit.mmd"
done
test -f "$OUTPUT/index.md"
test -f "$OUTPUT/activation.json"

grep -F -q 'include exact node in business graph' "$OUTPUT/index.md"
grep -F -q 'select source inputs for graph analysis' "$OUTPUT/index.md"
grep -F -q 'analysis audit Mermaid' "$OUTPUT/index.md"
grep -F -q 'projection audit Mermaid' "$OUTPUT/index.md"
grep -F -q 'redundant rule' "$NODE_BASE-business.mmd"
grep -F -q 'loop mechanics' "$NODE_BASE-business.mmd"
grep -F -q 'choose by node kind' "$NODE_BASE-structure.mmd"
grep -F -q 'technical predicate' "$NODE_BASE-business.mmd"
grep -F -q 'business rule' "$NODE_BASE-business.mmd"
grep -F -q 'business action' "$NODE_BASE-business.mmd"
grep -F -q 'REMOVED / TECHNICAL_CALCULATION' "$NODE_BASE-projection-audit.mmd"
grep -F -q 'REMOVED / TECHNICAL_CHOICE' "$NODE_BASE-projection-audit.mmd"
grep -F -q 'KEPT / BUSINESS_RULE' "$NODE_BASE-projection-audit.mmd"
grep -F -q 'REPLACED / TERMINAL_RESULT' "$NODE_BASE-projection-audit.mmd"
grep -F -q 'does not have graph entry source' "$SOURCE_BASE-structure.mmd"
grep -F -q 'REMOVED / TECHNICAL_PREDICATE' "$SOURCE_BASE-projection-audit.mmd"
grep -F -q 'use connected project sources with external sources classpath and entries' \
  "$SOURCE_BASE-business.mmd"
grep -F -q 'use modular project sources with external sources classpath and entries' \
  "$SOURCE_BASE-business.mmd"
grep -F -q 'AnalysisSourceSelector.java' "$SOURCE_BASE-analysis-audit.mmd"
grep -F -q '"schema":"fachtracing-activation/v3"' "$OUTPUT/activation.json"
grep -F -q '"memberHint":"classifyNode"' "$OUTPUT/activation.json"
grep -F -q '"memberHint":"selectPlan"' "$OUTPUT/activation.json"
if [ "${FACHTRACING_SELF_DEVELOPER_JSON:-false}" = "true" ]; then
  for developer in "$OUTPUT"/*-developer.json
  do
    grep -F -q '"schema":"fachtracing-developer-graph/v1"' "$developer"
  done
  test "$(find "$OUTPUT" -name '*-developer.json' -type f | wc -l | tr -d ' ')" = "3"
fi

CHECKSUMS="$ROOT/target/self-tracing-checksums.txt"
shasum -a 256 \
  "$NODE_BASE-structure.mmd" "$NODE_BASE-business.mmd" \
  "$NODE_BASE-analysis-audit.mmd" "$NODE_BASE-projection-audit.mmd" \
  "$SOURCE_BASE-structure.mmd" "$SOURCE_BASE-business.mmd" \
  "$SOURCE_BASE-analysis-audit.mmd" "$SOURCE_BASE-projection-audit.mmd" > "$CHECKSUMS"
if [ "${FACHTRACING_SELF_DEVELOPER_JSON:-false}" = "true" ]; then
  shasum -a 256 "$OUTPUT"/*-developer.json >> "$CHECKSUMS"
fi
run_analysis
shasum -a 256 -c "$CHECKSUMS"

PRODUCTION_ROOTS="fachtracing-api/src/main fachtracing-engine/src/main fachtracing-agent/src/main fachtracing-maven-plugin/src/main fachtracing-spring/src/main fachtracing-storage-jdbc/src/main"
if grep -E -i -r -q 'include exact node in business graph|select source inputs for graph analysis' $PRODUCTION_ROOTS
then
  echo "Production code contains a self-example decision label." >&2
  exit 1
fi
if grep -E -i -r -q 'openai|anthropic|language model|generative ai' $PRODUCTION_ROOTS
then
  echo "Production graph generation contains an AI integration." >&2
  exit 1
fi

CLASSPATH_FILE="$ROOT/target/self-tracing-classpath.txt"
mvn -q -pl fachtracing-maven-plugin dependency:build-classpath \
  -DincludeScope=test -Dmdep.outputFile="$CLASSPATH_FILE"

if [ -z "${JAVA21:-}" ]; then
  if [ -x /usr/libexec/java_home ]; then
    JAVA21="$(/usr/libexec/java_home -v 21)/bin/java"
  else
    JAVA21=$(command -v java)
  fi
fi
AGENT="$ROOT/fachtracing-agent/target/fachtracing-agent-0.1.0-rc.1.jar"
test -f "$AGENT"
DEPENDENCY_CLASSPATH=$(cat "$CLASSPATH_FILE")
RUNTIME_CLASSPATH="$ROOT/fachtracing-maven-plugin/target/test-classes:$ROOT/fachtracing-maven-plugin/target/classes:$ROOT/fachtracing-agent/target/classes:$ROOT/fachtracing-engine/target/classes:$ROOT/fachtracing-api/target/classes:$DEPENDENCY_CLASSPATH"
RUNTIME_OUTPUT="$ROOT/target/fachtracing-runtime"
rm -f "$RUNTIME_OUTPUT"/*.mmd "$RUNTIME_OUTPUT"/*.decision.json 2>/dev/null || true
RUNTIME_RESULT=$("$JAVA21" -ea -javaagent:"$AGENT" -cp "$RUNTIME_CLASSPATH" \
  at.gepardec.fachtracing.maven.SelfTracingRuntimeTest \
  "$OUTPUT/activation.json" "$RUNTIME_OUTPUT")
printf '%s\n' "$RUNTIME_RESULT"
printf '%s\n' "$RUNTIME_RESULT" | grep -F -q 'FACHTRACING_SELF_RUNTIME_TRACE_OK'

test "$(find "$RUNTIME_OUTPUT" -name '*.mmd' -type f | wc -l | tr -d ' ')" = "5"
test "$(find "$RUNTIME_OUTPUT" -name '*.decision.json' -type f | wc -l | tr -d ' ')" = "5"
for decision in "$RUNTIME_OUTPUT"/*.decision.json
do
  grep -F -q '"schema":"fachtracing-decision-record/v1"' "$decision"
  grep -F -q '"application":{"type":"string","canonicalValue":"fachtracing"' "$decision"
  grep -F -q '"selectedEdgeId":"' "$decision"
done
grep -F -q 'Result: TECHNICAL_PREDICATE' "$RUNTIME_OUTPUT/01-node-removed-technical-predicate.mmd"
grep -F -q 'Result: BUSINESS_ACTION' "$RUNTIME_OUTPUT/02-node-kept-business-action.mmd"
grep -F -q 'Result: SKIP_PROJECT_WITH_NO_ENTRY_SOURCE' "$RUNTIME_OUTPUT/03-source-no-entry.mmd"
grep -F -q 'Result: USE_CONNECTED_PROJECT_SOURCES_WITH_EXTERNAL_SOURCES_CLASSPATH_AND_ENTRIES' \
  "$RUNTIME_OUTPUT/04-source-connected.mmd"
grep -F -q 'Result: USE_MODULAR_PROJECT_SOURCES_WITH_EXTERNAL_SOURCES_CLASSPATH_AND_ENTRIES' \
  "$RUNTIME_OUTPUT/05-source-modular.mmd"

echo FACHTRACING_SELF_TRACE_OK
