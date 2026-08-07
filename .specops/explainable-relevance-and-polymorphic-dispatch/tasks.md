# Implementation Tasks: Explainable Relevance and Polymorphic Dispatch

## Task Breakdown

### Task 1: Add the relevance policy and audit model

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add the expression-bounded relevance policy and the immutable analysis-decision model.

**Acceptance Criteria:**

- [x] Descendants of sliced expressions remain relevant.
- [x] Unrelated descendants of sliced control statements are not relevant.
- [x] Old `AnalysisManifest` constructor calls remain valid.

### Task 2: Record included, excluded, and gap decisions

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add the source auditor and collect immutable decisions in the graph builder.

**Acceptance Criteria:**

- [x] Every source-derived node has an `INCLUDED` decision.
- [x] Each coverage-gap node has a `GAP` decision.
- [x] An irrelevant construct has one source-mapped `EXCLUDED` decision.
- [x] Audit data is absent from the business graph and runtime activation payload.

### Task 3: Explain polymorphic candidate selection

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 2
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Record decisions for concrete, abstract, and receiver-incompatible source-visible contract subtypes.

**Acceptance Criteria:**

- [x] Concrete compatible implementations remain dispatch alternatives.
- [x] Abstract implementations are excluded with an exact reason.
- [x] Receiver-incompatible contract subtypes are excluded with an exact reason.
- [x] Runtime dispatch targets still bind implementation entries to static edges.

### Task 4: Add contracts, documentation, and full verification

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Tasks 1-3
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add executable contracts and developer documentation. Run all required checks.

**Acceptance Criteria:**

- [x] Result-slice and polymorphism contracts pass.
- [x] The analysis manifest contract covers defensive copying and compatibility.
- [x] Developer documentation explains static candidates and runtime selection.
- [x] The complete repository verification passes.

### Task 5: Correct branch definitions and read-only enum effects

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Tasks 1-4
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Correct the strict relevance policy after the PR gate exposed two false gaps and missing branch paths.

**Acceptance Criteria:**

- [x] Final Java `Enum` queries do not create possible-mutation gaps.
- [x] Every result-relevant branch assignment remains in the graph.
- [x] A terminal source `throw` remains in the graph.
- [x] The pinned Mega graph stays complete and matches each reviewed topology.

## Implementation Order

1. Task 1
2. Task 2
3. Task 3
4. Task 4
5. Task 5

## Progress Tracking

- Total Tasks: 5
- Completed: 5
- In Progress: 0
- Blocked: 0
- Pending: 0
