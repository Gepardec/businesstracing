---
name: "Dependency Safety"
description: "Project dependencies, known issues, approved versions, and migration timelines"
inclusion: always
_generated: true
_generatedAt: "2026-08-07T11:25:46Z"
---

## Detected Dependencies

- Maven multi-module Java 21 build.
- ASM 9.10.1 in the Java agent.
- Maven Plugin API/Core 3.9.16 as provided plugin contracts.
- Maven Plugin Tools 3.15.2 for annotation-based descriptor generation.

## Runtime & Framework Status

Java 21 is the application/plugin baseline. Maven 3.9.x is the supported build baseline.

## Approved Versions

- ASM 9.10.1
- Maven Plugin API/Core 3.9.16 (provided)
- Maven Plugin Tools annotations/plugin 3.15.2 (provided/build)

## Banned Libraries

[Team-maintained: libraries that must not be used, with reasons]

## Migration Timelines

[Team-maintained: planned dependency upgrades and deadlines]

## Known Accepted Risks

[Team-maintained: acknowledged vulnerabilities with justification]

## Dependency Introduction Policy

**Default stance:** conservative (library vertical)
**Primary ecosystem:** Java (project intent; no build descriptor detected)

### Approved Patterns

- Compiler-provided source analysis APIs: approved when they avoid a third-party parser and preserve typed Java source structure.
- Focused bytecode visitor libraries: approved when runtime path correlation requires probe injection and the library has no transitive runtime dependencies.

### Rejected Patterns

[Accumulated from rejected dependencies with reasons]
