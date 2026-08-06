# Design: Runtime Evidence and Async Identity Correctness

## Decision 1: Use the prepared callback as the exact handle

The transformer stores each prepared callback in a new method-local slot. Post-invocation runtime
calls receive that exact callback. The runtime removes that exact identity from its pending set.
Terminal rollback cancels all remaining handles for the active graph. Nested callback execution
cannot change which handle an outer invocation confirms.

Thread constructors bind the exact prepared callback to the returned `Thread` object. A successful
`Thread.start()` removes the binding for that object. Terminal rollback cancels an unstarted thread
binding that belongs to the active graph.

The transformer obtains safe local slots from the original method `maxLocals` in a read-only first
pass. This avoids a new ASM dependency and prevents collision with application locals.

## Decision 2: Track the original Future by identity

The collector keeps a bounded-lifecycle identity map from the original future to its reservation.
Registration returns the same future. The reservation removes its identity binding when callback
execution starts. Instrumented `cancel(boolean)` calls report the receiver, argument, and original
Boolean result to the runtime. A successful pre-start cancellation removes the binding and releases
the reservation.

The exact async catalog classifies `Future`, `CompletableFuture`, and `ForkJoinTask` return
descriptors as cancellable. It never infers an arbitrary application return type.

## Decision 3: Read direct operands at the branch site

Entry-time evidence capture is removed. Before a correlated branch, the transformer reads each
direct parameter binding from its current local slot and stages the typed value for that predicate
evaluation. Predicate consumption already removes staged evidence, so each loop iteration gets a
new value.

The analyzer emits an unavailable evidence binding when the result-relevant fact is a property,
local, or calculated value outside the exact direct-parameter subset. The branch probe turns this
binding into a source-located runtime coverage gap. Evidence encoding failures also add a runtime
gap. The Boolean edge can still be recorded, but the execution is incomplete.

## Decision 4: Normalize business operations before export

One generic label normalizer removes construction verbs, Java type phrases, and helper-role nouns.
It keeps the operation and business operands. The artifact guard uses generic patterns for the same
prohibited vocabulary. It does not contain Mega names.

## Verification

1. Add failing-before-change fixtures for each review example.
2. Run engine and agent module tests during each task.
3. Run the standard verifier, external source-free integration, and Mega conformance.
4. Run PostgreSQL when configured and the 600-second 1,000-RPS release gate.

## Rollback

All manifest changes are additive or use compatible defaults. If exact operand capture is not
available, the runtime reports an incomplete execution. It does not guess. Async registration can
be disabled for one exact catalog binding without changing application objects.
