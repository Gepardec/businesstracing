# Tasks: Release Gate Timeout Budget

## Task 1: Correct and verify the release time budget

**Status:** In Progress
**Priority:** High
**Effort:** S
**Dependencies:** None
**IssueID:** None

**Description:** Align the hosted release timeout and its regression contract with the current
clean release workload.

**Implementation Steps:**

1. Raise the focused minimum and prove that the old workflow fails.
2. Raise the release job timeout to 90 minutes.
3. Run focused, full local, and hosted checks.
4. Commit, push, open a focused PR, merge it, and monitor the final `main` release gate.
5. Stream the captured release output while preserving exact failure status after the 90-minute
   run proves that the timeout increase alone is insufficient.

**Acceptance Criteria:**

- [ ] The old 60-minute workflow fails the updated focused contract.
- [ ] The 90-minute workflow passes the focused contract.
- [ ] Event routing and release commands remain unchanged.
- [ ] The full pull-request gate passes.
- [ ] Hosted PR and PostgreSQL checks pass.
- [ ] The final `main` release gate completes.
- [ ] Release output reaches the terminal and evidence file before producer completion.
- [ ] Producer failures retain their exact exit status.

## Progress

- Total: 1
- Completed: 0
- In Progress: 1
- Pending: 0
- Blocked: 0
