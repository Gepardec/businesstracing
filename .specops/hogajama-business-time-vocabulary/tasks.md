# Implementation Tasks: Hogajama business time vocabulary

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `generic-business-graph-projection` | It supplies the guard and projection contract. | Yes | completed |

## Task Breakdown

### Task 1: Correct structural marker validation

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** `generic-business-graph-projection`
**Priority:** High
**IssueID:** None
**Blocker:** None
**Documentation Required:** Review the business artifact contract.
**Breaking Change:** No

**Description:**

Add the missing acceptance contract, prove the false positive, and narrow only the structural
`start` and `stop` rules.

**Implementation Steps:**

1. Add accepted compound business phrases to `BusinessGraphProjectionTest`.
2. Run the focused contract and record the expected failure.
3. Anchor the `start` and `stop` guard patterns.
4. Run focused, full, and strict Hogajama integration checks.

**Acceptance Criteria:**

- [x] A pre-fix focused test rejects the valid compound phrase.
- [x] Compound business phrases that contain `start` or `stop` pass.
- [x] Exact structural markers and the existing prohibited matrix fail closed.
- [x] The combined strict Hogajama reactor generates both aggregate graphs.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessLogicArtifactGuard.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/business/BusinessGraphProjectionTest.java`

**Tests Required:**

- [x] Focused `BusinessGraphProjectionTest` before and after the fix.
- [x] `./scripts/verify-pr.sh`.
- [x] Strict real Hogajama reactor on the combined changes.

## Implementation Order

1. Task 1

## Progress Tracking

- Total Tasks: 1
- Completed: 1
- In Progress: 0
- Blocked: 0
- Pending: 0
