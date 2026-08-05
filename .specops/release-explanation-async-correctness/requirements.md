# Requirements: Release, Explanation, and Async Correctness

## Overview

This bug fix corrects five release-blocking findings in the generic Java tracer. It must make the
release result truthful, record only result-relevant business evidence, close asynchronous
reservation leaks, bind standard asynchronous APIs exactly, and remove indexed-loop mechanics from
business graphs. The implementation must stay generic. Mega Backend is a conformance corpus only.

## Severity and Scope

- Severity: P1 for release truth, explanation evidence, and asynchronous record integrity.
- Severity: P2 for indexed-loop business vocabulary.
- Scope: release scripts, static manifest generation, runtime instrumentation, async lifecycle,
  business graph lowering, activation serialization, focused tests, and Mega conformance output.
- Out of scope: new storage formats, a frontend, Mega-specific labels, and unsupported third-party
  asynchronous frameworks.

The findings can be separate specifications. This run keeps one remediation specification because
all five items block one release claim and must pass the same clean-clone, external, Mega, and load
gates.

## Root Cause Analysis

### RC-1: The release pipeline reports the consumer status

`verify-release.sh` runs the release commands in a subshell and pipes the output to `tee`. POSIX
shell pipeline status is the status of the last command. A failed producer can therefore be hidden
by a successful `tee`. The repository integrity script also uses `rg` without declaring or
installing that tool in CI.

### RC-2: Runtime evidence has no static result-slice binding

The transformer captures every annotated-method argument at the entry node. The explanation
projector intentionally removes entry nodes, so this evidence cannot explain a predicate. Predicate
probes record only the Boolean result. There is no manifest plan that maps a result-relevant operand
to its business predicate node.

### RC-3: Async reservations have no submission lifecycle

Context wrapping increments the invocation reservation count before an executor or stage accepts
the callback. The callback releases the count only when it runs. Rejection, cancellation before
execution, or a never-started callback can leave the reservation active and prevent record
publication.

### RC-4: Async API matching is heuristic

The transformer assumes that the first argument of a method on a broad concurrent owner is the
callback. Standard methods have different callback positions. The current rule can wrap a stage or
executor argument and leave the real callback unwrapped. It can also treat an unmatched API as
supported.

### RC-5: Indexed loops are scanned as source mechanics

The result slice includes a canonical loop counter, collection size comparison, indexed access,
and counter update. The flow scanner emits these nodes directly. Dynamic dispatch edges also use
ordinal `candidate N` labels. These labels are implementation details, not business facts.

## Impact and Blast Radius

- A failed release can appear green and can produce false release evidence.
- Business users can see a Boolean result without the fact value that caused it, while irrelevant
  identifiers can be captured in a stored record.
- A rejected or cancelled asynchronous operation can prevent an accepted decision invocation from
  producing a terminal record.
- Supported standard stage and thread methods can lose trace context or join the wrong work.
- Any Java project that uses an indexed collection loop can get technical business diagrams.
- Activation bundle readers and class fingerprint selection are affected when the manifest gains
  evidence plans.
- The five Mega Backend graphs are affected only through generic loop and dispatch lowering.

## Behavior Inventory

### Current behavior

- A producer failure before `tee` can still end with `RELEASE_GATE_OK`.
- Repository verification fails on a runner without `rg`.
- All method arguments are captured as `input N`, including result-irrelevant identifiers.
- Predicate evidence contains only `true` or `false`.
- Callback wrapping reserves immediately and releases only from callback execution.
- Async matching inspects only argument zero and a broad owner prefix.
- Canonical indexed loops show counter, size, index access, and ordinal candidates.

### Expected behavior

- Any release sub-gate failure returns a non-zero status and cannot print `RELEASE_GATE_OK`.
- Release scripts run on the declared GitHub runner tools without an undeclared `rg` dependency.
- Static analysis emits evidence bindings only for result-relevant, business-safe predicate
  operands.
- Runtime observations attach typed operand facts to the predicate node that used them.
- Rejection and cancellation before callback execution release the exact reservation once.
- Supported async APIs use an exact owner, method, descriptor, and callback-position binding.
- Unmatched relevant async APIs add one runtime coverage gap.
- Canonical indexed loops lower to collection iteration concepts and omit source mechanics.
- Dynamic selection uses business rule labels and no ordinal candidate labels.

### Unchanged behavior

- Application return values, thrown objects, callback ordering, and Java synchronization behavior
  stay unchanged.
- Inactive tracing returns the original callback when no lifecycle wrapper is needed.
- Manual context wrappers remain supported.
- Partial static or runtime evidence keeps an execution incomplete.
- Activation V2 remains readable and current activation bundles stay source-free at runtime.
- Mega Backend still produces five complete graphs with no Mega-specific production logic.
- Storage I/O does not run on the application thread.

## User Story 1: Truthful release evidence

**As a** release reviewer
**I want** the release command to fail on every failed sub-gate
**So that** a success marker proves that all checks passed

