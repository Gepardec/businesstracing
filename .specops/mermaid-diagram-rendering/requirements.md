# Feature: Mermaid Diagram Rendering

## Overview

Fachtracing currently persists structural and execution diagrams only as PlantUML. Applications
that render Markdown-native diagrams also need equivalent Mermaid output from the same generic
business graph and execution record.

## Acceptance Criteria

- [x] WHEN a business graph is supplied THE SYSTEM SHALL generate deterministic Mermaid flowchart source containing every node, edge, outcome, and visible coverage gap.
- [x] WHEN an execution is supplied THE SYSTEM SHALL distinguish visited and unvisited paths without changing business labels.
- [x] WHEN a decision record is saved THE SYSTEM SHALL persist structural and execution Mermaid alongside the existing PlantUML output.
- [x] THE SYSTEM SHALL escape Mermaid-sensitive label content and SHALL expose no Java provenance.
- [x] Existing PlantUML and persistence behavior SHALL remain unchanged.

## Scope

This feature adds Mermaid source generation and storage fields. Interactive viewers and Mermaid
runtime dependencies are out of scope.
