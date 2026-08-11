# Implementation Journal: Hogajama business time vocabulary

## Summary

Completed one task. The business artifact guard now rejects `Start` and `Stop` only when the full
label is that structural marker. Valid compound phrases such as `today start time` and `bus stop`
remain in the business graph. The strict combined Hogajama reactor produces two complete aggregate
graphs and all business formats.

## Phase 1 Context Summary

- Config: SpecOps defaults; vertical `library`; specsDir `.specops`; task tracking `none`.
- Context recovery: no incomplete matching spec.
- SpecOps fallback: the installed skill has no referenced `core/` modules or templates, so this spec
  follows the repository's established SpecOps artifact format.
- Steering files: loaded dependencies, product, repo map, structure, and technology.
- Repo map: loaded; the affected business package exists on the projection branch.
- Memory: loaded completed analyzer and projection decisions. No production learnings file exists.
- Vertical: library.
- Project state: brownfield.
- Affected files: the business vocabulary guard and its executable projection contract.
- Scope assessment: one guard-policy false positive; decomposition is not recommended.
- Vocabulary check: pass.
- Plan validation: pass; both planned files exist.

## Phase 2 Completion Summary

- Requirement: accept ordinary `start` and `stop` words inside longer business phrases.
- Design: anchor only the two exact structural-marker patterns.
- Tasks: one focused test-first implementation and verification task.
- Dependencies: one required completed projection spec; no new package.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
|---|----------|-----------|------|-----------|
| 1 | Match terminal markers as complete labels. | The same words have valid business meaning inside longer phrases. | Task 1 | 2026-08-11T10:42:56Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
|---------|--------|--------|------|

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
|---------|------------|--------|------|

## Documentation Review

- `README.md`: checked; the business-only artifact description remains correct.
- `docs/maven-plugin.md`: checked; the output and strict-analysis instructions remain correct.
- Public schemas: checked; no model or wire-contract field changed.

## Session Log

- 2026-08-11T10:33:26Z: Reproduced the strict Hogajama failure and traced it to an unanchored
  `start` pattern in the pending business-graph projection.
- 2026-08-11T10:35:38Z: Task 1 scope: add one acceptance contract for compound business words,
  anchor only the exact `start` and `stop` rules, and preserve every other guard rejection.
- 2026-08-11T10:37:03Z: The focused pre-fix contract failed because the guard rejected both
  `today start time` and `bus stop`.
- 2026-08-11T10:42:56Z: The focused contract passed after the two patterns were anchored.
- 2026-08-11T10:42:56Z: `./scripts/verify-pr.sh` passed, including 5,000 load decisions, external
  release, five complete Mega graphs, and the current Spring PetClinic expectations.
- 2026-08-11T10:42:56Z: A disposable integration worktree combined this fix with PR 19. The exact
  strict Hogajama reactor generated two complete aggregate graphs and all business formats.

## Phase 3 Completion Summary

- Added one accepted-vocabulary contract that failed before the fix and passed after it.
- Anchored only the exact `start` and `stop` patterns.
- Kept all other guard rules and interfaces unchanged.

## Phase 4 Completion Summary

- All acceptance criteria and required tests passed.
- The adversarial implementation evaluation passed.
- Documentation, memory, index, and run records were reviewed or updated.
