# Bug Fix: Normalize nested map transfer labels

## Problem Statement

The business graph exporter rejects a valid Hogarama graph because a collection transfer label
contains the source method word `map`: `add map sensor sensor data to sensor data list`.

## Root Cause Analysis

`BusinessLanguageNormalizer` rewrites map operations only when `map` starts the complete label.
The analyzer can embed the same map operation in an `add ... to ...` collection mutation label. The
normalizer does not process this nested form, and `BusinessLogicArtifactGuard` correctly rejects it.

**Affected Components:**

- Business-only graph language normalization.
- Business JSON, Mermaid, and PlantUML generation for mapped collection transfers.

**Error Symptoms:**

- Strict Hogarama analysis completes the exact graph but stops before it writes the business JSON.
- The exporter reports `business graph contains technical vocabulary` for the nested map label.

## Impact Assessment

- **Severity:** Low
- **Users Affected:** Applications that add a mapper result directly to a returned collection.
- **Frequency:** Always for this label shape.

## Dependencies & Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `generic-business-graph-projection` | It introduced normalization and guarded business exports. | Yes | completed |

## Reproduction Steps

1. Project an action labeled `add map order order details to order detail list`.
2. Export or guard the business graph.
3. Expected: the label states a business conversion and contains no technical map term.
4. Actual: the guard rejects the nested `map` word.

## Regression Risk Analysis

### Blast Radius

- `BusinessLanguageNormalizer.normalize` processes each projected node and edge label.
- `BusinessGraphProjectionTest` verifies generic label rewrites and guard behavior.

### Behavior Inventory

- Existing top-level map callback and setter labels keep their current output.
- Unrelated collection transfer labels keep their current output.
- A repeated mapper subject and input subject become one converted business subject.

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| Normalize the nested mapped transfer. | Must-Test | This is the defective path. |
| Preserve current map and collection rewrites. | Must-Test | The change uses the shared normalizer. |

## Proposed Fix

Add one generic nested-transfer pattern to `BusinessLanguageNormalizer`. Match a repeated subject
with a regular-expression back reference and replace the source map operation with `converted`.
Do not add Hogarama words or method names to production code.

## Unchanged Behavior

- WHEN a top-level map callback or setter is normalized THE SYSTEM SHALL CONTINUE TO use its current label.
- WHEN a collection transfer has no nested map operation THE SYSTEM SHALL CONTINUE TO keep its current label.

## Testing Plan

### Current Behavior

- WHEN a mapped value is added directly to a collection THE SYSTEM CURRENTLY rejects the nested map label.

### Expected Behavior

- WHEN a mapped value is added directly to a collection THE SYSTEM SHALL produce a guarded business label with `converted`.

### Unchanged Behavior

- Run the full business projection contract and the existing pull-request gate.
- Generate the two strict Hogarama business JSON files.

## Acceptance Criteria

- [x] Regression Risk Analysis is complete.
- [x] The focused regression fails before the production fix.
- [x] The nested mapped transfer contains no prohibited technical vocabulary.
- [x] Existing normalization contracts pass.
- [x] Strict Hogarama analysis writes two valid business JSON files.
- [x] Mega graph JSON files are generated from the pinned conformance revision.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep each component focused on one responsibility.
- Do not hardcode diagrams or application vocabulary.
