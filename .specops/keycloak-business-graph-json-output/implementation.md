# Implementation journal: Keycloak business graph JSON output

## Phase 1 Context Summary

- Config: SpecOps defaults; no `.specops.json` file exists.
- Context recovery: no matching incomplete spec exists.
- Steering: loaded the existing product, technology, structure, dependency, reference application,
  and repository map files.
- Memory: loaded the existing project context, decisions, and patterns.
- Vertical: library.
- Project state: brownfield.
- Affected files: the Keycloak conformance harness and its README.
- Scope assessment: one output adapter and one task; decomposition is not recommended.
- Git pre-flight: the working tree was clean before implementation.

## Summary

The pinned Keycloak conformance run now writes the generated `search users` overview as V1 business
graph JSON and writes the matching schema. It continues to generate all topology from source analysis.

## Decisions

- Reuse the production V1 exporter and schema generator.
- Keep the output beside the existing Mermaid and activation files.
- Do not add graph nodes, graph edges, or Keycloak-specific graph construction.

## Verification

- `./scripts/verify-self-tracing.sh`: passed.
- `MEGA_BACKEND_DIR=/private/tmp/fachtracing-mega-backend ./scripts/verify-mega-backend.sh`: passed.
- `KEYCLOAK_DIR=/private/tmp/fachtracing-keycloak ./scripts/verify-keycloak.sh`: passed.
- JSON contract checks for all nine generated graphs: passed.
- `./scripts/verify-pr.sh`: passed.

## Deviations

None.
