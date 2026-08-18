# Implementation Journal: Self-Explainable Runtime Dogfood

## Summary

The implementation generates structured analysis and projection audit diagrams for all Maven graph outputs. The repository self-analysis selects two unannotated production policies and proves five evaluated runtime paths through the Java agent.

## Phase 1 Context Summary

- Baseline: merged PR 27 at `e7b1bae` on `origin/main`.
- Existing active spec: `generic-call-specific-business-flow`; this spec does not change its runtime call-correlation scope.
- Relevant existing contracts: configured unannotated entry points, exact graph and manifest generation, business projection, activation bundle generation, Java-agent runtime capture, and business execution Mermaid.
- Gap: the merged baseline does not emit a structured reason for each projection decision and does not run its two core policies through its own configured runtime flow.
- Branch: `codex/self-explainable-runtime`.
- New dependencies: none planned.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
|---|---|---|---|---|
| 1 | Start from merged PR 27 and create a new spec. | The older self-analysis spec completed against a different baseline and excluded runtime proof for these algorithms. | 1 | 2026-08-18T08:18:41Z |
| 2 | Use structured audit records as the only input to audit Mermaid. | This proves that diagrams come from program decisions and prevents fixed topology. | 1 | 2026-08-18T08:18:41Z |
| 3 | Select production methods through root Maven configuration. | This uses the feature introduced by PR 27 and needs no demonstration annotation. | 2 | 2026-08-18T08:18:41Z |

## Session Log

- 2026-08-18: Recovered repository and GitHub state, confirmed PR 27 merge and green CI, and evaluated the old self-analysis and Keycloak branches.
- 2026-08-18: Created the completion spec from the agreed definition of done and started Task 1.
- 2026-08-18: Completed structured projection decisions, traceable summarization, grouped audit rendering, Maven output, cleanup, and focused tests.
- 2026-08-18: Extracted `AnalysisSourceSelector` from the analyzer and made `classifyNode` the production projector policy. The `self-tracing` profile selects both methods through `businessEntryPoints`.
- 2026-08-18: Generated complete static graphs, business graphs, JSON, two audit diagrams, and activation data for both policies. A second run produced byte-identical files.
- 2026-08-18: Executed five compiled calls through the Java agent. Each call produced one complete evaluated Mermaid path with the same result as the method call. No call was dropped or mixed.
- 2026-08-18: Passed focused contracts, repository integrity, Java capability, performance and isolation, external release, Mega, PetClinic, and pinned Keycloak conformance. Local PostgreSQL was unavailable and remains assigned to CI.
- 2026-08-18: Opened pull request 30 against `main`. Its core, Mega, PetClinic, and PostgreSQL jobs passed.

## Verification Evidence

- Self runtime: 2 node-policy paths and 3 source-policy paths, all complete.
- Load and isolation: 5,000 calls, 0 errors, 0 mismatches, 0 drops, and 0 contamination.
- Mega: 5 complete decisions.
- PetClinic: 3 complete decisions.
- Keycloak: 128 exact nodes, 45 business nodes, 11 evaluated nodes, complete.
- Dependencies: no new dependency or version change.

## Phase 4 Completion Summary

- All four tasks and all 20 acceptance criteria passed.
- Both production policies generate complete exact and business graphs plus structured audits.
- Five real policy calls generated five complete evaluated Mermaid paths.
- Generated static outputs are deterministic and synthetic input changes the audit output.
- Production graph generation contains no fixed self-example labels and no AI integration.
- Local and pull request gates passed, including Keycloak and PostgreSQL proof.
