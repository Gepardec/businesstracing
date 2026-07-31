# Design: Exact Runtime Decision Path Capture

## Architecture Overview

The current agent records that a predicate ran, and the diagram layer can infer edges between observed nodes. The change adds a static branch binding to the developer manifest. The agent uses this binding to emit the selected opaque edge. The collector stores this edge in the ordered execution. The same record model also gains a generic failed state, and each invocation owns its dispatch expectation stack.

## Technical Decisions

### Decision 1: Record edges at the branch site

**Decision:** Add developer-only branch bindings and emit a runtime edge event from the transformed conditional jump.

**Rationale:** The runtime sees the selected branch. A later shortest-path search does not have enough information when paths merge.

**Mapping rule:** The manifest binds each supported predicate jump to its zero-based occurrence within the transformed method and to one completion mode:

- `BOTH_OUTCOMES`: the jump target completes the full predicate as `false`, and fall-through completes it as `true`.
- `JUMP_FALSE`: only the jump target completes the full predicate, as `false`; fall-through continues evaluation.
- `JUMP_TRUE`: only the jump target completes the full predicate, as `true`; fall-through continues evaluation.

A simple predicate uses `BOTH_OUTCOMES`. A homogeneous flat conjunction uses `JUMP_FALSE` for each non-final operand and `BOTH_OUTCOMES` for the final operand. A homogeneous flat disjunction uses `JUMP_TRUE` for each non-final operand and `BOTH_OUTCOMES` for the final operand. The analyzer emits an exact group only when every atomic jump has a safe mapping. Mixed, nested, negated-compound, lambda-ambiguous, or incomplete groups keep legacy evaluated-node probes for the whole group.

The first jump of each predicate group must match its developer-only source line. Later jumps in the same group use the precomputed method occurrence because `javac` can assign either the group line or the operand line to those jumps. The next graph predicate must match its own source line.

### Decision 2: Keep legacy predicate probes

**Decision:** Use exact edge probes only when the manifest contains one `true` edge, one `false` edge, and a complete jump plan for the predicate group. Keep the existing evaluated-node probe as a fallback.

**Rationale:** Old and synthetic manifests remain valid. The runtime does not invent an edge when the static binding is incomplete.

### Decision 3: Complete failures at the annotated entry boundary

**Decision:** Add one synthetic catch-all handler to each instrumented entry method. The handler completes a failed execution and rethrows the same `Throwable` object.

**Rationale:** A caller method has no `ATHROW` instruction when a called method propagates an exception. An entry-boundary handler covers both explicit and propagated failures.

### Decision 4: Store dispatch expectations in the invocation

**Decision:** Move expected dispatch state from a scalar thread local to a stack in `InvocationContext`.

**Rationale:** The invocation already owns ordered mutable capture state. A stack preserves nested call order and keeps parent invocation state separate.

## Module Design

### Analysis manifest

**Responsibility:** Bind a source predicate probe to its opaque true and false graph edges.

**Interface:** Add immutable branch-target metadata with node ID, true and false edge IDs, owner and member hints, source line, method-local predicate index, and completion mode. Keep compatibility constructors for manifests without branch targets and for one simple predicate at index zero.

**Invariant:** A predicate group has exact targets for all its atomic probes or it has none. A partial group is invalid and uses the legacy fallback.

### Java agent transformer

**Responsibility:** Match the current method-local predicate occurrence, emit a two-sided or one-sided trampoline from its completion mode, and complete failed entry calls without changing application behavior.

**Failure response:** All calls enter the non-throwing `TraceRuntime` bridge. The synthetic failure handler rethrows the original application exception.

### Runtime collector

**Responsibility:** Validate and append edge events, complete success or failure, and manage invocation-local dispatch expectations.

**Performance:** Build an immutable `(node ID, edge ID)` index when a graph is registered. Runtime edge validation uses this index and does not scan the static graph.

**Failure response:** An invalid edge is ignored. A missing context is a no-op. A bridge failure becomes a developer diagnostic.

### Decision execution

**Responsibility:** Store the ordered path and one terminal state.

**Interface:** Add `SUCCEEDED` and `FAILED` state plus generic failure metadata. Preserve the existing successful constructor.

## Runtime Flow

```text
Instrumented entry -> RuntimeCollector: begin graph invocation
Instrumented branch -> RuntimeCollector: append node, outcome, and selected edge
Instrumented dispatch call -> InvocationContext: push expected dispatch
Implementation entry -> InvocationContext: consume matching dispatch
Instrumented return -> RuntimeCollector: complete success
Instrumented failure handler -> RuntimeCollector: complete failure
Instrumented failure handler -> Application: rethrow original Throwable
```

## Public API Surface

- `DecisionExecution` adds terminal status and optional generic failure data.
- `AnalysisManifest` adds branch-target metadata.
- `TraceRuntime` adds an exact edge operation.
- Existing successful constructors and methods remain available.

## Security and Data Handling

- Failure records contain no Java exception class, message, or stack trace.
- Branch bindings stay in the developer manifest.
- Business records contain only opaque node and edge identifiers and business outcomes.
- Existing value redaction remains mandatory.

## Testing Strategy

- Runtime unit tests cover exact edge validation, failed completion, nested dispatch, and parent-context cleanup.
- Transformer tests cover true, false, explicit failure, and propagated failure paths.
- Analyzer tests verify that complete boolean predicates get branch bindings and unsafe compound groups do not get partial bindings.
- Analyzer-to-transformer tests cover every decisive path of flat `&&` and `||` predicates and assert exactly one selected edge per predicate.
- Path resolver tests verify that an explicit edge is used as observed evidence and is not replaced by an inferred route.
- Existing engine, explanation, PlantUML, Mermaid, Maven plugin, concurrency, and load tests must pass.

## Risks and Mitigations

- **Risk:** The bytecode compiler can use a different conditional layout. **Mitigation:** Limit the exact binding contract to Java 21 `javac` output and retain fingerprint checks plus a safe legacy fallback.
- **Risk:** One source predicate can compile to several conditional jumps. **Mitigation:** Bind each jump by its method-local occurrence and completion mode. Emit an exact group only when the analyzer can map every jump.
- **Risk:** A partial compound plan can mix exact and inferred evidence. **Mitigation:** Validate the group as all-or-none and use legacy observations for the complete group when validation fails.
- **Risk:** A synthetic catch handler can process one exception twice. **Mitigation:** Remove the old direct `ATHROW` failure probe and install the handler only on entry methods.
- **Risk:** A failed record can break success-only projections. **Mitigation:** Preserve the success constructor and add explicit failed-result rendering tests.
- **Risk:** Edge validation cost can grow with graph size. **Mitigation:** Build the validation index once during graph registration and use constant-time lookup on application threads.

## Dependency Decisions

No new dependencies are introduced. The implementation uses the existing Java 21 and ASM 9.10.1 dependencies.
