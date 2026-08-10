# Tasks: Unified Developer Graph Contract

## Task 1: Freeze the single-contract behavior

**Status:** Completed
**Priority:** High
**Effort:** S
**Dependencies:** None
**IssueID:** None

- [x] Change consumer contracts to require the multi-origin V1 schema and document shape.
- [x] Require both exporter overloads and both Maven source modes to use the same V1 contract.
- [x] Run the focused contract against the current implementation and record the expected failure.

## Task 2: Remove the dual-contract implementation

**Status:** Completed
**Priority:** High
**Effort:** M
**Dependencies:** Task 1
**IssueID:** None

- [x] Route all developer JSON through one SourceCatalog serializer.
- [x] Reduce schema generation to `generate()` and one V1 definition set.
- [x] Reduce Maven output to one current schema filename with exact legacy cleanup.
- [x] Update README and Maven plugin documentation.
- [x] Run focused and complete verification.

## Progress

- Total: 2
- Completed: 2
- In Progress: 0
- Pending: 0
- Blocked: 0
