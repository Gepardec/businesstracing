# Feature: Self-Dogfood Business Tracing

## Overview

The project does not trace its own production decisions, so maintainers cannot inspect a real Fachtracing graph from this repository without using a fixture or an external application.

## Developer Use Cases

### Use Case 1: Trace a Project Decision

**As a** Fachtracing maintainer
**I want** the project to analyze one real production policy from its Maven plugin
**So that** I can inspect what Fachtracing extracts from its own code

**Acceptance Criteria (EARS):**

- WHEN a maintainer runs the self-tracing gate THE SYSTEM SHALL generate a business graph for the developer-graph export enablement decision.
- THE SYSTEM SHALL keep generated self-tracing artifacts under Maven `target/` directories.
- IF the expected graph or its core outcomes are missing THEN THE SYSTEM SHALL fail the self-tracing gate.

**Progress Checklist:**

- [x] The project generates its own business graph from production source.
- [x] Generated files remain build output and do not enter Git.
- [x] The gate verifies the decision label and its enabled and disabled result paths.

### Use Case 2: Understand the Result

**As a** developer evaluating Fachtracing
**I want** a short explanation of the self-generated graph
**So that** I can understand the product from one concrete example

**Acceptance Criteria (EARS):**

- WHEN a reader opens the dogfood guide THE SYSTEM SHALL show the generated Mermaid decision graph and explain the decision paths.
- WHEN a reader follows the guide THE SYSTEM SHALL provide one command that regenerates and verifies the graph.

**Progress Checklist:**

- [x] The guide shows and explains the actual generated graph.
- [x] The README links to the guide.

## Library Quality Requirements

- The change shall add no dependency.
- Existing direct dependencies and build plugins shall use the newest compatible stable release published on or before 2026-08-04.
- Selected direct dependency versions shall have no advisory at or above the SpecOps `medium` threshold.
- Existing Maven, module, and verification behavior shall remain unchanged except for the added self-trace gate.
- The self-trace shall use the same public annotation, analyzer, renderer, and Maven goal that an integrating project uses.

## Constraints & Assumptions

- The selected production decision is `ProjectGraphGenerator.developerOutput` because it controls a user-visible product policy and has clear result outcomes.
- The source method rejects partial configuration. The current result slice does not include that thrown validation path, so the guide shall state this limit.
- The self-trace is static. Runtime self-instrumentation is outside this spec.
- Maven installs the plugin before it invokes the plugin against the reactor.
- The project version and Java 21 baseline are product settings, not dependency versions, and remain unchanged.

## Dependencies & Blockers

No spec-level dependency or blocker exists.

## Success Metrics

- One production decision graph is generated from this repository.
- The normal repository verifier runs the self-tracing gate.
- The guide reflects the checked output from the current implementation.

## Out of Scope

- Runtime agent activation against Fachtracing's own JVM.
- Tracking generated diagrams in Git.
- Adding annotations to more than one production decision.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Use plain Java and the existing Maven build.
