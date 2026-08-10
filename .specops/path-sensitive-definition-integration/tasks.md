# Implementation Tasks: Path-Sensitive Definition Integration

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| explainable-relevance-and-polymorphic-dispatch | Supplies the reaching-definition implementation. | Yes | completed |
| implicit-field-local-callback-correctness | Supplies callback and alias fixes. | Yes | completed |

## Task Breakdown

### Task 1: Integrate and correct analyzer data flow

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Resolve the analyzer conflicts, add a regression fixture, keep use-site initializers, and verify all
contracts from both PRs.

**Implementation Steps:**

1. Add the failing conditional-initializer regression contract.
2. Merge the analyzer changes from PR #12 and PR #15 by responsibility.
3. Remove the method-wide initializer filter and retain use-site reaching definitions.
4. Regenerate derived SpecOps files after conflict resolution.
5. Run the focused analyzer contract and the full PR gate.

**Acceptance Criteria:**

- [x] The conditional initializer is in the graph.
- [x] A fully overwritten initializer stays out of the graph.
- [x] Callback, failure-path, audit, and dispatch contracts pass.
- [x] No merge conflict markers remain.
- [x] The full PR verification gate passes.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DependencyGraphBuilder.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/ReachingDefinitionIndex.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/BackwardDecisionSlicer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `fachtracing-engine/src/test/resources/fixtures/slicing/ResultSlicePolicy.java`
- `.specops/index.json`
- `.specops/memory/context.md`
- `.specops/memory/decisions.json`
- `.specops/memory/patterns.json`
- `.specops/steering/repo-map.md`

**Tests Required:**

- [x] Focused `StaticDecisionAnalyzerTest` contract
- [x] `./scripts/verify-pr.sh`
- [x] `git diff --check`

## Implementation Order

1. Task 1

## Progress Tracking

- Total Tasks: 1
- Completed: 1
- In Progress: 0
- Blocked: 0
- Pending: 0
