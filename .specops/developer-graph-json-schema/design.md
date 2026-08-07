# Design: Developer Graph JSON Schema

## Architecture Overview

`DeveloperGraphExporter` already owns stable V1 and V2 schema identifiers and emits a fixed wire shape. A new `DeveloperGraphJsonSchema` class describes these formats as deterministic JSON Schema Draft 2020-12 documents. `ProjectGraphGenerator` selects the matching schema after it selects V1 or V2 developer JSON and writes one shared schema file for all graphs in the output directory.

## Technical Decisions

### Decision 1: Use an explicit schema generator

**Decision:** Add `DeveloperGraphJsonSchema.generate(String dataSchemaId)` as a public, dependency-free API.

**Rationale:** The exporter does not use serializable DTO records, so reflection cannot recover its conditional V1 and V2 shapes. An explicit generator keeps the public contract readable and does not add annotations or a schema library to unrelated graph models.

### Decision 2: Derive enum arrays from Java enums

**Decision:** Generate `NodeKind`, `Completeness`, and `OriginKind` schema enums by iterating the production enum values.

**Rationale:** These values are already code-defined closed sets. Direct derivation prevents an independent copied list from drifting.

### Decision 3: Generate one matching Maven artifact

**Decision:** Write `fachtracing-developer-graph-v1.schema.json` for single-origin output and `fachtracing-developer-graph-v2.schema.json` for multi-origin output. Link the file once from the index header.

**Rationale:** Every graph from one generator invocation uses the same provenance mode. One shared file avoids duplicate schemas and gives frontend developers an obvious artifact to share.

## Module Design

### Developer Graph JSON Schema

**Responsibility:** Generate the formal schema for one supported developer graph data-schema identifier.

**Interface:**

```java
String schema = new DeveloperGraphJsonSchema().generate(
        DeveloperGraphExporter.SCHEMA_V2);
```

**Failure behavior:** An unsupported or null identifier throws `IllegalArgumentException` before output is returned.

### Maven Graph Generator

**Responsibility:** Select and write the schema artifact that matches its developer JSON mode.

**Failure behavior:** Schema output uses the existing fail-fast file-write behavior. Stale generated schema files use the same narrow cleanup rule as other generated artifacts.

## Public API Surface

- New class: `at.gepardec.fachtracing.developer.DeveloperGraphJsonSchema`
- New method: `String generate(String dataSchemaId)`
- Existing exporter APIs and data schema identifiers stay unchanged.

## Contract Shape

Both documents use JSON Schema Draft 2020-12, closed objects, local `$defs`, and these shared definitions:

- `graph`
- `node`
- `edge`
- `coverageGap`
- `source`
- `sourceFile`
- `revision`

V1 requires top-level `schema`, `graph`, `sourceRevision`, and `sourceFiles`. V2 requires `schema`, `graph`, `sourceOrigins`, and `sourceFiles`. V2 adds `originId`, permits a source without `url`, and uses a `sourceOrigin` definition with an optional Git `revision`.

## Testing Strategy

- Write the contract assertions before the generator implementation.
- Parse the complete V1 and V2 generated schemas with the independent Maven test parser.
- Verify the dialect, `$id`, data-schema constant, root required fields, local definitions, optional source fields, and enum values.
- Generate real Maven developer JSON and verify that the matching schema file and index link exist.
- Run the full repository verification after focused tests pass.

## Dependency Decisions

No new dependencies. The existing Java string builder, enums, and independent test parser cover the requirement.

## Risks and Mitigations

- **Risk:** An exporter field changes without a schema update. **Mitigation:** Consumer-level tests compare the complete expected field sets for both versions.
- **Risk:** A future enum value is absent from the schema. **Mitigation:** Enum arrays are generated directly from the Java enum types.
- **Risk:** A stale schema remains after developer JSON is disabled. **Mitigation:** Extend the existing narrow generated-file cleanup rule and its contract test.
