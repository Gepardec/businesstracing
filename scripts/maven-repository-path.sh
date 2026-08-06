#!/usr/bin/env sh
set -eu

if [ -n "${FACHTRACING_MAVEN_REPOSITORY:-}" ]; then
  printf '%s\n' "$FACHTRACING_MAVEN_REPOSITORY"
elif [ -n "${FACHTRACING_RELEASE_MAVEN_REPOSITORY:-}" ]; then
  printf '%s\n' "$FACHTRACING_RELEASE_MAVEN_REPOSITORY"
elif [ -n "${HOME:-}" ]; then
  printf '%s\n' "$HOME/.m2/repository"
else
  echo "MAVEN_REPOSITORY_FAILURE: HOME and repository overrides are unset" >&2
  exit 2
fi
