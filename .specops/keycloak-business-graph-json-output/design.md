# Design: Keycloak business graph JSON output

## Approach

Use `BusinessGraphJsonExporter` after the existing Keycloak source analysis and business projection.
Use `BusinessGraphJsonSchema` to write the shared V1 schema beside the graph.

## Responsibilities

- `KeycloakConformanceTest` generates and verifies the graph from the pinned source.
- `BusinessGraphJsonExporter` owns the stable JSON document format.
- `BusinessGraphJsonSchema` owns the JSON Schema.
- The Keycloak README lists the disposable generated files.

## Data and security

The generated business graph is internal review data. The existing business artifact guard checks it
before export. The output has no source paths, Java owners, method names, or runtime request values.

## Verification

Run the pinned Keycloak conformance command. Parse the generated document and check the V1 schema
identifier, complete coverage, and node and edge arrays. Run the pull-request verification gate.
