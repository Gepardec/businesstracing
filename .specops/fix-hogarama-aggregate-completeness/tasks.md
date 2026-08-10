# Implementation Tasks: Hogarama aggregate completeness

## Task Breakdown

### Task 1: Add archive-origin classification and the Hogarama-shaped regression contract

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None
**Documentation Required:** Add the archive reference-operation boundary to the Java capability contract and supported-construct guide.
**Breaking Change:** No

**Description:**

Add a compiled dependency-JAR fixture with both reported graph names. Prove the failure, then add exact classpath-origin classification and use it for invocation flow and receiver effects.

**Implementation Steps:**

1. Compile external query, options, collection, and Boolean rule APIs into a JAR.
2. Analyze application source that contains source-visible maximum-number, actor or sensor, and date predicates.
3. Confirm that both graphs fail before the production change.
4. Add `BinaryTypeOriginResolver`.
5. Apply the reference-operation boundary to call effects, invocations, and method references.
6. Confirm that both graphs are complete and all source predicates remain visible.
7. Confirm that the Boolean dependency rule and application class-directory rule remain incomplete.
8. Update the capability contract and supported-construct guide.
9. Run the real Hogarama reproduction and all repository gates.

**Acceptance Criteria:**

- [x] The fixture reproduces unavailable implementation, Boolean fallback, and unknown effect gaps.
- [x] The corrected fixture has two complete graphs.
- [x] Result-relevant source predicates remain visible.
- [x] Boolean dependency rules remain fail-closed.
- [x] Application class-directory rules remain fail-closed.
- [x] Strict Hogarama aggregate analysis succeeds.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/BinaryTypeOriginResolver.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `docs/java-capabilities.json`
- `docs/supported-java-constructs.md`

**Tests Required:**

- [x] `StaticDecisionAnalyzerTest`
- [x] Real Hogarama strict aggregate goal
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
