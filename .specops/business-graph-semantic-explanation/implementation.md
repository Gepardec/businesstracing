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

The specification passed review. The implementation proceeded without a new dependency or public
format.

## Phase 3 Implementation Summary

- Added immutable semantic attribute names for owner, call, argument, type, return type, polarity,
  statement call, subject context, and aggregate scope.
- Added one business semantic reducer. It owns materiality, business label reduction, positive
  predicate form, and business branch inversion.
- Kept all callback and helper nodes in the exact graph. Aggregate scope metadata lets the business
  projection collapse those nodes without removing exact tracing detail.
- Collapsed statically proved `anyMatch` callbacks. A polymorphic rule catalog stays expanded. A
  method-reference callback or other callback with possible material effects stays expanded.
- Limited aggregate collapse to `java.util.stream.Stream.anyMatch`. A callback with a proven or
  possible state change stays expanded.
- Split ternary results and constructed result types into distinct result nodes.
- Removed redundant direct edges only when a same-outcome explanatory route reaches the same target.
- Kept `fachtracing-business-graph/v1` unchanged.

## Deviations from the Draft Design

The draft proposed a large standalone semantic-role and statement hierarchy. The implementation
uses attributed immutable node metadata plus one internal `Reduction` value. This smaller design
preserves the same production boundary and avoids a second graph model. It also keeps exact graph
nodes authoritative.

The implementation does not translate application nouns. It preserves terms such as `svnr`,
`versicherungszeiten`, and `stichtag`. A generic analyzer must not guess that an identifier represents
a person, route, account, or other entity.

## Verification Evidence

- `StaticDecisionAnalyzerTest`: passed, including retained exact callback details and a generic
  employment aggregate.
- `BusinessGraphProjectionTest`: passed, including selector removal, positive polarity with inverted
  branches, material-action retention, runtime mapping, and V1 export.
- Pinned supplied application: 39 source files and two decisions, both `COMPLETE`.
- Entitlement: before 29 business nodes and 43 edges; after 7 nodes and 10 edges.
- Notification: before 31 business nodes and 45 edges; after 8 nodes and 11 edges.
- Entitlement exact graph: 88 nodes and 114 edges, including expanded callback evidence.
- Notification exact graph: 90 nodes and 116 edges, including expanded callback evidence.
- Final entitlement JSON SHA-256:
  `27852e7df01dceb9a65c0e03a46972040d83ccdeaaffc0ef319d63ff53c3f7b1`.
- Final notification JSON SHA-256:
  `71da79b3f9b47c137f692c4db5e627f83c8852860b64817e349b67b14b78785f`.

## Five-Part Review

| Criterion | Entitlement | Notification | Evidence |
| --- | ---: | ---: | --- |
| Orientation | 1 | 1 | Decision title and subject-bearing first rules remain. |
| Rule clarity | 1 | 1 | Each visible rule is one answerable business check. |
| Branch clarity | 1 | 1 | Rule edges use `yes` and `no`; all nodes reach a result. |
| Relevance | 1 | 1 | No adapter, query, conversion, selector, or collection mutation remains. |
| Result clarity | 1 | 1 | Entitlement and notification results are distinct and source-proven. |

Both reviewed graphs score 5 of 5.

## Phase 4 Completion Summary

The generic contracts pass. Both supplied graphs pass the direct review. The exact graph keeps the
expanded callbacks and probes. The V1 business JSON contract is unchanged.
