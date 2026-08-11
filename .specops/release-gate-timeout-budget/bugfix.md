# Bug Fix: Release Gate Timeout Budget

## Overview

The required `main` release gate is canceled at its 60-minute job limit before the clean release
command can finish. The gate has grown since the 60-minute limit was set. It now includes pinned
Spring PetClinic verification in addition to clean builds, external activation, Mega conformance,
and the 600-second load proof.

## Root Cause Analysis

The workflow still uses the 60-minute limit introduced when the cold release gate needed about 35
to 45 minutes. The current clean gate has more required work. Run `31371139242` and the following
nightly runs reached the job limit and ended as canceled. The last output had no test failure before
GitHub stopped the job.

The focused budget contract accepted any limit of at least 50 minutes, so it protected the obsolete
budget instead of the current required gate. A 90-minute remediation run then reached its new limit.
The release output helper buffered all command output until completion, so GitHub showed no stage
progress and the canceled run could not identify the blocking sub-gate.

## Impact Assessment

- Severity: High. The repository cannot produce complete required release evidence on `main`.
- Product behavior: Unchanged. This defect affects only the hosted release-job limit.
- Data and security: Unchanged. No credential, permission, or runtime dependency changes.

## Regression Risk Analysis

### Blast Radius

- `.github/workflows/verify.yml`: Sets the upper bound for non-PR release runs.
- `scripts/test-release-workflow-budget.sh`: Enforces the minimum accepted bound.
- `scripts/capture-gate-output.sh`: Captures evidence but currently hides all live progress.
- `scripts/test-capture-gate-output.sh`: Protects exact failure propagation and live output.
- `scripts/verify.sh`: Runs the budget contract in local and PR verification.
- `scripts/test-fast-pr-workflow.sh`: Protects event routing and the unchanged release command.

### Behavior Inventory and Risk Tier

| Behavior | Risk | Required evidence |
| --- | --- | --- |
| Release runs stay bounded | Must-Test | Focused budget contract reads 90 minutes |
| A future limit below 90 minutes fails | Must-Test | Negative focused contract |
| PR and release event routing stays unchanged | Must-Test | Fast workflow contract |
| Functional and conformance behavior stays unchanged | Nice-To-Test | Full PR gate |
| Release output streams without hiding failures | Must-Test | Streaming and exit-status contract |

## Proposed Fix

Set the release job limit and its focused minimum contract to 90 minutes. Stream captured release
output through a FIFO while retaining the producer and `tee` statuses. Do not change the release
command, gate content, event routing, concurrency, permissions, or PR timeout.

## Testing Plan

### Current Behavior

- Set the minimum contract to 90 while the workflow remains at 60 and confirm that it fails.

### Expected Behavior

- Confirm that the focused contract reports `timeout_minutes=90` after the workflow update.
- Confirm that the final `main` release job can run past the old 60-minute limit and complete.

### Unchanged Behavior

- Run the fast workflow contract.
- Run the full pull-request gate, including both pinned corpora and the short load test.
- Confirm the hosted PostgreSQL and pull-request gates pass.
- Confirm live evidence reaches both the terminal and evidence file before the producer exits.
- Confirm a producer failure still exits with the exact producer status.

## Acceptance Criteria

- [ ] THE RELEASE JOB SHALL have a bounded 90-minute timeout.
- [ ] THE BUDGET CONTRACT SHALL reject a release timeout below 90 minutes.
- [ ] WHEN standard verification runs THE SYSTEM SHALL accept the 90-minute workflow budget.
- [ ] THE SYSTEM SHALL keep PR routing, release routing, permissions, and release commands unchanged.
- [ ] THE SYSTEM SHALL pass local and hosted pull-request checks.
- [ ] WHILE the release command runs THE SYSTEM SHALL stream the same output to the job log and evidence file.
- [ ] IF the release command fails THEN THE SYSTEM SHALL return its exact nonzero status.
- [ ] WHEN the fix reaches `main` THE RELEASE JOB SHALL complete without the old timeout cancellation.

## Scope Assessment

This is one infrastructure bug fix. The workflow limit and its contract are one coupled change. No
decomposition is needed.
