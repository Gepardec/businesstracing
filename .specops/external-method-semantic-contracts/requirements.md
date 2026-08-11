# Requirements: External Method Semantic Contracts

## Overview

Fachtracing needs a framework-neutral way to describe result-relevant behavior for compiled methods
whose source is not available. The analyzer must use exact, explicit facts and must remain fail-closed.

## User Story

As a Fachtracing extension author, I want to supply exact method semantics so that the analyzer can
map supported library behavior without trusting an entire dependency.

## Acceptance Criteria

- [x] WHEN one exact external method contract matches a compiled call THE SYSTEM SHALL apply its predicate, mutation, return, and exception facts.
- [x] WHEN source code for the called method is available THE SYSTEM SHALL analyze the source instead of an external contract.
- [x] WHEN two providers match the same method THE SYSTEM SHALL emit a source-located coverage gap and SHALL NOT select a provider.
- [x] WHEN no source or contract proves a result-relevant call THE SYSTEM SHALL retain the current opaque-boundary or coverage-gap behavior.
- [x] THE SYSTEM SHALL keep contract APIs independent of Spring and application-specific vocabulary.

## Constraints

- Match by binary owner name, method name, and JVM descriptor.
- Use this precedence: source, semantic contract, opaque-library boundary, coverage gap.
- Do not add a production dependency.
- Preserve the current exact analysis and runtime graph contracts.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Apply the single-responsibility principle.
- Do not use subagents.
