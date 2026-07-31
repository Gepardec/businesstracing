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

### maven-project-analysis

Completed on 2026-07-31. Maven projects can add `fachtracing-api`, annotate decisions, and run a
fully qualified `fachtracing-maven-plugin:analyze` goal without plugin configuration. Projects can
optionally bind the goal to `process-classes`, making normal `mvn process-classes`, `package`, or
`verify` generate per-module Mermaid, PlantUML, and `index.md` output under `target/fachtracing`.
Maven source roots and resolved classpaths are discovered automatically; empty reactor modules skip,
strict incomplete coverage is configurable, and stale generated graphs are safely removed. Generic
and pinned Mega conformance remain green.

### developer-graph-json-export (feature) — 2026-07-31

Completed 2 tasks. Fachtracing now emits a deterministic schema-v1 JSON graph for developer tools.
The export contains stable topology, clean Git commit metadata, repository-relative locations,
source fingerprints, and commit-pinned links. Business graphs, records, Mermaid, and PlantUML stay
free of repository data. A final correction made source fingerprints an enforced check instead of
passive evidence. All executable contracts pass.

### maven-developer-graph-export (feature) — 2026-07-31

Completed 4 tasks. The Maven plugin can now write one opt-in UTF-8 `*-developer.json` artifact per
decision and link it from `index.md`. Diagram-only builds remain Git-free. Configured export captures
one clean revision and proves that every analyzed source matches the exact blob in that commit, so
ignored generated files cannot create false source links. A separate test parser verifies the full
artifact and non-empty coverage gaps. The missing Maven guide now documents the workflow. Full
verification passed with 0.146% p95 overhead and zero errors, mismatches, drops, or contamination.

### runtime-decision-path-capture

Completed on 2026-07-31. Runtime executions now store validated opaque branch edges, generic failed
terminal state, and invocation-local nested dispatch expectations. Java 21 `javac` predicates use
compile-time occurrence and completion metadata, so runtime probes only record which precomputed
edge completed the full predicate. Flat conjunctions and disjunctions record one exact edge;
ambiguous compound forms use legacy observations. Registered graph edges are pre-indexed for
constant-time validation. Full verification passed at 1,000 requests per second with 0.150% p95
overhead and zero errors, mismatches, drops, or contamination.
