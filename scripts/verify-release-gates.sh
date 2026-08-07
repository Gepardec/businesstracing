#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
LOCAL=${FACHTRACING_RELEASE_MAVEN_REPOSITORY:?release Maven repository is required}
MEGA_SOURCE=${MEGA_BACKEND_DIR:?Mega Backend directory is required}
MAVEN_REPOSITORY=$("$ROOT/scripts/maven-repository-path.sh")

cd "$ROOT"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Dmaven.repo.local=$LOCAL"
./scripts/verify.sh
MEGA_BACKEND_DIR="$MEGA_SOURCE" ./scripts/verify-mega-backend.sh
CP="fachtracing-api/target/classes:fachtracing-engine/target/classes:fachtracing-engine/target/test-classes:$MAVEN_REPOSITORY/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar:$MAVEN_REPOSITORY/org/ow2/asm/asm-tree/9.10.1/asm-tree-9.10.1.jar"
"$(/usr/libexec/java_home -v 21)/bin/java" -ea --add-modules jdk.compiler -cp "$CP" \
  at.gepardec.fachtracing.performance.FachtracingLoadTest
