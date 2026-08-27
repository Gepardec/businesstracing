#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

if [ -n "$(git status --porcelain --untracked-files=no)" ]; then
  echo "Viewer dogfood generation requires a clean tracked worktree." >&2
  exit 1
fi

if [ "${FACHTRACING_SKIP_PROJECT_BUILD:-false}" != "true" ]; then
  mvn -q -DskipTests install
fi

FACHTRACING_SKIP_PROJECT_BUILD=true \
FACHTRACING_SELF_DEVELOPER_JSON=true \
  ./scripts/verify-self-tracing.sh

DOGFOOD="$ROOT/target/viewer-dogfood"
GRAPH_DIR="$DOGFOOD/graphs"
RUN_DIR="$DOGFOOD/runs"
mkdir -p "$GRAPH_DIR" "$RUN_DIR"
find "$GRAPH_DIR" -type f -name '*.json' -delete
find "$RUN_DIR" -type f -name '*.decision.json' -delete
cp "$ROOT"/target/fachtracing/*-developer.json "$GRAPH_DIR"/
cp "$ROOT"/target/fachtracing-runtime/*.decision.json "$RUN_DIR"/

test "$(find "$GRAPH_DIR" -name '*-developer.json' -type f | wc -l | tr -d ' ')" = "3"
test "$(find "$RUN_DIR" -name '*.decision.json' -type f | wc -l | tr -d ' ')" = "5"
for graph in "$GRAPH_DIR"/*-developer.json
do
  grep -F -q '"schema":"fachtracing-developer-graph/v1"' "$graph"
done
for run in "$RUN_DIR"/*.decision.json
do
  grep -F -q '"schema":"fachtracing-decision-record/v1"' "$run"
  grep -F -q '"canonicalValue":"fachtracing"' "$run"
done

echo "FACHTRACING_VIEWER_DOGFOOD_OK graphs=3 runs=5 directory=$DOGFOOD"
