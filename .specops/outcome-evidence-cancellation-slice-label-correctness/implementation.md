# Implementation Journal: Outcome Evidence, Cancellation Reach, Slice, and Label Correctness

## Summary

All four review defects are fixed. Terminal observations retain available receiver facts. Activation
includes exact cancellation callers from compiled application output. Static slicing includes only
proven writes and reports unknown result effects. Helper labels use proven roles and other receivers
keep their business meaning.

## Phase 1 Context Summary

- Config: `.specops.json` is absent. SpecOps 1.8.0 defaults apply.
- Context recovery: no incomplete specification exists for this new review.
- Steering files: loaded product, technology, structure, reference application, dependency, and
  repository map files.
- Repo map: loaded the existing generated map.
- Memory: loaded completed-spec context, decisions, and recurring patterns.
- Vertical: Java library.
- Project state: brownfield, clean branch at the reviewed PR head.
- Scope assessment: four findings cross runtime, agent, analyzer, and plugin code, but they form one
  release-blocking correctness increment and share the same conformance gate. No split is needed.
- Vocabulary check: pass for the library vertical.
- Plan validation: pass; all existing paths resolve and new fixture paths are marked for creation.
- Dependency gate: the required prior spec is completed. No cycle exists.
- AGENTS.md prohibits subagents. Work stays in this task.

## Regression Risk Analysis

| Behavior | Risk | Detection |
| --- | --- | --- |
| Complete explanation keeps available facts | Must-Test | direct receiver runtime explanation |
| Cancelled work publishes one record | Must-Test | separate controller integration |
| Graph contains result causes only | Must-Test | read-only call and mutation fixtures |
| Business labels keep object distinctions | Must-Test | helper and non-helper fixtures plus Mega |
| Generic release behavior | Must-Test | external, Mega, and long load gates |

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Scan project output bytecode for exact cancellation calls | It covers separate callers without fingerprinting dependency jars or guessing runtime types. | 2 | 2026-08-06 |
| 2 | Project only non-result Stop evidence as a reason | The result already has a dedicated explanation field; extra facts explain why it was returned. | 2 | 2026-08-06 |
| 3 | Propagate proven callback writes to the enclosing functional call | A lambda mutation executes through its `forEach` or stream boundary, so the outer call carries that result dependency. | 3 | 2026-08-06 |
| 4 | Classify call effects from attributed JDK contracts and source mutation summaries | Identifier use is not a write; proof or an explicit gap is required. | 3 | 2026-08-06 |

## Deviations from Design

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
| --- | --- | --- | --- |
| The sandbox did not permit writes to the home Maven repository. | Re-ran verification with the approved Maven access. | No product or test change. | 4 |

## Documentation Review

- `docs/java-capabilities.json` adds independent terminal evidence, result-slice, unknown-effect,
  and receiver-label contracts.
- `docs/supported-java-constructs.md` defines proven writes, unknown result effects, terminal facts,
  exact cancellation caller selection, and receiver-preserving labels.
- `docs/runtime-integration.md` and `docs/maven-plugin.md` define the application-output fingerprint
  boundary and the terminal evidence merge.
- `docs/release-evidence.md` records the clean release result at commit `8e62f24`.

## Session Log

- 2026-08-06: Started a high-severity bug-fix spec from the review of PR #5 at `d1b16b3`.
- Task 1 scope: add executable contracts for saved outcome evidence, separate-class cancellation,
  proven result slicing, unknown side-effect gaps, and role-based validation labels before changing
  production code.
- 2026-08-06: Task 1 completed. The analyzer fails with the unwanted audit and validate nodes. The
  agent test fails because the Stop observation contains only `result` and no `city` evidence.
- Task 2 scope: merge staged Stop evidence into the saved observation and explanation, detect exact
  cancellation caller bytecode in compiled application outputs, and keep the SHA-256 transform
  boundary. Runtime, agent, plugin, and external activation contracts must pass.
- 2026-08-06: Task 2 completed. Stop observations now keep staged facts. Explanations show those
  facts as reasons. Activation V3 fingerprints exact cancel callers from application outputs, and
  the external source-free application passes with cancellation from a separate controller.
- Task 3 scope: replace identifier-use effects with attributed proven writes, create source-located
  gaps for unknown effects on returned references, and use proven helper roles for concise labels
  while preserving all other call receivers. Existing mutation graphs and all five Mega graphs must
  remain correct.
- 2026-08-06: Task 3 completed. Read-only ignored calls are absent. Proven JDK, source, and callback
  mutations stay in the slice. Unknown reference effects create a source-located gap. Helper roles
  produce distinct labels, and Mega passes with five complete graphs and improved generic labels.
- Task 4 scope: run repository integrity, standard verification, external activation, Mega, and the
  clean 600-second 1,000-RPS release gate; then update docs, evaluation, memory, metrics, commit, and
  push the branch.
- 2026-08-06: Standard verification passed. The short 1,000-RPS run completed 5,000 decisions with
  zero errors, mismatches, drops, or contamination and 0.162% p95 overhead. The external release
  integration passed. PostgreSQL was skipped because no connection was configured.
- 2026-08-06: Task 4 completed. Commit `8e62f243850c06fbd16d57c0c807c4177d91c9df`
  passed the clean release gate. Mega produced five complete graphs from 420 source files. The long
  run completed 600,000 decisions at 1,000 RPS with 0.051% p95 overhead and zero errors,
  mismatches, drops, or contamination.

## Final Evaluation

- Root cause accuracy: 10/10. Each production defect maps to one false-before-fix contract.
- Fix completeness: 10/10. Runtime, agent, analyzer, plugin, docs, and activation integration agree.
- Regression safety: 9/10. Five complete Mega graphs and the original application results remain
  stable; six reviewed labels changed because the generic renderer is more precise.
- Test verification: 10/10. Focused, standard, external, Mega, and clean long-load gates pass.
