---
name: "Dependency Safety"
description: "Project dependencies, known issues, approved versions, and migration timelines"
inclusion: always
_generated: true
_generatedAt: "2026-07-10T13:56:48Z"
---

## Detected Dependencies

No dependency ecosystem or dependency manifest exists yet.

## Runtime & Framework Status

The Java runtime baseline has not been selected.

## Approved Versions

[Team-maintained: list approved dependency versions and ranges]

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
