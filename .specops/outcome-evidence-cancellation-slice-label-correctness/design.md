# Design: Outcome Evidence, Cancellation Reach, Slice, and Label Correctness

## Architecture Overview

The fix keeps the current static-graph and runtime-enrichment design. It makes each boundary use
evidence that the build or compiler can prove: pending runtime evidence, compiled cancellation
calls, attributed mutation targets, and source helper roles. If proof is absent, Fachtracing keeps
the graph incomplete instead of adding a guessed cause.

## Technical Decisions

### Decision 1: Merge evidence at the terminal observation

`RuntimeCollector.complete` consumes pending evidence for the Stop node, adds the encoded `result`,
and writes one terminal observation. `DecisionExplanationProjector` includes a Stop observation only
when it has evidence other than `result`. `BusinessStatementRenderer` renders those extra values as
reasons; the final result remains in the explanation header.

This keeps one observation and one Stop node. It does not create a synthetic predicate.

### Decision 2: Fingerprint compiled cancellation callers

`ClassFingerprintResolver` scans only selected Maven project output directories for the exact JDK
`cancel(boolean)` bytecode contracts supported by the agent. It adds those caller classes to the
existing graph-owner set. Dependency directories and jars are still lookup locations only.

The shared scanner uses the existing ASM dependency. The agent still checks the SHA-256 fingerprint
before it changes a class. A class with neither graph probes nor a supported cancel call returns
unchanged bytes.

### Decision 3: Slice from proven writes

`DependencyGraphBuilder` receives call-effect classification from the attributed analyzer. It adds a
call to `effectsByIdentifier` only for a proven mutation target:

- a supported JDK collection, map, array, or `Collections` mutation;
- a source method that directly writes receiver state or a parameter-backed mutable value.

Calls with no source and no safe platform contract are possible effects, not causes. If such a call
can change a result-dependent reference, the analyzer adds a source-located coverage gap. Primitive
arguments cannot be mutated and do not cause a gap. Calls whose returned value feeds the result
remain relevant through the normal definition and return dependency.

### Decision 4: Render labels from proven source roles

The generic renderer always keeps a normal call receiver. `FlowScanner` records a validation-helper
role only when one new-object local is used only as the receiver of `validate` calls. Calls through
that proven helper use the helper subject, such as `evaluate journey direction`. Other calls keep
the full receiver, such as `fraud validator validate` and `credit validator validate`.

Source mutation calls use their receiver and property, such as `set journey warning date`. A local
whose initializer call already expands into graph nodes uses the short label `derive <result>` and
does not repeat Java call order.

## Failure Handling

- Evidence encoding failure keeps the existing non-throwing runtime behavior and diagnostic path.
- A compiled class with an unreadable or conflicting fingerprint fails activation generation.
- An unknown result-relevant effect creates an incomplete graph at its source line.
- No runtime implementation or side effect is guessed.

## Performance

Build-time class scanning is linear in compiled application bytecode. Runtime work adds one small
map merge at decision completion and no new per-request reflection. The existing 1,000-RPS long gate
remains the performance contract.

## Testing Strategy

1. Add failing generic runtime, activation, slice, gap, and label contracts first.
2. Run focused module tests after each production change.
3. Run source-free external activation with a separate cancellation controller.
4. Regenerate and review the five Mega semantic graphs.
5. Run standard verification and the 600-second release gate.

## Dependency Decisions

| Package | Version | Ecosystem | Decision | Rationale |
| --- | --- | --- | --- | --- |
| None | — | Java | No new dependency | The project already uses ASM and compiler tree APIs. |

