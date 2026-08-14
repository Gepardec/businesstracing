# Feature: Deterministic Self-Analysis Audit Graphs

## Overview

The current self-trace generates exact and business graphs, but it does not export why the analyzer
included source code or why the business projector kept or removed exact nodes. Maintainers need
graphs that the tool derives from its own recorded decisions, without AI and without a diagram
template for one method.

## Developer Use Cases

### Use Case 1: Inspect Source Relevance Decisions

**As a** Fachtracing maintainer
**I want** one generated analysis audit graph for each traced decision
**So that** I can see why source constructs entered or did not enter the exact graph

**Acceptance Criteria (EARS):**

- WHEN static analysis completes THE SYSTEM SHALL generate an analysis audit graph from
  `AnalysisManifest.analysisDecisions()`.
- WHEN the audit contains one source decision THE SYSTEM SHALL show its source construct,
  `INCLUDED`, `EXCLUDED`, or `GAP` action, stable reason, and related exact nodes.
- WHEN a source construct has no result effect THE SYSTEM SHALL show
  `EXCLUDED / NO_RESULT_EFFECT` and SHALL NOT create an exact-node relation for it.

**Progress Checklist:**

- [x] Analysis audit Mermaid is derived from analysis decisions.
- [x] The graph explains included, excluded, and gap decisions.
- [x] Excluded source snippets are readable and bounded.

### Use Case 2: Inspect Technical-to-Business Projection Decisions

**As a** developer who evaluates the business projection
**I want** one generated projection audit graph for each traced decision
**So that** I can see which exact nodes are business content and which nodes are technical

**Acceptance Criteria (EARS):**

- WHEN business projection classifies an exact node THE SYSTEM SHALL record one audit decision
  with the exact label, exact kind, keep or remove action, and stable reason.
- WHEN an exact node becomes a business node THE SYSTEM SHALL relate it to the generated
  `RULE`, `ACTION`, `RESULT`, or `GAP` node.
- WHEN an exact node is structural, redundant, loop mechanics, or a technical calculation,
  predicate, choice, or dispatch THE SYSTEM SHALL record the matching remove reason.
- WHEN an exact terminal edge creates a business result THE SYSTEM SHALL record the result
  replacement and its generated result node.

**Progress Checklist:**

- [x] Every exact node has one final projection decision.
- [x] Terminal result creation is visible.
- [x] Audit data does not enter the business graph contract.

### Use Case 3: Apply the Audits to Fachtracing Itself

**As a** developer who studies the full project
**I want** the self-tracing command to generate and check both audit graphs
**So that** the project proves its own analysis and projection algorithms on production code

**Acceptance Criteria (EARS):**

- WHEN `verify-self-tracing.sh` analyzes the reactor THE SYSTEM SHALL create
  `enable-developer-graph-export-analysis-audit.mmd` and
  `enable-developer-graph-export-projection-audit.mmd`.
- WHEN the projection audit is checked THE SYSTEM SHALL show that entry and outcome nodes are
  structural, derivations are technical calculations, decision predicates are business rules,
  and terminal paths become business results.
- THE SYSTEM SHALL generate audit content from generic decision records and SHALL NOT use AI,
  network access, project-specific rules, method-specific labels, or checked-in diagram bodies.
- WHEN the same analysis runs twice THE SYSTEM SHALL produce identical audit Mermaid text.

**Progress Checklist:**

- [x] The self-trace creates and checks both files.
- [x] The guide tells readers how to inspect generated files.
- [x] Tests prove deterministic, input-driven rendering.

## Library Quality Requirements

- The change shall add no dependency.
- Each component shall have one responsibility: classification, audit data, rendering, export, or
  verification.
- Existing exact graphs, business graphs, activation data, and runtime records shall keep their
  current contracts.
- Audit graphs shall stay in developer build output.
- Repository text shall use ASD-STE100 Simplified Technical English.

## Constraints and Assumptions

- `AnalysisManifest.AnalysisDecision` remains the source-relevance audit contract.
- The business projector must report the final result after unreachable-node removal.
- Mermaid is the required audit graph format. JSON and PlantUML audit formats are out of scope.
- The active `release-gate-timeout-budget` spec does not own the engine projector, audit renderer,
  self-tracing script, or guide.

## Dependencies and Blockers

This feature depends on the completed `self-runtime-tracing`,
`explainable-relevance-and-polymorphic-dispatch`, and `generic-business-graph-projection` specs.

## Success Metrics

- One self-tracing command generates two algorithm audit graphs from current production analysis.
- Each exact node in the self projection has one auditable keep or remove reason.
- Focused tests and the full repository verification pass.

## Out of Scope

- AI classification or natural-language inference.
- A fixed architecture diagram of Fachtracing.
- Changes to business-facing JSON or runtime activation schemas.
- More production tracing annotations.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Use the single-responsibility principle.
- Do not use subagents.
