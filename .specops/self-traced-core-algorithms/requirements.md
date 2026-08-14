# Feature: Self-Traced Core Algorithms

## Overview

The current self-analysis traces one Maven export policy. It does not show the production method
that keeps or removes exact nodes, and it does not show how the analyzer selects entry, resolution,
external, and classpath inputs. Maintainers need these two focused algorithm graphs from current
production code.

## Developer Use Cases

### Use Case 1: Inspect the Exact-Node Keep or Remove Decision

**As a** Fachtracing maintainer
**I want** the exact-node projection classifier to be a traced production decision
**So that** I can see the rules that keep, remove, or replace graph content

**Acceptance Criteria (EARS):**

- WHEN an exact node is projected THE SYSTEM SHALL use the traced production classifier that the
  business graph projector calls.
- WHEN self-analysis runs THE SYSTEM SHALL generate exact, business, analysis-audit, and
  projection-audit Mermaid files for `include exact node in business graph`.
- WHEN the generated exact graph is inspected THE SYSTEM SHALL show structural, loop, node-kind,
  and technical-or-business classification paths that exist in the classifier source.

**Progress Checklist:**

- [x] The real keep or remove classifier is traced.
- [x] Production projection uses that traced method.
- [x] Generated files prove the classifier paths.

### Use Case 2: Inspect Graph Analysis Source Selection

**As a** developer who studies graph construction
**I want** the analysis source selector to be a traced production decision
**So that** I can see which source roles and project relationships enter an analysis request

**Acceptance Criteria (EARS):**

- WHEN a project has no entry source THE SYSTEM SHALL omit that project from graph analysis.
- WHEN a project has an entry source THE SYSTEM SHALL use connected project resolution sources,
  external resolution sources, connected classpaths, the entry sources, and the project compiler
  character set.
- WHEN the entry project is modular THE SYSTEM SHALL use only connected projects with module
  descriptors as project source inputs while retaining the connected classpath.
- WHEN self-analysis runs THE SYSTEM SHALL generate exact, business, analysis-audit, and
  projection-audit Mermaid files for `select source inputs for graph analysis`.
- THE SYSTEM SHALL use the traced source selector from `StaticDecisionAnalyzer`; it SHALL NOT keep
  a second untraced selection algorithm.

**Progress Checklist:**

- [x] One source-selector component owns the production algorithm.
- [x] Flat, modular, external, entry, resolution, and classpath roles have executable tests.
- [x] Generated files prove the source-selection paths.

### Use Case 3: Prove That Graphs Come From Code

**As a** maintainer
**I want** the self-tracing gate to check both algorithms twice
**So that** a manual or stale diagram cannot satisfy the proof

**Acceptance Criteria (EARS):**

- WHEN the same self-analysis runs twice THE SYSTEM SHALL produce identical audit Mermaid text for
  both new decisions.
- THE SYSTEM SHALL derive graph topology and labels from current analysis records and SHALL NOT use
  AI, network access, method-specific renderer content, or checked-in diagram bodies.
- WHEN source changes remove either traced decision THE self-tracing gate SHALL fail because the
  required generated files or current algorithm labels are absent.

**Progress Checklist:**

- [x] The gate checks both generated decisions and repeated output.
- [x] The guide points to generated files and their production call sites.
- [x] No manual Mermaid body documents either algorithm.

## Library Quality Requirements

- The change shall add no dependency.
- Each component shall have one responsibility: source selection, node projection, rendering,
  export, or verification.
- Existing exact, business, activation, runtime, and Maven output contracts shall stay compatible.
- Repository text shall use ASD-STE100 Simplified Technical English.

## Constraints and Assumptions

- `BusinessGraphProjector.projectNode` is the authoritative keep or remove decision.
- Project-aware source roles remain defined by `ApplicationSourceBoundary`.
- Mermaid files stay generated under `target/fachtracing` and are not checked in.
- The active `release-gate-timeout-budget` spec has no affected-file overlap.

## Dependencies and Blockers

This feature depends on the completed `deterministic-self-analysis-audits`,
`generic-business-graph-projection`, and `reactor-wide-implementation-resolution` specs.

## Success Metrics

- One self-tracing command generates both new algorithm graph sets.
- Focused source-selection and projection tests pass.
- Repeated self-analysis and the full repository verifier pass.

## Out of Scope

- Runtime execution tests for the two new algorithms.
- A checked-in architecture diagram.
- AI classification or diagram generation.
- A new public API or output format.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Use the single-responsibility principle.
- Do not use subagents.
