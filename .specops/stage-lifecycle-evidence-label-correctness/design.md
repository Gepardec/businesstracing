# Design: Stage Lifecycle, Evidence, and Label Correctness

## Decision 1: Observe the returned stage

The exact async catalog classifies stage results separately from cancellable future results. After
the application call, the agent passes the returned stage and exact prepared callback to the
runtime. The collector adds a `whenComplete` observer. The observer releases the reservation only
if it is still reserved. The existing atomic reservation state prevents a second release.

Failure mode: registration can run after a synchronous callback. The reservation is already
released, so the observer does no work. A runtime registration error becomes a diagnostic and does
not change the application result.

## Decision 2: Instrument cancel in verified classes

The transformer applies one cancellation-only class pass to each class that has an activation
fingerprint. This pass visits all methods, including methods with no graph bindings. Graph probes
remain limited to selected methods. The fingerprint gate prevents changes to unknown class bytes.

Failure mode: an application class without an activation fingerprint is not changed. Analysis and
activation generation must include the source class for this support contract; the tracer does not
guess types or change third-party classes.

## Decision 3: Spill exact invocation arguments

For a catalog match, the method visitor stores arguments from right to left in new local slots. It
stores the receiver for non-static calls, wraps only the catalog callback slot, and reloads the
receiver and all arguments in order. Constructor calls keep their existing safe stack handling
because an uninitialized receiver must not cross a runtime call.

## Decision 4: Scan explicit receivers

The evidence scanner visits an explicit method receiver. A direct parameter creates the normal
typed evidence target. `this` and `super` are not facts. Any other explicit receiver that cannot be
read exactly creates the existing unavailable evidence target at the source line.

## Decision 5: Lower helper calls by role

The normalizer no longer deletes `validator`. The source renderer omits a receiver only when the
call operation itself is `validate`; the verb already states the business action. Other calls keep
their receiver words. The artifact guard rejects Java construction terms, not arbitrary domain
nouns.

## Verification

1. Add tests that fail for each review example before production changes.
2. Run engine and agent module tests after each code change.
3. Run standard, external activation, and Mega conformance checks.
4. Run the 600-second 1,000-RPS release gate.

## Dependency Safety

The change adds no dependency. It uses Java `CompletionStage` and the existing ASM version.
