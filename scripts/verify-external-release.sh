#!/usr/bin/env sh
set -eu

VERSION="0.1.0-rc.1"
FIXTURE="fachtracing-maven-plugin/src/test/resources/it/external-release"
WORK=$(mktemp -d "${TMPDIR:-/tmp}/fachtracing-external.XXXXXX")
trap 'rm -rf "$WORK"' EXIT
REPOSITORY="$WORK/repository"
LOCAL="$WORK/local"
SOURCE_JAR="$WORK/external-rules-sources.jar"
BINARY_JAR="$WORK/external-rules.jar"
RULE_CLASSES="$WORK/external-rule-classes"

mvn -q deploy -DskipTests \
  -DaltDeploymentRepository="fachtracing-rc::default::file:$REPOSITORY"

mkdir -p "$RULE_CLASSES"
"$(/usr/libexec/java_home -v 21)/bin/javac" -d "$RULE_CLASSES" \
  "$FIXTURE/external-sources/example/rules/ExternalRules.java"
"$(/usr/libexec/java_home -v 21)/bin/jar" --create --file "$BINARY_JAR" -C "$RULE_CLASSES" .
"$(/usr/libexec/java_home -v 21)/bin/jar" --create --file "$SOURCE_JAR" \
  -C "$FIXTURE/external-sources" .
mvn -q deploy:deploy-file \
  -Dfile="$BINARY_JAR" -Dsources="$SOURCE_JAR" -DgroupId=example.rules -DartifactId=external-rules \
  -Dversion=1.0.0 -Dpackaging=jar -DgeneratePom=true \
  -DrepositoryId=fachtracing-rc -Durl="file:$REPOSITORY"

mvn -q -f "$FIXTURE/pom.xml" -Dmaven.repo.local="$LOCAL" \
  -Dfachtracing.repository="file:$REPOSITORY" clean compile \
  -Dfachtracing.sourceDependencies=example.rules:external-rules:1.0.0 \
  "at.gepardec.fachtracing:fachtracing-maven-plugin:$VERSION:analyze-reactor"

test -f "$FIXTURE/target/fachtracing/external-approval-structure.mmd"
test -f "$FIXTURE/target/fachtracing/external-approval-structure.puml"
test -f "$FIXTURE/target/fachtracing/index.md"
grep -q '"schema":"fachtracing-activation/v3"' "$FIXTURE/target/fachtracing/activation.json"
grep -q '"classFingerprints"' "$FIXTURE/target/fachtracing/activation.json"
grep -q 'example/ExternalController' "$FIXTURE/target/fachtracing/activation.json"
grep -q '"manifest"' "$FIXTURE/target/fachtracing/activation.json"
grep -q '"javaAgentOption":"-javaagent:' "$FIXTURE/target/fachtracing/activation.json"
test -f "$FIXTURE/target/fachtracing/external-rule-approval-structure.mmd"
grep -q 'amount is below 500' "$FIXTURE/target/fachtracing/external-rule-approval-structure.mmd"
grep -q 'external rule approval.*COMPLETE' "$FIXTURE/target/fachtracing/index.md"

rm -rf "$FIXTURE/target/fachtracing"
mvn -q -o -f "$FIXTURE/pom.xml" -Dmaven.repo.local="$LOCAL" \
  -Dfachtracing.repository="file:$REPOSITORY" \
  -Dfachtracing.sourceDependencies=example.rules:external-rules:1.0.0 \
  "at.gepardec.fachtracing:fachtracing-maven-plugin:$VERSION:analyze-reactor"
test -f "$FIXTURE/target/fachtracing/external-rule-approval-structure.mmd"
grep -q 'external rule approval.*COMPLETE' "$FIXTURE/target/fachtracing/index.md"

mvn -q -f "$FIXTURE/pom.xml" -Dmaven.repo.local="$LOCAL" \
  -Dfachtracing.repository="file:$REPOSITORY" dependency:build-classpath \
  -Dmdep.outputFile="$WORK/classpath"
CP="$FIXTURE/target/classes:$(cat "$WORK/classpath")"
AGENT="$LOCAL/at/gepardec/fachtracing/fachtracing-agent/$VERSION/fachtracing-agent-$VERSION.jar"
"$(/usr/libexec/java_home -v 21)/bin/java" -javaagent:"$AGENT" -cp "$CP" example.ExternalRuntime \
  "$PWD/$FIXTURE/target/fachtracing/activation.json" \
  | grep -q EXTERNAL_RUNTIME_TRACE_OK

echo EXTERNAL_RELEASE_OK
