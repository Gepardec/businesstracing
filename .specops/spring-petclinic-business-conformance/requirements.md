# Requirements: Spring PetClinic Business Conformance

## Overview

Spring PetClinic must prove that annotations plus the generic Spring adapter can produce complete,
business-readable graphs for realistic controller methods.

## User Story

As a Fachtracing evaluator, I want reviewed PetClinic Mermaid and JSON artifacts so that I can see
what annotated business methods do without reading their Java implementation.

## Acceptance Criteria

- [ ] WHEN the conformance gate analyzes owner search, visit booking, and pet registration THE SYSTEM SHALL report all three business graphs as complete.
- [ ] THE SYSTEM SHALL cover every return and failure path inside each annotated method.
- [ ] THE SYSTEM SHALL generate and review business Mermaid, PlantUML, and JSON artifacts.
- [ ] THE SYSTEM SHALL compare business JSON with committed reviewed oracles.
- [ ] THE SYSTEM SHALL reject prohibited technical vocabulary in business artifacts.
- [ ] THE SYSTEM SHALL reject PetClinic knowledge in all production modules, including the Spring adapter.

## Constraints

- Keep the pinned PetClinic revision.
- Use an annotation-only source overlay.
- Treat Spring binding and `@Valid` results as method inputs.
- Keep exact analysis artifacts as developer regressions; show business artifacts first.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Apply the single-responsibility principle.
- Do not use subagents.
