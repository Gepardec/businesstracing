#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

if [ "${FACHTRACING_SKIP_PROJECT_BUILD:-false}" != "true" ]; then
  mvn -q install
fi

mvn -q -f "$ROOT/pom.xml" compile \
  at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-rc.1:analyze-reactor

OUTPUT="$ROOT/target/fachtracing"
MERMAID="$OUTPUT/enable-developer-graph-export-structure.mmd"

test -f "$MERMAID"
test -f "$OUTPUT/enable-developer-graph-export-structure.puml"
test -f "$OUTPUT/index.md"
test -f "$OUTPUT/activation.json"

grep -F -q 'enable developer graph export' "$OUTPUT/index.md"
grep -F -q 'returns optional empty' "$MERMAID"
grep -F -q 'returns optional of new developer output' "$MERMAID"
grep -F -q '"schema":"fachtracing-activation/v3"' "$OUTPUT/activation.json"

echo FACHTRACING_SELF_TRACE_OK
