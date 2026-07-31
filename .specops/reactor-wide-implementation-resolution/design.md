# Design: Reactor-wide Implementation Resolution

## Architecture Overview

The current analysis request uses one source list for two different jobs: selecting annotated roots and indexing source-visible callees. This change separates those jobs. `rootSourceFiles` selects graph entries, while `sourceFiles` remains the complete attributed source universe. The Maven plugin supplies current-module sources as roots and the union of reactor sources as the source universe.

## Technical Decisions

### Decision 1: Add an explicit root-source scope

**Decision:** Add `rootSourceFiles` to `AnalysisRequest` and keep the existing three-argument constructor and `of` factory as compatibility paths that treat all sources as roots.

**Rationale:** Filtering analysis results after full extraction would still create duplicate work and can fail on unrelated annotated sources. Root selection before extraction keeps module output isolated and lets the source index resolve sibling implementations.

### Decision 2: Build reactor inputs in the Maven adapter

**Decision:** Inject Maven reactor projects into `AnalyzeMojo`, collect all compile source roots and compile classpaths in stable project order, and pass current-module roots separately to `ProjectGraphGenerator`.

**Rationale:** Maven owns the active reactor and its resolved module inputs. The engine stays independent of Maven.

## Module Design

### Analysis request and source index

**Responsibility:** Carry the complete source universe and the smaller root-entry scope.
**Interface:** `AnalysisRequest(sourceFiles, compilationClasspath, charset, rootSourceFiles)` plus the compatible three-argument constructor and `of` factory.
**Failure mode:** Reject an empty root list or a root that is not in the source universe with `IllegalArgumentException`.

### Maven reactor input adapter

**Responsibility:** Build stable, duplicate-free source and classpath inputs for one module execution.
**Interface:** Private helpers in `AnalyzeMojo`; `ProjectGraphGenerator.generate` accepts root sources and all analysis sources.
**Failure mode:** If Maven exposes no reactor project list, use the current project only. If a source root does not exist, ignore it as before. If the current module has no Java sources, remove stale output and skip before reactor classpath resolution.

## Usage Example

```text
reactor:
  decision-entry/    -> @FachTracing method and Strategy interface
  local-strategy/    -> LocalStrategy implementation
  regional-strategy/ -> RegionalStrategy implementation

analysis roots: decision-entry sources
analysis universe: decision-entry + local-strategy + regional-strategy sources
output: one graph for decision-entry with two dispatch candidates
```

## Testing Strategy

- Engine contract: use separate fixture source roots and verify root filtering plus two compatible candidates.
- Maven generator contract: generate from a current-module root set and a reactor-wide source set.
- Maven reactor integration: run a two-module build where implementations are in a sibling module.
- Parent-POM lifecycle integration: run the plugin in a source-empty parent and both child modules, then verify that only the decision-entry module has output.
- Regression: run the full `scripts/verify.sh` suite, including source-empty module skip behavior.

## Risks & Mitigations

- **Risk:** Duplicate paths produce unstable compiler inputs. **Mitigation:** Normalize, deduplicate, and sort sources and classpath entries.
- **Risk:** Reactor sources create diagrams in the wrong module. **Mitigation:** Select annotated roots from `rootSourceFiles` before extraction.
- **Risk:** Broad sources include incompatible implementations with the same erased interface. **Mitigation:** Keep the analyzer's attributed subtype checks and receiver compatibility rules.

## Dependencies & Blockers

### Dependency Decisions

No new dependencies introduced.

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `maven-project-analysis` | Defines the Maven adapter extended by this feature. | No | completed record in project memory |

## Release Plan

Ship the request-contract and Maven-adapter changes together after the full verification suite passes.
