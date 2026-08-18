#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
MAVEN_REPOSITORY=$("$ROOT/scripts/maven-repository-path.sh")
PIN=85da1d6861fea14579b1c6eb76253f0549a8e80f
SOURCE=${JAKARTAEE_REST_SAMPLE_DIR:-/tmp/fachtracing-jakartaee-rest-sample}
WORKTREE=$(mktemp -d /tmp/fachtracing-jakartaee-rest-conformance.XXXXXX)
cleanup() { git -C "$SOURCE" worktree remove --force "$WORKTREE" >/dev/null 2>&1 || true; }
trap cleanup EXIT INT TERM

test -d "$SOURCE/.git"
test "$(git -C "$SOURCE" rev-parse HEAD)" = "$PIN"
git -C "$SOURCE" diff --quiet
git -C "$SOURCE" diff --cached --quiet
git -C "$SOURCE" worktree add --detach "$WORKTREE" "$PIN" >/dev/null
if [ "${FACHTRACING_SKIP_PROJECT_BUILD:-false}" != "true" ]; then mvn -q -f "$ROOT/pom.xml" package; fi
mvn -q -f "$WORKTREE/pom.xml" -DskipTests test-compile
mvn -q -f "$WORKTREE/pom.xml" dependency:build-classpath -DincludeScope=test \
  -Dmdep.outputFile="$WORKTREE/target/conformance-classpath.txt"
TEST_CLASSES="$ROOT/conformance/jakartaee-rest/target/test-classes"
mkdir -p "$TEST_CLASSES"
JAVA_HOME_21=$(/usr/libexec/java_home -v 21)
"$JAVA_HOME_21/bin/javac" --release 21 \
  -cp "$ROOT/fachtracing-api/target/classes:$ROOT/fachtracing-engine/target/classes:$ROOT/fachtracing-jakartaee/target/classes" \
  -d "$TEST_CLASSES" \
  "$ROOT/conformance/jakartaee-rest/src/test/java/at/gepardec/fachtracing/conformance/JakartaEeRestConformanceTest.java"
CP="$ROOT/fachtracing-api/target/classes:$ROOT/fachtracing-engine/target/classes:$ROOT/fachtracing-jakartaee/target/classes:$TEST_CLASSES:$MAVEN_REPOSITORY/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar:$MAVEN_REPOSITORY/org/ow2/asm/asm-tree/9.10.1/asm-tree-9.10.1.jar:$(cat "$WORKTREE/target/conformance-classpath.txt")"
"$JAVA_HOME_21/bin/java" -ea --add-modules jdk.compiler -cp "$CP" \
  at.gepardec.fachtracing.conformance.JakartaEeRestConformanceTest \
  "$ROOT" "$WORKTREE" "$WORKTREE/target/conformance-classpath.txt"
