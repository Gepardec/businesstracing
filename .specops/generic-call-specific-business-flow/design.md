# Design: Generic Call-Specific Business Flow

## Architecture Overview

Fachtracing has an exact runtime graph and a separate business projection, but it does not preserve the relation between them. The agent therefore renders runtime observations directly from the exact graph. This change keeps projection traceability in memory, selects the business subgraph supported by one execution, and gives the same selected model to the automatic text and Mermaid renderers. A separate graph-semantic summarizer makes the build-time overview smaller without using reference-application facts.

## Technical Decisions

### Decision 1: Preserve projection traceability outside business artifacts

**Decision:** Record exact-node to business-node mappings, exact terminal-edge to business-result mappings, and the alternative exact edge paths represented by each business edge.

**Rationale:** A business edge can skip technical exact nodes. Runtime selection is correct only when one complete represented exact path was visited. The mapping stays in memory and is not part of JSON, Mermaid, text, activation, or stored business records.

### Decision 2: Select before summary

**Decision:** Project the exact graph with traceability, select the call-specific business subgraph, and then summarize that selected graph.

**Rationale:** Summary can merge equivalent states. Selection needs the pre-merge mapping so it can prove each included edge from exact runtime evidence.

### Decision 3: Summarize with graph semantics only

**Decision:** Collapse connected gap regions first. Then merge nodes only when their kind, label, and complete outgoing outcome-to-state behavior are equivalent under stable partition refinement.

**Rationale:** These rules depend on graph meaning and work for unknown applications. A node name, package, selected method, or reference label cannot affect the result.

### Decision 4: Use one evaluated graph for both automatic files

**Decision:** Add a business execution text renderer and use the existing business Mermaid renderer on the same selected `BusinessLogicGraph`.

**Rationale:** One model prevents text and diagram drift. The exact developer explanation API stays unchanged for existing consumers.

### Decision 5: Fail closed on inconsistent runtime evidence

**Decision:** Reject graph ID or version mismatch. If valid evidence does not identify one terminal result, add one safe incomplete result path instead of choosing a branch.

**Rationale:** A plausible but unproved business result is worse than an explicit gap.

## Component Design

### Component 1: `BusinessGraphProjection`

**Responsibility:** Store one immutable business graph and its in-memory exact-to-business traceability.

**Interface:** Business graph, exact node mapping, terminal edge mapping, and alternative exact paths per business edge.

**Failure mode:** Constructor validation rejects mappings that refer to absent business nodes or edges.

### Component 2: `BusinessGraphProjector`

**Responsibility:** Derive the detailed business graph and traceability from one exact graph.

**Interface:** Keep `project(AnalysisResult)` compatible and add a graph-only traceable projection method for runtime use.

**Failure mode:** Invalid or technical business output fails through the existing artifact guard.

### Component 3: `BusinessGraphSummarizer`

**Responsibility:** Condense one business graph through gap-region collapse and behavioral equivalence.

**Interface:** `BusinessLogicGraph summarize(BusinessLogicGraph graph)`.

**Failure mode:** If a rewrite cannot preserve a referenced entry or edge endpoint, construction fails and no artifact is written.

### Component 4: `BusinessExecutionGraphProjector`

**Responsibility:** Select the business nodes, edges, and result proved by one execution.

**Interface:** `BusinessLogicGraph project(BusinessDecisionGraph graph, DecisionExecution execution)`.

**Failure mode:** Graph identity mismatch throws a validation error. Missing terminal proof returns an incomplete business flow with one safe gap.

### Component 5: `BusinessExecutionTextRenderer`

**Responsibility:** Render one selected business graph as ordered, non-technical text.

**Interface:** `String render(BusinessLogicGraph graph)`.

**Failure mode:** Multiple selected exits remain explicit in graph order and make coverage incomplete; the renderer does not choose one.

### Component 6: Business trace file sink

**Responsibility:** Receive a completed execution, build one selected business graph on the daemon thread, and write both automatic artifacts.

**Interface:** Existing sink constructor and file naming remain unchanged.

**Failure mode:** Projection or file failure uses the existing diagnostic channel and cannot change endpoint control flow.

## Sequence Diagrams

### Build-time overview

```text
Exact graph -> Business graph projector: detailed graph
Business graph projector -> Business graph summarizer: application-neutral graph
Business graph summarizer -> Maven output: concise overview
```

### Runtime call

```text
Completed execution -> Daemon sink: exact selected edges
Daemon sink -> Traceable projection: detailed business graph and mappings
Traceable projection -> Execution projector: selected business subgraph
Execution projector -> Graph summarizer: concise evaluated flow
Graph summarizer -> Text and Mermaid renderers: one shared model
```

## Security Considerations

- **Data classification:** Runtime request and result values are Restricted. The selected business graph contains no values or technical provenance.
- **Data protection:** Traceability is in memory only. It is not serialized or written to automatic artifacts.
- **Input validation:** Graph ID, graph version, node IDs, edge IDs, and all mapping targets are validated before projection.
- **Application isolation:** Production code contains no Keycloak or Mega selector, label, method, or topology.

## Performance Considerations

- Projection and summary run on the existing daemon sink thread.
- Stable partition refinement has a bounded input equal to the generated business graph. It uses Java collections only.
- Runtime selection reuses the existing exact visited-edge resolver.

## Testing Strategy

- Add direct synthetic graph tests before any external conformance assertion.
- Prove two branch executions create different selected graphs.
- Prove a changed synthetic branch changes the overview.
- Prove connected gap collapse preserves external paths.
- Prove graph-version mismatch fails closed.
- Prove agent text and Mermaid contain the same selected labels and omit unselected labels and private values.
- Run repository integrity, Mega, Keycloak, and the full pull-request gate.

## Risks & Mitigations

- **Risk:** A business edge represents more than one exact route. **Mitigation:** Store alternative exact edge sequences and require one complete sequence to be visited.
- **Risk:** Equivalent-state merge joins different business contexts. **Mitigation:** Require equal node kind, equal normalized label, and equal complete outgoing behavior before merge.
- **Risk:** Sparse observations make the path ambiguous. **Mitigation:** Use only validated selected edges and the existing deterministic connector resolver; otherwise show an incomplete gap.
- **Risk:** Summary hides incomplete analysis. **Mitigation:** Collapse gaps but never remove the last gap in a selected or static incomplete flow.

## Dependencies & Blockers

### Dependency Decisions

No new dependencies are introduced. The implementation uses the Java standard library and current project modules.

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `generic-business-graph-projection` | It supplies the current business graph contract. | Yes | Completed |
| `runtime-decision-path-capture` | It supplies validated observed path evidence. | Yes | Completed |
| `configured-endpoint-business-tracing` | It supplies automatic endpoint output. | Yes | Completed |

### Cross-Spec Blockers

| Blocker | Blocking Spec | Resolution Type | Resolution Detail | Status |
| --- | --- | --- | --- | --- |
| — | — | — | — | — |
