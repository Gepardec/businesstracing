# Requirements: Stage Lifecycle, Evidence, and Label Correctness

## Overview

This P1 bug fix prevents valid decision records from staying open and prevents complete graphs from
hiding missing business facts. It also keeps callback support consistent with the exact catalog and
keeps legitimate domain words in business labels.

## Root Causes

1. A stage reservation ends only when its callback starts or an instrumented cancel call runs.
2. The evidence scan visits method arguments but does not visit the method receiver.
3. Cancel probes exist only in methods that have graph bindings.
4. Callback wrapping supports only two stack layouts, not every catalog position.
5. Label cleanup removes `validator` from all labels without using the source call role.

## Impact and Blast Radius

- A skipped stage callback can prevent the only terminal record for a decision.
- A direct input such as `city` can be absent from an explanation while the graph is complete.
- A controller can cancel queued work without releasing its trace reservation.
- A supported three-argument stage call can produce a false coverage gap.
- A valid domain term can be removed and change the business meaning.
- The agent, runtime collector, static analyzer, generated labels, activation fingerprints, and Mega
  conformance artifacts are in scope.

## Required Behavior

### Stage lifecycle

- Each supported call that returns `CompletionStage` SHALL register the returned stage with the
  exact callback reservation.
- When the returned stage completes before its callback starts, the collector SHALL release the
  reservation once.
- Normal, exceptional, and binary-stage completion SHALL not leave a record open.

### Receiver evidence

- A direct annotated-method parameter used as a call receiver SHALL create an exact evidence target.
- An explicit receiver that cannot be captured exactly SHALL create a source-located coverage gap.
- An implicit `this` or `super` receiver SHALL not create a false business evidence gap.

### Cancellation reach

- The agent SHALL instrument supported `Future.cancel(boolean)` call sites in every verified
  application class in the activation bundle, including methods with no graph bindings.
- The cancel probe SHALL preserve receiver identity, arguments, return value, and thrown behavior.

### Exact callback position

- The agent SHALL wrap every callback position declared by `AsyncInvocationCatalog`.
- The agent SHALL save and reload receiver and invocation arguments in their original order.
- Explicit-executor binary stage calls SHALL run without an unsupported-boundary gap.

### Business labels

- Generic normalization SHALL preserve the standalone word `validator` when it is part of a
  business object name.
- Technical helper cleanup SHALL use source syntax or call role. It SHALL not use a global domain
  word deletion rule.
- Production rules SHALL contain no Mega package, class, method, or domain-specific mapping.

## Compatibility

- Existing activation bundles and public APIs SHALL remain readable.
- Application results, callback order, future identity, and exceptions SHALL stay unchanged.
- Unsupported cases SHALL stay incomplete and SHALL not be guessed.

## Definition of Done

- [ ] Independent tests cover skipped unary, recovery, and binary stage callbacks.
- [ ] Independent tests cover direct and unsupported method receivers.
- [ ] A cancel call in a method with no graph binding releases the queued reservation.
- [ ] An explicit-executor binary stage method is instrumented and records the exact path.
- [ ] A legitimate `validator` business label is unchanged, and technical helper labels stay clean.
- [ ] External source-free activation and all five Mega graphs pass.
- [ ] Standard verification and the 600-second 1,000-RPS gate pass with zero result changes,
  contamination, or silent record loss.
