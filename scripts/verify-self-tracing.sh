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
ANALYSIS_AUDIT="$OUTPUT/enable-developer-graph-export-analysis-audit.mmd"
PROJECTION_AUDIT="$OUTPUT/enable-developer-graph-export-projection-audit.mmd"
NODE_STRUCTURE="$OUTPUT/include-exact-node-in-business-graph-structure.mmd"
NODE_ANALYSIS_AUDIT="$OUTPUT/include-exact-node-in-business-graph-analysis-audit.mmd"
NODE_PROJECTION_AUDIT="$OUTPUT/include-exact-node-in-business-graph-projection-audit.mmd"
SOURCE_STRUCTURE="$OUTPUT/select-source-inputs-for-graph-analysis-structure.mmd"
SOURCE_ANALYSIS_AUDIT="$OUTPUT/select-source-inputs-for-graph-analysis-analysis-audit.mmd"
SOURCE_PROJECTION_AUDIT="$OUTPUT/select-source-inputs-for-graph-analysis-projection-audit.mmd"

test -f "$MERMAID"
test -f "$OUTPUT/enable-developer-graph-export-structure.puml"
test -f "$ANALYSIS_AUDIT"
test -f "$PROJECTION_AUDIT"
test -f "$NODE_STRUCTURE"
test -f "$NODE_ANALYSIS_AUDIT"
test -f "$NODE_PROJECTION_AUDIT"
test -f "$SOURCE_STRUCTURE"
test -f "$SOURCE_ANALYSIS_AUDIT"
test -f "$SOURCE_PROJECTION_AUDIT"
test -f "$OUTPUT/index.md"
test -f "$OUTPUT/activation.json"

grep -F -q 'enable developer graph export' "$OUTPUT/index.md"
grep -F -q 'include exact node in business graph' "$OUTPUT/index.md"
grep -F -q 'select source inputs for graph analysis' "$OUTPUT/index.md"
grep -F -q 'decision cannot continue' "$MERMAID"
grep -F -q 'fails' "$MERMAID"
grep -F -q 'returns optional empty' "$MERMAID"
grep -F -q 'returns optional of new developer output' "$MERMAID"
grep -F -q 'INCLUDED / DATA_DEPENDENCY' "$ANALYSIS_AUDIT"
grep -F -q 'INCLUDED / CONTROL_DEPENDENCY' "$ANALYSIS_AUDIT"
grep -F -q 'derive has repository' "$ANALYSIS_AUDIT"
grep -F -q 'has repository does not equal has template' "$ANALYSIS_AUDIT"
grep -F -q 'REMOVED / STRUCTURAL_ENTRY' "$PROJECTION_AUDIT"
grep -F -q 'REMOVED / STRUCTURAL_OUTCOME' "$PROJECTION_AUDIT"
grep -F -q 'REMOVED / TECHNICAL_CALCULATION' "$PROJECTION_AUDIT"
grep -F -q 'KEPT / BUSINESS_RULE' "$PROJECTION_AUDIT"
grep -F -q 'REPLACED / TERMINAL_RESULT' "$PROJECTION_AUDIT"
grep -F -q 'derive has repository' "$PROJECTION_AUDIT"
grep -F -q 'has repository does not equal has template' "$PROJECTION_AUDIT"
grep -F -q 'redundant rule' "$NODE_STRUCTURE"
grep -F -q 'loop mechanics' "$NODE_STRUCTURE"
grep -F -q 'choose by node kind' "$NODE_STRUCTURE"
grep -F -q 'use technical predicate' "$NODE_STRUCTURE"
grep -F -q 'use technical calculation' "$NODE_STRUCTURE"
grep -F -q 'use business rule' "$NODE_STRUCTURE"
grep -F -q 'use business action' "$NODE_STRUCTURE"
grep -F -q 'BusinessGraphProjector.java' "$NODE_ANALYSIS_AUDIT"
grep -F -q 'REMOVED / TECHNICAL_CALCULATION' "$NODE_PROJECTION_AUDIT"
grep -F -q 'project entry source files is empty' "$SOURCE_STRUCTURE"
grep -F -q 'evaluate project closure' "$SOURCE_STRUCTURE"
grep -F -q 'derive modular as project module descriptor is present' "$SOURCE_STRUCTURE"
grep -F -q 'derive project resolution sources' "$SOURCE_STRUCTURE"
grep -F -q 'derive external resolution sources' "$SOURCE_STRUCTURE"
grep -F -q 'derive analysis sources' "$SOURCE_STRUCTURE"
grep -F -q 'derive connected classpath' "$SOURCE_STRUCTURE"
grep -F -q 'derive entry sources as project entry source files' "$SOURCE_STRUCTURE"
grep -F -q 'true; returns optional empty' "$SOURCE_STRUCTURE"
grep -F -q 'AnalysisSourceSelector.java' "$SOURCE_ANALYSIS_AUDIT"
grep -F -q 'INCLUDED / DATA_DEPENDENCY' "$SOURCE_ANALYSIS_AUDIT"
grep -F -q 'REMOVED / TECHNICAL_CALCULATION' "$SOURCE_PROJECTION_AUDIT"
grep -F -q '"schema":"fachtracing-activation/v3"' "$OUTPUT/activation.json"

