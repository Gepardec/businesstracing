#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
WORK=$(mktemp -d "${TMPDIR:-/tmp}/fachtracing-release.XXXXXX")
CLONE="$WORK/fachtracing"
LOCAL="$WORK/m2"
EVIDENCE_OUTPUT=${EVIDENCE_OUTPUT:-$ROOT/target/release-evidence.txt}
MEGA_SOURCE=${MEGA_BACKEND_DIR:-/tmp/fachtracing-mega-backend-clean}
PETCLINIC_SOURCE=${SPRING_PETCLINIC_DIR:-/tmp/fachtracing-spring-petclinic-clean}
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

if [ ! -d "$PETCLINIC_SOURCE/.git" ]; then
  git clone -q https://github.com/spring-projects/spring-petclinic.git "$PETCLINIC_SOURCE"
  git -C "$PETCLINIC_SOURCE" checkout -q 88e37c15cf6fc8490b01bc3e8e2c800cec1ac272
fi

FACHTRACING_RELEASE_MAVEN_REPOSITORY="$LOCAL" MEGA_BACKEND_DIR="$MEGA_SOURCE" \
  SPRING_PETCLINIC_DIR="$PETCLINIC_SOURCE" \
  "$CLONE/scripts/capture-gate-output.sh" "$EVIDENCE_OUTPUT" \
  "$CLONE/scripts/verify-release-gates.sh"

{
  echo "release_commit=$(git -C "$CLONE" rev-parse HEAD)"
  echo "java_version=$("$(/usr/libexec/java_home -v 21)/bin/java" -version 2>&1 | head -1)"
  echo "maven_version=$(mvn -version | head -1)"
  echo "mega_commit=$(git -C "$MEGA_SOURCE" rev-parse HEAD)"
  echo "spring_petclinic_commit=$(git -C "$PETCLINIC_SOURCE" rev-parse HEAD)"
  echo "capability_sha256=$(shasum -a 256 "$CLONE/docs/java-capabilities.json" | awk '{print $1}')"
} >> "$EVIDENCE_OUTPUT"

echo RELEASE_GATE_OK
