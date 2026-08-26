# Implementation: Release Gate Timeout Budget

## Summary

Implementation is pending.

## Phase 1 Context Summary

- Config: SpecOps defaults; no `.specops.json`; no task tracker.
- Context recovery: none; all prior specifications are complete.
- Steering directory: verified; stale repo map refreshed from 215 discovered files.
- Memory directory: verified; release-gate integrity pattern loaded.
- Vertical: infrastructure for a Java library release workflow.
- Affected files: workflow timeout, budget contract, and release output capture helper and contract.
- Project state: brownfield.
- Scope assessment: one coupled timeout-and-contract fix; no decomposition.

## Phase 2 Completion Summary

- Version 1 tried a 90-minute clean release bound. The hosted job still timed out.
- Version 2 requires each hosted job to finish in three minutes.
- The design runs core, Mega, PetClinic, and PostgreSQL work in parallel.
- The optional clean release command keeps the long load proof outside required CI.
- Dependencies `ci-isolated-maven-repository` and `fast-pr-ci-pipeline` are complete.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Use a 90-minute bounded release budget. | Current runs reach 60 minutes; 90 preserves a bound and allows corpus growth and cold-run variance. | 1 | 2026-08-11T12:21:03Z |
| 2 | Stream through a POSIX FIFO and preserve both process statuses. | The 90-minute run exposed no stage output, so another timeout increase would be guesswork. | 1 | 2026-08-11T14:22:09Z |
| 3 | Replace the serial release job with four parallel three-minute jobs. | A 600-second test cannot fit in three minutes, and the user set a hard three-minute limit. | 1 | 2026-08-12T07:09:21Z |
| 4 | Move standalone viewer verification to a sixth parallel job. | Remote evidence shows that the independent 50-second viewer gate leaves too little time for the PostgreSQL browser journey. | 1 | 2026-08-26T20:00:00Z |

## Verification

- Updated minimum against old workflow: expected failure at 60 minutes.
- Focused budget contract: PASS with `timeout_minutes=90`.
- Fast workflow routing contract: PASS.
- Full local pull-request gate: PASS.
- Short load: 5,000 decisions, zero errors, mismatches, drops, or contamination.
- Mega: five complete graphs from 420 source files.
- Spring PetClinic: three complete business graphs from 30 source files.
- PR #25 hosted PR and PostgreSQL checks: PASS.
- PR #25 merged at `c5ff6933bdd210e578eba4f9125ef28df0db97e9`.
- Main run `31491965409`: PostgreSQL passed; release command canceled at 90 minutes with no buffered output.
- Streaming regression: failed before the helper change and passed after it.
- Exact failure propagation, timeout budget, and event routing contracts: PASS.
- Full local pull-request gate after streaming fix: PASS with five complete Mega graphs, three
  complete PetClinic business graphs, and zero short-load correctness failures.
- Final live-output diagnosis and `main` release completion: pending.
- Version 2 maximum-budget contract against old workflow: expected failure because `pr-gate` used
  10 minutes.
- Version 2 focused budget and workflow contracts: PASS with four three-minute jobs.
- Workflow YAML parse: PASS.
- Local core verification: PASS in 66.98 seconds with 5,000 short-load decisions and zero errors,
  mismatches, drops, or contamination.
- Local Mega unit from a clean Fachtracing build: PASS in 28.20 seconds with five complete graphs
  from 420 source files.
- Local PetClinic unit from a clean Fachtracing build: PASS in 15.91 seconds with three complete
  business graphs from 30 source files.
- Hosted PR and final `main` timing proof: pending.
- PR run `33007667194`: five jobs completed, but PostgreSQL was cancelled during the browser
  journey after the independent viewer gate consumed 50 seconds.
- Version 3 focused budget, workflow routing, and repository integrity contracts: PASS with six
  parallel three-minute jobs.
