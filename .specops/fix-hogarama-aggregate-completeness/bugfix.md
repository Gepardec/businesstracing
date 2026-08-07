# Bug Fix: Complete Hogarama aggregate graphs at external library boundaries

## Problem Statement

Strict aggregate analysis fails for `getAllDataMaxNumber` and `getAllWateringDataMaxNumber`. The graphs include source-visible DAO logic, but calls to Morphia and Apache Commons from dependency JARs create coverage gaps.

## Root Cause Analysis

`StaticDecisionAnalyzer` treats every source-unavailable call outside the Java, Javax, and Jakarta namespaces as unavailable application decision logic. This classification does not use the compiler classpath origin. As a result, reference-returning calls from external dependency archives enter dynamic dispatch or Boolean-only bytecode analysis.

The same calls also receive unknown reference effects. Source helpers that configure a returned query or options object then create side-effect gaps.

**Affected Components:**

- Binary type-origin classification.
- Static invocation flow.
- Result-relevant call-effect analysis.
- Aggregate Maven analysis with `fachtracing.failOnIncomplete=true`.

**Error Symptoms:**

- `getAllDataMaxNumber` has seven coverage gaps.
- `getAllWateringDataMaxNumber` has five coverage gaps.
- The gaps report unavailable implementations, a Boolean-only binary fallback, or an unknown result effect.

## Impact Assessment

- **Severity:** Medium
- **Users Affected:** Applications that use source-visible business predicates around reference-returning APIs from dependency JARs.
- **Frequency:** Always when the API call is in the returned result slice.

## Reproduction Steps

1. Analyze the Hogarama reactor at commit `09c914268ccbbacab39fb407d926307ad7bef939` with the two reported methods annotated.
2. Let the DAO implementations use Morphia query APIs and Apache Commons collection APIs from resolved dependency JARs.
3. Expected: The graphs retain the application predicates and are complete.
4. Actual: External library calls create twelve coverage gaps across the two graphs.

## Regression Risk Analysis

### Blast Radius

- Invocation flow and result-effect classification share the new binary origin contract.
- Calls from application class directories and Boolean dependency rules must stay fail-closed.

### Behavior Inventory

- Source-visible application implementations continue to expand.
- Boolean decision calls without source continue to use the controlled bytecode fallback or create a gap.
- Source-unavailable application classes in directories continue to create a gap.
- Reference-returning calls whose exact owner comes from a dependency archive become opaque library boundaries.
- Instance calls at that boundary keep a receiver effect so source-visible predicates that configure the receiver remain in the result slice.

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| Hogarama-shaped reference APIs do not create gaps | Must-Test | This is the corrected path. |
| Source predicates around external receiver changes remain visible | Must-Test | A read-only classification could hide application rules. |
| Boolean dependency decisions remain fail-closed | Must-Test | A broad archive rule could hide decision logic. |
| Application class-directory binaries remain fail-closed | Must-Test | The application boundary must not weaken. |

## Proposed Fix

Add one binary type-origin resolver with cache support. Classify a source-unavailable operation as an opaque external library operation only when its exact owner resolves first from an archive and its result is a reference value. Treat an instance operation at this boundary as an opaque receiver effect. Keep static value operations effect-free. Also accept a Boolean archive call only when it is inside an explicit source control condition, where the source call site is already the graph predicate. Use the same reference classification in invocation and method-reference flow.

## Unchanged Behavior

- WHEN a source-unavailable Boolean call affects a decision THE SYSTEM SHALL CONTINUE TO analyze it with the safe fallback or mark the graph incomplete.
- WHEN a source-unavailable application class from a class directory affects a decision THE SYSTEM SHALL CONTINUE TO mark the graph incomplete.

## Testing Plan

### Current Behavior

- WHEN an annotated source uses external archive query and collection APIs THE SYSTEM CURRENTLY marks the graph incomplete.

### Expected Behavior

- WHEN an annotated source uses reference-returning operations from a dependency archive THE SYSTEM SHALL keep the archive internals outside the application graph.
- WHEN source predicates control receiver configuration THE SYSTEM SHALL keep those predicates in the graph.

### Unchanged Behavior

- WHEN the external archive operation returns Boolean decision logic THE SYSTEM SHALL CONTINUE TO fail closed.

## Acceptance Criteria

- [x] The regression test fails before the production change.
- [x] Both reported graph names are complete in the compiled archive fixture.
- [x] Source predicates for maximum number, actor or sensor, and date remain visible.
- [x] A Boolean dependency decision remains incomplete.
- [x] The real Hogarama reproduction passes with strict completeness.
- [x] Existing analyzer, Maven, capability, integrity, and pull-request gates pass.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep each component focused on one responsibility.
