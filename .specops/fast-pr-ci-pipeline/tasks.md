# Tasks: Fast Pull-Request CI Pipeline

## Task 1: Freeze workflow routing and cache contracts

**Status:** Completed
**Priority:** High
**Effort:** S
**Dependencies:** None
**IssueID:** None

- [x] Add a POSIX contract for fast PR routing, caching, cancellation, and full release routing.
- [x] Run it against the current workflow and record the failure.

## Task 2: Implement the fast cached gate

**Status:** Completed
**Priority:** High
**Effort:** M
**Dependencies:** Task 1
**IssueID:** None

- [x] Add `verify-pr.sh` and keep all short functional and conformance checks.
- [x] Let Mega reuse an already completed root build only through an explicit input.
- [x] Split pull-request and full-release workflow jobs.
- [x] Add Maven cache, pinned Mega cache, event routing, schedule, tags, and PR cancellation.

## Task 3: Verify, document, commit, and push

**Status:** In Progress
**Priority:** High
**Effort:** M
**Dependencies:** Task 2
**IssueID:** None

- [ ] Run workflow contracts, standard verification, fast gate, and Mega conformance.
- [ ] Update release evidence guidance and SpecOps records.
- [ ] Commit and push all changes to PR #5.

## Progress

- Total: 3
- Completed: 2
- In Progress: 1
- Pending: 2
- Blocked: 0
