#!/usr/bin/env sh
set -eu

./scripts/verify-repository-integrity.sh
./scripts/test-capture-gate-output.sh
./scripts/test-maven-repository-path.sh
./scripts/test-release-workflow-budget.sh
./scripts/test-fast-pr-workflow.sh
./scripts/verify-java-capabilities.sh
mvn -q install
FACHTRACING_SKIP_PROJECT_BUILD=true ./scripts/verify-self-tracing.sh
MAVEN_REPOSITORY=$(./scripts/maven-repository-path.sh)
mkdir -p target/verification-classpaths
mvn -q -pl fachtracing-engine dependency:build-classpath -DincludeScope=test \
  -Dmdep.outputFile="$PWD/target/verification-classpaths/engine.txt"
mvn -q -pl fachtracing-maven-plugin dependency:build-classpath -DincludeScope=test \
  -Dmdep.outputFile="$PWD/target/verification-classpaths/maven-plugin.txt"
mvn -q -pl fachtracing-storage-jdbc dependency:build-classpath -DincludeScope=test \
  -Dmdep.outputFile="$PWD/target/verification-classpaths/storage-jdbc.txt"
for dependency_classpath in target/verification-classpaths/*.txt
do
  grep -F -q "$MAVEN_REPOSITORY/" "$dependency_classpath"
done
ENGINE_DEPENDENCIES=$(cat target/verification-classpaths/engine.txt)
CP="fachtracing-api/target/classes:fachtracing-engine/target/classes:fachtracing-engine/target/test-classes:$ENGINE_DEPENDENCIES"
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
AGENT_CP="$CP:fachtracing-agent/target/classes:fachtracing-agent/target/test-classes"
"$JAVA21" -ea --add-modules jdk.compiler -cp "$AGENT_CP" at.gepardec.fachtracing.agent.FachtracingTransformerTest
MAVEN_DEPENDENCIES=$(cat target/verification-classpaths/maven-plugin.txt)
PLUGIN_CP="$CP:fachtracing-maven-plugin/target/classes:fachtracing-maven-plugin/target/test-classes:$MAVEN_DEPENDENCIES"
"$JAVA21" -ea --add-modules jdk.compiler -cp "$PLUGIN_CP" at.gepardec.fachtracing.maven.AnalyzeMojoTest
JDBC_DEPENDENCIES=$(cat target/verification-classpaths/storage-jdbc.txt)
JDBC_CP="$CP:fachtracing-storage-jdbc/target/classes:fachtracing-storage-jdbc/target/test-classes:$JDBC_DEPENDENCIES"
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
grep -q 'Start' "$FIXTURE/target/fachtracing/approve-application-structure.mmd"
grep -q 'Stop' "$FIXTURE/target/fachtracing/approve-application-structure.mmd"
grep -q 'returns approved' "$FIXTURE/target/fachtracing/approve-application-structure.mmd"
REACTOR_FIXTURE="fachtracing-maven-plugin/src/test/resources/it/reactor"
mvn -q -f "$REACTOR_FIXTURE/pom.xml" clean process-classes
test -f "$REACTOR_FIXTURE/decision-entry/target/fachtracing/reactor-approval-structure.mmd"
test -f "$REACTOR_FIXTURE/decision-entry/target/fachtracing/index.md"
test ! -e "$REACTOR_FIXTURE/target/fachtracing/index.md"
test ! -e "$REACTOR_FIXTURE/decision-implementations/target/fachtracing/index.md"
grep -q 'local decision rule' "$REACTOR_FIXTURE/decision-entry/target/fachtracing/reactor-approval-structure.mmd"
grep -q 'regional decision rule' "$REACTOR_FIXTURE/decision-entry/target/fachtracing/reactor-approval-structure.mmd"
grep -q 'reactor approval' "$REACTOR_FIXTURE/decision-entry/target/fachtracing/index.md"
mvn -q -f "$REACTOR_FIXTURE/pom.xml" clean compile \
  at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-rc.1:analyze-reactor
test -f "$REACTOR_FIXTURE/target/fachtracing/reactor-approval-structure.mmd"
test -f "$REACTOR_FIXTURE/target/fachtracing/reactor-approval-structure.puml"
test -f "$REACTOR_FIXTURE/target/fachtracing/index.md"
test -f "$REACTOR_FIXTURE/target/fachtracing/activation.json"
grep -q '"schema":"fachtracing-activation/v3"' "$REACTOR_FIXTURE/target/fachtracing/activation.json"
grep -q '"classFingerprints"' "$REACTOR_FIXTURE/target/fachtracing/activation.json"
grep -q '"decisions"' "$REACTOR_FIXTURE/target/fachtracing/activation.json"
grep -q 'regional decision rule' "$REACTOR_FIXTURE/target/fachtracing/reactor-approval-structure.mmd"
./scripts/verify-external-release.sh
if [ -n "${FACHTRACING_POSTGRES_URL:-}" ]; then
  ./scripts/verify-postgres.sh
else
  echo POSTGRES_JDBC_SKIPPED_NO_CONNECTION
fi
