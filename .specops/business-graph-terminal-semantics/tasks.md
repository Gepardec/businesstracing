# Tasks: Business Graph Terminal Semantics

### Task 1: Implement shared Start/Stop and business identifiers

**Status:** Completed
**Priority:** High
**Dependencies:** generic-tracing-walking-skeleton and mermaid-diagram-rendering (completed)
**IssueID:** None

**Implementation Steps:**

1. Create and probe one shared root Stop node.
2. Label terminal edges with returned expressions and failures.
3. Normalize `id`/`ids` tokens from business labels.
4. Render null comparisons as absence/existence statements.
5. Update renderer snapshots, topology contracts, and Mega reviewed oracles.
6. Regenerate PlantUML/Mermaid artifacts and run full verification.

**Acceptance Criteria:**

- [x] Start and Stop occur exactly once in every generated graph.
- [x] All return and failure paths converge on Stop with explicit terminal semantics.
- [x] Business labels contain no standalone `id` or `ids` token.
- [x] Business labels contain no Java `null` token and retain meaningful absent/exists branches.
- [x] Runtime captures still preserve typed results for multiple returns.
- [x] Generic and pinned Mega suites pass with both diagram formats.
