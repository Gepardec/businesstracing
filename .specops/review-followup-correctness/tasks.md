# Implementation Tasks: Review Follow-up Correctness

## Task 1: Freeze the four review regressions

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Acceptance Criteria:**

- [x] The analyzer suite reproduces the three false-complete or missing-effect results.
- [x] The protocol suite reproduces the shortened graceful-drain window.

**Files to Modify:**

- `fachtracing-engine/src/test/resources/fixtures/slicing/ResultSlicePolicy.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/store/DecisionRecordProtocolTest.java`

---

## Task 2: Correct analyzer definition and callback proof

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Acceptance Criteria:**

- [x] Branch definitions preserve each reachable value.
- [x] Wrapped callbacks preserve mutation transfers.
- [x] Mutating Boolean predicate callbacks create a source-located gap.
- [x] Existing alias and method-reference contracts pass.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/LocalDefinitionResolver.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DependencyGraphBuilder.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/BackwardDecisionSlicer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`

---

## Task 3: Preserve the graceful shutdown window

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Acceptance Criteria:**

- [x] A cooperative save can finish after half of a long shutdown bound.
- [x] An uncooperative save still stops inside a short shutdown bound.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/DecisionRecordDelivery.java`

---

## Task 4: Verify and publish

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Tasks 2 and 3
**Priority:** High
**IssueID:** None
**Blocker:** None

**Acceptance Criteria:**

- [x] Documentation and capability contracts match the final support boundary.
- [x] Full local pull-request verification passes.
- [x] Changes are committed and pushed to draft PR #15.
- [x] Required hosted checks pass.

## Progress

- Total: 4
- Completed: 4
- In Progress: 0
- Pending: 0
- Blocked: 0