RENDERER="$ROOT/fachtracing-engine/src/main/java/at/gepardec/fachtracing/developer/DecisionAuditMermaidRenderer.java"
if grep -E -i -q 'enable developer graph export|has repository|source url template|include exact node|select source inputs|project entry source|redundant rule' "$RENDERER"; then
  echo "The generic renderer contains self-trace content." >&2
  exit 1
fi

PROJECTOR="$ROOT/fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphProjector.java"
ANALYZER="$ROOT/fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java"
grep -F -q 'BusinessGraphProjection.Reason reason = classifyNode(' "$PROJECTOR"
grep -F -q 'AnalysisSourceSelector.select(boundary, project);' "$ANALYZER"

ANALYSIS_AUDIT_CHECKSUM=$(cksum < "$ANALYSIS_AUDIT")
PROJECTION_AUDIT_CHECKSUM=$(cksum < "$PROJECTION_AUDIT")
NODE_STRUCTURE_CHECKSUM=$(cksum < "$NODE_STRUCTURE")
NODE_ANALYSIS_AUDIT_CHECKSUM=$(cksum < "$NODE_ANALYSIS_AUDIT")
NODE_PROJECTION_AUDIT_CHECKSUM=$(cksum < "$NODE_PROJECTION_AUDIT")
SOURCE_STRUCTURE_CHECKSUM=$(cksum < "$SOURCE_STRUCTURE")
SOURCE_ANALYSIS_AUDIT_CHECKSUM=$(cksum < "$SOURCE_ANALYSIS_AUDIT")
SOURCE_PROJECTION_AUDIT_CHECKSUM=$(cksum < "$SOURCE_PROJECTION_AUDIT")
mvn -q -f "$ROOT/pom.xml" compile \
  at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-rc.1:analyze-reactor
test "$(cksum < "$ANALYSIS_AUDIT")" = "$ANALYSIS_AUDIT_CHECKSUM"
test "$(cksum < "$PROJECTION_AUDIT")" = "$PROJECTION_AUDIT_CHECKSUM"
test "$(cksum < "$NODE_STRUCTURE")" = "$NODE_STRUCTURE_CHECKSUM"
test "$(cksum < "$NODE_ANALYSIS_AUDIT")" = "$NODE_ANALYSIS_AUDIT_CHECKSUM"
test "$(cksum < "$NODE_PROJECTION_AUDIT")" = "$NODE_PROJECTION_AUDIT_CHECKSUM"
test "$(cksum < "$SOURCE_STRUCTURE")" = "$SOURCE_STRUCTURE_CHECKSUM"
test "$(cksum < "$SOURCE_ANALYSIS_AUDIT")" = "$SOURCE_ANALYSIS_AUDIT_CHECKSUM"
test "$(cksum < "$SOURCE_PROJECTION_AUDIT")" = "$SOURCE_PROJECTION_AUDIT_CHECKSUM"

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
