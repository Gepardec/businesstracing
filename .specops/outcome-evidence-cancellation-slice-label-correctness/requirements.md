# Bug Fix: Outcome Evidence, Cancellation Reach, Slice, and Label Correctness

## Problem Statement

Four defects let Fachtracing publish a false complete explanation, lose a cancelled asynchronous
record, include work that cannot affect the result, or remove real business context from a label.
These defects block PR #5 because they break the generic tracing contract.

## Root Cause Analysis

1. `RuntimeCollector.complete` stores only the encoded result. It does not consume evidence staged
   for the Stop node. The explanation projector also drops every Stop-node observation.
2. Activation generation fingerprints graph owners only. A separate class that calls
   `Future.cancel(boolean)` is outside the transformer boundary.
3. `DependencyGraphBuilder` treats each identifier read by a method call as a write. The backward
   slice then includes ignored calls that only read a returned input.
4. `renderCall` removes the receiver from each method named `validate`, without proof that the
   receiver is a technical helper.

## Impact Assessment

- **Severity:** High
- **Users Affected:** All applications that use direct return receivers, separate cancellation
  controllers, ignored calls near decision inputs, or distinct validation business objects.
- **Frequency:** Deterministic when one of the four source patterns occurs.

## Dependencies and Blockers

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `stage-lifecycle-evidence-label-correctness` | It added the staged receiver evidence and class-wide cancel pass corrected here. | Yes | Completed |

No open blocker exists.

## Reproduction Steps

1. Return `city.equals("Vienna")` from an annotated method. Expected: the saved execution and text
   explanation contain the typed city fact. Actual: the fact is staged, discarded, and the complete
   explanation says that no reason was recorded.
2. Submit traced work in an annotated class and cancel its `Future` from a separate controller
   class. Expected: cancellation releases the reservation. Actual: the controller is not
   fingerprinted or transformed, so no record completes.
3. Call `auditValidator.validate(age)` and ignore its result before returning `age >= 24`.
   Expected: the audit call is absent. Actual: it appears as a decision cause.
4. Call `fraudValidator.validate()` and `creditValidator.validate()`. Expected: labels keep the
   distinct receivers. Actual: the global method-name rule removes them.

## Regression Risk Analysis

### Blast Radius

- Runtime observation assembly and explanation projection.
- Activation fingerprint generation and agent class selection.
- Static dependency construction, backward slicing, and unknown side-effect coverage.
- Generic call labels and the five reviewed Mega Backend graphs.

### Behavior Inventory

- Predicate evidence still records typed, redacted values and exact selected edges.
- Only verified application bytecode can be transformed.
- Proven collection and source-method mutations remain in the result slice.
- Unknown result-relevant effects remain visible as source-located coverage gaps.
- Application results, exceptions, future identity, and activation V3 remain unchanged.

### Test Coverage Assessment

- **Covered:** exact branch evidence, same-class cancellation, generic slicing, and Mega conformance
  have existing tests.
- **Gap:** no runtime explanation test checks direct receiver evidence at Stop.
- **Gap:** no activation test uses a cancel caller in a separate class.
- **Gap:** no fixture separates an ignored read-only call from a proven mutation.
- **Gap:** no fixture compares two non-helper `validate` receivers.

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| One complete saved record per accepted decision | Must-Test | Runtime and cancellation changes affect terminal publication. |
| Exact business evidence | Must-Test | Merging evidence can change stored observations. |
| Result-only static slice | Must-Test | Mutation classification changes graph topology. |
| Five reviewed Mega graphs | Must-Test | The generic slice and labels affect the reference corpus. |
| Activation V3 compatibility | Must-Test | The bundle gains more application fingerprints but no schema field. |

### Scope Escalation Check

**Scope:** Contained. The fix corrects four existing contracts. It adds no public feature or
application-specific rule.

## Proposed Fix

- Merge staged Stop evidence with the final result observation. Project non-result Stop evidence as
  a business reason.
- Detect supported cancellation call sites in compiled reactor output and include only those caller
  classes, plus graph owners, in activation fingerprints.
- Build call effects from proven writes. Use attributed JDK mutation contracts and source mutation
  summaries. Report an exact gap for an unknown effect on a result-dependent reference.
- Remove the global `validate` renderer rule. Use a proven new-object, validate-only helper role to
  shorten helper labels, while all other receivers stay visible.

## Unchanged Behavior

- WHEN a predicate has exact evidence, THE SYSTEM SHALL CONTINUE TO store the same typed values and
  selected edge.
- WHEN a class fingerprint does not match, THE AGENT SHALL CONTINUE TO leave its bytecode unchanged.
- WHEN relevant mutation cannot be proved or ruled out, THE GRAPH SHALL CONTINUE TO be incomplete
  and show a source-located gap.
- WHEN Mega Backend runs as the reference corpus, THE EXTRACTOR SHALL CONTINUE TO use no Mega-specific
  package, class, method, or vocabulary rule.

## Testing Plan

### Current Behavior

- [x] A direct receiver result produces a complete explanation with no reasons.
- [x] A separate cancellation controller leaves the execution unpublished.
- [x] An ignored read-only validation call appears in the graph.
- [x] A non-helper validation receiver disappears from its label.

### Expected Behavior

- [x] The saved execution and text explanation contain the typed receiver fact.
- [x] The activation fingerprints and transforms a separate cancellation controller.
- [x] The ignored read-only call is absent, while proven mutations remain.
- [x] Unknown result-relevant effects create a source-located gap.
- [x] Helper labels are concise and non-helper receivers remain distinct.

### Unchanged Behavior

- [x] Focused engine, agent, Maven plugin, and external activation contracts pass.
- [x] Mega Backend produces five complete generic graphs.
- [ ] The 600-second, 1,000-RPS gate has zero result changes, contamination, and silent record loss.

## Acceptance Criteria

- [x] The regression risk analysis and all four failing fixtures are executable.
- [x] All four root causes are fixed in production code, not only in docs or fixtures.
- [x] Runtime explanations never claim complete coverage after discarding available result evidence.
- [x] Activation cancellation reach includes separate application callers without transforming
  unverified dependency classes.
- [x] Static graphs contain proven result dependencies only; unknown relevant effects are gaps.
- [x] Label cleanup uses proven helper roles and preserves other receivers.
- [ ] Standard, external, Mega, and long release gates pass.
