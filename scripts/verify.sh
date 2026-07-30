#!/usr/bin/env sh
set -eu

mvn -q package
CP="fachtracing-api/target/classes:fachtracing-engine/target/classes:fachtracing-engine/target/test-classes"
JAVA21="$(/usr/libexec/java_home -v 21)/bin/java"
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.model.ApiModelTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.analysis.StaticDecisionAnalyzerTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.runtime.RuntimeCollectorTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.explain.DecisionExplanationProjectorTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.plantuml.PlantUmlRendererTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.mermaid.MermaidRendererTest
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.FachtracingEngineIT
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" at.gepardec.fachtracing.performance.FachtracingLoadTest \
  --baseline-seconds=5 --enabled-seconds=5 --rate=1000 --work-micros=10000
AGENT_CP="$CP:fachtracing-agent/target/classes:fachtracing-agent/target/test-classes:$HOME/.m2/repository/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar"
"$JAVA21" -ea --add-modules jdk.compiler -cp "$AGENT_CP" at.gepardec.fachtracing.agent.FachtracingTransformerTest
