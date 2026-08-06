# Tasks: CI Isolated Maven Repository

## Task 1: Add and adopt the repository resolver

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** None
**Priority:** High
**IssueID:** PR #5 release-gate

**Description:** Add one resolver and replace hard-coded Maven dependency paths in all verification
scripts.

**Acceptance Criteria:**

- [x] Explicit and release overrides have stable precedence.
- [x] Default local use stays compatible.
- [x] Standard, Mega, PostgreSQL, and release load classpaths use the resolved path.

## Task 2: Add the isolated-repository regression contract

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 1
**Priority:** High
**IssueID:** PR #5 release-gate

**Description:** Add a network-free shell contract and run it from standard verification.

**Acceptance Criteria:**

- [x] The test proves explicit override, release override, and default resolution.
- [x] The test rejects direct home-repository dependency paths in consumer scripts.

## Task 3: Verify and update PR #5

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Tasks 1 and 2
**Priority:** High
**IssueID:** PR #5 release-gate

**Description:** Run focused, standard, and clean release verification. Commit and push the fix,
then monitor the pull-request checks.

**Acceptance Criteria:**

- [x] Local verification passes.
- [x] The clean release gate passes.
- [x] The fix is committed and ready to push.
- [x] PR check monitoring is part of the active handoff after the push.

## Task 4: Correct the hosted release-job time budget

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 3
**Priority:** High
**IssueID:** PR #5 release-gate run 31086887346

**Description:** Increase the bounded release-job timeout and add a focused regression contract
after GitHub cancels the correct release command at the old 35-minute limit.

**Acceptance Criteria:**

- [x] The release job has a 60-minute upper bound.
- [x] Standard verification rejects a release timeout below 50 minutes.
- [x] The new focused contract passes.
- [x] PR #5 receives the pushed correction and the new checks are monitored.
