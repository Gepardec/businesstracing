# Project Memory

## Completed Specs

### generic-tracing-walking-skeleton

Completed version 2 on 2026-07-24. The framework-neutral Java 21 library derives typed
business-decision graphs from `@FachTracing` methods, correlates source-line-bound runtime probes
and polymorphic edges, stores typed/redacted evidence without arbitrary object stringification,
and produces business explanations plus structural/execution PlantUML.

The mandatory opaque brownfield proof uses pinned
`Gepardec/mega-backend@782cdec8dfe5b4062eb5c1859e6a9e53afe02770`. Five complete graphs
across four areas exactly match immutable independently source-derived oracles. A real
journey-warning manager execution records an empty typed collection input/result and all three
selected strategy edges. Production modules/configuration contain no Mega-specific hints.

Final verification passed the generic suite, pinned brownfield suite, and 600-second enabled load:
600,000 traces at 1,000 RPS, 0.267% p95 overhead, zero errors, mismatches, drops, or contamination.

Important follow-on constraint: new Java coverage must remain construct-level and target-neutral;
unsupported relevant constructs must create visible coverage gaps rather than guessed topology.

### mermaid-diagram-rendering

Completed on 2026-07-24. Decision records now provide deterministic structural and execution
Mermaid source alongside PlantUML. Both formats share one execution-path resolver, so observed and
inferred connecting edges are highlighted consistently. Mermaid output has stable aliases,
business-only labels, escaped sensitive characters, and explicit incomplete-coverage content.
Mega conformance generates five structural `.mmd` files and one highlighted runtime `.mmd` file.

### business-graph-terminal-semantics

Completed on 2026-07-24. Every generated decision uses exactly one Start and one Stop; return
edges state the returned expression and exceptional paths, including failures inside expanded
callees, converge on Stop. Standalone id/ids vocabulary is stripped generically. Result-relevant
null comparisons remain meaningful as absent/exists while raw Java null never reaches business
artifacts. Generic and pinned Mega verification pass; the journey-warning graph is 72 nodes and
90 edges after its nested validator failure was connected to Stop.
