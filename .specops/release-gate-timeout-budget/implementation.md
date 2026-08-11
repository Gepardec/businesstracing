# Implementation: Release Gate Timeout Budget

## Summary

Implementation is pending.

## Phase 1 Context Summary

- Config: SpecOps defaults; no `.specops.json`; no task tracker.
- Context recovery: none; all prior specifications are complete.
- Steering directory: verified; stale repo map refreshed from 215 discovered files.
- Memory directory: verified; release-gate integrity pattern loaded.
- Vertical: infrastructure for a Java library release workflow.
- Affected files: `.github/workflows/verify.yml` and `scripts/test-release-workflow-budget.sh`.
- Project state: brownfield.
- Scope assessment: one coupled timeout-and-contract fix; no decomposition.

## Phase 2 Completion Summary

- Requirement: the clean release job must have a verified 90-minute bound.
- Design: change only the workflow limit and its focused minimum contract.
- Tasks: one task covers reproduction, implementation, verification, PR, merge, and final CI.
- Dependencies: `ci-isolated-maven-repository` and `fast-pr-ci-pipeline` are complete.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Use a 90-minute bounded release budget. | Current runs reach 60 minutes; 90 preserves a bound and allows corpus growth and cold-run variance. | 1 | 2026-08-11T12:21:03Z |

## Verification

- Updated minimum against old workflow: expected failure at 60 minutes.
- Focused budget contract: PASS with `timeout_minutes=90`.
- Fast workflow routing contract: PASS.
- Full local pull-request gate: PASS.
- Short load: 5,000 decisions, zero errors, mismatches, drops, or contamination.
- Mega: five complete graphs from 420 source files.
- Spring PetClinic: three complete business graphs from 30 source files.
- Hosted PR, PostgreSQL, and final `main` release checks: pending.
