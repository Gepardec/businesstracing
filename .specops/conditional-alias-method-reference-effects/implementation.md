# Implementation Journal: Conditional Alias and Method-Reference Effects

## Summary

Completed the conditional-alias and bound method-reference effect fix. Conditional branch merges
now preserve possible external roots and fail closed. Bound collection method references now show
their source-to-target transfer. The focused analyzer contract, the full local pull-request gate,
and both required hosted checks pass.

## Phase 1 Context Summary

- Config: SpecOps 1.8.0 defaults; `.specops.json` is absent; task tracking and spec review are off.
- Context recovery: No unfinished spec exists.
- Steering files: Loaded dependency, product, reference-application, repository-map, structure, and technology context.
- Repo map: Fresh at source hash `8ec191d14aa6b6081e8c2f8105fa2584e56d1e62dd672f1767ffc9bea8840a3f`.
- Memory: Loaded 67 decisions and recurring static-effect, runtime-correlation, and source-analysis patterns.
- Vertical: Brownfield Java library.
- Affected files: `LocalAliasResolver.java`, `DependencyGraphBuilder.java`, `StaticDecisionAnalyzer.java`, analyzer tests and fixtures, and supported-construct documentation.
- Project state: Brownfield.
- Vocabulary check: Pass for the library vertical.
- Plan validation: Pass; all listed implementation and test files exist.
- Scope assessment: One effect-model correction with two regression forms; no decomposition.

## Phase 2 Completion Summary

- Requirements: Unsupported result-relevant aliases and callback effects cannot yield a false complete graph.
- Design: Merge alias roots by certainty and reuse normal receiver mutation contracts for bound member references.
- Tasks: Add failing tests, implement the shared effect correction, then verify and publish.
- Dependencies: No new package; the required prior effect-model spec is completed.

## Phase 3 Completion Summary

- Added failing contracts for both reported false-complete graphs before production edits.
- Added proved and possible alias roots with conservative `if` branch merging.
- Reused normal receiver mutation contracts for bound method-reference callbacks.
- Added capability IDs and updated the supported Java construct guide.
- Kept direct aliases, detached aliases, lambdas, predicate references, and five Mega graphs stable.
- Passed the full local pull-request gate and hosted `pr-gate` and `postgres` checks.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Keep proved and possible alias roots in one resolver result. | Both consumers need the same certainty contract after a branch merge. | 2 | 2026-08-07T11:33:15Z |
| 2 | Render a bound callback mutation as a source-to-target transfer. | A business graph must show what the callback moves and which state it changes. | 2 | 2026-08-07T11:33:15Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |
| Static analyzer changes only | Added a separate shutdown timing correction after the first hosted run | Current `main` left only 100 ms for worker cancellation. The loaded hosted runner failed the same bounded-shutdown contract twice. | 3 |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
| --- | --- | --- | --- |
| The first hosted `pr-gate` failed twice in the existing bounded-shutdown contract. | Reserved half of the configured shutdown bound for cancellation, passed the protocol test 20 times, and reran the full gate. | One separate corrective commit; no analyzer scope change. | 3 |

## Documentation Review

- `README.md`: Checked. No update is required because setup and configuration did not change.
- `docs/supported-java-constructs.md`: Updated with conditional alias and bound callback behavior.
- `docs/java-capabilities.json`: Updated with executable capability contracts for both fixes.
- `docs/maven-plugin.md`: Checked. No update is required because Maven goal behavior did not change.

## Session Log

- 2026-08-07T11:25:46Z: Started the follow-up bug-fix workflow from merged PR #5 review feedback.
- 2026-08-07T11:28:15Z: Task 1 scope: Add two independent false-complete fixtures and
  assertions. The task ends only after the focused suite reproduces both missing effects.
- 2026-08-07: Task 1 completed. One focused run reported both defects. The conditional alias graph
  had only Start, reasons, and Stop. The method-reference graph had no candidates-to-accepted
  transfer. Both graphs were falsely complete.
- 2026-08-07: Task 2 scope: Add conservative branch-aware alias roots and bound
  member-reference receiver effects. The full analyzer contract, including prior alias and callback
  cases, must pass before completion.
- 2026-08-07: Task 2 completed. Conditional alias roots now become possible roots after an `if`
  merge. Bound `accepted::add` callbacks use the collection mutation contract and render as
  `add candidates to accepted`. The full static analyzer contract passes.
- 2026-08-07: Task 3 scope: Review capability documentation, run the local pull-request gate,
  finish the spec record, then commit, push, open a draft PR, and wait for required hosted CI.
- 2026-08-07: The full local pull-request gate passed. It verified repository integrity,
  Java capabilities, all modules, self-tracing, external activation, a 5,000-decision short load,
  and five complete Mega graphs from 420 source files. The short load had 0.604% p95 overhead and
  zero errors, mismatches, drops, or contamination. PostgreSQL was skipped because no connection
  was configured.
- 2026-08-07T11:40:00Z: Merged current `main` to resolve the draft pull request conflict. The
  focused analyzer contract and full local pull-request gate passed after the merge.
- 2026-08-07T11:46:11Z: The hosted PR gate failed twice in the bounded delivery shutdown contract
  from current `main`. Both failures occurred after the analyzer and capability stages passed.
- 2026-08-07T11:51:00Z: Reserved half of the shutdown bound for worker cancellation. The affected
  protocol test passed 20 consecutive runs, and the full local pull-request gate passed again.
- 2026-08-07T11:53:56Z: Task 3 completed. Draft PR #11 is published. Hosted `pr-gate` and
  `postgres` checks pass; the release-only job is correctly skipped for pull requests.
