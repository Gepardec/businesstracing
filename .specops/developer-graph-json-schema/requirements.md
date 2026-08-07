# Feature: Developer Graph JSON Schema

## Overview

Frontend developers cannot validate or generate types for the developer graph JSON because the repository does not provide a formal JSON Schema. This feature generates the schema from the same Java contract that produces the graph export and writes a shareable schema artifact during Maven analysis.

## Developer Use Cases

### Use Case 1: Generate a formal schema from Java code

**As a** developer who consumes Fachtracing graph data
**I want** a public Java schema generator for each supported developer graph version
**So that** I can validate documents and generate frontend types without copying an informal contract

**Acceptance Criteria (EARS):**

- WHEN a caller requests `fachtracing-developer-graph/v1` or `/v2` THE SYSTEM SHALL return deterministic JSON Schema Draft 2020-12 text for that exact data schema.
- THE SYSTEM SHALL derive closed enum values for node kind, completeness, and source origin kind from the Java enums that the exporter uses.
- THE SYSTEM SHALL reject an unsupported developer graph schema identifier.
- THE SYSTEM SHALL set `additionalProperties` to `false` for contract objects and SHALL mark every emitted field as required except fields that the exporter can omit.

**Progress Checklist:**

- [x] V1 and V2 schemas are available through a public Java API.
- [x] Java enum changes affect generated schemas without a copied enum list.
- [x] Unsupported schema identifiers fail explicitly.
- [x] The schema distinguishes required and optional export fields.

### Use Case 2: Give the schema to a frontend developer

**As a** Maven plugin user
**I want** the build to write and link the schema that matches generated developer JSON
**So that** I can give both artifacts to the frontend developer

**Acceptance Criteria (EARS):**

- WHEN developer JSON output is enabled THE SYSTEM SHALL write one UTF-8 `fachtracing-developer-graph-v1.schema.json` or `fachtracing-developer-graph-v2.schema.json` file that matches the generated documents.
- WHEN the schema file is written THE SYSTEM SHALL link it from `index.md`.
- WHEN developer JSON output is disabled THE SYSTEM SHALL remove a prior generated developer graph schema without deleting unrelated files.
- WHEN the Maven contract test reads the schema THE SYSTEM SHALL parse the complete JSON and verify its dialect, data-schema constant, required fields, definitions, and Java-derived enums.

**Progress Checklist:**

- [x] Maven writes the matching schema artifact.
- [x] The generated index links to the schema.
- [x] Stale schema artifacts are removed safely.
- [x] Executable contracts parse and inspect the complete schema.

## API Design Principles

- The schema generator has one responsibility: describe the developer graph wire format.
- The exporter remains responsible only for graph data serialization.
- The Maven plugin selects the schema version and writes the artifact.
- The implementation adds no dependency and remains Java 21 compatible.

## Compatibility Requirements

- Existing developer JSON field names and schema identifiers do not change.
- Both the compatible single-origin V1 format and the multi-origin V2 format remain supported.
- JSON Schema consumers can resolve all local references without network access.

## Constraints and Assumptions

- JSON Schema Draft 2020-12 is the frontend contract dialect.
- The `$id` value is a stable URN because this repository does not host a public schema URL.
- Existing same-day dependency verification remains valid because this feature adds no dependency.

## Spec Dependencies

None. Related completed specs define the V1, V2, and Maven developer JSON behavior.

## Out of Scope

- Generating TypeScript types inside Fachtracing.
- Hosting schemas on a public web server.
- Adding formal schemas for activation bundles or decision records.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Do not use subagents.
- Keep each production class to one responsibility.
