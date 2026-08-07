# Tasks: JDK Mutation and Alias Effect Correctness

## Task 1: Freeze the regressions

**Status:** Completed
**Priority:** High
**Dependencies:** None

- [x] Add independent `Deque.offer` and direct local alias fixtures.
- [x] Add an unknown JDK effect and alias reassignment safety contract.
- [x] Run the focused test against current production code and record both failures.

## Task 2: Correct the generic effect model

**Status:** Completed
**Priority:** High
**Dependencies:** Task 1

- [x] Expand proved platform mutation contracts.
- [x] Replace namespace purity with explicit proved read-only contracts.
- [x] Track and invalidate direct local aliases in source mutation summaries.
- [x] Pass all focused analyzer contracts.

## Task 3: Verify and release

**Status:** Completed
**Priority:** High
**Dependencies:** Task 2

- [x] Update capability, memory, repository map, and release evidence documents.
- [x] Run standard, external, Mega, and 600-second gates.
- [x] Complete the SpecOps record, commit all changes, and push the PR branch.

## Progress

- Total: 3
- Completed: 3
- In Progress: 0
- Pending: 0
- Blocked: 0
