# Implementation Journal: Fast Pull-Request CI Pipeline

## Phase 1 Context Summary

- Config: `.specops.json` is absent; SpecOps 1.8.0 defaults apply.
- Context recovery: no incomplete specification exists.
- Steering: product, technology, structure, dependency, and repository-map files loaded.
- Memory: completed-spec context, decisions, and recurring patterns loaded.
- Repo map: current for the source file inventory at session start.
- Vertical: infrastructure for a brownfield Java library.
- Affected files: `.github/workflows/verify.yml`, verification scripts, workflow contracts, and
  release evidence documentation.
- Working tree: clean at session start. `AGENTS.md` prohibits subagents.

## Decisions

| # | Decision | Reason | Task |
| --- | --- | --- | --- |
| 1 | Separate fast PR evidence from full release evidence | The 660 seconds of measured load time cannot fit in a three-minute gate. | 2 |
| 2 | Cache dependencies and pinned source, not generated output | This saves setup time without permitting stale graphs or binaries. | 2 |
| 3 | Cancel only superseded pull-request runs | Release and scheduled evidence must run to completion. | 2 |

## Session Log

- 2026-08-07 07:29 UTC: Started the refactor from the accepted CI proposal.
- 2026-08-07 07:29 UTC: Spec evaluation passed after event routing and cache boundaries were made
  explicit. Task 1 is in progress before test changes.
- 2026-08-07 07:33 UTC: The new workflow contract failed because the old workflow had no scheduled
  full gate. This is the expected test-first failure. Task 1 is complete and Task 2 is in progress.
- 2026-08-07 07:36 UTC: Added the fast wrapper, opt-in Mega build reuse, cached PR job, full-gate
  event routing, and PR-only cancellation. Both workflow contracts pass. Task 2 is complete and
  Task 3 is in progress.
