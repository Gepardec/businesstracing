# Design: Self-Dogfood Business Tracing

## Architecture Overview

The project already supplies every part of the self-analysis path except a production annotation and a repeatable repository command. The change marks one Maven-plugin policy with `@FachTracing`, installs the current reactor artifacts, invokes `analyze-reactor`, checks the generated business graph, and explains that graph in project documentation.

## Technical Decisions

### Decision 1: Trace Developer-Graph Export Enablement

**Decision:** Annotate `ProjectGraphGenerator.developerOutput` with the business label `enable developer graph export`.

**Rationale:** The method implements a real user-facing policy. It disables export when both settings are absent and enables export when both settings are present. The method also rejects partial configuration, but the current result slice does not include that thrown validation path. The guide shall state this observed limit.

### Decision 2: Keep Generated Graphs in `target/`

**Decision:** Generate the aggregate result under `target/fachtracing` and verify it through a shell gate.

**Rationale:** Existing project memory requires reproducible output to stay outside Git. The guide can include the reviewed graph as documentation while Maven remains the source of generated artifacts.

### Decision 3: Use the Public Integration Path

**Decision:** The gate shall use the released-style Maven goal coordinates after it installs the current project.

**Rationale:** This path proves the annotation, reactor adapter, source analyzer, Mermaid renderer, and artifact writer work together. A direct unit call would skip the integration that users run.

### Decision 4: Refresh Existing Dependencies Before Self-Analysis

**Decision:** Use the newest compatible stable dependency and build-plugin releases published on or before 2026-08-04.

**Rationale:** The three-day floor avoids a release that is too new for this change. The refresh removes the blocking `plexus-utils:3.5.1` advisory before the project runs its own plugin.

## Module Design

### Production Decision Entry

**Responsibility:** Expose one existing Maven-plugin configuration policy to Fachtracing analysis.
**Interface:** `@FachTracing("enable developer graph export")` on `ProjectGraphGenerator.developerOutput`.
**Dependencies:** Existing transitive access to `fachtracing-api` through `fachtracing-engine`.

### Self-Tracing Gate

**Responsibility:** Build the plugin, generate the aggregate graph, and reject missing or incorrect output.
**Interface:** `./scripts/verify-self-tracing.sh`.
**Failure response:** Exit non-zero when Maven fails, required files are absent, or expected business paths are absent.

### Dogfood Guide

**Responsibility:** Show the generated Mermaid graph, explain its two result outcomes, and state the missing validation-path limit.
**Interface:** `docs/self-tracing.md`, linked from `README.md`.

## Usage Example

```sh
./scripts/verify-self-tracing.sh
```

The command writes `target/fachtracing/enable-developer-graph-export-structure.mmd` and the matching PlantUML file.

## Testing Strategy

- Run the self-tracing shell gate from a clean project state.
- Assert that the aggregate index, Mermaid graph, PlantUML graph, and activation bundle exist.
- Assert that the Mermaid graph contains the decision label and both result outcomes.
- Run the focused Maven test suite after the source annotation change.

## Risks & Mitigations

- **Risk:** A self-referential Maven plugin cannot execute before it exists in the local repository. **Mitigation:** Install the selected plugin module and its reactor dependencies before aggregate analysis.
- **Risk:** A broad production annotation creates a graph that is too technical to explain. **Mitigation:** Trace one small, user-visible configuration policy.
- **Risk:** Generated files create review noise. **Mitigation:** Keep all generated files under ignored Maven `target/` directories.
- **Risk:** A reader assumes that the result graph contains input validation. **Mitigation:** State that partial configuration throws in source but is not present in the current graph projection.

### Dependency Decisions

No new dependencies are introduced.

| Component | Selected version | Decision |
|-----------|------------------|----------|
| ASM and ASM Tree | 9.10.1 | Keep; current stable release |
| Maven Core and Plugin API | 3.9.16 | Keep; current stable Maven 3 release |
| Maven Plugin Annotations and Plugin Plugin | 3.15.2 | Keep; current stable release |
| Plexus Utils | 3.6.1 | Upgrade from 3.5.1; newest compatible 3.x release and advisory fix. Version 4 removes the required `Xpp3Dom` API. |
| Maven JAR Plugin | 3.5.1 | Upgrade from 3.5.0; current stable release |
| H2 | 2.4.240 | Keep; current stable release |
| PostgreSQL JDBC | 42.7.13 | Keep; current stable release |
| GitHub Actions | Existing major tags | Keep; current releases satisfy the cutoff |

## Future Enhancements

- Add a runtime self-trace only if recursion and trace ownership have an explicit design.
