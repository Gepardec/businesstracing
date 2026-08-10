# Bug Fix: Implicit Field and Local Callback Correctness

## Problem Statement

The analyzer can report `COMPLETE` while it omits a result-relevant input:

1. A conditional local alias can lose an implicit field definition at a branch join.
2. A callback stored in a local variable is not resolved at a stream predicate use site.

## Root Cause Analysis

- `DependencyGraphBuilder` limits known state roots to tracked locals, parameters, `this`, and
  `super`. An attributed field identifier is outside that set.
- Callback effect analysis accepts only a lambda or member reference that is directly in the
  invocation argument list. It does not use active local definitions.

## Impact Assessment

- **Severity:** High
- **Users Affected:** Applications that use implicit fields through conditional aliases or local
  callback variables in traced decisions.
- **Frequency:** Always for the two listed forms.

## Proposed Fix

- Supply attributed enclosing field names to dependency construction and keep these names as state
  roots at branch joins.
- Supply active local definitions to callback analysis, resolve callback identifiers at each use
  site, and keep the source-located Boolean-result coverage gap for a mutating predicate callback.

## Unchanged Behavior

- Unconditional local reassignment uses only the latest definition.
- Direct, cast, and parenthesized callbacks keep their current effects.
- A conditional alias that does not refer to state does not gain unrelated dependencies.

## Testing Plan

- Add one regression for a conditional alias of an implicit field.
- Add one regression for a local `Predicate` that contains a bound mutating member reference.
- Run the focused analyzer contract, the full pull-request gate, and hosted checks.

## Acceptance Criteria

- [x] Both regressions fail before production changes.
- [x] The implicit field remains in the complete result graph.
- [x] The local callback keeps its mutation transfer and reports an incomplete Boolean-result gap.
- [x] Existing analyzer and Mega contracts pass.
- [x] No dependency is added.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Give each component one responsibility.
- Do not use subagents.
