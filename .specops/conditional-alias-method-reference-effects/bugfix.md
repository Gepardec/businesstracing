# Bug Fix: Conditional Alias and Method-Reference Effects

## Problem Statement

The static analyzer can publish a `COMPLETE` graph when conditional alias reassignment or a
method-reference callback can change the returned decision. The graph then omits result-relevant
inputs and mutations without a coverage gap.

## Root Cause Analysis

The defects share the call-effect boundary but have separate causes:

1. `mutationSummary` uses one source-ordered alias map. It scans an assignment in one `if` branch
   as if that assignment occurred on every path. A later alias mutation therefore loses the
   possible parameter root.
2. `callbackEffects` scans only `LambdaExpressionTree` arguments. It does not classify
   `MemberReferenceTree` callbacks such as `accepted::add`.

**Affected Components:**

- `LocalAliasResolver` and its two consumers.
- `StaticDecisionAnalyzer` callback and mutation-summary analysis.
- `DependencyGraphBuilder` result-effect indexing.
- Static analyzer fixtures, contracts, and Java capability documentation.

**Error Symptoms:**

- Conditional helper logic can disappear from a caller graph that remains `COMPLETE`.
- A collection transfer through `forEach(accepted::add)` can disappear from a graph that remains
  `COMPLETE`.

## Impact Assessment

- **Severity:** High
- **Users Affected:** Applications whose annotated decisions use either source form.
- **Frequency:** Always for the reported forms.

## Dependencies & Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `jdk-mutation-alias-effect-correctness` | It introduced the direct-alias and explicit-effect model corrected here. | Yes | Completed |

### Cross-Spec Blockers

| Blocker | Blocking Spec | Resolution Type | Resolution Detail | Status |
| --- | --- | --- | --- | --- |
| None | — | — | — | — |

## Reproduction Steps

1. Analyze a helper that assigns `alias = target`, conditionally assigns a new list, then mutates
   `alias` under a second condition.
2. Analyze a decision that calls `candidates.stream().forEach(accepted::add)` and returns a value
   derived from `accepted`.
3. Expected: The graph includes proved effects or reports a source-located coverage gap.
4. Actual: Both graphs omit result-relevant logic and report `COMPLETE`.

## Regression Risk Analysis

### Blast Radius

- `StaticDecisionAnalyzer.callEffects` supplies mutation evidence to caller slicing.
- `StaticDecisionAnalyzer.mutationSummary` maps source helper writes to receiver and parameter
  roots.
- `DependencyGraphBuilder.build` maps local aliases to proved and possible result effects.
- `FlowScanner.visitMemberReference` expands result-relevant decision callbacks.
- `StaticDecisionAnalyzerTest` is the executable contract for complete and incomplete graphs.

### Behavior Inventory

- Direct, unconditional aliases map mutations back to the source parameter.
- A fully detached alias does not map its mutation to the source parameter.
- Lambda callback mutations stay in the result slice.
- Predicate method references still expand their source decision logic.
- Unknown result-relevant effects fail closed with an actionable gap.
- The five reviewed Mega graphs stay complete.

### Test Coverage Assessment

- **Covered:** Direct alias mutation and full alias invalidation use
  `ResultSlicePolicy.java` and `StaticDecisionAnalyzerTest`.
- **Covered:** Lambda callback mutation uses `StrategyAggregationPolicy.java`.
- **Covered:** Predicate method references use `JavaConstructPolicy.java`.
- **Gap:** No contract covers a branch-dependent alias binding.
- **Gap:** No contract covers a mutating bound method-reference callback.

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| Conditional alias roots create a possible effect or an exact effect | Must-Test | The changed resolver controls graph completeness. |
| Bound collection method references expose their receiver mutation | Must-Test | The callback classifier controls the returned collection slice. |
| Direct and invalidated aliases keep their present behavior | Must-Test | Both use the same resolver. |
| Predicate and lambda callbacks keep their present behavior | Must-Test | Both use the same callback path. |
| Mega conformance stays complete | Must-Test | It is the required brownfield regression proof. |

### Scope Escalation Check

**Scope:** Contained. The fix extends the existing local effect proof. It does not add a public API,
a dependency, or general interprocedural points-to analysis.

## Proposed Fix

Keep proved and possible alias roots and merge `if` branch states conservatively. A write through a
root that exists on only some paths becomes a possible write and causes the existing fail-closed
coverage behavior. Resolve bound method-reference targets and classify their receiver with the same
platform and source mutation contracts used for normal calls.

## Unchanged Behavior

- WHEN an alias has one unconditional parameter root, THE SYSTEM SHALL CONTINUE TO classify its
  mutation as proved.
- WHEN an alias is unconditionally detached, THE SYSTEM SHALL CONTINUE TO exclude the old parameter
  root.
- WHEN a lambda or predicate method reference is result-relevant, THE SYSTEM SHALL CONTINUE TO
  include its supported decision logic.
- WHEN an effect is not proved, THE SYSTEM SHALL CONTINUE TO avoid guessed topology.

## Testing Plan

### Current Behavior (verify the bug exists)

- WHEN either reported fixture is analyzed before the fix, THE SYSTEM CURRENTLY reports a false
  `COMPLETE` graph without the result-relevant mutation.

### Expected Behavior (verify the fix works)

- WHEN a conditional alias can still refer to a caller parameter, THE SYSTEM SHALL include its
  exact result effect or mark the graph `INCOMPLETE` with a source-located side-effect gap.
- WHEN `forEach` uses a bound mutating method reference, THE SYSTEM SHALL include the transfer input,
  callback receiver mutation, and controlling result logic in a `COMPLETE` graph.

### Unchanged Behavior (verify no regressions)

- WHEN direct and detached alias fixtures run, THE SYSTEM SHALL CONTINUE TO produce their present
  complete graphs.
- WHEN lambda and predicate method-reference fixtures run, THE SYSTEM SHALL CONTINUE TO pass.
- WHEN Mega conformance runs, THE SYSTEM SHALL CONTINUE TO produce five complete reviewed graphs.

## Acceptance Criteria

- [x] Regression Risk Analysis is complete for the high severity.
- [x] Both false-complete regressions have failing pre-fix tests.
- [x] Conditional aliases preserve all possible external roots or create a coverage gap.
- [x] Bound mutating method-reference callbacks preserve result-relevant transfers.
- [x] All Must-Test unchanged behaviors pass.
- [x] No new dependency or target-specific rule is added.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Use one responsibility per component.
- Do not use subagents.
