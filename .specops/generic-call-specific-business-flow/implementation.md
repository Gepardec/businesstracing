# Implementation Journal: Generic Call-Specific Business Flow

## Summary

Implementation and automated verification are complete. The manual definition-of-done review is pending.

## Phase 1 Context Summary

- Config: SpecOps defaults; backend vertical; `.specops` specification directory; no external task tracker; review not required; evaluation enabled with a 7/10 threshold and two iterations.
- Context recovery: New feature. The unrelated `release-gate-timeout-budget` spec remains in progress and is not modified.
- Steering files: Loaded six files: `dependencies.md`, `product.md`, `reference-application.md`, `repo-map.md`, `structure.md`, and `tech.md`.
- Repo map: Fresh from the current implementation commit before this run's documentation changes.
- Memory: Loaded 118 decisions from completed specs, 18 recurring categories, and 37 file-overlap records. No production learnings file exists.
- Vertical: Backend Java library.
- Affected files: Business projection and rendering in `fachtracing-engine`, automatic output in `fachtracing-agent`, external conformance, documentation, and repository integrity checks.
- Project state: Brownfield multi-module Maven project on `codex/endpoint-business-tracing`.
- Vocabulary check: Pass; backend vocabulary needs no adaptation.
- Plan validation: Pass. Ten existing file paths resolve, and each new path is marked as new.
- Scope assessment: One spec. The overview, runtime flow, and proof touch several code areas, but each later part depends on the same new traceability contract and cannot meet the definition of done alone.
- Primary constraint: Preserve exact-to-business traceability before summary or automatic-output changes.

## Phase 2 Completion Summary

- Requirements: Generate a concise application-neutral overview and a call-specific business flow, use one model for automatic files, and prove the behavior on unknown synthetic inputs before Mega and Keycloak.
- Design: Preserve alternative exact paths for every business edge, select before summary, summarize by graph semantics, and fail closed on inconsistent evidence.
- Tasks: Four ordered tasks cover traceability, summary and selection, automatic output, and black-box conformance.
- Dependencies: Three completed SpecOps features are required. No new package is introduced.
- Spec evaluation: Passed in one iteration with scores 9, 8, 9, and 8.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Store alternative exact edge sequences per business edge. | A projected edge can represent several technical routes. Runtime selection must prove one whole route instead of accepting one shared edge. | Task 1 | 2026-08-14T09:25:28Z |
| 2 | Compute call completeness from selected static gaps and runtime-only gaps. | An incomplete full graph can contain a gap on a branch that the call did not take. The call diagram must describe the call, not every possible path. | Task 2 | 2026-08-14T09:29:33Z |
| 3 | Insert a runtime-only gap before an isolated result without using the result as a predecessor. | A result must remain terminal even when the selected flow has no visible rule or action. | Task 2 | 2026-08-14T09:49:20Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |
| Keep existing brownfield business-graph snapshots unchanged. | PetClinic merges two identical `correction required` result nodes into one node. | The graph-semantic summarizer correctly identifies equivalent terminal states. Both incoming paths and all three distinct result labels remain. | Task 4 |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
| --- | --- | --- | --- |

## Documentation Review

- `docs/runtime-integration.md` now states that automatic text and Mermaid files use one selected business graph and do not expose request or result values.
- `conformance/keycloak/README.md` separates the generated overview, generated evaluated example, and live endpoint output. It defines the four-question non-Java review.
- `conformance/keycloak/selection.md` records the lazy-stream coverage limit.
- The PetClinic report and reviewed oracle record the equivalent-result merge.
- The specification stays in implementation because a non-Java reviewer has not yet completed the final Keycloak endpoint review.

## Session Log

### Session 1 — Specification created (2026-08-14)

Created one generic feature spec. Keycloak and Mega are conformance inputs only. Production rules must be justified by graph or Java semantics and must pass synthetic unknown-project fixtures first.

### Session 2 — Task 1 started (2026-08-14)

Task 1 scope: Add immutable in-memory mappings from exact nodes and terminal edges to projected business nodes, plus every exact path represented by a business edge. Keep the current business graph output and public `project(AnalysisResult)` behavior unchanged.

Task 1 completed. A synthetic shipment graph proves node, result, hidden-path, and alternative-path mappings. The focused projection contract passes, and the existing business graph output stays unchanged.

### Session 3 — Task 2 started (2026-08-14)

Task 2 scope: Add deterministic graph-semantic gap and equivalence summary, select only business edges whose complete exact path was visited, retain one proved named result, and reject graph identity mismatch. Prove all behavior with synthetic graph contracts before external conformance.

Task 2 completed. The summarizer collapses connected gaps and equivalent states. Synthetic shipment and parcel graphs prove branch-specific named results, off-path gap exclusion, selected-path gap retention, semantic mutation, and graph-version rejection.

### Session 4 — Task 3 started (2026-08-14)

Task 3 scope: Render automatic text and Mermaid from one selected `BusinessLogicGraph` on the existing daemon sink thread. Prove that two calls omit each other's branches, share the same labels across formats, and keep private or technical data out of both files.

Task 3 completed. The sink now builds one selected business graph and gives it to the text and Mermaid renderers. Three automatic calls prove two different named results, one runtime-only coverage gap, shared labels, and the absence of private values. Existing programmatic explanation tests still pass.

### Session 5 — Task 4 started (2026-08-14)

Task 4 scope: Strengthen repository isolation, derive a Keycloak selected-path proof from its analyzed graph, retain Mega conformance, document the non-Java review rubric, and run all local and hosted gates. External-project assertions must not feed production topology.

Keycloak conformance passes with 169 exact nodes, 41 overview nodes, and 13 nodes in the generated evaluated example. Mega conformance now projects all five static decisions and its real runtime execution through the generic business-flow components. Repository integrity and the full local repository gate pass.

The PetClinic gate found one expected summary change. Two `correction required` terminal nodes had the same kind, label, and outgoing behavior. The summarizer merged them and preserved both incoming paths. The reviewed oracle and report now record 15 nodes and 20 edges. PetClinic conformance passes after that review.

Task 4 completed after hosted CI passed all four jobs. The implementation stays open for the manual non-Java review in the specification definition of done.
