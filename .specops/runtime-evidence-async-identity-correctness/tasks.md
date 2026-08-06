# Tasks: Runtime Evidence and Async Identity Correctness

## Task 1: Freeze the review regressions

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** PR #5 review at `810c0d5963ccd64b276b752b524c2a418364b54d`
**Blocker:** None

- Add independent fixtures for nested synchronous submission, cancellation types, future
  transparency, predicate timing, unsupported operands, encoding failure, and Java graph terms.

## Task 2: Correct async ownership and cancellation

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

- Pass submission-specific handles through transformed bytecode.
- Bind thread handles to actual thread objects.
- Track original future objects and instrument cancellation without wrappers.

## Task 3: Correct predicate evidence and completeness

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

- Move direct operand reads to predicate sites.
- Add precise runtime gaps for unavailable or unencodable required facts.

## Task 4: Remove Java vocabulary from business graphs

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** Medium
**IssueID:** None
**Blocker:** None

- Add generic normalization and guard rules.
- Regenerate and review the five Mega graph artifacts.

## Task 5: Run release conformance and close the specification

**Status:** In Progress
**Estimated Effort:** L
**Dependencies:** Tasks 2 through 4
**Priority:** High
**IssueID:** None
**Blocker:** None

- Run standard, external, Mega, database, and long-load gates.
- Update documents, memory, implementation evidence, and SpecOps status.
