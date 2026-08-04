#!/usr/bin/env sh
set -eu

./scripts/verify-repository-integrity.sh
./scripts/verify-java-capabilities.sh
mvn -q install
CP="fachtracing-api/target/classes:fachtracing-engine/target/classes:fachtracing-engine/target/test-classes"
JAVA21="$(/usr/libexec/java_home -v 21)/bin/java"
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.model.ApiModelTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.analysis.StaticDecisionAnalyzerTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.runtime.RuntimeCollectorTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.store.DecisionRecordProtocolTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.diagram.ExecutionPathResolverTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.explain.DecisionExplanationProjectorTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.plantuml.PlantUmlRendererTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.mermaid.MermaidRendererTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.FachtracingEngineIT
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.performance.FachtracingLoadTest \
  --baseline-seconds=5 --enabled-seconds=5 --rate=1000 --work-micros=10000
AGENT_CP="$CP:fachtracing-agent/target/classes:fachtracing-agent/target/test-classes:$HOME/.m2/repository/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar"
"$JAVA21" -ea --add-modules jdk.compiler -cp "$AGENT_CP" at.gepardec.fachtracing.agent.FachtracingTransformerTest
MAVEN_CP="$HOME/.m2/repository/org/apache/maven/maven-core/3.9.16/maven-core-3.9.16.jar:$HOME/.m2/repository/org/apache/maven/maven-model/3.9.16/maven-model-3.9.16.jar:$HOME/.m2/repository/org/apache/maven/maven-artifact/3.9.16/maven-artifact-3.9.16.jar:$HOME/.m2/repository/org/codehaus/plexus/plexus-utils/3.5.1/plexus-utils-3.5.1.jar:$HOME/.m2/repository/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar"
PLUGIN_CP="$CP:fachtracing-maven-plugin/target/classes:fachtracing-maven-plugin/target/test-classes:$MAVEN_CP"
"$JAVA21" -ea --add-modules jdk.compiler -cp "$PLUGIN_CP" at.gepardec.fachtracing.maven.AnalyzeMojoTest
JDBC_CP="$CP:fachtracing-storage-jdbc/target/classes:fachtracing-storage-jdbc/target/test-classes:$HOME/.m2/repository/com/h2database/h2/2.4.240/h2-2.4.240.jar"
"$JAVA21" -ea --add-modules jdk.compiler -cp "$JDBC_CP" at.gepardec.fachtracing.storage.jdbc.JdbcDecisionRecordRepositoryTest
FIXTURE="fachtracing-maven-plugin/src/test/resources/it/basic"
mvn -q -f "$FIXTURE/pom-command.xml" clean compile \
  at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-rc.1:analyze
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
mvn -q -f "$REACTOR_FIXTURE/pom.xml" clean compile \
  -Dfachtracing.additionalSourceRoots="$PWD/$REACTOR_FIXTURE/external-rules" \
  -Dfachtracing.additionalEntrySourceRoots="$PWD/$REACTOR_FIXTURE/external-entries" \
  at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-rc.1:analyze-reactor
test -f "$REACTOR_FIXTURE/target/fachtracing/reactor-approval-structure.mmd"
test -f "$REACTOR_FIXTURE/target/fachtracing/reactor-approval-structure.puml"
test -f "$REACTOR_FIXTURE/target/fachtracing/index.md"
test -f "$REACTOR_FIXTURE/target/fachtracing/activation.json"
rg -q '"schema":"fachtracing-activation/v1"' "$REACTOR_FIXTURE/target/fachtracing/activation.json"
rg -q '"graphCount":2' "$REACTOR_FIXTURE/target/fachtracing/activation.json"
rg -q 'candidate 3' "$REACTOR_FIXTURE/target/fachtracing/reactor-approval-structure.mmd"
test -f "$REACTOR_FIXTURE/target/fachtracing/imported-approval-structure.mmd"
rg -q 'imported approval' "$REACTOR_FIXTURE/target/fachtracing/index.md"
./scripts/verify-external-release.sh
