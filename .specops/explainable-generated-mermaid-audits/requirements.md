# Feature: Explainable Generated Mermaid Audits

## Overview

The Keycloak branch generates an exact graph, a business overview, and an evaluated path. It does
not generate a readable explanation of how source became the exact graph or why exact nodes stayed
in or left the business graph. The tool must generate these explanations from recorded analysis
and projection decisions. It must not use AI or a diagram that is fixed for Keycloak.

## Use Case 1: Explain Source-to-Graph Analysis

**As a** developer who reviews a generated graph
**I want** an analysis audit Mermaid file
**So that** I can see which source constructs were included, excluded, or left as gaps

**Acceptance Criteria (EARS):**

- WHEN analysis completes THE SYSTEM SHALL generate an analysis audit from
  `AnalysisManifest.analysisDecisions()`.
- WHEN a source decision is included or unresolved THE SYSTEM SHALL relate it to its exact graph
  node kind and current label.
- WHEN a source decision is excluded THE SYSTEM SHALL show its source location and
  `EXCLUDED / NO_RESULT_EFFECT` without an exact-node relation.
- THE SYSTEM SHALL group repeated decisions by source kind, action, and reason so that a large
  application produces a compact diagram.
- THE SYSTEM SHALL include current counts and bounded examples that come from the analyzed input.

## Use Case 2: Explain Exact-to-Business Projection

**As a** developer who reviews business relevance
**I want** a projection audit Mermaid file
**So that** I can see which exact nodes are business content and which nodes are technical

**Acceptance Criteria (EARS):**

- WHEN projection classifies an exact node THE SYSTEM SHALL record one final decision with the
  exact kind, exact label, action, stable reason, and final business-node relation.
- WHEN an exact node is structural, redundant, loop mechanics, unreachable, or technical THE
  SYSTEM SHALL record the matching removal reason.
- WHEN an exact node becomes a rule, action, or gap THE SYSTEM SHALL record the matching keep
  reason and final business node.
- WHEN a terminal edge becomes a result THE SYSTEM SHALL record a replacement decision and the
  final result node.
- WHEN business summary merges equivalent nodes or connected gaps THE SYSTEM SHALL map each kept
  exact decision to the final summarized node.
- THE SYSTEM SHALL preserve the current runtime traceability mappings and current business graph.

## Use Case 3: Generate and Prove the Files on Keycloak

**As a** user who evaluates Fachtracing on Keycloak
**I want** the conformance run to generate both audit files
**So that** I can inspect proof from real application source

**Acceptance Criteria (EARS):**

- WHEN Keycloak conformance runs THE SYSTEM SHALL write `search-users-analysis-audit.mmd` and
  `search-users-projection-audit.mmd` beside the current diagrams.
- WHEN the projection audit is checked THE SYSTEM SHALL contain structural removal, technical
  removal, business-rule retention, coverage-gap retention, and terminal-result replacement.
- WHEN the analysis audit is checked THE SYSTEM SHALL identify `UsersResource.java`, recorded
  analysis reasons, and exact graph node kinds.
- WHEN the same input runs twice THE SYSTEM SHALL produce byte-identical audit Mermaid.
- Production renderer and projector code SHALL NOT contain Keycloak owner names, method names,
  reviewed Keycloak labels, or fixed Keycloak topology.
- The Maven plugin SHALL generate and index the same two audit file types for every analysis and
  SHALL remove stale audit files through its normal generated-file lifecycle.

## Quality Requirements

- Add no dependency.
- Keep audit data outside business graphs, activation data, and runtime records.
- Keep one responsibility per analysis, projection, summary, rendering, and file-output component.
- Use ASD-STE100 Simplified Technical English in repository text.

## Out of Scope

- AI classification or generated prose.
- A manually maintained Keycloak diagram.
- Changes to the exact graph, runtime observation contract, or business JSON schema.
- PlantUML or JSON audit formats.

## Dependencies

This feature depends on `explainable-relevance-and-polymorphic-dispatch`,
`generic-business-graph-projection`, and `generic-call-specific-business-flow`.

## Success Measures

- Four Keycloak Mermaid files are generated: overview, evaluated path, analysis audit, and
  projection audit.
- Every exact Keycloak node has one final projection decision.
- Focused tests, Keycloak conformance, repository verification, and hosted CI pass.

