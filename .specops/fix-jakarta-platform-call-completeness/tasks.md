# Implementation Tasks: Jakarta platform-call completeness

## Task Breakdown

### Task 1: Add the Jakarta platform regression test and classifier fix

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None
**Documentation Required:** Add the supported Jakarta platform operation to the Java capability contract.
**Breaking Change:** No

**Description:**

Add one compiled Jakarta response fixture test, prove the current failure, then extend the platform operation classification.

**Implementation Steps:**

1. Add a contract test that compiles `jakarta.ws.rs.core.Response` without giving its source to the analyzer.
2. Assert that the Jakarta response-chain graph is complete and keeps its predicate.
3. Run the test to confirm the current failure.
4. Add the Jakarta namespace to the existing platform classifier.
5. Run analyzer and repository verification.

**Acceptance Criteria:**

- [x] The test reproduces the current false incomplete result.
- [x] Nested Jakarta response calls do not create coverage gaps.
- [x] The business predicate remains in the graph.
- [x] Unsupported application binary calls remain incomplete.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `docs/java-capabilities.json`
- `docs/supported-java-constructs.md`

**Tests Required:**

- [x] `StaticDecisionAnalyzerTest`
- [x] Full Maven test suite
- [x] Pull-request verification gate

## Implementation Order

1. Task 1

## Progress Tracking

- Total Tasks: 1
- Completed: 1
- In Progress: 0
- Blocked: 0
- Pending: 0
