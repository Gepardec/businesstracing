# Design: Deterministic Self-Analysis Audit Graphs

## Architecture Overview

The analyzer already records source-relevance decisions. The business projector will now return a
business graph and a separate immutable projection audit. One developer renderer will turn these
two audit inputs into Mermaid. The Maven output layer will write the two Mermaid files beside the
current technical structure files.

```text
source analysis decisions -> analysis audit renderer -> analysis audit Mermaid
exact graph -> business projector -> business graph
                                  -> projection decisions -> projection audit Mermaid
```

The diagram topology is generic. Labels, actions, reasons, and graph relations come only from the
current analysis and projection records.

## Technical Decisions

### Decision 1: Record Projection Decisions in the Projector

**Decision:** Add `BusinessGraphProjection` as an immutable result with the current
`BusinessLogicGraph` and one ordered decision for each exact node and generated terminal result.

**Rationale:** The projector knows the rule that classified each node. A renderer that tries to
infer the reason after projection can lose redundant, loop, and unreachable-node information.

### Decision 2: Keep `project` as a Compatibility Method

**Decision:** Add `projectWithAudit` and make the current `project` method return only the graph
from that result.

**Rationale:** Existing engine consumers need no change. The Maven exporter can opt in to the new
developer audit.

### Decision 3: Use Stable Action and Reason Enums

**Decision:** Projection decisions will use `KEPT`, `REMOVED`, and `REPLACED` actions with stable
reasons for structural nodes, technical patterns, business rules, business actions, gaps, loops,
terminal results, and unreachable output.

**Rationale:** Enum values make the algorithm testable and remove the need to parse prose or use
AI. The Mermaid renderer only changes enum names into readable labels.

### Decision 4: Render Recorded Decisions, Not Source-Specific Diagrams

**Decision:** `DecisionAuditMermaidRenderer` will accept analysis or projection results and build
one generic source -> decision -> output relation for each recorded decision.

**Rationale:** This produces a different graph for each analyzed method. Production renderer code
contains no self-trace label, package rule, business vocabulary, or fixed node list.

### Decision 5: Add Bounded Subjects for Excluded Source

**Decision:** The exclusion auditor will store a one-line bounded source subject for excluded
constructs.

**Rationale:** Excluded constructs have no exact node label. The subject makes the developer audit
readable while the existing source path and position remain the authoritative location.

## Module Design

### `BusinessGraphProjection`

**Responsibility:** Hold one business graph and its ordered developer-only projection decisions.

**Failure response:** Reject null fields, missing subject identity, and invalid output relations.

### `BusinessGraphProjector`

**Responsibility:** Classify exact nodes, create the business graph, and report the final reason and
mapping for each classification.

**Failure response:** Keep the current business artifact guard and fail on prohibited business
vocabulary.

### `DecisionAuditMermaidRenderer`

**Responsibility:** Render recorded analysis and projection decisions as deterministic Mermaid.

**Failure response:** Reject a graph and audit that do not identify the same graph version.

### `ProjectGraphGenerator`

**Responsibility:** Write the two new developer audit files and link them from `index.md`.

**Failure response:** Use the current fail-fast file output behavior.

## Audit Relations

### Analysis Audit

```text
source construct and location -> analysis action and reason -> exact node labels or no exact node
```

### Projection Audit

```text
exact node or terminal edge -> projection action and reason -> business node or no business node
```

These are format rules. The analyzed source decides the number, labels, actions, reasons, and
relations in each generated graph.

## Compatibility

- `BusinessGraphProjector.project` keeps its signature and result.
- `BusinessLogicGraph` does not gain developer fields.
- `AnalysisManifest` does not change its wire behavior.
- Runtime activation and decision records do not serialize either audit.
- Existing structure and business file names do not change.

## Testing Strategy

- Verify that source decisions with different actions and reasons produce matching analysis audit
  nodes and relations.
- Verify that an excluded fixture includes its bounded source subject and no exact-node relation.
- Verify all exact node classifications and terminal result replacements in projector tests.
- Render two different graphs and prove the output follows input labels and reasons.
- Run the self-tracing command and check the generated self audit files.
- Run the full repository verifier.

## Risks and Mitigations

- **Risk:** An initially kept node is later unreachable. **Mitigation:** Build the audit after
  unreachable-node removal and record `REMOVED / UNREACHABLE`.
- **Risk:** Audit data leaks into business output. **Mitigation:** Keep it in a separate projection
  result and developer renderer.
- **Risk:** A renderer becomes a hidden classifier. **Mitigation:** Store action and reason in the
  projector and test that the renderer only formats them.
- **Risk:** Source subjects make output very large. **Mitigation:** normalize whitespace and apply
  a fixed length bound.

## Dependency Decisions

No new dependency is introduced. Java records, enums, collections, and string builders provide all
required behavior.
