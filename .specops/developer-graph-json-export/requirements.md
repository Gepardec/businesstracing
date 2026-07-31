# Feature: Developer Graph JSON Export

## Overview

Fachtracing cannot yet hand its graph and source provenance to a developer tool as one stable artifact, so this feature adds a versioned JSON interchange export that can visualize a decision and open the exact committed source that produced each source-backed node.

## Developer Use Cases

### Use Case 1: Visualize the graph in another tool

**As a** developer integrating Fachtracing
**I want** a documented JSON graph export
**So that** graph tools can consume nodes, edges, labels, completeness, and coverage gaps without parsing Mermaid or PlantUML

**Acceptance Criteria (EARS):**

- WHEN an analysis result is exported THE SYSTEM SHALL produce deterministic UTF-8 JSON with a versioned schema identifier, graph metadata, nodes, edges, completeness, and coverage gaps.
- THE SYSTEM SHALL preserve opaque node and edge identifiers so an external tool can correlate its rendered elements with runtime observations.
- IF a label, attribute, path, or URL contains JSON control characters THEN THE SYSTEM SHALL escape them without changing their value after decoding.

**Progress Checklist:**

- [x] A deterministic versioned JSON graph is produced.
- [x] External tools receive stable node and edge identifiers.
- [x] JSON strings are encoded safely.

### Use Case 2: Open the exact committed source

**As a** developer inspecting a decision node
**I want** its source reference tied to the Git revision analyzed
**So that** navigation does not silently open newer or uncommitted code

**Acceptance Criteria (EARS):**

- WHEN Git provenance is captured from a clean repository THE SYSTEM SHALL record the repository URL, full commit identifier, commit timestamp, and repository-relative source paths.
- WHEN a graph node has a source mapping THE SYSTEM SHALL export its line, column, syntax kind, source fingerprint, and a source URL whose commit, path, and line placeholders are resolved.
- BEFORE JSON output THE SYSTEM SHALL compare each analyzed source fingerprint with the file in the captured clean revision and SHALL reject a mismatch.
- IF a source-backed path is outside the declared repository root THEN THE SYSTEM SHALL reject the export instead of exposing an absolute path.
- IF the Git working tree contains tracked or untracked changes THEN THE SYSTEM SHALL reject strict revision capture instead of claiming those changes belong to `HEAD`.
- WHEN a graph node is synthetic and has no source mapping THE SYSTEM SHALL export the node without a fabricated source location.

**Progress Checklist:**

- [x] Export provenance identifies a clean Git commit and its commit time.
- [x] Source-backed nodes contain revision-pinned navigation URLs.
- [x] Export rejects source content that does not match the analyzed fingerprints.
- [x] Absolute paths never enter JSON.
- [x] Dirty repositories and out-of-root paths fail explicitly.
- [x] Synthetic nodes remain valid without fake locations.

## API Design Principles

- The developer export is separate from `BusinessDecisionGraph`, `DecisionExplanation`, and persisted business records.
- Git is invoked only during explicit build-time provenance capture, never from runtime probes or application decision threads.
- The source-link URL is template-driven so GitHub, GitLab, and internal source browsers do not require product-specific renderer code.
- No JSON or Git library dependency is introduced.

## Compatibility Requirements

- The export remains Java 21 and framework neutral.
- JSON property order is deterministic for snapshot and content-addressed storage.
- The schema starts at `fachtracing-developer-graph/v1`; incompatible changes require a new schema identifier.

## Out of Scope

- A hosted graph UI or IDE plugin.
- Embedding source coordinates in business-facing Mermaid, PlantUML, explanations, or decision records.
- Navigating uncommitted source as though it were committed.

## Team Conventions

- Preserve developer provenance outside business records.
- Prefer dependency-free deterministic projections.
