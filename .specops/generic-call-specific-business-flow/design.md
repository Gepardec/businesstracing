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

### Decision 6: Treat the source-visible caller as a semantic boundary

**Decision:** Represent a source-unavailable call as one atomic rule or action when its attributed caller use-site states that role. Omit a collaborator lookup when only later caller-visible operations give it meaning.

**Rationale:** A method overview explains what the caller does. It does not need the internal paths of each dependency. A returned opaque value with no caller-visible meaning remains a gap.

### Decision 7: Resolve binary owners with compiler binary names

**Decision:** Use the compiler's JVM binary name for bytecode lookup, including nested types, and keep source names only for business labels and developer provenance.

**Rationale:** A nested Java type uses `$` in its class-file path. Replacing dots in a qualified source name creates a path that cannot exist.

### Decision 8: Connect proved runtime segments through an explicit gap

**Decision:** When exact edge selection leaves two observed business segments disconnected, connect them only when the full generated business graph proves their order. Treat nodes with the same kind and label as equivalent path targets. Put one safe gap between the segments.

**Rationale:** Reused source methods can produce equivalent projected nodes for different call sites. Runtime probes can select one equivalent node while the full static route contains another. The connector must keep one path without inventing the hidden rule.

### Decision 9: Keep the overview at the method boundary

**Decision:** Expand source that is already in the analysis request, but keep source-unavailable dependencies as atomic caller rules and actions when the caller proves their role.

**Rationale:** Recursive project expansion makes a method graph large and mixes dependency internals with the method under review. The caller boundary keeps the graph concise and target-neutral.

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

### Component 7: Source-unavailable call boundary classifier

**Responsibility:** Decide whether a missing callee is already represented by source-visible caller semantics or still needs a coverage gap.

**Interface:** Classify one attributed call or callback from its use site, return type, enclosing control flow, and available binary proof.

**Failure mode:** An opaque returned value with no caller-visible meaning returns `UNRESOLVED`, which keeps one coverage gap.

### Component 8: Observed business segment connector

**Responsibility:** Connect selected runtime business segments when hidden exact edges or equivalent projected call-site nodes leave a break.

**Interface:** Accept the complete generated business graph, its exact mappings, one execution, and the selected nodes and edges. Return one selected node and edge set.

**Failure mode:** If the complete generated graph does not prove an order, leave the components separate. Never add a direct rule outcome without an explicit gap.

### Component 9: `BusinessLanguageNormalizer`

**Responsibility:** Convert general implementation phrases to plain business language.

**Interface:** Normalize one generated business label before artifact validation and rendering.

**Failure mode:** The business artifact guard rejects any technical phrase that remains.

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
- Prove direct Boolean decisions, caller-observed predicates, statement actions, collaborators, lazy callback actions, broad caught paths, and nested binary owners use their generic rules.
- Prove agent text and Mermaid contain the same selected labels and omit unselected labels and private values.
- Prove that an opaque returned value with no caller-visible meaning remains a gap.
- Prove general collection and empty-check phrases become plain business language.
- Generate the static Keycloak method overview from the endpoint source and assert zero gaps and zero prohibited technical terms.
- Run two live Keycloak calls and reject disconnected flows or contradictory outcomes for one rule.
- Run repository integrity, Mega, Keycloak, and the full pull-request gate.

## Risks & Mitigations

- **Risk:** A business edge represents more than one exact route. **Mitigation:** Store alternative exact edge sequences and require one complete sequence to be visited.
- **Risk:** Equivalent-state merge joins different business contexts. **Mitigation:** Require equal node kind, equal normalized label, and equal complete outgoing behavior before merge.
- **Risk:** Sparse observations make the path ambiguous. **Mitigation:** Use only validated selected edges and the existing deterministic connector resolver; otherwise show an incomplete gap.
- **Risk:** Summary hides incomplete analysis. **Mitigation:** Collapse gaps but never remove the last gap in a selected or static incomplete flow.
- **Risk:** A caller boundary hides dependency internals. **Mitigation:** State the dependency as one atomic rule or action only when the caller proves that role. Keep an opaque value unresolved when the caller gives it no meaning.
- **Risk:** Equivalent call-site nodes connect unrelated runtime segments. **Mitigation:** Require a directed path from the selected source to a node with the target kind and label in the complete generated graph, and put an explicit gap on the selected path.
- **Risk:** Generic language rewriting changes a valid domain phrase. **Mitigation:** Rewrite only recognized structural phrase forms and reject remaining technical vocabulary.

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
