# Implementation Journal: JDK Mutation and Alias Effect Correctness

## Phase 1 Context

- SpecOps 1.8.0 defaults apply; `.specops.json` is absent.
- The branch was clean at reviewed head `daa38e5`.
- Product, technology, structure, dependency, reference, repository-map, and memory context were read.
- The repository map source hash was stale and will be refreshed in the completion phase.
- The required prior spec is completed. No dependency cycle or blocker exists.
- The vertical is a brownfield Java library. `AGENTS.md` prohibits subagents.

## Regression Risk

| Behavior | Tier | Detection |
| --- | --- | --- |
| Complete graphs contain every result-changing JDK mutation | Must-Test | `Deque.offer` fixture |
| Helper writes map through direct local aliases | Must-Test | alias helper fixture |
| Unknown platform effects fail closed | Must-Test | unknown JDK effect fixture |
| Existing generic graphs remain stable | Must-Test | full analyzer and Mega gates |

## Decision Log

| # | Decision | Reason | Task |
| --- | --- | --- | --- |
| 1 | Separate proved mutation, proved read-only, and unknown effects | A namespace is not proof of purity. | 2 |
| 2 | Resolve only direct reference identity aliases | It fixes the defect without guessing general points-to state. | 2 |

## Session Log

- 2026-08-07 06:30 UTC: Started the high-severity bug-fix workflow from the review at `daa38e5`.
- 2026-08-07 06:33 UTC: Specification evaluation passed. Task 1 is in progress before test edits.
- 2026-08-07: Task 1 completed. The existing analyzer failed both fixtures with only `Start`, the
  local collection, and `Stop`; neither age predicate was present.
- 2026-08-07: Task 2 completed. Explicit JDK effects, flow-ordered alias resolution, effect-root
  slicing, and fail-closed unknown platform calls pass the full analyzer contract suite.
- 2026-08-07: Task 3 is in progress before documentation and repository-wide verification.
- 2026-08-07: Standard verification passed. The short 1,000-RPS run completed 5,000 decisions with
  zero errors, mismatches, drops, or contamination and 0.228% p95 overhead. External activation
  passed. PostgreSQL was skipped because no connection was configured.
- 2026-08-07: Mega passed with five complete graphs from 420 source files. The reviewed journey
  graph now follows result-changing helper mutations and has 96 nodes and 130 edges.
- 2026-08-07: The first clean release attempt stopped at repository integrity because the reviewed
  journey oracle hash guard still named the prior approved topology. The guard now names the new
  reviewed hash; no runtime or analyzer behavior changed.
- 2026-08-07: Task 3 completed. Clean release commit `e5365f26bdbb52a12f9bb571dcaf8e0e128fc7d4`
  passed. The long run completed 600,000 decisions at 1,000 RPS with 0.059% p95 overhead and zero
  errors, mismatches, drops, or contamination.

## Documentation Review

- `docs/java-capabilities.json` has independent JDK mutation, alias, invalidation, and unknown-JDK
  effect contracts.
- `docs/supported-java-constructs.md` defines explicit purity and direct alias behavior.
- The Mega report, generated diagrams, semantic oracle, hash guard, and reviewed oracle hash agree.
- `docs/release-evidence.md` records the clean release result.

## Final Evaluation

- Root cause accuracy: 10/10.
- Fix completeness: 9/10. The direct-alias contract is complete; arbitrary points-to analysis is not
  claimed.
- Regression safety: 9/10. All five Mega graphs stay complete and the reviewed journey graph now
  contains the previously hidden result-changing helper logic.
- Test verification: 10/10. Focused, standard, external, Mega, and clean long gates pass.
