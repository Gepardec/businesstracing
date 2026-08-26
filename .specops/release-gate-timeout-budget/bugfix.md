# Bug Fix: Three-Minute CI Budget

## Overview

The required CI does not give timely feedback. The PostgreSQL job was cancelled during its browser
journey because it ran storage, standalone viewer verification, dogfood generation, and browser
integration in one job. All required CI jobs must finish within a three-minute execution budget.

## Root Cause Analysis

The PostgreSQL job runs one independent viewer unit and build gate before its database and dogfood
browser integration. The remote run reached the browser journey after 2 minutes 26 seconds and was
cancelled before the journey completed. The viewer gate does not need PostgreSQL and can run in
parallel.

The timeout contract checked for a minimum timeout. It allowed slow jobs instead of rejecting them.

## Impact Assessment

- Severity: High. CI feedback is too slow and the release job cannot complete.
- Product behavior: Unchanged. This fix changes only CI scheduling and budgets.
- Data and security: Unchanged.

## Regression Risk Analysis

### Blast Radius

- `.github/workflows/verify.yml`: Splits required work into parallel jobs.
- `scripts/test-release-workflow-budget.sh`: Enforces the maximum job budget.
- `scripts/test-fast-pr-workflow.sh`: Enforces parallel gate content and event routing.
- `scripts/capture-gate-output.sh`: Keeps useful live output for manual release evidence.
- Release documentation: Separates required CI from optional long evidence.

### Behavior Inventory and Risk Tier

| Behavior | Risk | Required evidence |
| --- | --- | --- |
| Each required job has a three-minute limit | Must-Test | Workflow budget contract |
| The required workflow has no long release command | Must-Test | Workflow routing contract |
| Core, Mega, PetClinic, and PostgreSQL checks run | Must-Test | Local and hosted checks |
| The long load command stays available for manual evidence | Nice-To-Test | Script and documentation review |
| Captured output streams and keeps exact failures | Must-Test | Output helper contract |

## Proposed Fix

Run the core suite, Mega Backend conformance, Spring PetClinic conformance, Jakarta EE conformance,
viewer verification, and PostgreSQL integration as independent parallel jobs for every workflow
event. Give each job a three-minute timeout. Keep dogfood and the database browser journey together
in the PostgreSQL job, and move the independent viewer gate to its own job.

## Testing Plan

### Current Behavior

- Change the budget contract to a maximum of three minutes and confirm that the current workflow
  fails because its jobs use 10, 15, and 90 minutes.

### Expected Behavior

- Confirm that every required job has `timeout-minutes: 3`.
- Confirm that all four jobs start independently for pull requests and release events.
- Confirm that no required job calls `verify-release.sh` or the 600-second load test.

### Unchanged Behavior

- Run the complete core suite.
- Run both pinned conformance suites.
- Run the PostgreSQL contract in GitHub Actions.
- Confirm that the optional release evidence command remains executable.
- Confirm that captured output streams and returns the exact producer status.

## Acceptance Criteria

- [ ] THE WORKFLOW SHALL give each required job a three-minute timeout.
- [ ] THE BUDGET CONTRACT SHALL reject a required job timeout above three minutes.
- [ ] THE WORKFLOW SHALL run core, Mega, PetClinic, Jakarta EE, viewer, and PostgreSQL checks in
  parallel.
- [ ] THE WORKFLOW SHALL run the same required jobs for pull requests and release events.
- [ ] THE REQUIRED WORKFLOW SHALL NOT call the 600-second release gate.
- [ ] THE OPTIONAL RELEASE COMMAND SHALL remain available for manual long evidence.
- [ ] THE SYSTEM SHALL pass local and hosted checks.
- [ ] EACH HOSTED REQUIRED JOB SHALL complete within its three-minute execution limit.

## Scope Assessment

This is one CI budget fix. Scheduling, job limits, and their contracts are one coupled change. No
decomposition is needed.
