---
name: "Project Structure"
description: "Directory layout, key files, and module boundaries"
inclusion: always
---

## Directory Layout

```text
fachtracing/
  fachtracing-api/          Public annotations and extension interfaces
  fachtracing-analysis/     Static Java analysis and business-tree construction
  fachtracing-runtime/      Lightweight execution-path and value capture
  fachtracing-model/        Decision tree, execution trace, and result types
  fachtracing-persistence/  Storage ports and reference adapters
  fachtracing-testing/      Fixtures, sample application, and performance tests
  docs/                     Architecture diagrams and protocol documentation
```

## Key Files

- A root build descriptor will coordinate the Java modules once the build system is selected.
- Protocol documentation will define stable identifiers that correlate static tree nodes with runtime events.
- PlantUML files under `docs/` document architecture; runtime decision records contain generated PlantUML and Mermaid business-flow diagrams.

## Module Boundaries

- The API module exposes annotations without binding applications to analysis or storage internals.
- Analysis produces a business-only tree and stable node identifiers.
- Runtime capture records only the selected path and necessary evidence, then combines it with the analysis output.
- Persistence consumes versioned decision records through a storage-neutral port.
