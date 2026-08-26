# Implementation journal: Architecture Dojo Anspruch conformance

## Phase 1 Context Summary

- Config: SpecOps defaults; no `.specops.json` file exists.
- Context recovery: `release-gate-timeout-budget` and `generic-call-specific-business-flow` are
  unrelated incomplete specs and are not continued.
- Steering: loaded product, technology, structure, dependency, reference application, and repository
  map files.
- Memory: loaded existing context, decisions, and patterns.
- Vertical: library.
- Project state: brownfield.
- External state: repository `main` has no application source; `feature/onion` is pinned at
  `5767ba85bffd82520d7ee7f72c281a9395d1b7ee`.
- Affected files: one conformance harness, one verification script, two README files, and SpecOps
  artifacts.
- Scope assessment: one conformance adapter with two generated graphs; decomposition is not
  recommended.
- Git pre-flight: clean after merging current `main` and PR 33.

## Phase 2 Summary

- Requirements define two source-generated V1 graph documents.
- Design keeps checkout orchestration separate from source analysis and output checks.
- No graph topology or diagram is stored in source.
- No dependency is introduced.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
|---|----------|-----------|------|-----------|
| 1 | Pin the complete Onion solution branch. | Repository main has no application source to analyze. | Task 1 | 2026-08-26T09:02:22Z |
| 2 | Select application boundary methods only. | The analyzer must derive all downstream graph structure from source. | Task 1 | 2026-08-26T09:02:22Z |

## Deviations from Design

None.

## Verification

- Pinned external Maven tests: passed.
- Architecture Dojo conformance: passed for 39 source files and two decisions.
- Benefit entitlement: 86 exact nodes, 110 exact edges, 29 business nodes, 43 business edges,
  `COMPLETE`.
- Incapacity notification: 88 exact nodes, 112 exact edges, 31 business nodes, 45 business edges,
  `COMPLETE`.
- V1 JSON Schema validation: passed for both documents.
- PR 33 viewer: check, 43 tests, production build, audit gate, and parsing of both generated files
  passed.
- Full pull-request gate: passed with fresh pinned Mega, Spring PetClinic, and Jakarta EE corpora.

## Phase 3 Completion Summary

- Added one external conformance adapter with separate orchestration and analysis responsibilities.
- Generated JSON, Mermaid, and PlantUML from the unchanged pinned source.
- Added no production logic, dependency, topology oracle, or fixed diagram.

## Phase 4 Completion Summary

All six acceptance criteria and both tasks pass. Documentation, viewer compatibility, full Java
verification, memory, index, and repository map are complete.
