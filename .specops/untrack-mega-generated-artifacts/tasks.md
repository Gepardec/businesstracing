# Tasks: Untrack Mega Generated Artifacts

## Task 1: Define and guard the artifact boundary

**Status:** Completed
**Priority:** Medium
**Effort:** S
**Dependencies:** None
**IssueID:** None

- [x] Confirm that generated files are outputs and reviewed oracles are inputs.
- [x] Add a repository check that rejects tracked files in the old output path.

## Task 2: Move reproducible output out of Git

**Status:** Completed
**Priority:** Medium
**Effort:** S
**Dependencies:** Task 1
**IssueID:** None

- [x] Write Mega artifacts under `target/generated`.
- [x] Remove the 18 generated files from Git.
- [x] Update conformance documentation.

## Task 3: Verify, record, commit, and push

**Status:** Completed
**Priority:** Medium
**Effort:** S
**Dependencies:** Task 2
**IssueID:** None

- [x] Run repository integrity, Mega conformance, and the pull-request gate.
- [x] Complete SpecOps records and repository memory.
- [x] Commit and push the complete refactor.

## Progress

- Total: 3
- Completed: 3
- In Progress: 0
- Pending: 0
- Blocked: 0
