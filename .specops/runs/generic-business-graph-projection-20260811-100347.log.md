# Run: Generic Business Graph Projection

- Started from `codex/external-method-contracts`.
- Added a separate `BusinessLogicGraph` with four business-only node kinds.
- Added loop folding, path-specific result projection, and a strict artifact guard.
- Added Mermaid, PlantUML, business JSON V1, and Draft 2020-12 schema output.
- Added default Maven generation and kept compatible technical artifacts.
- Passed focused tests and `./scripts/verify-pr.sh`.
- Local PostgreSQL verification was skipped because no connection was configured.
