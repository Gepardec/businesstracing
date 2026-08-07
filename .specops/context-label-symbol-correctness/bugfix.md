# Bug Fix: Context label symbol correctness

## Problem Statement

Three label paths use syntax or source spelling when attributed compiler data identifies a more
accurate business subject. Static utility mutations name the utility class, `var` declarations lose
their inferred type, and a local subject can leak to another identifier with the same spelling.

## Root Cause Analysis

`StaticDecisionAnalyzer.FlowScanner` stores local subjects by name and reads the syntactic
declaration type. `platformMutationLabel` also treats every member-select expression as the
mutation target, including static `Collections` and `Arrays` calls whose first argument is the
actual target.

**Affected Components:**

- Static mutation label generation in `StaticDecisionAnalyzer`.
- Attributed variable-subject resolution in `StaticDecisionAnalyzer`.
- Static analyzer label fixtures and executable contracts.

**Error Symptoms:**

- `Collections.sort(warnings)` produces `sort collections with warnings`.
- `var c = new GregorianCalendar()` produces `item` instead of `gregorian calendar`.
- A block-local identifier can change the later label of a field with the same spelling.

## Impact Assessment

- **Severity:** Medium
- **Users Affected:** Readers of graphs with static collection utilities, inferred local types, or
  identifier shadowing.
- **Frequency:** Deterministic for each affected source form.

## Reproduction Steps

1. Analyze one traced method for each error symptom.
2. Expected: each operation names the attributed mutation target or inferred type.
3. Actual: the graph names a utility class, `item`, or a stale local subject.

## Regression Risk Analysis

### Blast Radius

- `FlowScanner.visitVariable` records subjects for later receiver labels.
- `FlowScanner.receiverSubject` resolves identifier receivers.
- `FlowScanner.variableSubject` selects names from declaration types.
- `FlowScanner.platformMutationLabel` renders supported platform writes.
- `StaticDecisionAnalyzerTest` runs every static analyzer contract.

### Behavior Inventory

- Explicit declaration labels such as `Calendar c` remain type-aware.
- Instance collection mutations such as `sensors.add(value)` keep receiver-first semantics.
- Static effect roots continue to use the first argument of supported `Collections` and `Arrays`
  calls.
- Graph topology, completeness, evidence, and source mappings do not change.

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| Explicit and inferred declaration subjects | Must-Test | Both use the changed type resolver. |
| Instance and static mutation labels | Must-Test | Both use the changed mutation label path. |
| Identifier shadowing | Must-Test | Receiver lookup changes from text to compiler symbols. |
| Graph topology and completeness | Nice-To-Test | The change affects label text only. |

## Proposed Fix

Bind local subjects to attributed elements, derive declaration subjects from attributed type
mirrors, and select the first argument as the label receiver for supported static mutation
utilities. Keep static-utility classification in one helper so effect slicing and labels use the
same contract.

## Unchanged Behavior

- WHEN a declaration uses an explicit reference type THE SYSTEM SHALL CONTINUE TO produce its
  useful type-aware subject.
- WHEN an instance collection mutation changes returned state THE SYSTEM SHALL CONTINUE TO name
  the mutated receiver and its operands.
- WHEN existing analyzer fixtures run THE SYSTEM SHALL CONTINUE TO preserve graph completeness and
  topology contracts.

## Testing Plan

### Current Behavior

- WHEN a static collection utility mutates its first argument THE SYSTEM CURRENTLY names the
  utility class as the receiver.
- WHEN a short `var` local has an inferred reference type THE SYSTEM CURRENTLY emits `item`.
- WHEN a block local shadows a field THE SYSTEM CURRENTLY can reuse the local subject after the
  block ends.

### Expected Behavior

- WHEN `Collections.sort(warnings)` is relevant THE SYSTEM SHALL emit `sort warnings`.
- WHEN `Arrays.fill(buffer, value)` is relevant THE SYSTEM SHALL emit `fill buffer with value`.
- WHEN `var c` infers `GregorianCalendar` THE SYSTEM SHALL use `gregorian calendar`.
- WHEN two identifiers have the same spelling THE SYSTEM SHALL bind each label to its attributed
  symbol.

### Unchanged Behavior

- WHEN the existing context-aware label contracts run THE SYSTEM SHALL CONTINUE TO pass them.
- WHEN the full pull-request gate runs THE SYSTEM SHALL CONTINUE TO pass all required checks.

## Acceptance Criteria

- [x] Regression Risk Analysis is complete for the medium severity.
- [x] Regression contracts fail before the production fix and pass after it.
- [x] Static utility labels name the first mutated argument and exclude it from operands.
- [x] `var` declarations use their attributed inferred type.
- [x] Receiver subjects use attributed symbols and do not leak across scopes.
- [x] Existing focused and pull-request checks pass.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep each component and helper focused on one responsibility.
