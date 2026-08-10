# Refactor: Unify the Developer Graph Contract

## Rationale

Fachtracing has no released V1 consumer. The new schema work created two data contracts only because
the exporter already had a single-Git form and a multi-origin form. Keeping both forms makes the
frontend select between two schemas before compatibility requires that choice.

The multi-origin form already represents one Git repository with one `sourceOrigins` entry. It can
therefore serve all current export cases without a second contract.

## Current Behavior

- `fachtracing-developer-graph/v1` uses `sourceRevision` and source entries without `originId`.
- `fachtracing-developer-graph/v2` uses `sourceOrigins` and source entries with `originId`.
- The schema generator accepts a contract identifier and branches between two schemas.
- Maven selects one of two schema filenames from the source-input shape.
- Documentation tells frontend developers to select a matching V1 or V2 schema.

## Required Behavior

1. THE SYSTEM SHALL publish only `fachtracing-developer-graph/v1`.
2. WHEN a graph has one or more source origins THE SYSTEM SHALL use `sourceOrigins` and `originId`
   for every developer JSON document.
3. WHEN callers provide one `SourceRevision` THE SYSTEM SHALL represent it as one Git source origin
   in the same V1 document shape.
4. THE SYSTEM SHALL generate one Draft 2020-12 schema through a no-argument Java API.
5. WHERE Maven developer JSON is enabled THE SYSTEM SHALL write and link only
   `fachtracing-developer-graph-v1.schema.json`.
6. WHEN Maven cleans prior generated output THE SYSTEM SHALL remove the old V2 schema artifact
   without treating V2 as a supported contract.

## Unchanged Behavior

- Developer JSON stays deterministic and separate from business-facing records.
- Git source content stays commit-pinned and verified.
- External sources keep their origin identity and do not receive false Git URLs.
- Nodes, edges, coverage gaps, source fingerprints, and closed enum values remain available.
- Diagram-only Maven builds remain free of developer JSON and schema output.

## Acceptance Criteria

- [x] `DeveloperGraphExporter` exposes one schema identifier and one serialization shape.
- [x] Single-Git and multi-origin exports both declare `fachtracing-developer-graph/v1`.
- [x] Both export forms contain `sourceOrigins`; source mappings and files contain `originId`.
- [x] `DeveloperGraphJsonSchema.generate()` produces the multi-origin V1 schema.
- [x] No supported Java constant, schema branch, or documentation names V2.
- [x] Maven writes and links one V1 schema for every developer-output mode.
- [x] Maven removes a stale old V2 schema and keeps unrelated files.
- [x] Focused contracts and the complete repository gate pass.

## Scope Assessment

This is one library refactor. The exporter, formal schema, Maven adapter, tests, and documentation are
one coupled wire contract. There is one deliverable and one acceptance cluster, so decomposition is
not required.
