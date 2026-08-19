# Implementation Tasks: Normalize nested map transfer labels

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `generic-business-graph-projection` | It supplies the business normalization boundary. | Yes | completed |

## Task Breakdown

### Task 1: Normalize and verify nested mapped transfers

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** `generic-business-graph-projection`
**Priority:** High
**IssueID:** None
**Blocker:** None
**Documentation Required:** Review the business artifact contract.
**Breaking Change:** No

**Description:**

Add a focused failing contract, implement one generic normalization rule, and generate the requested
Mega and Hogarama graph JSON files.

**Implementation Steps:**

1. Add the generic nested-transfer case to `BusinessGraphProjectionTest`.
2. Run the focused test and record the expected failure.
3. Add the narrow normalization rule.
4. Run focused and repository verification.
5. Generate and validate the pinned Mega and Hogarama business JSON files.

**Acceptance Criteria:**

- [x] The pre-fix focused test fails on the nested map label.
- [x] The post-fix focused and repository tests pass.
- [x] The generated business JSON files parse and use the V1 schema identifier.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessLanguageNormalizer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/business/BusinessGraphProjectionTest.java`
- `conformance/mega-backend/src/test/java/at/gepardec/fachtracing/conformance/MegaBackendConformanceTest.java`

**Tests Required:**

- [x] Focused `BusinessGraphProjectionTest` before and after the fix.
- [x] `./scripts/verify-pr.sh`.
- [x] Strict pinned Hogarama analysis.
- [x] Pinned Mega graph generation.

## Implementation Order

1. Task 1

## Progress Tracking

- Total Tasks: 1
- Completed: 1
- In Progress: 0
- Blocked: 0
- Pending: 0
