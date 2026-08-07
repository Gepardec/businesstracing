#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
MAVEN_REPOSITORY=$("$ROOT/scripts/maven-repository-path.sh")
PIN=88e37c15cf6fc8490b01bc3e8e2c800cec1ac272
SOURCE=${SPRING_PETCLINIC_DIR:-/tmp/fachtracing-spring-petclinic}
WORKTREE=$(mktemp -d /tmp/fachtracing-petclinic-conformance.XXXXXX)
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
mvn -q -f "$WORKTREE/pom.xml" -DskipTests test-compile
mvn -q -f "$WORKTREE/pom.xml" dependency:build-classpath \
  -Dmdep.outputFile="$WORKTREE/target/conformance-classpath.txt" -DincludeScope=test
git -C "$WORKTREE" apply --unidiff-zero "$ROOT/conformance/spring-petclinic/annotation-overlay.patch"

TEST_CLASSES="$ROOT/conformance/spring-petclinic/target/test-classes"
mkdir -p "$TEST_CLASSES"
JAVA_HOME_21=$(/usr/libexec/java_home -v 21)
"$JAVA_HOME_21/bin/javac" --release 21 \
  -cp "$ROOT/fachtracing-api/target/classes:$ROOT/fachtracing-engine/target/classes" \
  -d "$TEST_CLASSES" \
  "$ROOT/conformance/spring-petclinic/src/test/java/at/gepardec/fachtracing/conformance/SpringPetClinicConformanceTest.java" \
  "$ROOT/conformance/spring-petclinic/src/test/java/at/gepardec/fachtracing/conformance/SpringPetClinicIsolationTest.java"

CP="$ROOT/fachtracing-api/target/classes:$ROOT/fachtracing-engine/target/classes:$TEST_CLASSES:$MAVEN_REPOSITORY/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar:$MAVEN_REPOSITORY/org/ow2/asm/asm-tree/9.10.1/asm-tree-9.10.1.jar"
"$JAVA_HOME_21/bin/java" -ea --add-modules jdk.compiler -cp "$CP" \
  at.gepardec.fachtracing.conformance.SpringPetClinicIsolationTest "$ROOT"
"$JAVA_HOME_21/bin/java" -ea --add-modules jdk.compiler -cp "$CP" \
  at.gepardec.fachtracing.conformance.SpringPetClinicConformanceTest \
  "$ROOT" "$WORKTREE" "$WORKTREE/target/conformance-classpath.txt" \
  "$ROOT/conformance/spring-petclinic/target/generated" \
  "$ROOT/conformance/spring-petclinic/src/test/resources/oracles"
