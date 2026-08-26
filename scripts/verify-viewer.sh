#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT/fachtracing-viewer"

npm ci --ignore-scripts
npm run check
npm test
npm run build
npm run audit

if test "${FACHTRACING_VIEWER_BROWSER_TESTS:-false}" = "true"
then
  npm run test:browser
fi

echo "VIEWER_OK"
