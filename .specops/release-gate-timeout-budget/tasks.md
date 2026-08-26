# Tasks: Three-Minute CI Budget

## Task 1: Parallelize and bound required CI

**Status:** In Progress
**Priority:** High
**Effort:** M
**Dependencies:** None
**IssueID:** None

**Description:** Make all required CI work finish within a three-minute job execution budget.

**Implementation Steps:**

1. Make the budget contract reject every timeout above three minutes and prove the old workflow
   fails.
2. Split core, Mega, PetClinic, Jakarta EE, viewer, and PostgreSQL work into six independent jobs.
3. Set all six job limits to three minutes and keep required work off the long release command.
4. Update the workflow contract and release documentation.
5. Run focused and full local checks.
6. Commit, push, update PR #26, and verify hosted jobs.
7. Merge the PR and verify the final `main` workflow.

**Acceptance Criteria:**

- [ ] The old workflow fails the maximum budget contract.
- [ ] All six required jobs have a three-minute timeout.
- [ ] Required work runs in parallel for all workflow events.
- [ ] The hosted workflow does not call the long release gate.
- [ ] Local core and conformance checks pass.
- [ ] Hosted PR jobs pass within three minutes.
- [ ] The final `main` jobs pass within three minutes.

## Progress

- Total: 1
- Completed: 0
- In Progress: 1
- Pending: 0
- Blocked: 0
