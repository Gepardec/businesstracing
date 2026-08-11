# Requirements: Generic Business Graph Projection

## Overview

The exact analysis graph supports runtime tracing, but it exposes Java control mechanics. Fachtracing
needs a separate build-time graph that explains business rules, actions, and results.

## User Story

As a business reader, I want a concise graph from an annotated method so that I can understand its
rules and results without reading Java control flow.

## Acceptance Criteria

- [ ] WHEN analysis completes THE SYSTEM SHALL project a separate graph with only `RULE`, `ACTION`, `RESULT`, and `GAP` nodes.
- [ ] THE SYSTEM SHALL remove entry and stop markers, temporary calculations, loop mechanics, route literals, and raw Boolean labels.
- [ ] WHEN an annotated method returns or fails THE SYSTEM SHALL produce a named result for that path.
- [ ] IF a business artifact contains prohibited technical vocabulary THEN THE SYSTEM SHALL reject that artifact.
- [ ] THE SYSTEM SHALL preserve the existing exact graph and runtime correlation behavior.
- [ ] THE SYSTEM SHALL generate Mermaid, PlantUML, JSON, and a Draft 2020-12 JSON schema by default.

## Constraints

- The projection is deterministic and static.
- Labels come from source, annotations, or method contracts.
- Runtime probe correlation remains out of scope.
- Current structure and developer JSON files remain compatible.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Apply the single-responsibility principle.
- Do not use subagents.
