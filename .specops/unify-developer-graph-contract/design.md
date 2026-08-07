# Design: Unified Developer Graph Contract

## Contract

The current multi-origin document shape becomes the only V1 shape:

- `schema` is always `fachtracing-developer-graph/v1`;
- the root requires `schema`, `graph`, `sourceOrigins`, and `sourceFiles`;
- each graph source and source-file entry requires `originId`;
- each origin declares `id`, `kind`, `identity`, and `checksum`;
- Git origins also require `revision`; only Git-mapped nodes can have `url`.

One origin is the normal single-repository case. More origins use the same arrays and references.

## Export API

`export(analysis, SourceCatalog)` remains the canonical implementation. The
`export(analysis, SourceRevision)` convenience method creates a `SourceCatalog` with one Git origin
whose stable identifier is `git`, then delegates to the canonical method. The old V1-only graph,
source, source-file, and verification helpers are removed.

## Schema API

`DeveloperGraphJsonSchema.generate()` has no input because the library supports one contract. It
generates the existing closed multi-origin definitions under the V1 identifier and V1 URN. Closed
enum values continue to come from production Java enums.

## Maven Output

`ProjectGraphGenerator` always builds a source catalog after it captures the clean Git revision. It
always writes `fachtracing-developer-graph-v1.schema.json` and links it as V1. Cleanup removes the
current filename and the exact old V2 filename so existing build directories do not retain a false
contract artifact.

## Compatibility Decision

This change intentionally replaces both new pre-consumer formats. There is no released V1 consumer,
so maintaining two shapes has no user value. The final current format starts at V1 and can gain V2
only when a future incompatible consumer contract needs it.

## Dependencies

No dependency is added or changed. The implementation uses existing Java, Maven, and test support.

## Verification

Consumer-level tests parse the schema independently and check the exact root, definitions, required
fields, origin rules, and Java-derived enums. Export and Maven contracts prove both one-origin and
multi-origin data use the same identifier and schema file. The repository PR gate then checks all
modules and both external conformance corpora.
