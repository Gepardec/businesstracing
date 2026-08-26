# Implementation Notes: Business Graph Semantic Explanation

## Phase 1 Context Summary

### Request

Define a generic correction for generated business graphs that are structurally valid but not
explainable. The supplied insurance application checks entitlement and submits incapacity
notifications. Its generated graph exposes architecture calls, selectors, generic values, mixed
language, negated comparisons, lost subjects, and code-shaped results.

### Current implementation

- `StaticDecisionAnalyzer` creates `choose by <expression>` for switches.
- `predicateLabel` falls back to rendered expression text.
- `negateBusinessLabel` adds `not` when no narrow rewrite applies.
- `BusinessGraphProjector` keeps each computation that does not match a short technical deny list.
- `BusinessLanguageNormalizer` applies text patterns after semantic context is already lost.
- `BusinessGraphSummarizer` merges gaps and equivalent graph states only.
- `BusinessLogicArtifactGuard` rejects selected tokens but does not validate complete statements.
- `BusinessGraphJsonExporter` writes the stable V1 contract and needs no shape change.

### Evidence

- BG-ENTITLEMENT has 29 nodes and 43 edges.
- BG-NOTIFICATION has 31 nodes and 45 edges.
- Both are structurally complete.
- Both contain the same entitlement subgraph and the same semantic defects.
- The notification graph adds a material save action that currently includes the port role.

### Scope assessment

One specification is sufficient. Role propagation, statement extraction, materiality, reduction,
and rendering are separate components, but they form one exact-to-business semantic pipeline. A
split would create unstable intermediate contracts and could allow text cleanup without subject or
path correctness.

### Assumptions

- The age limit is not assumed to be 18 unless source analysis proves it.
- The word `Kind` is not translated globally to `child`. Ownership and call context must prove the
  entity role. The renderer can retain the source noun when no generic translation exists.
- A save-port call is material when static effect analysis or an exact contract proves the write.
- The V1 business JSON has enough public fields for the improved graph.
- The supplied application does not need project-specific code, a glossary, or a contract provider
  for source-visible application methods.

### Dependencies

- Required: completed `generic-business-graph-projection`.
- Related: completed `context-aware-operation-labels`, `context-label-symbol-correctness`,
  `external-method-semantic-contracts`, and `business-graph-terminal-semantics`.
- No new library dependency is required.

### Security and privacy

The change uses static source and configuration. It does not add runtime customer data to labels.
Existing runtime evidence redaction stays unchanged.

## Phase 2 Status

The specification is complete and ready for implementation review. No production code changed in
this phase.
