# Implementation Journal: Explainable Relevance and Polymorphic Dispatch

## Summary

Completed four tasks. The analyzer now uses expression-bounded relevance, so a relevant control statement does not make unrelated work in its body relevant. The developer-only manifest explains source-derived inclusions, exclusions, coverage gaps, and Java polymorphic candidates. Static analysis keeps each proven concrete compatible implementation. Runtime evidence still selects the implementation that ran. The business graph schema and activation format do not change. The full repository verifier passed.

## Phase 1 Context Summary

- Config: SpecOps 1.8.0 defaults; library vertical; no external task tracking.
- Context recovery: No incomplete spec was present.
- Steering: Loaded product, technology, structure, dependency, reference-application, and repository-map context.
- Repo map: Fresh at the start. A completion refresh is required because two analysis classes were added.
- Memory: Loaded 67 prior decisions and nine recurring patterns.
- Affected area: Static relevance, developer manifest provenance, dynamic-dispatch classification, analyzer contracts, and supported-construct documentation.
- Project state: Brownfield Java 21 library.
- Scope assessment: One cohesive analyzer feature with four ordered tasks. No decomposition was required.
- Plan validation: Existing paths and symbols resolved. The two planned single-responsibility classes were new paths.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Bound descendant relevance to sliced expressions. | Calls nested in returned and predicate expressions remain relevant, while unrelated calls in a relevant branch body do not enter the graph. | Task 1 | 2026-08-07T11:41:40Z |
| 2 | Keep all proven compatible dispatch candidates and let runtime evidence select one. | Java polymorphism can make the concrete target unknown during static analysis. The analyzer must not guess a target. | Task 3 | 2026-08-07T11:44:30Z |

## Phase 2 Completion Summary

- Requirements: Defined source-mapped inclusion, exclusion, gap, and dispatch decisions.
- Design: Separated relevance policy, exclusion auditing, graph collection, and dispatch classification.
- Dependencies: No dependency was added or changed.
- Evaluation: All four specification dimensions passed.

## Phase 3 Completion Summary

- Tasks completed: 4 of 4.
- Production changes: Added `DecisionRelevance`, `AnalysisDecisionAuditor`, immutable manifest decisions, builder collection, and dispatch candidate classification.
- Test changes: Added result-slice, manifest, sealed-interface, abstract-subtype, and receiver-compatibility contracts.
- Documentation: Updated `docs/supported-java-constructs.md`.
- Deviations: None.
- Verification: Focused analyzer and API contracts passed. `./scripts/verify.sh` passed repository integrity, Java capabilities, self-tracing, external release, all executable contracts, and the short load test. The load completed 5,000 decisions at 1,000 RPS with 0.180% p95 overhead and no errors, mismatches, drops, or contamination. PostgreSQL verification was skipped because no connection was configured.

## Documentation Review

| Document | Status | Result |
| --- | --- | --- |
| `docs/supported-java-constructs.md` | Updated | Explains expression-bounded relevance, manifest audit actions, polymorphic candidates, and runtime selection. |
| `README.md` | Up-to-date | Its high-level static-candidate and runtime-correlation description remains correct. |
| `docs/runtime-integration.md` | Up-to-date | Runtime dispatch correlation and activation behavior did not change. |

## Session Log

- 2026-08-07T11:41:40Z — Task 1 completed. Added the expression-bounded relevance policy and compatible immutable manifest model.
- 2026-08-07T11:44:01Z — Task 2 completed. Added source-derived inclusion, exclusion, and gap decisions.
- 2026-08-07T11:44:30Z — Task 3 completed. Added exact candidate decisions for concrete, abstract, and receiver-incompatible subtypes.
- 2026-08-07T11:49:35Z — Task 4 completed. Focused and full verification passed; documentation is current.