### Acceptance Criteria

- WHEN any standard, external, Mega, repository, or load command fails THE SYSTEM SHALL return a
  non-zero release status.
- WHEN a release command fails THE SYSTEM SHALL NOT print `RELEASE_GATE_OK`.
- THE SYSTEM SHALL preserve the failed command output in the evidence file.
- THE SYSTEM SHALL NOT require `rg` unless the workflow installs and verifies it.
- A negative executable contract SHALL inject a failing sub-gate and prove that the success marker
  is absent.

## User Story 2: Result-relevant operand evidence

**As a** business reviewer
**I want** each reason to include the business fact that was tested
**So that** I can understand why an edge was selected

### Acceptance Criteria

- WHEN a statically known parameter operand is part of a returned decision predicate THE SYSTEM
  SHALL add one manifest evidence binding for that operand and predicate node.
- WHEN that predicate runs THE SYSTEM SHALL record the typed operand value with its business-safe
  label and the exact selected edge.
- WHEN a method argument is outside the backward result slice THE SYSTEM SHALL NOT record it.
- THE SYSTEM SHALL NOT capture an argument only because its method has `@FachTracing`.
- THE explanation SHALL show a statement such as `age is below 24 (age was 20) — true` without an
  entry node, Java name, or identifier value.
- IF an operand cannot be captured exactly and that fact is required for the explanation THE SYSTEM
  SHALL add a precise coverage gap instead of capturing unrelated arguments.

## User Story 3: Async submission and cancellation integrity

**As an** application operator
**I want** each traced invocation to reach one terminal state after async rejection or cancellation
**So that** no decision record waits forever or disappears

### Acceptance Criteria

- WHEN a supported executor rejects a wrapped task THE SYSTEM SHALL release its reservation exactly
  once and preserve the original rejection.
- WHEN application code catches the rejection and returns a decision THE SYSTEM SHALL publish the
  successful decision after rollback.
- WHEN rejection escapes the annotated method THE SYSTEM SHALL publish one failed decision and
  preserve the same thrown object.
- WHEN a supported submitted future is cancelled before callback execution THE SYSTEM SHALL release
  the reservation exactly once.
- WHEN callback execution races with cancellation THE SYSTEM SHALL prevent reservation underflow,
  duplicate publication, and cross-trace evidence.

## User Story 4: Exact standard async bindings

**As an** application developer
**I want** standard async callbacks to be found by their actual API signature
**So that** context propagation works without manual wrappers

### Acceptance Criteria

- THE SYSTEM SHALL use an explicit binding table keyed by invocation owner, method, descriptor, and
  callback argument position.
- THE table SHALL include executor submission, common `CompletionStage` unary and binary methods,
  platform-thread constructors, and Java 21 virtual-thread factories used by the supported
  capability contract.
- WHEN `thenCombine`, `thenAcceptBoth`, `runAfterBoth`, or `Thread(ThreadGroup,Runnable)` runs THE
  SYSTEM SHALL wrap the callback argument, not the preceding stage or thread group.
- WHEN an invocation on a known async owner has no exact supported binding and can cross a trace
  boundary THE SYSTEM SHALL add one execution coverage gap.
- THE SYSTEM SHALL NOT guess a callback position from argument count or type order.

## User Story 5: Business-safe indexed iteration

**As a** business reviewer
**I want** collection decisions to read as business iteration
**So that** a diagram does not show Java loop mechanics

### Acceptance Criteria

- WHEN a canonical indexed loop iterates over one collection THE SYSTEM SHALL lower it to `for each
  entry` or `a following entry exists` concepts derived without application-specific vocabulary.
- THE SYSTEM SHALL omit counter initialization, counter update, `index`, collection `size`, and
  indexed item access from business nodes and edge labels.
- THE SYSTEM SHALL preserve result-relevant predicates, calls, mutations, early returns, and loop
  exits inside the body.
- THE SYSTEM SHALL replace ordinal dynamic labels such as `candidate 1` with business rule labels or
  neutral selected-rule outcomes.
- A generic conformance guard SHALL reject indexed-loop syntax patterns and ordinal candidate labels
  in all exported business graphs.

## Definition of Done

- [ ] Each root cause has a failing-before-change executable contract.
- [ ] The release negative test proves producer status propagation and no false success marker.
- [ ] A runtime explanation shows `age was 20` and does not record an irrelevant identifier.
- [ ] Caught and uncaught executor rejection tests publish one terminal record each.
- [ ] Cancellation before callback execution has no reservation leak.
- [ ] Binary stage methods and the thread-group constructor propagate context through the exact
  callback position.
- [ ] Unmatched async calls create an actionable coverage gap.
- [ ] Generic indexed-loop fixtures and all five Mega graphs contain no technical loop patterns or
  ordinal candidate labels.
- [ ] The external activation-bundle integration passes without runtime source analysis.
- [ ] The clean-clone release gate and 1,000-RPS gate pass with zero result changes, contamination,
  or silent record loss.
