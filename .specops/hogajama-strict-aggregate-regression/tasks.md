# Implementation Tasks: Hogajama strict aggregate regression

## Task Breakdown

### Task 1: Add the integrated regression and correct the analyzer contracts

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** explicit-opaque-library-boundaries; path-sensitive-definition-integration
**Priority:** High
**IssueID:** None
**Blocker:** None
**Documentation Required:** Update the supported Java construct contract if public behavior changes.
**Breaking Change:** No

**Description:**

Reproduce the three generic false gaps, correct each analyzer contract without application-specific
rules, and verify the real Hogajama aggregate goal.

**Implementation Steps:**

1. Add a compiled-source regression for caught platform calls, anonymous helper classes, and a
   generated source implementation.
2. Prove that the regression fails before the production change.
3. Align caught-call availability with supported operation evidence.
4. Bound method dependency and mutation scans to one executable body.
5. Verify generated source implementation selection in the documented same Maven invocation.
6. Add fail-closed counterexamples for unsupported binary calls and missing implementations.
7. Run the real Hogajama strict aggregate goal with explicit opaque library coordinates.
8. Run all repository gates and update SpecOps artifacts.

**Acceptance Criteria:**

- [x] The regression reproduces all false gap classes.
- [x] Both integrated decision graphs are complete.
- [x] Unsupported caught calls remain incomplete.
- [x] Missing source implementations remain incomplete.
- [x] Strict Hogajama aggregate analysis succeeds.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DependencyGraphBuilder.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `docs/java-capabilities.json`
- `docs/supported-java-constructs.md`

**Tests Required:**

- [x] `StaticDecisionAnalyzerTest`
- [x] Real Hogajama strict aggregate goal
- [x] Full pull-request verification gate

## Implementation Order

1. Task 1

## Progress Tracking

- Total Tasks: 1
- Completed: 1
- In Progress: 0
- Blocked: 0
- Pending: 0
