# Feature: Architecture Dojo Anspruch conformance

## Context

The linked repository `Gepardec/ArchitectureDojoAnspruch` has no `dojo-leistung` application source
on `main`. The newer solution is on `feature/solution1` at commit
`c87dda139553ee51c87d8ed640e9bddff11a2df6`. This feature pins that revision.

## User story

As a Fachtracing evaluator, I want source-generated business graphs for the benefit-entitlement and
incapacity-notification flows, so that I can review the Dojo solution with the V1 graph contract and
the graph viewer.

## Acceptance criteria

- [x] WHEN the pinned clean checkout is analyzed THE SYSTEM SHALL generate one business graph for
  benefit-entitlement checking and one business graph for incapacity notification.
- [x] THE SYSTEM SHALL derive all graph nodes and edges from the pinned Java source and SHALL NOT
  hardcode a diagram or graph topology.
- [x] WHEN a graph is exported THE SYSTEM SHALL validate it against the
  `fachtracing-business-graph/v1` JSON Schema.
- [x] WHEN the conformance run completes THE SYSTEM SHALL report the source count, node count, edge
  count, and completeness for each graph.
- [x] IF a generated business graph exposes Java source paths, owners, method syntax, or unresolved
  technical output THEN THE SYSTEM SHALL fail the conformance run.
- [x] WHEN the graph files are passed to the new viewer contract THE SYSTEM SHALL accept them.

## Out of scope

- Changes to the linked repository.
- A topology oracle or a manually maintained graph.
- Analysis of other solution branches.
