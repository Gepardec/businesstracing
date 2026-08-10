# Bug Fix: Review Follow-up Correctness

## Problem Statement

Four review cases remain after the conditional-alias and method-reference fix:

1. A branch-dependent alias that is read directly can omit its original value and report
   `COMPLETE`.
2. A cast or parenthesized method-reference callback can lose its mutation and report `COMPLETE`.
3. A mutating callback whose Boolean result controls a predicate operation can report `COMPLETE`
   without predicate semantics.
4. The hosted-CI shutdown correction reserves half of every shutdown bound for cancellation. It
   can interrupt a valid slow save too early.

## Root Cause Analysis

- `DependencyGraphBuilder` forks alias state for an `if`, but it keeps one shared definition map.
- `callbackEffects` classifies only direct lambda and member-reference argument nodes.
- `FlowScanner.visitMemberReference` treats a platform mutator only as a computation, even when its
  Boolean result controls `anyMatch`, `filter`, or another predicate callback.
- `DecisionRecordDelivery.close` uses a proportional cancellation reserve without an upper bound.

## Impact Assessment

- **Severity:** High
- **Users Affected:** Applications that use the listed Java forms or slow repository saves during
  shutdown.
- **Frequency:** Always for the analyzer forms; timing-dependent for shutdown.

## Regression Risk Analysis

### Blast Radius

- `DependencyGraphBuilder` and `BackwardDecisionSlicer` own local definition dependencies.
- `StaticDecisionAnalyzer` owns callback effects, callback graph flow, and coverage gaps.
- `DecisionRecordDelivery` owns graceful drain and bounded shutdown.
- `StaticDecisionAnalyzerTest` and `DecisionRecordProtocolTest` are the executable contracts.

### Behavior Inventory and Coverage

- Unconditional reassignment must keep only the latest definition.
- Conditional definitions must retain every reachable value and their branch control.
- Direct and cast bound callbacks must preserve the same mutation transfer.
- Source method-reference predicates must continue to expand.
- Mutating Boolean callbacks must not claim exact predicate support.
- Short shutdown bounds must remain bounded, and long bounds must preserve most graceful time.
- Existing analyzer, protocol, and Mega contracts must continue to pass.

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| Conditional definition merge | Must-Test | It controls result dependencies and completeness. |
| Wrapped and Boolean callback references | Must-Test | They control mutation and predicate effect proof. |
| Graceful and bounded shutdown | Must-Test | Accepted delivery records can otherwise become unknown early. |
| Five Mega graphs | Must-Test | They are the required brownfield regression proof. |

### Scope Assessment

The request has two independent components: analyzer proof and runtime shutdown. SpecOps recommends
a split. This environment is non-interactive, so the work stays in one follow-up spec with separate
tasks and commits. This preserves one responsibility per implementation change.

## Proposed Fix

- Track the active definition set per local name, fork it at `if` branches, and merge reachable
  definitions at the join.
- Unwrap casts and parentheses before callback classification and callback-parent lookup.
- Keep the mutation transfer, but add a source-located gap when a platform mutator Boolean controls
  a predicate callback.
- Cap the cancellation reserve at 500 ms while keeping half of short shutdown bounds available for
  cancellation.

## Unchanged Behavior

- WHEN a local is reassigned unconditionally THE SYSTEM SHALL use only its latest definition.
- WHEN a direct bound callback mutates returned state THE SYSTEM SHALL preserve its transfer.
- WHEN a source method reference supplies a predicate THE SYSTEM SHALL continue to expand it.
- WHEN shutdown completes inside its graceful window THE SYSTEM SHALL preserve accepted records.
- WHEN shutdown meets an uncooperative store THE SYSTEM SHALL remain bounded.

## Testing Plan

### Current Behavior

- Add executable regressions for direct conditional alias reads, cast callbacks, mutating Boolean
  predicate callbacks, and a valid save that completes after half of a long shutdown bound.
- Run them before production edits and record their failures.

### Expected Behavior

- Conditional alias reads include both reachable definitions and branch control in a complete
  graph.
- Cast callbacks keep the same complete transfer as direct callbacks.
- Mutating Boolean predicate callbacks keep the transfer and report an incomplete predicate gap.
- A valid save can use most of a long shutdown bound.

### Unchanged Behavior

- Run all analyzer, protocol, capability, local PR, and Mega checks.

## Acceptance Criteria

- [x] All four review regressions fail before production changes.
- [x] Conditional definitions preserve all reachable inputs without stale unconditional values.
- [x] Cast and parenthesized callbacks preserve receiver effects.
- [x] Mutating Boolean predicate callbacks do not report false complete graphs.
- [x] Graceful shutdown keeps most of long bounds and stays bounded for short bounds.
- [x] Full local and hosted pull-request checks pass.
- [x] No dependency is added.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Use one responsibility per component and commit.
- Do not use subagents.
