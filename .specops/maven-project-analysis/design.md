# Design: Maven Project Analysis

## Architecture

Add a `fachtracing-maven-plugin` reactor module with an `analyze` Mojo. Maven injects the current
`MavenProject`; the goal walks every compile source root, obtains Maven's resolved compile
classpath, invokes the existing `StaticDecisionAnalyzer`, and projects each graph through the
existing Mermaid and PlantUML renderers.

The analyzer's plural `analyzeAll` operation returns an empty list when no annotated methods are
present; singular `analyze` retains its explicit failure. A Maven-independent project generator
owns naming, cleanup, rendering, and index creation so it can be tested without embedding Maven.

The goal is thread-safe, requires compile dependency resolution, and defaults to
`process-classes`. It writes `<slug>-structure.mmd`, `<slug>-structure.puml`, and `index.md` under
`${project.build.directory}/fachtracing`. Duplicate business labels receive a short stable graph-ID
suffix rather than overwriting one another. Only prior generated structure files and the index are
replaced, so unrelated output-directory content remains untouched.

Modules with no Java sources or no annotations are normal in a reactor and are skipped. Attribution
errors are build failures. Incomplete graphs are emitted with warnings by default and become build
failures when `fachtracing.failOnIncomplete=true`. `fachtracing.skip=true` disables the goal.

## Maven Flow

```mermaid
flowchart LR
    P["mvn process-classes"] --> M["Maven resolves module and compile classpath"]
    M --> S["Discover Java source files"]
    S --> A["Analyze @FachTracing methods"]
    A --> R["Render Mermaid and PlantUML"]
    R --> O["target/fachtracing/index.md"]
```

## Failure Semantics

- No source or no annotation: successful skip with an informational log entry.
- Unresolvable compile classpath, attribution error, write error, or duplicate output failure:
  `MojoExecutionException` with module context.
- Incomplete graph: warning unless strict mode is enabled.

## Security and Data Classification

Generated diagrams are **Internal** and may contain business vocabulary derived from source. The
plugin performs no network calls and does not inspect test sources, resources, runtime values, or
credentials. Existing business-projection privacy rules remain authoritative.

### Dependency Decisions

| Dependency | Version | Scope | Decision | Rationale |
| --- | --- | --- | --- | --- |
| `org.apache.maven:maven-plugin-api` | 3.9.16 | provided | Approved | Official Maven Mojo API; matches the verified Maven 3.9 baseline. |
| `org.apache.maven:maven-core` | 3.9.16 | provided | Approved | Supplies the injected `MavenProject` and resolved classpath contract. |
| `org.apache.maven.plugin-tools:maven-plugin-annotations` | 3.15.2 | provided | Approved | Official annotation-based descriptor generation recommended by Maven. |
| `org.apache.maven.plugins:maven-plugin-plugin` | 3.15.2 | build | Approved | Generates the standard plugin descriptor and goal prefix metadata. |

All are Apache Maven project components under Apache-2.0. No target-application runtime dependency
is introduced; Maven provides the API/core dependencies to the plugin.
