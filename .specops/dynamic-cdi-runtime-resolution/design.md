# Design: Dynamic CDI runtime resolution

## Architecture

The CDI selector reports `RUNTIME_OBSERVABLE` for `Instance<T>` and `Provider<T>`. The analyzer uses
that result to add one coverage gap and to emit dispatch targets for all source-compatible concrete
implementations. Other unresolved CDI mechanisms stay fail-closed. Existing entry probes confirm
the implementation that the container actually invokes.

## Decisions

### Decision 1: Observe CDI instead of reimplementing it

**Decision:** Keep CDI resolution passive. Instrument candidate method entry and correlate it with
the pending dispatch node.

**Rationale:** Runtime lookup can depend on qualifiers, alternatives, extensions, producers, and
context. An independent lookup could create beans or select a different contextual reference.

### Decision 2: Use a conservative candidate superset

**Decision:** For `UNRESOLVED`, retain all source-compatible concrete implementations and mark the
graph incomplete.

**Rationale:** A candidate that is absent cannot be confirmed at runtime. Extra candidates are safe
because an edge is recorded only when its implementation entry executes after the dispatch probe.

### Decision 3: Use real Weld SE conformance

**Decision:** Start Weld 6 with explicit bean classes. Execute two qualified `Instance.select()`
calls in a child JVM with the Fachtracing agent. Assert the business output for both calls.

**Rationale:** This covers CDI injection, runtime qualifier selection, client proxies, class
transformation, dispatch correlation, and output projection in one executable test.

## Data Flow

1. Static analysis finds a call through `Instance<T>`.
2. The CDI selector returns `RUNTIME_OBSERVABLE`.
3. The analyzer adds the static gap and emits compatible candidate edges and target probes.
4. Weld selects a contextual reference for the runtime qualifier.
5. The selected bean method entry confirms its dispatch edge.
6. The runtime output contains the selected business path.

## Failure Modes

- No source-compatible candidate: keep the static unavailable-implementation gap.
- CDI selects an implementation outside the source set: keep the runtime unresolved-dispatch gap.
- A class fingerprint differs from analysis output: do not transform the class.
- Weld cannot start or resolve the bean: fail conformance.

## Dependency Decision

| Package | Version | Scope | Decision | Rationale |
| --- | --- | --- | --- | --- |
| `org.jboss.weld.se:weld-se-core` | 6.0.4.Final | Test | Approved | It is the stable CDI 4.1 reference implementation for Jakarta EE 11 conformance. |
