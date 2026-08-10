# Design: Explicit opaque library boundaries

## Architecture Overview

The engine receives exact archive paths, not Maven coordinates. The Maven plugin owns coordinate resolution because Maven owns the effective dependency graph. The analyzer owns graph extraction and tests the resolved binary owner against the explicit archive set.

## Technical Decisions

### Decision 1: Fail closed by default

**Decision:** Existing analyzer methods use an empty opaque-library boundary.

**Rationale:** A JAR location proves packaging, not whether the code is technical or business code.

### Decision 2: Add one engine boundary value

**Decision:** Add `OpaqueLibraryBoundary` as an immutable set of normalized, existing JAR paths. Add analyzer overloads that accept this value.

**Rationale:** This keeps user trust separate from source discovery and avoids changing the `AnalysisRequest` and `ApplicationSourceBoundary` record contracts.

### Decision 3: Resolve exact paths in the Maven adapter

**Decision:** Add `OpaqueLibraryArtifactResolver`. It accepts `groupId:artifactId` values, searches resolved artifacts from the selected projects, intersects them with the compile classpath, and returns exact archive paths.

**Rationale:** The engine must stay independent of Maven artifact metadata. Exact paths prevent package-name and coordinate-prefix inference.

### Decision 4: Keep Boolean rules fail closed

**Decision:** Selection enables reference-returning operations and Boolean calls that are already explicit source control predicates. It does not make a direct binary Boolean decision complete.

**Rationale:** A direct binary Boolean result can contain the full business decision. A source control call site already supplies the visible predicate boundary.

## Components

### `OpaqueLibraryBoundary`

Owns validation and immutable exact archive-path membership.

### `BinaryTypeOriginResolver`

Resolves the first ordered classpath match and returns both origin kind and normalized location.

### `StaticDecisionAnalyzer`

Uses an empty boundary by default. Overloads carry an explicit boundary through normal and JPMS analysis into each extractor.

### `OpaqueLibraryArtifactResolver`

Parses exact Maven coordinates and maps them to resolved compile-classpath JAR files. It reports all unresolved configured coordinates.

### Maven goals

Both goals expose `fachtracing.opaqueLibraryArtifacts` and pass one resolved boundary to `ProjectGraphGenerator`.

## Configuration

XML:

```xml
<opaqueLibraryArtifacts>
  <opaqueLibraryArtifact>org.mongodb.morphia:morphia</opaqueLibraryArtifact>
</opaqueLibraryArtifacts>
```

Command line:

```sh
-Dfachtracing.opaqueLibraryArtifacts=org.mongodb.morphia:morphia,org.apache.commons:commons-lang3
```

## Test Strategy

- Change the compiled archive contract to prove that default analysis is incomplete.
- Pass the exact fixture JAR and prove the two reference-operation graphs are complete.
- Keep direct Boolean and class-directory regressions incomplete.
- Add focused Maven resolver contracts for valid, missing, invalid, and directory cases.
- Run Hogarama twice: default strict failure, then explicit strict success.
- Run Java capability, full Maven, repository integrity, and pull-request gates.

## Risks and Controls

- **Risk:** Users can select a business-rule JAR. **Control:** The option is explicit, exact, fail-fast, and documented as a trust declaration.
- **Risk:** A coordinate resolves differently across reactor projects. **Control:** Include all exact matching compile-classpath JAR paths from the selected project set.
- **Risk:** Public API growth. **Control:** Add one immutable value and overloads. Keep existing methods source-compatible and fail closed.
- **Risk:** Configuration spelling drift. **Control:** Use one property name and the same parameter type in both goals.

## Dependencies

No new software dependency is required.
