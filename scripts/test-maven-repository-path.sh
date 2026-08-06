#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
RESOLVER="$ROOT/scripts/maven-repository-path.sh"
WORK=$(mktemp -d "${TMPDIR:-/tmp}/fachtracing-maven-path.XXXXXX")
NORMALIZED_WORK=$(printf '%s\n' "$WORK" | sed 's#//*#/#g')
trap 'rm -rf "$WORK"' EXIT INT TERM

actual=$(HOME="$WORK/home" FACHTRACING_MAVEN_REPOSITORY= \
  FACHTRACING_RELEASE_MAVEN_REPOSITORY= "$RESOLVER")
test "$actual" = "$NORMALIZED_WORK/home/.m2/repository"

actual=$(HOME="$WORK/home" FACHTRACING_MAVEN_REPOSITORY= \
  FACHTRACING_RELEASE_MAVEN_REPOSITORY="$WORK//release repository" "$RESOLVER")
test "$actual" = "$NORMALIZED_WORK/release repository"

actual=$(HOME="$WORK/home" FACHTRACING_MAVEN_REPOSITORY="$WORK//explicit repository" \
  FACHTRACING_RELEASE_MAVEN_REPOSITORY="$WORK//release repository" "$RESOLVER")
test "$actual" = "$NORMALIZED_WORK/explicit repository"

for consumer in verify.sh verify-mega-backend.sh verify-postgres.sh verify-release-gates.sh
do
  grep -q 'maven-repository-path.sh' "$ROOT/scripts/$consumer"
  if grep -q '\$HOME/.m2/repository' "$ROOT/scripts/$consumer"; then
    echo "MAVEN_REPOSITORY_FAILURE: direct home repository path in $consumer" >&2
    exit 1
  fi
done

echo MAVEN_REPOSITORY_PATH_OK
