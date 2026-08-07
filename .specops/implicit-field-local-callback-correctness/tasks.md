# Tasks: Implicit Field and Local Callback Correctness

## Task 1: Freeze both regressions

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** None
**Priority:** High

**Acceptance Criteria:**

- [x] The implicit-field alias regression fails before production changes.
- [x] The local-callback regression fails before production changes.

## Task 2: Preserve attributed field roots

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 1
**Priority:** High

**Acceptance Criteria:**

- [x] Enclosing field names are supplied to dependency construction.
- [x] Conditional joins preserve implicit field definitions.

## Task 3: Resolve callbacks stored in locals

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** High

**Acceptance Criteria:**

- [x] Callback identifiers resolve against active definitions at the use site.
- [x] The mutation transfer remains in the graph.
- [x] A mutating Boolean predicate callback reports an incomplete source-located gap.

## Task 4: Verify and publish

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Tasks 2 and 3
**Priority:** High

**Acceptance Criteria:**

- [x] Focused and full local checks pass.
- [x] The fix is pushed to PR #15.
- [x] Required hosted checks pass.

## Progress

- Total: 4
- Completed: 4
- In Progress: 0
- Pending: 0
- Blocked: 0
