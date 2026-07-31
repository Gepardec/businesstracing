#!/usr/bin/env sh
set -eu

mvn -q install
CP="fachtracing-api/target/classes:fachtracing-engine/target/classes:fachtracing-engine/target/test-classes"
JAVA21="$(/usr/libexec/java_home -v 21)/bin/java"
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.model.ApiModelTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.analysis.StaticDecisionAnalyzerTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.runtime.RuntimeCollectorTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.diagram.ExecutionPathResolverTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.explain.DecisionExplanationProjectorTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.plantuml.PlantUmlRendererTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.mermaid.MermaidRendererTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.FachtracingEngineIT
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.performance.FachtracingLoadTest \
  --baseline-seconds=5 --enabled-seconds=5 --rate=1000 --work-micros=10000
AGENT_CP="$CP:fachtracing-agent/target/classes:fachtracing-agent/target/test-classes:$HOME/.m2/repository/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar"
"$JAVA21" -ea --add-modules jdk.compiler -cp "$AGENT_CP" at.gepardec.fachtracing.agent.FachtracingTransformerTest
PLUGIN_CP="$CP:fachtracing-maven-plugin/target/classes:fachtracing-maven-plugin/target/test-classes"
"$JAVA21" -ea --add-modules jdk.compiler -cp "$PLUGIN_CP" at.gepardec.fachtracing.maven.AnalyzeMojoTest
FIXTURE="fachtracing-maven-plugin/src/test/resources/it/basic"
mvn -q -f "$FIXTURE/pom-command.xml" clean compile \
  at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-SNAPSHOT:analyze
test -f "$FIXTURE/target/fachtracing/approve-application-structure.mmd"
test -f "$FIXTURE/target/fachtracing/approve-application-structure.puml"
test -f "$FIXTURE/target/fachtracing/index.md"
mvn -q -f "$FIXTURE/pom.xml" clean process-classes
test -f "$FIXTURE/target/fachtracing/approve-application-structure.mmd"
test -f "$FIXTURE/target/fachtracing/approve-application-structure.puml"
test -f "$FIXTURE/target/fachtracing/index.md"
rg -q 'Start' "$FIXTURE/target/fachtracing/approve-application-structure.mmd"
rg -q 'Stop' "$FIXTURE/target/fachtracing/approve-application-structure.mmd"
rg -q 'returns approved' "$FIXTURE/target/fachtracing/approve-application-structure.mmd"
REACTOR_FIXTURE="fachtracing-maven-plugin/src/test/resources/it/reactor"
mvn -q -f "$REACTOR_FIXTURE/pom.xml" clean process-classes
test -f "$REACTOR_FIXTURE/decision-entry/target/fachtracing/reactor-approval-structure.mmd"
test -f "$REACTOR_FIXTURE/decision-entry/target/fachtracing/index.md"
test ! -e "$REACTOR_FIXTURE/target/fachtracing/index.md"
test ! -e "$REACTOR_FIXTURE/decision-implementations/target/fachtracing/index.md"
rg -q 'candidate 1' "$REACTOR_FIXTURE/decision-entry/target/fachtracing/reactor-approval-structure.mmd"
rg -q 'candidate 2' "$REACTOR_FIXTURE/decision-entry/target/fachtracing/reactor-approval-structure.mmd"
rg -q 'reactor approval' "$REACTOR_FIXTURE/decision-entry/target/fachtracing/index.md"
