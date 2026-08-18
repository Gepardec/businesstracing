#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
MAVEN_REPOSITORY=$("$ROOT/scripts/maven-repository-path.sh")
PIN=eba869ee597b933efc8fa2c84713db9e6c0983cf
SOURCE=${KEYCLOAK_DIR:-/tmp/fachtracing-keycloak}
WORKTREE=$(mktemp -d /tmp/fachtracing-keycloak-conformance.XXXXXX)
JAVA_HOME_21=$(/usr/libexec/java_home -v 21)
export JAVA_HOME="$JAVA_HOME_21"
cleanup() {
  git -C "$SOURCE" worktree remove --force "$WORKTREE" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

test -d "$SOURCE/.git"
test "$(git -C "$SOURCE" rev-parse HEAD)" = "$PIN"
git -C "$SOURCE" diff --quiet
git -C "$SOURCE" diff --cached --quiet
git -C "$SOURCE" worktree add --detach "$WORKTREE" "$PIN" >/dev/null

if [ "${FACHTRACING_SKIP_PROJECT_BUILD:-false}" != "true" ]; then
  mvn -q -f "$ROOT/pom.xml" package
fi
"$WORKTREE/mvnw" -q -f "$WORKTREE/pom.xml" -pl services -am -DskipTests install
"$WORKTREE/mvnw" -q -f "$WORKTREE/services/pom.xml" dependency:build-classpath \
  -Dmdep.outputFile="$WORKTREE/services/target/conformance-classpath.txt" -DincludeScope=test

TEST_CLASSES="$ROOT/conformance/keycloak/target/test-classes"
mkdir -p "$TEST_CLASSES"
"$JAVA_HOME_21/bin/javac" --release 21 \
  -cp "$ROOT/fachtracing-api/target/classes:$ROOT/fachtracing-engine/target/classes" \
  -d "$TEST_CLASSES" \
  "$ROOT/conformance/keycloak/src/test/java/at/gepardec/fachtracing/conformance/KeycloakConformanceTest.java"

CP="$ROOT/fachtracing-api/target/classes:$ROOT/fachtracing-engine/target/classes:$TEST_CLASSES:$MAVEN_REPOSITORY/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar:$MAVEN_REPOSITORY/org/ow2/asm/asm-tree/9.10.1/asm-tree-9.10.1.jar"
USERS_SOURCE="$WORKTREE/services/src/main/java/org/keycloak/services/resources/admin/UsersResource.java"
"$JAVA_HOME_21/bin/java" -ea --add-modules jdk.compiler -Xmx3g -cp "$CP" \
  at.gepardec.fachtracing.conformance.KeycloakConformanceTest \
  "$USERS_SOURCE" \
  "$WORKTREE/services/target/classes" \
  "$WORKTREE/services/target/conformance-classpath.txt" \
  "$ROOT/conformance/keycloak/target/generated"
