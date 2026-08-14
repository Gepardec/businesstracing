# Design: Explainable Generated Mermaid Audits

## Architecture

The analyzer already records source decisions. The projector already keeps runtime traceability.
This change adds final projection decisions, a traceable summary result, one compact audit
renderer, and output wiring.

```text
analysis decisions -> audit grouping -> analysis audit Mermaid
exact graph -> traceable projection -> traceable summary -> projection decisions
                                                   -> business graph
projection decisions -> audit grouping -> projection audit Mermaid
```

The renderer receives data records. It does not classify source or exact nodes.

## Decisions

### Decision 1: Extend the Existing Projection Result

Add ordered developer-only decisions to `BusinessGraphProjection`. Keep a compatibility
constructor for the current four traceability maps. The runtime selector continues to use the
same exact-node, terminal-edge, and exact-edge-path mappings.

### Decision 2: Return Summary Traceability

Add `BusinessGraphSummarizer.summarizeTraceable`. It returns the summarized graph and a map from
each input business node to its final representative. The current `summarize` method remains and
returns only the graph.

### Decision 3: Separate the Final Audit View

Add `BusinessGraphAudit`. It holds the final summarized business graph and the remapped projection
decisions. It does not hold runtime edge paths. `BusinessGraphProjector.projectWithAudit` returns
this type. The existing `project` method returns its graph.

### Decision 4: Store Stable Reasons in the Projector

Use `KEPT`, `REMOVED`, and `REPLACED` actions. Use enum reasons for structural nodes, redundant
rules, loop mechanics, loop rules, technical node types, business rules, business actions,
coverage gaps, terminal results, completed fallback, and unreachable nodes. The projector records
the reason at the classification point.

### Decision 5: Render Compact Input-Driven Groups

`DecisionAuditMermaidRenderer` groups actual decisions by source kind or exact kind, action and
reason, and target kind. Each decision node shows the actual count and up to three current labels.
This keeps a large Keycloak audit readable. A changed input changes counts, examples, reasons, or
relations. The class contains no application-specific labels or topology.

## Component Responsibilities

- `BusinessGraphProjection`: immutable raw projection, runtime mappings, and raw decisions.
- `BusinessGraphSummarizer`: graph summary and original-to-final node mapping.
- `BusinessGraphAudit`: immutable final business graph and final decisions.
- `BusinessGraphProjector`: node classification and final audit assembly.
- `DecisionAuditMermaidRenderer`: deterministic grouping and Mermaid formatting.
- `ProjectGraphGenerator`: file names, writes, index links, and stale-file cleanup.
- `KeycloakConformanceTest`: black-box application proof only.

## Compatibility

- `BusinessGraphProjector.project` keeps its signature.
- `BusinessGraphProjector.projectTraceable` keeps its runtime mappings.
- `BusinessLogicGraph`, activation JSON, developer JSON, and runtime records do not change.
- Current business Mermaid, PlantUML, JSON, and technical structure files keep their names.

## Failure Behavior

- Reject a graph and manifest with different identity or version.
- Reject a projection decision that points to an unknown final business node.
- Reject a projection whose runtime mappings do not match its raw graph.
- Keep the current business artifact guard and incomplete-analysis behavior.

## Test Strategy

- Use synthetic graphs to cover every keep, remove, and replace category.
- Prove that summary mappings send merged gaps and equivalent nodes to final representatives.
- Render analysis decisions with included, excluded, and gap actions.
- Render two changed inputs and prove deterministic, input-driven output.
- Verify Maven file generation, index links, and stale cleanup.
- Run pinned Keycloak conformance twice and compare audit hashes.
- Run repository integrity, full verification, and hosted CI.

## Risks

- **Audit and final graph can diverge.** Remap decisions through summary traceability and validate
  every target.
- **Large source produces a large diagram.** Group decisions and bound examples.
- **Audit data can enter business output.** Keep it in developer-only records and Mermaid files.
- **Application-specific checks can become production rules.** Keep Keycloak assertions in the
  conformance module and add repository literal guards.

## Dependencies

No new dependency is introduced. Java 21 records, collections, and string builders are sufficient.

