#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
MAVEN_REPOSITORY=$("$ROOT/scripts/maven-repository-path.sh")
PIN=782cdec8dfe5b4062eb5c1859e6a9e53afe02770
SOURCE=${MEGA_BACKEND_DIR:-/tmp/fachtracing-mega-backend}
WORKTREE=$(mktemp -d /tmp/fachtracing-mega-conformance.XXXXXX)
cleanup() {
  git -C "$SOURCE" worktree remove --force "$WORKTREE" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

test -d "$SOURCE/.git"
test "$(git -C "$SOURCE" rev-parse HEAD)" = "$PIN"
git -C "$SOURCE" diff --quiet
git -C "$SOURCE" diff --cached --quiet
git -C "$SOURCE" worktree add --detach "$WORKTREE" "$PIN" >/dev/null

mvn -q -f "$ROOT/pom.xml" package
mvn -q -f "$WORKTREE/pom.xml" -DskipTests test-compile
mvn -q -f "$WORKTREE/pom.xml" dependency:build-classpath \
  -Dmdep.outputFile="$WORKTREE/target/conformance-classpath.txt" -DincludeScope=test
git -C "$WORKTREE" apply "$ROOT/conformance/mega-backend/annotation-overlay.patch"

OVERLAY_CLASSES="$WORKTREE/target/conformance-overlay-classes"
mkdir -p "$OVERLAY_CLASSES"
MEGA_CP=$(cat "$WORKTREE/target/conformance-classpath.txt")
"$(/usr/libexec/java_home -v 21)/bin/javac" --release 21 \
  -cp "$ROOT/fachtracing-api/target/classes:$WORKTREE/target/classes:$MEGA_CP" \
  -d "$OVERLAY_CLASSES" \
  "$WORKTREE/src/main/java/com/gepardec/mega/domain/calculation/time/TimeOverlapCalculator.java" \
  "$WORKTREE/src/main/java/com/gepardec/mega/service/helper/WarningCalculatorsManager.java"

TEST_CLASSES="$ROOT/conformance/mega-backend/target/test-classes"
mkdir -p "$TEST_CLASSES"
JAVA_HOME_21=$(/usr/libexec/java_home -v 21)
"$JAVA_HOME_21/bin/javac" --release 21 \
  -cp "$ROOT/fachtracing-api/target/classes:$ROOT/fachtracing-engine/target/classes:$ROOT/fachtracing-agent/target/classes:$MAVEN_REPOSITORY/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar" \
  -d "$TEST_CLASSES" \
  "$ROOT/conformance/mega-backend/src/test/java/at/gepardec/fachtracing/conformance/MegaBackendConformanceTest.java" \
  "$ROOT/conformance/mega-backend/src/test/java/at/gepardec/fachtracing/conformance/ForbiddenReferenceTest.java"

CP="$ROOT/fachtracing-api/target/classes:$ROOT/fachtracing-engine/target/classes:$ROOT/fachtracing-agent/target/classes:$TEST_CLASSES:$MAVEN_REPOSITORY/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar:$MAVEN_REPOSITORY/org/ow2/asm/asm-tree/9.10.1/asm-tree-9.10.1.jar"
"$JAVA_HOME_21/bin/java" -ea --add-modules jdk.compiler -cp "$CP" \
  at.gepardec.fachtracing.conformance.ForbiddenReferenceTest "$ROOT"
"$JAVA_HOME_21/bin/java" -ea --add-modules jdk.compiler -Xmx3g -cp "$CP" \
  at.gepardec.fachtracing.conformance.MegaBackendConformanceTest \
  "$ROOT" "$WORKTREE" "$WORKTREE/target/conformance-classpath.txt" \
  "$ROOT/conformance/mega-backend/generated" \
  "$ROOT/conformance/mega-backend/src/test/resources/oracles" "$OVERLAY_CLASSES"
