#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

if [ "${FACHTRACING_SKIP_PROJECT_BUILD:-false}" != "true" ]; then
  mvn -q install
fi

mkdir -p "$ROOT/target/verification-classpaths"
CLASSPATH_FILE="$ROOT/target/verification-classpaths/jakartaee.txt"
mvn -q -pl fachtracing-jakartaee dependency:build-classpath -DincludeScope=test \
  -Dmdep.outputFile="$CLASSPATH_FILE"

JAVA21="$(/usr/libexec/java_home -v 21)/bin/java"
DEPENDENCIES=$(cat "$CLASSPATH_FILE")
CLASSES="$ROOT/fachtracing-jakartaee/target/test-classes"
CP="$ROOT/fachtracing-api/target/classes:$ROOT/fachtracing-engine/target/classes:$ROOT/fachtracing-jakartaee/target/classes:$CLASSES:$DEPENDENCIES"
SOURCE="$ROOT/fachtracing-jakartaee/src/test/java/at/gepardec/fachtracing/jakartaee/conformance/DynamicCdiRuntimeConformance.java"
ACTIVATION="$ROOT/target/dynamic-cdi-conformance/activation.json"
OUTPUT="$ROOT/target/dynamic-cdi-conformance/traces"
AGENT="$ROOT/fachtracing-agent/target/fachtracing-agent-0.1.0-rc.1.jar"

mkdir -p "$OUTPUT"
find "$OUTPUT" -type f -delete
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" \
  at.gepardec.fachtracing.jakartaee.conformance.DynamicCdiRuntimeConformance \
  prepare "$SOURCE" "$CLASSES" "$ACTIVATION"
"$JAVA21" -ea -javaagent:"$AGENT"=activation="$ACTIVATION",output="$OUTPUT" -cp "$CP" \
  at.gepardec.fachtracing.jakartaee.conformance.DynamicCdiRuntimeConformance run

test "$(find "$OUTPUT" -name '*.txt' -type f | wc -l | tr -d ' ')" = "2"
test "$(find "$OUTPUT" -name '*.mmd' -type f | wc -l | tr -d ' ')" = "2"
EU_TRACE=$(grep -F -l 'european rule' "$OUTPUT"/*.txt)
US_TRACE=$(grep -F -l 'american rule' "$OUTPUT"/*.txt)
test "$(printf '%s\n' "$EU_TRACE" | wc -l | tr -d ' ')" = "1"
test "$(printf '%s\n' "$US_TRACE" | wc -l | tr -d ' ')" = "1"
test "$EU_TRACE" != "$US_TRACE"
! grep -F -q 'american rule' "$EU_TRACE"
! grep -F -q 'european rule' "$US_TRACE"
grep -F -q 'Coverage: complete' "$EU_TRACE"
grep -F -q 'Coverage: complete' "$US_TRACE"
if grep -F -q 'runtime decision implementation did not match a proven candidate' "$OUTPUT"/*.txt; then
  echo "DYNAMIC_CDI_CONFORMANCE_FAILURE: runtime target was unresolved" >&2
  exit 1
fi

echo "DYNAMIC_CDI_CONFORMANCE_OK"
