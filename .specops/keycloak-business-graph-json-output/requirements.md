# Feature: Keycloak business graph JSON output

## User story

As a Fachtracing evaluator, I want the pinned Keycloak conformance run to write its generated
business graph as JSON, so that I can use the same V1 contract as Fachtracing and Mega.

## Acceptance criteria

- [x] WHEN the pinned Keycloak conformance run completes THE SYSTEM SHALL write the `search users`
  business graph with the `fachtracing-business-graph/v1` schema identifier.
- [x] WHEN the graph JSON is written THE SYSTEM SHALL also write the matching V1 JSON Schema.
- [x] THE SYSTEM SHALL generate the graph from the analyzed Keycloak source and SHALL NOT hardcode
  nodes or edges.
- [x] WHEN the output is generated THE SYSTEM SHALL report complete coverage and valid node and edge
  arrays.

## Scope

This feature changes only the Keycloak conformance output adapter and its output documentation.
It does not change the business graph contract or graph projection.
