# Bug Fix: Preserve Path-Sensitive Definitions During PR Integration

## Problem Statement

The open analyzer PRs compute local definitions in different ways. Their overlap can omit an
initializer when only some control-flow paths overwrite the local. The exported decision graph
then excludes a source expression that can determine the returned result.

## Root Cause Analysis

`ReachingDefinitionIndex` computes the correct reaching definitions for each use site, but its
snapshot step removes every initializer for a name that is assigned anywhere in the method. That
global filter discards an initializer even when one branch reaches the return without an
assignment. The alternative branch-merge implementation in PR #15 also selects one scalar
definition instead of retaining all reachable scalar definitions.

**Affected Components:**

- `ReachingDefinitionIndex` and `BackwardDecisionSlicer`
- The analyzer integration of PR #12 and PR #15
- `StaticDecisionAnalyzerTest` and the result-slicing fixture

**Error Symptoms:**

- A fallback initializer can be absent from the graph.
- A test can accept the omission because it checks only branch assignments.

## Impact Assessment

- **Severity:** Medium
- **Users Affected:** Consumers that analyze a local with a conditional overwrite
- **Frequency:** Sometimes

## Dependencies & Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| explainable-relevance-and-polymorphic-dispatch | It adds the reaching-definition index. | Yes | completed |
| implicit-field-local-callback-correctness | It adds callback and alias fixes that the integration must preserve. | Yes | completed |

### Cross-Spec Blockers

| Blocker | Blocking Spec | Resolution Type | Resolution Detail | Status |
| --- | --- | --- | --- | --- |
| None | — | — | — | resolved |

## Reproduction Steps

1. Initialize a local with a result-relevant method call.
2. Overwrite the local in only one branch.
3. Return the local.
4. Expected: the graph contains the initializer and the branch assignment.
5. Actual: the initializer is removed from the reaching-definition snapshot.

## Regression Risk Analysis

### Blast Radius

- `DependencyGraphBuilder` creates dependency data for every analyzed method.
- `BackwardDecisionSlicer` consumes the reaching definitions for result slicing.
- `StaticDecisionAnalyzer` uses the slice for graph inclusion, callback effects, throws, and audit decisions.

### Behavior Inventory

- A definition that is overwritten on all paths stays excluded.
- Branch-dependent reference aliases and cast or local method-reference callbacks stay supported.
- Uncaught failure paths, relevance audit entries, and dispatch candidate audit entries stay supported.

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| Conditional initializer retention | Must-Test | It is the defective path. |
| Fully overwritten definition exclusion | Must-Test | Removing the global filter must not restore dead definitions. |
| Callback and method-reference effects | Must-Test | PR #15 changes the same dependency builder. |
| Failure and analysis audit behavior | Must-Test | PR #12 changes the same slicer and analyzer. |

## Proposed Fix

Keep initializer definitions in the flow state and let path-sensitive assignment and state merging
remove only definitions that no longer reach a use. Combine this model with the callback-definition
data from PR #15. Regenerate derived SpecOps index and memory files after conflict resolution.

## Unchanged Behavior

- WHEN every path overwrites an initializer THE SYSTEM SHALL CONTINUE TO exclude the initializer.
- WHEN a callback mutates result-relevant state THE SYSTEM SHALL CONTINUE TO include the effect or report a coverage gap.
- WHEN an uncaught throw can terminate the decision THE SYSTEM SHALL CONTINUE TO include its failure path.

## Testing Plan

### Current Behavior

- WHEN only one branch overwrites a result local THE SYSTEM CURRENTLY omits its fallback initializer.

### Expected Behavior

- WHEN one path keeps the initializer THE SYSTEM SHALL include the initializer and all reaching branch definitions.

### Unchanged Behavior

- WHEN every path overwrites the local THE SYSTEM SHALL CONTINUE TO exclude the dead initializer.
- WHEN callback, failure-path, relevance-audit, and dispatch tests run THE SYSTEM SHALL CONTINUE TO pass them.

## Acceptance Criteria

- [x] Regression risk analysis is complete for the medium-severity defect.
- [x] A regression test fails before the fix and passes after the fix.
- [x] Path-sensitive initializer retention is correct.
- [x] The PR #12 and PR #15 analyzer contracts pass together.
- [x] The repository PR verification gate passes.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep each component responsible for one concern.
