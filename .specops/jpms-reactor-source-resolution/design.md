# Design: JPMS Reactor Source Resolution

## Architecture Overview

Maven module descriptors define compilation boundaries but contain no business methods. The Maven adapter will keep them out of the framework-neutral analyzer input. Maven's normal compiler phase still reads the descriptors, so the fix does not weaken JPMS validation.

## Technical Decision

### Filter module descriptors at the Maven boundary

**Decision:** `AnalyzeMojo.sourceFiles` accepts regular `.java` files except files named `module-info.java`.

**Rationale:** The analyzer cannot derive graph nodes from a module declaration, and a single javac analysis task cannot accept descriptors for several modules without module-source-path orchestration. A narrow boundary filter fixes the failure and does not add module-aware logic to the engine.

## Module Design

### Maven source discovery

**Responsibility:** Return stable, duplicate-free Java type sources for root selection and reactor-wide implementation resolution.

**Failure mode:** If filtering leaves the current module with no sources, the existing generator skip path removes stale output.

## Testing Strategy

- Add valid JPMS descriptors to both modules of the Maven reactor fixture.
- Run the real Maven lifecycle and verify both dispatch candidates.
- Run focused Maven tests and the complete verifier.

## Dependency Decisions

No new dependencies introduced.
