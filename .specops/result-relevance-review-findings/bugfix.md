# Bug Fix: Result Relevance Review Findings

## Problem Statement

The result slicer can retain a stale overwritten assignment, treat a caught result-independent failure as a business path, and classify one unresolved call as both excluded and unresolved. These faults make the generated graph or its audit trail incorrect.

## Root Cause Analysis

`DependencyGraphBuilder` stores all assignments by local name without the use site where each assignment can reach. `BackwardDecisionSlicer` expands that flat history and seeds every `throw`, including throws that a local `catch` handles. `StaticDecisionAnalyzer` also runs exclusion audit before it identifies unresolved result effects, so the auditor can exclude the same tree that later creates a gap.

**Affected Components:**

- `DependencyGraphBuilder` and `BackwardDecisionSlicer`
- `AnalysisDecisionAuditor` and `StaticDecisionAnalyzer`

**Error Symptoms:**

- A helper call in an assignment that a later unconditional assignment overwrites enters the graph.
- A caught throw that cannot change the returned value creates a choice and an alternative-result path.
- One unresolved result effect gets both `EXCLUDED/NO_RESULT_EFFECT` and `GAP/UNRESOLVED_RELEVANCE` decisions at the same source location.

## Impact Assessment

- **Severity:** High
- **Users Affected:** Developers and business users who inspect affected graphs or manifest decisions
- **Frequency:** Every time one of the three source patterns occurs

## Dependencies & Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `explainable-relevance-and-polymorphic-dispatch` | This fix corrects the relevance and audit feature added by that spec. | Yes | Completed |

### Cross-Spec Blockers

| Blocker | Blocking Spec | Resolution Type | Resolution Detail | Status |
| --- | --- | --- | --- | --- |
| None | — | — | — | Resolved |

## Reproduction Steps

1. Analyze a method that assigns a result local from a decision helper and then unconditionally replaces that local before return.
2. Analyze a method that catches an internal throw, performs only audit work in the catch, and returns an independent parameter.
3. Analyze `unknown customer effect` in `ResultSlicePolicy`.
4. Expected: stale work and caught audit failures are excluded, and the unresolved call has only a gap decision.
5. Actual: stale work and the caught failure enter the graph, and the unresolved call also has an exclusion decision.

## Regression Risk Analysis

### Blast Radius

- Static graph extraction for local definitions, branch assignments, and failure paths
- Developer manifest audit decisions for excluded and unresolved source trees
- Pinned Mega graph topology and full pull-request verification

### Behavior Inventory

- Mutually exclusive branch assignments that can reach a return stay in the graph.
- An uncaught source throw stays as a terminal failure path.
- An unresolved result effect stays visible as a source-located coverage gap.
- Irrelevant calls still get one exclusion decision.

### Test Coverage Assessment

- **Covered:** Alternative branch assignments and uncaught throws -> `StaticDecisionAnalyzerTest.preservesEveryBranchDefinitionAndFailurePath`
- **Covered:** Unresolved result effects and manifest gaps -> `StaticDecisionAnalyzerTest.excludesIgnoredReadsAndReportsUnknownEffects` and `explainsIncludedExcludedAndGapDecisions`
- **Gap:** Sequential overwritten assignments have no contract.
- **Gap:** Caught result-independent throws have no contract.
- **Gap:** No contract forbids a gap tree from also getting an exclusion decision.

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| Keep all reachable branch definitions | Must-Test | Definition selection changes directly. |
| Keep uncaught terminal failures | Must-Test | Throw sink selection changes directly. |
| Keep unresolved effects as gaps | Must-Test | Audit order and exclusion rules change directly. |
| Keep irrelevant calls excluded | Must-Test | The auditor receives a new unresolved-tree boundary. |
| Keep pinned Mega topology | Must-Test | The analyzer change can affect all generated graphs. |

### Scope Escalation Check

**Scope:** Contained. A structured use-site definition index is sufficient. A full control-flow graph, SSA conversion, and Java exception-type solver remain outside this fix.

## Proposed Fix

Index reaching local definitions at each source use. Sequential assignments replace earlier definitions, while structured alternatives merge their definitions. Expand the backward slice from an identifier and its use tree instead of from a global name history. Seed only throws that can leave the current protected region. Identify unknown result effects before exclusion audit and prevent unresolved trees from entering an excluded subtree.

## Unchanged Behavior

- WHEN alternative branch assignments can reach a returned local, THE SYSTEM SHALL CONTINUE TO keep each assignment in the graph.
- WHEN a source throw can leave the analyzed method path, THE SYSTEM SHALL CONTINUE TO keep the terminal failure path.
- WHEN an irrelevant graph-eligible call has no unresolved result effect, THE SYSTEM SHALL CONTINUE TO record one `EXCLUDED/NO_RESULT_EFFECT` decision.
- WHEN static analysis cannot prove a result effect, THE SYSTEM SHALL CONTINUE TO create a source-located coverage gap.

## Testing Plan

### Current Behavior

- WHEN an unconditional assignment overwrites an earlier result assignment, THE SYSTEM CURRENTLY includes both definitions.
- WHEN a local catch handles a result-independent throw, THE SYSTEM CURRENTLY includes the throw path.
- WHEN a possible result effect is unresolved, THE SYSTEM CURRENTLY can record both exclusion and gap actions for the same call.

### Expected Behavior

- WHEN a later unconditional assignment overwrites a local before its return use, THE SYSTEM SHALL exclude the earlier definition and its dependencies.
- WHEN a local catch handles a throw and the try/catch has no result effect, THE SYSTEM SHALL exclude that control path.
- WHEN a tree creates an unresolved-relevance gap, THE SYSTEM SHALL not classify that tree or its containing unresolved subtree as no-result-effect.

### Unchanged Behavior

- WHEN branch definitions remain reachable, THE SYSTEM SHALL CONTINUE TO include each definition.
- WHEN a throw can escape, THE SYSTEM SHALL CONTINUE TO include its terminal failure path.
- WHEN a call is irrelevant and resolved, THE SYSTEM SHALL CONTINUE TO record one exclusion decision.
- WHEN the pinned Mega corpus is analyzed, THE SYSTEM SHALL CONTINUE TO match all reviewed graph topologies.

## Acceptance Criteria

- [x] Regression Risk Analysis is complete for the high-severity fix.
- [x] Each reported behavior has a failing regression contract before production code changes.
- [x] All three expected-behavior contracts pass.
- [x] All Must-Test unchanged behaviors pass.
- [x] The full pull-request verification passes.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Use no subagents.
- Keep each class focused on one responsibility.
