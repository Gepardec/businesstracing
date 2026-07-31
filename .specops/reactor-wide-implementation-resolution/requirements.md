# Feature: Reactor-wide Implementation Resolution

## Overview

Maven analysis cannot expand an interface call when its concrete implementations are source-visible only in sibling reactor modules. The analyzer must use all reactor sources for implementation resolution while it generates decision graphs only for entries in the current module.

## Developer Use Cases

### Use Case 1: Resolve sibling-module implementations

**As a** developer who analyzes a multi-module Maven build
**I want** Fachtracing to resolve interface and abstract-method implementations across the reactor
**So that** generated decision graphs include source-visible business rules from sibling modules.

**Acceptance Criteria (EARS):**

- WHEN an annotated entry calls an interface or abstract method and compatible implementations exist in sibling reactor modules, THE SYSTEM SHALL include each compatible source-visible implementation as a dispatch candidate.
- WHEN reactor sources include annotated entries from other modules, THE SYSTEM SHALL generate graphs only for annotated entries in the current Maven module.
- IF no sibling reactor sources are available, THEN THE SYSTEM SHALL retain the current single-module analysis behavior.
- IF the current Maven module has no Java sources, THEN THE SYSTEM SHALL skip before it resolves reactor classpaths.

**Progress Checklist:**

- [x] Compatible sibling-module implementations appear as dispatch candidates.
- [x] Sibling annotations do not create duplicate current-module output.
- [x] Single-module analysis remains compatible.
- [x] Source-empty modules skip before reactor classpath resolution.

## Library Quality Requirements

- The public analysis request SHALL preserve the existing three-argument construction and `of` factory behavior.
- Source and classpath inputs SHALL be deterministic and duplicate-free.
- The change SHALL add no dependency.

## Constraints & Assumptions

- The Maven reactor supplies source roots for projects in the active build.
- Only source-visible implementations can be expanded. Binary-only implementations remain an explicit coverage gap.
- Runtime dispatch correlation and graph semantics do not change.

## Dependencies & Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `maven-project-analysis` | Introduced the current-module Maven adapter. | No | completed record in project memory |

### Cross-Spec Blockers

| Blocker | Blocking Spec | Resolution Type | Resolution Detail | Status |
| --- | --- | --- | --- | --- |
| — | — | — | — | — |

## Success Metrics

- The new cross-module contract test finds all expected dispatch candidates and one current-module graph.
- All existing verification suites pass without changes to their expected behavior.

## Out of Scope

- Binary decompilation or source-jar discovery outside the active Maven reactor.
- A reactor-wide combined output directory.
- Changes to runtime instrumentation or dispatch selection.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Do not use subagents.
