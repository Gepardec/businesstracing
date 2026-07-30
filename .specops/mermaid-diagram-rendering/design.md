# Design: Mermaid Diagram Rendering

Add a dependency-free `MermaidRenderer` that consumes the existing immutable graph/execution
models. Extract the execution-path edge resolver from the PlantUML renderer so both formats use
identical path semantics. Mermaid uses `flowchart LR`, stable generated aliases, shape syntax per
node kind, neutral dashed unvisited edges, emphasized visited edges, and a visible coverage note.

Extend `DecisionRecord` with structural and execution Mermaid strings. Preserve the existing
explicit `FachtracingEngine` constructor as a compatibility overload and add a constructor that
accepts both renderers.

## Dependency Decisions

No dependency is introduced; output is plain Mermaid text.
