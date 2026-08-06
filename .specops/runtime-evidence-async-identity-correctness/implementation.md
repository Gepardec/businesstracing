# Implementation Journal: Runtime Evidence and Async Identity Correctness

## Phase 1 Context Summary

- `.specops.json` is absent. SpecOps 1.8.0 defaults apply.
- The repository is a brownfield Java 21 Maven library.
- No incomplete specification existed at session start.
- Related completed specifications are `release-explanation-async-correctness`,
  `generic-java-extractor-completion`, and `generic-application-readiness`.
- The attached PR review is the source of truth. GitHub has no live review threads for PR #5.
- The five findings form one release-correctness change and use the same conformance gates.
- AGENTS.md prohibits subagents. All work stays in this task.

## Dependency Audit

- No new dependency is planned.
- The transformer will use an original-bytecode `maxLocals` pass with existing ASM 9.10.1.
- Existing approved dependency versions stay unchanged.

## Regression Risk Analysis

| Behavior | Risk | Gate |
| --- | --- | --- |
| Application result and thrown-object identity | Must-Test | Agent transparency fixtures |
| Future identity and concrete behavior | Must-Test | Future cancellation fixtures |
| One terminal record per invocation | Must-Test | Nested async and load tests |
| No trace contamination | Must-Test | Concurrent and 1,000-RPS gates |
| Exact evidence or honest incomplete status | Must-Test | Predicate evidence fixtures |
| Activation bundle compatibility | Must-Test | API and external release tests |
| Five generic Mega graphs | Must-Test | Mega conformance and artifact guard |

## Decision Log

| # | Decision | Rationale | Task | Date |
| --- | --- | --- | --- | --- |
| 1 | Use exact callback identity as the submission handle | Stack order is not stable during synchronous nested callbacks. | 2 | 2026-08-06 |
| 2 | Track original future objects by identity | Wrapping changes observable application behavior. | 2 | 2026-08-06 |
| 3 | Read direct parameters at each predicate branch | Entry values become stale after reassignment and across loops. | 3 | 2026-08-06 |
| 4 | Mark unavailable required facts as runtime gaps | Exact explanations must not guess or hide missing evidence. | 3 | 2026-08-06 |
| 5 | Normalize Java terms with generic rules | Mega must validate the general mechanism, not shape it. | 4 | 2026-08-06 |

## Session Log

- 2026-08-06: Created this high-severity bug-fix specification from the attached PR review.
- 2026-08-06: Started Task 1. The code anchors are `TraceRuntime` pending-submission handling,
  `AsyncInvocationCatalog` result classification, `FachtracingTransformer` callback and evidence
  probes, `RuntimeCollector` future tracking, and `BusinessArtifactGuard`.
- 2026-08-06: Tasks 1 through 3 completed. Independent tests prove exact nested reservation
  ownership, actual Thread-object ownership, transparent cancellation for Future,
  CompletableFuture, and ForkJoinTask, unchanged future identity and behavior, current
  predicate-site parameter values, repeat-evaluation behavior or a source-located exact-path gap,
  property and calculated operand gaps, and fail-closed value encoding.
- 2026-08-06: Standard verification passed. The short 1,000-RPS gate completed 5,000 records with
  0.178% p95 overhead and zero errors, mismatches, drops, or contamination. External source-free
  activation passed. PostgreSQL was skipped because no connection was configured.
- 2026-08-06: Task 4 completed. One generic label normalizer and artifact guard remove Java
  construction, enum-type, and helper-role terms. Mega conformance produced five complete graphs
  from 420 sources. The reviewed journey-warning oracle and all generated structure and execution
  diagrams now use business-only labels. No Mega-specific production term was added.
- 2026-08-06: Task 5 completed. Commit `d9a1d4cc7c38a604b41ce91ade069ffe3ca50de2`
  passed the clean-clone release gate. The long run completed 600,000 decisions at 1,000 RPS with
  0.054% p95 overhead and zero errors, result mismatches, dropped records, or trace contamination.
  External source-free activation and five complete Mega graphs also passed. PostgreSQL was not
  rerun because no database connection was configured; the existing production-database contract
  remains unchanged.

## Blockers

- None.

## Final Evaluation

- Functionality: 10/10. All five attached review findings have executable passing contracts.
- Code quality: 9/10. Exact identity protocols are small, fail closed, and add no dependency.
- Test coverage: 10/10. Focused, standard, external, Mega, and long-load gates passed.
- Spec compliance: 10/10. No finding was closed by documentation alone.

## Documentation Review

- `docs/java-capabilities.json`: added independent contracts for predicate-site evidence, nested
  reservation identity, transparent future cancellation, and business Java vocabulary.
- `docs/runtime-integration.md`: documented exact handles, Thread ownership, original Future
  identity, predicate-site evidence, and coverage gaps.
- `docs/supported-java-constructs.md`: documented the supported exact subsets and fail-closed cases.
- `docs/release-evidence.md`: recorded the clean release result.

## Summary

All five tasks are complete. Nested async calls now confirm their own reservation. Supported
cancellation keeps the original Future object. Direct facts are read at the predicate, and missing
or unsafe facts make the execution incomplete. Generic graph normalization removes the reviewed
Java terms. Standard, external, Mega, and clean long-load verification passed.
