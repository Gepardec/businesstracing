# Requirements: Runtime Evidence and Async Identity Correctness

## Overview

This P1 bug fix closes five review findings in the generic Java tracer. It corrects nested
asynchronous reservation ownership, cancellation coverage, future-object transparency, predicate
evidence timing, and Java vocabulary in business graphs. Mega Backend is only a conformance corpus.

## Root Causes

1. Prepared callbacks use one thread-local last-in-first-out stack. Submission completion removes
   the newest callback instead of the callback for that invocation.
2. Cancellation tracking is enabled only when the declared return descriptor is exactly
   `Future`, and it replaces the application future with a wrapper.
3. Operand evidence is read at annotated-method entry instead of when its predicate runs.
4. Evidence encoding failures are diagnostic-only and can leave an execution falsely complete.
5. The generic business-label lowerer and guard do not reject construction and type vocabulary.

## Impact and Blast Radius

- A nested synchronous stage callback can leak an async reservation and suppress a terminal record.
- Cancellation of `CompletableFuture` or `ForkJoinTask` can leave an execution active.
- Replacing a future can change identity, type tests, equality, interfaces, and displayed values.
- Reassigned parameters, loops, property access, and calculated operands can produce stale or false
  explanations.
- Business users can see Java construction, type, or helper-role terms in exported diagrams.
- Activation-bundle compatibility, agent bytecode, runtime memory, generic fixtures, and all five
  Mega graphs are in scope.

## Required Behavior

### Submission-specific reservations

- Each prepared callback SHALL be a submission-specific handle.
- Success, rejection, cancellation, and rollback SHALL act on that exact handle once.
- A synchronous callback that creates a nested rejected submission SHALL not remove or release the
  outer submission handle.
- A platform or virtual `Thread` SHALL bind its handle to the actual thread object until start.
- A traced invocation SHALL produce exactly one terminal record with no reservation underflow or
  cross-trace contamination.

### Cancellation without result replacement

- `Future`, `CompletableFuture`, and `ForkJoinTask` results from supported boundaries SHALL register
  cancellation against the original returned object.
- The agent SHALL return the exact object from the application call.
- Instrumented `cancel(boolean)` calls SHALL keep the original return value and application behavior.
- Successful cancellation before callback start SHALL release the exact reservation once.
- Started or completed callbacks SHALL not release twice.

### Predicate-site evidence

- A direct parameter operand SHALL be read from its current local slot immediately before the
  correlated predicate branch.
- Reassignment and loop reuse SHALL produce the value for each actual predicate evaluation.
- A property, local, or calculated operand that cannot be captured exactly SHALL create a precise,
  source-located execution coverage gap.
- An unsupported or failed evidence encoding SHALL make the execution incomplete. It SHALL not
  silently replace the required fact with only the Boolean predicate result.
- Result-irrelevant identifiers SHALL not be recorded.

### Business-only vocabulary

- Generic lowering SHALL remove Java construction and type mechanics such as `initialize`, `new`,
  and `enum type` from business labels.
- Generic lowering SHALL remove technical helper-role nouns such as `validator` when the label can
  express the business operation.
- The artifact guard SHALL reject these patterns in every graph.
- No production rule SHALL contain a Mega package, class, method, or domain-specific mapping.

## Compatibility

- Existing public tracing APIs and activation bundles SHALL remain readable.
- Application results, thrown objects, callback order, and future identity SHALL stay unchanged.
- Manual context wrappers SHALL continue to work.
- Unsupported cases SHALL fail closed with actionable coverage gaps.

## Definition of Done

- [ ] Independent fixtures cover nested synchronous callbacks, rejected inner submission, and
  thread-object ownership.
- [ ] Independent fixtures cover cancellation of `Future`, `CompletableFuture`, and `ForkJoinTask`.
- [ ] Tests prove result object identity, runtime type, equality, hash code, text, and interfaces do
  not change.
- [ ] Independent fixtures cover reassigned parameters, loop evidence, property operands, local or
  calculated operands, and evidence encoding failure.
- [ ] Generic graph fixtures and all five Mega graphs contain no prohibited Java vocabulary.
- [ ] External source-free activation still passes.
- [ ] Standard verification and the 600-second 1,000-RPS gate pass with zero result changes,
  contamination, or silent record loss.
