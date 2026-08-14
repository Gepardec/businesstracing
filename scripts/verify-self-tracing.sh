#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

if [ "${FACHTRACING_SKIP_PROJECT_BUILD:-false}" != "true" ]; then
  mvn -q install
fi

mvn -q -f "$ROOT/pom.xml" compile \
  at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-rc.1:analyze-reactor

OUTPUT="$ROOT/target/fachtracing"
MERMAID="$OUTPUT/enable-developer-graph-export-structure.mmd"

test -f "$MERMAID"
test -f "$OUTPUT/enable-developer-graph-export-structure.puml"
test -f "$OUTPUT/index.md"
test -f "$OUTPUT/activation.json"

grep -F -q 'enable developer graph export' "$OUTPUT/index.md"
grep -F -q 'decision cannot continue' "$MERMAID"
grep -F -q 'fails' "$MERMAID"
grep -F -q 'returns optional empty' "$MERMAID"
grep -F -q 'returns optional of new developer output' "$MERMAID"
grep -F -q '"schema":"fachtracing-activation/v3"' "$OUTPUT/activation.json"

CLASSPATH_FILE="$ROOT/target/self-tracing-classpath.txt"
mvn -q -pl fachtracing-maven-plugin dependency:build-classpath \
  -DincludeScope=test -Dmdep.outputFile="$CLASSPATH_FILE"

JAVA21="$(/usr/libexec/java_home -v 21)/bin/java"
AGENT="$ROOT/fachtracing-agent/target/fachtracing-agent-0.1.0-rc.1.jar"
test -f "$AGENT"

DEPENDENCY_CLASSPATH=$(cat "$CLASSPATH_FILE")
RUNTIME_CLASSPATH="$ROOT/fachtracing-maven-plugin/target/test-classes:$ROOT/fachtracing-maven-plugin/target/classes:$ROOT/fachtracing-agent/target/classes:$ROOT/fachtracing-engine/target/classes:$ROOT/fachtracing-api/target/classes:$DEPENDENCY_CLASSPATH"
RUNTIME_RESULT=$("$JAVA21" -ea -javaagent:"$AGENT" -cp "$RUNTIME_CLASSPATH" \
  at.gepardec.fachtracing.maven.SelfTracingRuntimeTest "$OUTPUT/activation.json")
printf '%s\n' "$RUNTIME_RESULT"
printf '%s\n' "$RUNTIME_RESULT" | grep -F -q 'FACHTRACING_SELF_RUNTIME_TRACE_OK'

echo FACHTRACING_SELF_TRACE_OK
