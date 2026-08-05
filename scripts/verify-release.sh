#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
WORK=$(mktemp -d "${TMPDIR:-/tmp}/fachtracing-release.XXXXXX")
CLONE="$WORK/fachtracing"
LOCAL="$WORK/m2"
EVIDENCE_OUTPUT=${EVIDENCE_OUTPUT:-$ROOT/target/release-evidence.txt}
MEGA_SOURCE=${MEGA_BACKEND_DIR:-/tmp/fachtracing-mega-backend-clean}
cleanup() { rm -rf "$WORK"; }
trap cleanup EXIT INT TERM

git -C "$ROOT" diff --quiet
git -C "$ROOT" diff --cached --quiet
git clone -q --no-local "$ROOT" "$CLONE"
mkdir -p "$LOCAL" "$(dirname "$EVIDENCE_OUTPUT")"

if [ ! -d "$MEGA_SOURCE/.git" ]; then
  git clone -q https://github.com/Gepardec/mega-backend.git "$MEGA_SOURCE"
  git -C "$MEGA_SOURCE" checkout -q 782cdec8dfe5b4062eb5c1859e6a9e53afe02770
fi

(
  cd "$CLONE"
  export MAVEN_OPTS="${MAVEN_OPTS:-} -Dmaven.repo.local=$LOCAL"
  ./scripts/verify.sh
  MEGA_BACKEND_DIR="$MEGA_SOURCE" ./scripts/verify-mega-backend.sh
  CP="fachtracing-api/target/classes:fachtracing-engine/target/classes:fachtracing-engine/target/test-classes:$LOCAL/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar:$LOCAL/org/ow2/asm/asm-tree/9.10.1/asm-tree-9.10.1.jar"
  "$(/usr/libexec/java_home -v 21)/bin/java" -ea --add-modules jdk.compiler -cp "$CP" \
    at.gepardec.fachtracing.performance.FachtracingLoadTest
) | tee "$EVIDENCE_OUTPUT"

{
  echo "release_commit=$(git -C "$CLONE" rev-parse HEAD)"
  echo "java_version=$("$(/usr/libexec/java_home -v 21)/bin/java" -version 2>&1 | head -1)"
  echo "maven_version=$(mvn -version | head -1)"
  echo "mega_commit=$(git -C "$MEGA_SOURCE" rev-parse HEAD)"
  echo "capability_sha256=$(shasum -a 256 "$CLONE/docs/java-capabilities.json" | awk '{print $1}')"
} >> "$EVIDENCE_OUTPUT"

echo RELEASE_GATE_OK
