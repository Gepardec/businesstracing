#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

./scripts/verify.sh
FACHTRACING_SKIP_PROJECT_BUILD=true ./scripts/verify-mega-backend.sh
FACHTRACING_SKIP_PROJECT_BUILD=true ./scripts/verify-spring-petclinic.sh
FACHTRACING_SKIP_PROJECT_BUILD=true ./scripts/verify-jakartaee-rest.sh

echo "FAST_PR_GATE_OK"
