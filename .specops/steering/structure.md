---
name: "Project Structure"
description: "Directory layout, key files, and module boundaries"
inclusion: always
---

## Directory Layout

```text
fachtracing/
  fachtracing-api/          Public annotations and extension interfaces
  fachtracing-engine/       Static analysis, graph model, runtime capture, rendering, and storage ports
  fachtracing-agent/        Lightweight bytecode runtime instrumentation
  fachtracing-maven-plugin/ Maven project discovery and static diagram generation
  conformance/              Pinned brownfield validation harnesses and reviewed outputs
  docs/                     Architecture diagrams and protocol documentation
```

## Key Files

- The root Maven POM coordinates API, engine, agent, and Maven plugin modules.
- `docs/maven-plugin.md` is the copyable arbitrary-project integration guide.
- Protocol documentation will define stable identifiers that correlate static tree nodes with runtime events.
- PlantUML files under `docs/` document architecture; runtime decision records contain generated PlantUML and Mermaid business-flow diagrams.

## Module Boundaries

- The API module exposes annotations without binding applications to analysis or storage internals.
- Analysis produces a business-only tree and stable node identifiers.
- Runtime capture records only the selected path and necessary evidence, then combines it with the analysis output.
- Persistence consumes versioned decision records through a storage-neutral port.
- The Maven plugin depends on the engine but keeps build-tool metadata outside the analyzer.
