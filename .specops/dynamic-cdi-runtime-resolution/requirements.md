# Feature: Dynamic CDI runtime resolution

## Overview

CDI can select a bean at runtime through `Instance<T>`. Static analysis cannot know the qualifier
values or deployment state for every call. Fachtracing must keep a safe candidate set and let the
real CDI container confirm the selected edge through normal method execution.

## Story 1: Keep runtime-observable candidates

**As a** Jakarta EE developer
**I want** dynamic CDI candidates to be instrumented
**So that** the runtime trace can show the bean that the container selected.

**Acceptance Criteria (EARS):**

- [x] WHEN a recognized dynamic CDI lookup cannot be proved statically THE SYSTEM SHALL add a visible static coverage gap and SHALL retain each source-compatible concrete candidate as a runtime-observable dispatch target.
- [x] WHEN execution enters one retained candidate after the dynamic dispatch THE SYSTEM SHALL record only that candidate edge.
- [x] IF execution does not enter a retained candidate THEN THE SYSTEM SHALL keep the existing unresolved runtime dispatch gap.

## Story 2: Prove behavior in a real CDI container

**As a** maintainer
**I want** an executable CDI conformance test
**So that** bytecode instrumentation and container proxies are tested together.

**Acceptance Criteria (EARS):**

- [x] WHEN Weld SE resolves `Instance<Rule>.select(qualifier).get()` for two qualifier values THE SYSTEM SHALL produce one trace for each call with the exact selected rule and without the other rule.
- [x] THE SYSTEM SHALL run the dynamic CDI conformance in the pull-request verification gate.
- [x] THE SYSTEM SHALL keep Weld and Jakarta CDI binaries in test scope only.

## Constraints

- CDI remains the only authority for bean resolution and contextual instance creation.
- The agent does not call `CDI.current()`, `BeanManager`, producers, or application constructors.
- The static graph remains incomplete because runtime observations cannot prove all future calls.
- Use ASD-STE100 Simplified Technical English.
- Keep each component focused on one responsibility.
- Do not use hardcoded diagrams.
