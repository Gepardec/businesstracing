# Bug Fix: Restore strict aggregate completeness for Hogajama

## Problem Statement

The aggregate Maven goal marks `getAllDataMaxNumber` and
`getAllWateringDataMaxNumber` as incomplete in the Hogajama reactor. The failure remains after the
caller selects the exact Morphia and Apache Commons dependency archives as opaque technical
boundaries.

## Root Cause Analysis

The current graph contains two false gaps after the required opaque-library configuration is
applied:

- A source-visible `catch` path around supported Java platform calls is marked as having an
  unavailable exception trigger.
- Calls inside an anonymous comparator class leak into the enclosing method's effect analysis and
  create a possible mutation of returned state.
The analyzer uses exception availability and effect scope at separate points. The Hogajama path
combines both with generated mapper dispatch and exposes missing integration coverage.

**Affected Components:**

- Static exception-trigger classification.
- Method-local dependency and mutation-effect scans.
- Existing dynamic implementation selection for generated Java source, as an integration boundary.
- Aggregate Maven analysis with strict completeness enabled.

**Error Symptoms:**

- `getAllDataMaxNumber` has caught-exception and unknown-effect gaps.
- `getAllWateringDataMaxNumber` has caught-exception gaps.
- The Maven goal fails when `fachtracing.failOnIncomplete=true`.

## Impact Assessment

- **Severity:** Medium
- **Users Affected:** Maven reactors that combine source-visible catches, anonymous helper classes,
  generated implementations, and selected opaque dependency archives.
- **Frequency:** Always for the reported Hogajama methods.

## Reproduction Steps

1. Use the Hogajama reactor at commit `09c914268ccbbacab39fb407d926307ad7bef939`.
2. Add `@FachTracing` to the two reported REST methods and use Fachtracing `0.1.0-rc.1`.
3. Select the Morphia, Commons Lang, and Commons Collections dependency archives as opaque.
4. Run the aggregate goal with `fachtracing.failOnIncomplete=true`.
5. Expected: Both graphs are complete.
6. Actual: Both graph names are reported as incomplete.

## Regression Risk Analysis

### Blast Radius

- `StaticDecisionAnalyzer` classifies calls inside source catches and selects dynamic source
  implementations.
- `DependencyGraphBuilder` collects method-local definitions and effects for every analyzed method.
- Existing source-unavailable calls, callback effects, application dispatch, and opaque library
  boundaries use the same data.

### Behavior Inventory

- A source-visible catch around supported platform logic has an exact graph path and runtime control
  target.
- A call in a nested class body belongs to that nested method, not to the enclosing method.
- Source-visible generated implementations are valid dispatch candidates when compile and analysis
  run in the same Maven invocation.
- An unsupported binary exception trigger stays incomplete.
- An unknown result-relevant call effect stays incomplete.
- A dynamic contract with no source implementation stays incomplete.

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| Reported Hogajama graphs become complete | Must-Test | This is the defective path. |
| Unsupported caught binary logic stays incomplete | Must-Test | The exception fix must stay fail-closed. |
| Nested callback and mutation effects stay scoped correctly | Must-Test | The effect fix changes shared scan input. |
| Missing dynamic implementations stay incomplete | Must-Test | The dispatch fix must not guess. |

## Proposed Fix

Add a generic, compiled-source regression that combines the three constructs. Make exception-trigger
availability use the same supported-operation contract as invocation analysis. Keep method effect
scans out of nested class bodies. Verify the existing generated implementation selection without a
production dispatch change. Do not add Hogajama packages, names, or rules to production code.

## Unchanged Behavior

- WHEN a caught result path depends on unsupported source-unavailable logic THE SYSTEM SHALL
  CONTINUE TO mark the graph incomplete.
- WHEN a result-relevant call can change returned state and no effect is proved THE SYSTEM SHALL
  CONTINUE TO mark the graph incomplete.
- WHEN a dynamic decision contract has no compatible source implementation THE SYSTEM SHALL
  CONTINUE TO mark the graph incomplete.

## Testing Plan

### Current Behavior

- WHEN the compiled generic fixture uses the reported construct combination THE SYSTEM CURRENTLY
  produces the same three gap classes as Hogajama.

### Expected Behavior

- WHEN supported platform calls are inside a source-visible catch THE SYSTEM SHALL preserve the
  catch result path without an unavailable-trigger gap.
- WHEN an anonymous helper method reads values THE SYSTEM SHALL keep its effects out of the
  enclosing method.
- WHEN a compatible generated Java implementation is in the source boundary THE SYSTEM SHALL
  include it as a dispatch candidate.
- WHEN the strict aggregate goal analyzes the real Hogajama checkout with its declared opaque
  libraries THE SYSTEM SHALL complete both reported graphs.

### Unchanged Behavior

- WHEN unsupported binary exception, effect, and dispatch fixtures run THE SYSTEM SHALL CONTINUE TO
  report their exact gaps.
- WHEN the full repository verification gates run THE SYSTEM SHALL CONTINUE TO pass all existing
  contracts and conformance graphs.

## Acceptance Criteria

- [x] A focused regression fails before the production fix.
- [x] The generic regression completes both aggregate-style decisions after the fix.
- [x] Fail-closed exception, effect, and dispatch contracts remain incomplete.
- [x] The real Hogajama strict aggregate goal succeeds with explicit opaque library coordinates.
- [x] The full pull-request verification gate passes.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep each component responsible for one concern.
