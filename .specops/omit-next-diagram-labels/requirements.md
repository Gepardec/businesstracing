# Feature: Omit Redundant Next Diagram Labels

## Overview

The exact edge label `next` repeats the direction already shown by an arrow and makes business diagrams harder to scan.

## User Story

**As a** business-diagram reader  
**I want** ordinary sequence arrows to have no label  
**So that** the diagram shows only outcomes that add meaning

## Acceptance Criteria

- [x] WHEN Mermaid or PlantUML renders an edge whose exact outcome is `next` THE SYSTEM SHALL omit the visible edge label.
- [x] WHEN an edge has another outcome, including `next item` or `next entry`, THE SYSTEM SHALL show that outcome without change.
- [x] THE SYSTEM SHALL preserve graph edges, outcomes, identifiers, and execution-path resolution outside the diagram presentation.

## Out of Scope

- Changes to serialized graph data, developer JSON, runtime activation, or conformance oracles.
- Removal of meaningful iteration, branch, dispatch, failure, or result outcomes.
