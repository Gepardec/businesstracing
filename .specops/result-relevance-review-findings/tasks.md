# Implementation Tasks: Result Relevance Review Findings

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `explainable-relevance-and-polymorphic-dispatch` | Supplies the relevance and audit feature corrected here. | Yes | Completed |

## Dependency Resolution Log

| Blocker | Resolution Type | Resolution Detail | Date |
| --- | --- | --- | --- |
| None | completed | Required spec is complete. | 2026-08-07 |

## Task Breakdown

### Task 1: Correct use-site relevance and audit classification

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add regression contracts for all three review findings. Then add use-site definition selection, caught-throw filtering, and gap-aware exclusion audit.

**Implementation Steps:**

1. Add failing contracts for overwritten definitions, caught result-independent throws, and exclusive gap decisions.
2. Create `ReachingDefinitionIndex` and connect it to the dependency builder and slicer.
3. Filter caught throws from terminal slice sinks.
4. Compute unresolved result effects before exclusion audit and pass the unresolved tree boundary to the auditor.
5. Run focused, pinned Mega, and full pull-request verification.

**Acceptance Criteria:**

- [x] An overwritten assignment and its helper dependencies do not enter the graph.
- [x] All definitions that can reach a result from alternative branches remain in the graph.
- [x] A caught result-independent throw does not create a business path.
- [x] An escaping throw remains a terminal failure path.
- [x] An unresolved result-effect tree has a gap decision and no no-result-effect decision.
- [x] Other irrelevant resolved calls still get exclusion decisions.
- [x] Production classes keep one responsibility.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/ReachingDefinitionIndex.java` (new)
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/CaughtThrowResolver.java` (new)
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DependencyGraphBuilder.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/BackwardDecisionSlicer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisDecisionAuditor.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/test/resources/fixtures/slicing/ResultSlicePolicy.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `docs/supported-java-constructs.md`

**Tests Required:**

- [x] Focused `StaticDecisionAnalyzerTest` contract passes.
- [x] Pinned Mega graph and runtime conformance pass.
- [x] Full pull-request verification passes.
- [x] `git diff --check` passes.

## Implementation Order

1. Task 1

## Progress Tracking

- Total Tasks: 1
- Completed: 1
- In Progress: 0
- Blocked: 0
- Pending: 0
