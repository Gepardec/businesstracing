# Design: Generic Business Graph Projection

## Architecture

Add `BusinessLogicGraph` as a separate public model. `BusinessGraphProjector` reads one
`AnalysisManifest.AnalysisResult` and lowers exact nodes into stable business nodes. It folds
technical dependency chains into the nearest rule or action, collapses loop searches into one rule,
and maps every terminal path to a result.

Add dedicated business renderers and a dependency-free JSON exporter. `ProjectGraphGenerator`
writes business artifacts first and keeps the current structure and developer artifacts.

## Public Contract

- Schema identifier: `fachtracing-business-graph/v1`.
- Node kinds: `RULE`, `ACTION`, `RESULT`, `GAP`.
- Files: `*-business.mmd`, `*-business.puml`, `*-business.json`.
- Schema file: `fachtracing-business-graph-v1.schema.json`.

## Vocabulary Policy

Reject `Start`, `Stop`, loop instructions, temporary derivation labels, comparison variables,
identifier/null implementation terms, raw `true` or `false` outcomes, source file names, framework
types, and route strings. Keep detailed gap reasons in developer output; use a concise gap label in
the business graph.

## Architecture Decisions

- Add a model and projector. Do not rename or replace `BusinessDecisionGraph` in this change.
- Keep each renderer and exporter responsible for one format.
- Make Maven output additive.
- Add no dependency.

### Dependency Decisions

| Package | Decision | Reason |
| --- | --- | --- |
| Java standard library | Approved | It can generate JSON and text formats. |
