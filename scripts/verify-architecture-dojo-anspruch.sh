#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
MAVEN_REPOSITORY=$("$ROOT/scripts/maven-repository-path.sh")
PIN=5767ba85bffd82520d7ee7f72c281a9395d1b7ee
SOURCE=${ARCHITECTURE_DOJO_ANSPRUCH_DIR:-/tmp/fachtracing-architecture-dojo-anspruch}
WORKTREE=$(mktemp -d /tmp/fachtracing-architecture-dojo-anspruch-conformance.XXXXXX)
JAVA_HOME_21=$(/usr/libexec/java_home -v 21)
export JAVA_HOME="$JAVA_HOME_21"

cleanup() {
  git -C "$SOURCE" worktree remove --force "$WORKTREE" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

test -d "$SOURCE/.git"
git -C "$SOURCE" cat-file -e "$PIN^{commit}"
git -C "$SOURCE" diff --quiet
git -C "$SOURCE" diff --cached --quiet
git -C "$SOURCE" worktree add --detach "$WORKTREE" "$PIN" >/dev/null

if [ "${FACHTRACING_SKIP_PROJECT_BUILD:-false}" != "true" ]; then
  mvn -q -f "$ROOT/pom.xml" package
fi
mvn -q -f "$WORKTREE/pom.xml" -DskipTests install
mvn -q -f "$WORKTREE/dojo-leistung/pom.xml" dependency:build-classpath \
  -Dmdep.outputFile="$WORKTREE/dojo-leistung/target/conformance-classpath.txt" \
  -DincludeScope=test

TEST_CLASSES="$ROOT/conformance/architecture-dojo-anspruch/target/test-classes"
mkdir -p "$TEST_CLASSES"
"$JAVA_HOME_21/bin/javac" --release 21 \
  -cp "$ROOT/fachtracing-api/target/classes:$ROOT/fachtracing-engine/target/classes" \
  -d "$TEST_CLASSES" \
  "$ROOT/conformance/spring-petclinic/src/test/java/at/gepardec/fachtracing/conformance/BusinessJsonSchemaConformance.java" \
  "$ROOT/conformance/architecture-dojo-anspruch/src/test/java/at/gepardec/fachtracing/conformance/ArchitectureDojoAnspruchConformanceTest.java"

CP="$ROOT/fachtracing-api/target/classes:$ROOT/fachtracing-engine/target/classes:$TEST_CLASSES:$MAVEN_REPOSITORY/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar:$MAVEN_REPOSITORY/org/ow2/asm/asm-tree/9.10.1/asm-tree-9.10.1.jar"
"$JAVA_HOME_21/bin/java" -ea --add-modules jdk.compiler -cp "$CP" \
  at.gepardec.fachtracing.conformance.ArchitectureDojoAnspruchConformanceTest \
  "$WORKTREE" "$WORKTREE/dojo-leistung/target/conformance-classpath.txt" \
  "$ROOT/conformance/architecture-dojo-anspruch/target/generated"
