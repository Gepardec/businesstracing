# Bug Fix: Hogajama business time vocabulary

## Problem Statement

The business graph guard rejects the valid action `set from date to today start time`. This stops
Maven reactor analysis after the exact graph is complete.

## Root Cause Analysis

`BusinessLogicArtifactGuard` uses unanchored whole-word patterns for `start` and `stop`. The guard
therefore treats these words as structural markers in every phrase, including business time and
location terms.

**Affected Components:**

- Business-only graph vocabulary validation.
- Maven business artifact generation for any label that contains `start` or `stop`.

**Error Symptoms:**

- Hogajama reactor analysis fails on `set from date to today start time`.
- A graph with valid compound business vocabulary cannot reach its exporters or renderers.

## Impact Assessment

- **Severity:** Medium
- **Users Affected:** Applications whose business labels contain `start` or `stop`.
- **Frequency:** Always for an affected projected graph.

## Dependencies & Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `generic-business-graph-projection` | It introduced the business-only artifact guard. | Yes | completed |

### Cross-Spec Blockers

| Blocker | Blocking Spec | Resolution Type | Resolution Detail | Status |
| --- | --- | --- | --- | --- |
| — | — | — | — | — |

## Reproduction Steps

1. Project a complete graph with an action labeled `set from date to today start time`.
2. Run `BusinessLogicArtifactGuard.requireClean`.
3. Expected: the guard accepts the valid compound phrase.
4. Actual: the guard reports technical vocabulary because the phrase contains `start`.

## Regression Risk Analysis

### Blast Radius

- `BusinessLogicArtifactGuard` validates every business graph before export or rendering.
- `BusinessGraphProjectionTest` supplies executable vocabulary contracts.

### Behavior Inventory

- The exact structural labels `Start` and `Stop` are prohibited.
- Other technical terms and Java tokens remain prohibited.
- Compound business phrases can contain ordinary words that also name structural markers.

### Test Coverage Assessment

- **Covered:** rejection of exact `Start` and other technical labels → `BusinessGraphProjectionTest`.
- **Gap:** acceptance of valid compound phrases containing `start` or `stop` → no existing test.

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| Reject exact structural terminal labels. | Must-Test | The fix changes the same two patterns. |
| Reject all other prohibited vocabulary. | Must-Test | The shared guard list must remain unchanged. |
| Accept compound business vocabulary. | Must-Test | This is the reported failure. |

## Proposed Fix

Anchor the `start` and `stop` patterns to the full label. Keep all other prohibited patterns and the
guard interface unchanged.

## Unchanged Behavior

- WHEN a label is exactly `Start` or `Stop` THE SYSTEM SHALL CONTINUE TO reject it.
- WHEN a label contains another prohibited technical term THE SYSTEM SHALL CONTINUE TO reject it.

## Testing Plan

### Current Behavior

- WHEN a business action contains `today start time` THE SYSTEM CURRENTLY rejects the graph.

### Expected Behavior

- WHEN `start` or `stop` is part of a longer business phrase THE SYSTEM SHALL accept that phrase.

### Unchanged Behavior

- WHEN the complete label is `Start` or `Stop` THE SYSTEM SHALL CONTINUE TO reject it.
- WHEN the label contains another prohibited term THE SYSTEM SHALL CONTINUE TO reject it.

## Acceptance Criteria

- [x] Regression Risk Analysis is complete.
- [x] The regression fails before the production fix.
- [x] Valid compound business phrases pass the guard.
- [x] Exact structural labels and all other technical vocabulary remain rejected.
- [x] The combined strict Hogajama reactor succeeds.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep each component focused on one responsibility.
