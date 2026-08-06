# Implementation Journal: Stage Lifecycle, Evidence, and Label Correctness

## Phase 1 Context Summary

- Config: `.specops.json` is absent. SpecOps 1.8.0 defaults apply.
- Context recovery: no incomplete specification existed.
- Steering files: loaded dependency, product, reference, repository, structure, and technology files.
- Repo map: loaded the map generated on 2026-08-06.
- Memory: loaded completed-spec context, decisions, and patterns.
- Vertical: Java library.
- Project state: brownfield.
- Vocabulary check: pass for the library vertical.
- Plan validation: pass; all production and test paths exist or are marked as generated outputs.
- AGENTS.md prohibits subagents. Work stays in this task.

## Regression Risk Analysis

| Behavior | Risk | Detection |
| --- | --- | --- |
| One terminal record per decision | Must-Test | skipped callback fixtures and long load |
| Original application call behavior | Must-Test | transformed callback and cancellation fixtures |
| Exact evidence or honest gap | Must-Test | receiver fixtures |
| Business label meaning | Must-Test | generic label fixture and Mega guard |
| Activation compatibility | Must-Test | external release integration |

## Decision Log

| # | Decision | Rationale | Task | Date |
| --- | --- | --- | --- | --- |

## Deviations from Design

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
| --- | --- | --- | --- |

## Documentation Review

Pending.

## Session Log

- 2026-08-06: Created a new high-severity bug-fix specification from the attached PR review.
- Task 1 scope: add independent contracts for skipped stages, direct and unsupported receivers,
  cancellation outside graph-bound methods, argument-one callbacks in three-argument stage calls,
  and preservation of legitimate validator labels.
- 2026-08-06: Task 1 completed. The fixture set now covers all five review examples.
- Task 2 scope: observe returned stage completion, apply cancellation probes to all fingerprinted
  application methods, and spill invocation operands so every catalog callback position works.
- 2026-08-06: Task 2 completed. Returned stages now close skipped reservations. One fingerprinted
  class pass covers cancel calls in unselected methods. Catalog arguments use typed local slots.
- Task 3 scope: bind direct parameter receivers to outcome or predicate evidence, report unsupported
  value receivers, preserve legitimate validator words, and regenerate generic Mega artifacts.
- 2026-08-06: Task 3 completed. Direct receiver facts bind to predicate and outcome nodes.
  Unsupported value receivers add source-located gaps. Validator cleanup now uses a new-object plus
  validate-only call role. Mega produces five complete graphs with no target-specific rule.
- Task 4 scope: run all release gates, update capability and runtime documents, record evidence,
  close all acceptance criteria, and commit the complete change.
