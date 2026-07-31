#!/usr/bin/env sh
set -eu

VERSION="0.1.0-rc.1"
FIXTURE="fachtracing-maven-plugin/src/test/resources/it/external-release"
WORK=$(mktemp -d "${TMPDIR:-/tmp}/fachtracing-external.XXXXXX")
trap 'rm -rf "$WORK"' EXIT
REPOSITORY="$WORK/repository"
LOCAL="$WORK/local"

mvn -q deploy -DskipTests \
  -DaltDeploymentRepository="fachtracing-rc::default::file:$REPOSITORY"

mvn -q -f "$FIXTURE/pom.xml" -Dmaven.repo.local="$LOCAL" \
  -Dfachtracing.repository="file:$REPOSITORY" clean compile \
  "at.gepardec.fachtracing:fachtracing-maven-plugin:$VERSION:analyze"

test -f "$FIXTURE/target/fachtracing/external-approval-structure.mmd"
test -f "$FIXTURE/target/fachtracing/external-approval-structure.puml"
test -f "$FIXTURE/target/fachtracing/index.md"

mvn -q -f "$FIXTURE/pom.xml" -Dmaven.repo.local="$LOCAL" \
  -Dfachtracing.repository="file:$REPOSITORY" dependency:build-classpath \
  -Dmdep.outputFile="$WORK/classpath"
CP="$FIXTURE/target/classes:$(cat "$WORK/classpath")"
AGENT="$LOCAL/at/gepardec/fachtracing/fachtracing-agent/$VERSION/fachtracing-agent-$VERSION.jar"
"$(/usr/libexec/java_home -v 21)/bin/java" -javaagent:"$AGENT" -cp "$CP" example.ExternalRuntime \
  | grep -q EXTERNAL_RUNTIME_OK

echo EXTERNAL_RELEASE_OK
