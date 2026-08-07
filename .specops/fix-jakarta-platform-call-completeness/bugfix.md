# Bug Fix: Classify Jakarta platform calls as platform operations

## Problem Statement

Strict aggregate analysis fails for Jakarta REST graph entries because the analyzer treats Jakarta response-builder calls as unavailable business logic.

## Root Cause Analysis

`StaticDecisionAnalyzer.isSupportedLibraryOperation` accepts `java.*` and `javax.*` owners. It does not accept the equivalent `jakarta.*` namespace. A result-relevant call such as `jakarta.ws.rs.core.Response.ok(value).build()` therefore enters binary decision analysis and creates a coverage gap.

**Affected Components:**

- Static source call classification.
- Aggregate Maven analysis with `fachtracing.failOnIncomplete=true`.

**Error Symptoms:**

- `getAllDataMaxNumber` is incomplete.
- `getAllWateringDataMaxNumber` is incomplete.

## Impact Assessment

- **Severity:** Medium
- **Users Affected:** Jakarta applications with annotated methods that return or use Jakarta platform types.
- **Frequency:** Always when a result-relevant Jakarta call has no source body.

## Reproduction Steps

1. Put a compiled `jakarta.ws.rs.core.Response` type on the analysis classpath.
2. Analyze an annotated source method that returns `Response.ok(value).build()`.
3. Expected: The graph contains the source predicate and is complete.
4. Actual: The response calls create an unavailable-binary-logic coverage gap.

## Regression Risk Analysis

### Blast Radius

- `isSupportedLibraryOperation` controls method invocations and method references in both extraction paths.
- Existing binary fallback must still reject unsupported application calls.

### Behavior Inventory

- `java.*` and `javax.*` platform operations stay complete.
- Unsupported application binary logic stays incomplete.

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| Jakarta response-builder calls do not create a gap | Must-Test | This is the corrected classification path. |
| Unsupported application binary logic creates a gap | Must-Test | A broad allow-list could hide real decision logic. |

## Proposed Fix

Classify `jakarta.*` owners as platform library operations in the same method that classifies `java.*` and `javax.*` owners. Add a binary-classpath regression test with the two nested Jakarta response-builder calls.

## Unchanged Behavior

- WHEN an unsupported application binary call affects a returned decision THE SYSTEM SHALL CONTINUE TO mark the graph incomplete.

## Testing Plan

### Current Behavior

- WHEN an annotated source returns a compiled Jakarta response chain THE SYSTEM CURRENTLY marks the graph incomplete.

### Expected Behavior

- WHEN an annotated source returns a compiled Jakarta response chain THE SYSTEM SHALL keep the graph complete and retain its source predicate.

### Unchanged Behavior

- WHEN unsupported application binary logic affects a decision THE SYSTEM SHALL CONTINUE TO create a coverage gap.

## Acceptance Criteria

- [x] Regression Risk Analysis is complete for medium severity.
- [x] The regression test fails before the production change.
- [x] Jakarta platform-call regression tests pass after the fix.
- [x] Existing analyzer and project verification tests pass.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep each component focused on one responsibility.
