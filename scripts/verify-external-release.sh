#!/usr/bin/env sh
set -eu

VERSION="0.1.0-rc.1"
FIXTURE="fachtracing-maven-plugin/src/test/resources/it/external-release"
WORK=$(mktemp -d "${TMPDIR:-/tmp}/fachtracing-external.XXXXXX")
trap 'rm -rf "$WORK"' EXIT
REPOSITORY="$WORK/repository"
LOCAL="$WORK/local"
SOURCE_JAR="$WORK/external-rules-sources.jar"

mvn -q deploy -DskipTests \
  -DaltDeploymentRepository="fachtracing-rc::default::file:$REPOSITORY"

"$(/usr/libexec/java_home -v 21)/bin/jar" --create --file "$SOURCE_JAR" \
  -C "$FIXTURE/external-sources" .
mvn -q deploy:deploy-file \
  -Dfile="$SOURCE_JAR" -DgroupId=example.rules -DartifactId=external-rules \
  -Dversion=1.0.0 -Dclassifier=sources -Dpackaging=jar -DgeneratePom=true \
  -DrepositoryId=fachtracing-rc -Durl="file:$REPOSITORY"

mvn -q -f "$FIXTURE/pom.xml" -Dmaven.repo.local="$LOCAL" \
  -Dfachtracing.repository="file:$REPOSITORY" clean compile \
  -Dfachtracing.sourceDependencies=example.rules:external-rules:1.0.0 \
  "at.gepardec.fachtracing:fachtracing-maven-plugin:$VERSION:analyze-reactor"

test -f "$FIXTURE/target/fachtracing/external-approval-structure.mmd"
test -f "$FIXTURE/target/fachtracing/external-approval-structure.puml"
test -f "$FIXTURE/target/fachtracing/index.md"
test -f "$FIXTURE/target/fachtracing/external-rule-approval-structure.mmd"
rg -q 'candidate 1' "$FIXTURE/target/fachtracing/external-rule-approval-structure.mmd"
rg -q 'external rule approval.*COMPLETE' "$FIXTURE/target/fachtracing/index.md"

rm -rf "$FIXTURE/target/fachtracing"
mvn -q -o -f "$FIXTURE/pom.xml" -Dmaven.repo.local="$LOCAL" \
  -Dfachtracing.repository="file:$REPOSITORY" \
  -Dfachtracing.sourceDependencies=example.rules:external-rules:1.0.0 \
  "at.gepardec.fachtracing:fachtracing-maven-plugin:$VERSION:analyze-reactor"
test -f "$FIXTURE/target/fachtracing/external-rule-approval-structure.mmd"
rg -q 'external rule approval.*COMPLETE' "$FIXTURE/target/fachtracing/index.md"

mvn -q -f "$FIXTURE/pom.xml" -Dmaven.repo.local="$LOCAL" \
  -Dfachtracing.repository="file:$REPOSITORY" dependency:build-classpath \
  -Dmdep.outputFile="$WORK/classpath"
CP="$FIXTURE/target/classes:$(cat "$WORK/classpath")"
AGENT="$LOCAL/at/gepardec/fachtracing/fachtracing-agent/$VERSION/fachtracing-agent-$VERSION.jar"
"$(/usr/libexec/java_home -v 21)/bin/java" -javaagent:"$AGENT" -cp "$CP" example.ExternalRuntime \
  "$PWD/$FIXTURE/src/main/java/example/ExternalDecision.java" \
  "$PWD/$FIXTURE/target/classes/example/ExternalDecision.class" \
  | grep -q EXTERNAL_RUNTIME_TRACE_OK

echo EXTERNAL_RELEASE_OK
