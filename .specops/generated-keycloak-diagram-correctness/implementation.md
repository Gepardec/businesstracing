# Implementation Journal: Generated Keycloak Diagram Correctness

## Summary

Completed both tasks. The Keycloak Mermaid file now renders the generic business projection of the
selected endpoint analysis. No manual graph or tracked flowchart remains. Generic filtering removes
technical data-building actions and rejects Java method references without removing ordinary
business actions.

## Phase 1 Context Summary

- Config: SpecOps defaults; no `.specops.json`; backend vertical; no task tracker; review disabled.
- Context recovery: `release-gate-timeout-budget` is an unrelated implementing specification.
- Steering files: loaded dependencies, product, reference application, repository map, structure,
  and technology context.
- Repo map: fresh at workflow start; the latest main change did not change the tracked file list.
- Memory: loaded completed project history, 109 recorded decisions, and recurring conformance and
  business-projection patterns.
- Production learnings: no learning file exists.
- Vertical: backend.
- Affected files: Keycloak conformance harness and guide, repository integrity script, SpecOps
  records, memory, and repository map.
- Project state: brownfield Java 21 multi-module Maven repository.
- Scope assessment: one contained artifact-correctness fix with two ordered tasks; no decomposition.
- Vocabulary check: not required for the backend vertical.
- Plan validation: pass; all listed implementation paths exist and SpecOps paths are generated.

## Phase 2 Completion Summary

- The generated Keycloak diagram must come from the existing generic business projection.
- Reviewed labels remain assertions and never become graph input.
- The exact activation graph and runtime behavior remain unchanged.
- Two tasks cover the implementation guard and the external proof. No dependency is introduced.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Use reviewed Keycloak labels only as assertions against exact and projected data. | The assertions detect semantic loss without supplying topology to the generated graph. | 1 | 2026-08-14T08:47:55Z |
| 2 | Remove technical calculation forms through the generic projector and guard. | Keycloak-specific filtering would reproduce the same conformance shortcut in a different layer. | 2 | 2026-08-14T08:58:18Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |
| Reuse the generic projector without production changes. | Tighten generic calculation filtering and the business vocabulary guard. | Pinned artifact inspection found Java method references and technical data-building actions that the existing guard accepted. | 2 |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
| --- | --- | --- | --- |

## Documentation Review

| File | Status | Result |
| --- | --- | --- |
| `conformance/keycloak/README.md` | Updated | States that the generic business projection generates the disposable diagram. |
| `README.md` | Up-to-date | Its endpoint tracing summary does not embed or claim a fixed Keycloak topology. |
| `conformance/keycloak/selection.md` | Up-to-date | Exact endpoint and incomplete-coverage behavior did not change. |

## Verification

- Test-first repository check: failed on `reviewedOverview()` before the fix and passed after it.
- Test-first projection contract: failed on technical attributes, map population, and Java method
  references before the generic fix and passed after it.
- Focused projection test: pass with a valid `put order on hold` business action retained.
- Full `./scripts/verify.sh`: pass, including 5,000 load decisions with zero errors, mismatches,
  drops, or contamination. PostgreSQL was skipped because no connection was configured.
- Pinned clean Keycloak conformance: pass with 169 exact nodes and incomplete coverage.
- Generated artifact inspection: required search rules and explicit gaps present; Java owners,
  source names, paths, and method references absent.
- Activation inspection: one decision, 169 exact nodes, probe sites, and class fingerprints present.

## Phase 3 Completion Summary

- Tasks completed: 2 of 2.
- Dependency gate: pass. The required `configured-endpoint-business-tracing` specification is
  complete, and no dependency cycle or dependency change exists.
- Test result: focused, repository, complete, and pinned external gates pass.

## Phase 4 Completion Summary

- All acceptance criteria pass locally.
- Implementation evaluation passed in one iteration.
- Memory, repository map, metrics, and run records were refreshed.
- Hosted CI runs after the required push.

## Session Log

- 2026-08-14: Created the bug-fix contract after merging the latest `origin/main`.
- 2026-08-14: Started Task 1. Scope: add the failing integrity guard, connect the
  Keycloak artifact to the generic projection, remove manual graph construction, and keep reviewed
  rules as assertions only.
- 2026-08-14: Completed Task 1. The new guard failed on `reviewedOverview()` before the fix.
  Repository integrity and all Maven tests passed after the harness rendered `fullBusinessGraph`.
- 2026-08-14: Started Task 2. Scope: remove the fixed guide diagram, run the complete gate and
  pinned Keycloak proof, inspect generated output, and complete project records.
- 2026-08-14: Pinned Keycloak passed, but artifact inspection found Java method references and
  technical data-building actions. Task 2 now includes generic projector and guard corrections.
- 2026-08-14: Completed Task 2. Final full verification and pinned Keycloak conformance passed.
  The generated artifact contains required rules and explicit gaps without Java method references.
