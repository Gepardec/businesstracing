# Tasks: Three-Minute CI Budget

## Task 1: Parallelize and bound required CI

**Status:** In Progress
**Priority:** High
**Effort:** M
**Dependencies:** None
**IssueID:** None

**Description:** Make required CI finish within explicit job-specific execution budgets.

**Implementation Steps:**

1. Make the budget contract reject every timeout above three minutes and prove the old workflow
   fails.
2. Split core, Mega, PetClinic, Jakarta EE, viewer, and PostgreSQL work into six parallel jobs.
3. Set five job limits to three minutes and PostgreSQL to five minutes. Keep required work off the
   long release command.
4. Update the workflow contract and release documentation.
5. Run focused and full local checks.
6. Commit, push, update PR #26, and verify hosted jobs.
7. Merge the PR and verify the final `main` workflow.

**Acceptance Criteria:**

- [x] The old workflow fails the maximum budget contract.
- [x] Five required jobs have a three-minute timeout and PostgreSQL has a five-minute timeout.
- [x] Required work runs in parallel for all workflow events.
- [x] The hosted workflow does not call the long release gate.
- [x] Local core and conformance checks pass.
- [x] Hosted PR jobs pass within their configured limits.
- [ ] The final `main` jobs pass within their configured limits.

## Progress

- Total: 1
- Completed: 0
- In Progress: 1
- Pending: 0
- Blocked: 0
