# Self-Tracing Fachtracing

Fachtracing applies its configured-entry-point feature to two production policies:

- `BusinessGraphProjector.classifyNode` decides why an exact graph node stays in or leaves the business graph.
- `AnalysisSourceSelector.selectPlan` decides whether analysis uses no project, connected project sources, or modular project sources. Both source plans also include external sources, the connected classpath, and entry sources.

The production callers use these methods. The example does not duplicate the algorithms and does not add `@FachTracing` annotations.

## Generate and Prove the Graphs

Run:

```sh
./scripts/verify-self-tracing.sh
```

The script uses the `self-tracing` Maven profile and the public `analyze-reactor` goal. It verifies these generated files under `target/fachtracing` for both policies:

- `*-structure.mmd`: the exact result-relevant algorithm;
- `*-business.mmd`: the concise business view;
- `*-business.json`: the same business topology as data;
- `*-analysis-audit.mmd`: source constructs and their inclusion decisions;
- `*-projection-audit.mmd`: exact nodes and their keep, remove, or replacement decisions; and
- `activation.json`: selected owners, methods, probes, fingerprints, and graphs for runtime tracing.

The script runs the compiled policies through the Java agent. It writes five evaluated Mermaid paths under `target/fachtracing-runtime`: two node-inclusion paths and three source-selection paths. Each path contains the recorded result of the actual call.

The gate also checks byte-identical static output on a second run. A focused renderer test changes synthetic input and checks that the generated audit changes. Repository integrity checks prevent fixed self-example labels and AI integrations in production graph generation.

All generated files stay under `target`. Git does not track them.
