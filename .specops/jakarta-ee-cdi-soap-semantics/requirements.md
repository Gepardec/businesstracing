# Feature: Jakarta EE CDI and service semantics

## Overview

Jakarta EE applications use CDI to select an implementation at an injection point, but generic
interface dispatch includes every source subtype. This produces paths that the container cannot
select. The feature adds an optional, exact adapter that selects source-visible CDI beans and
describes common Jakarta EE, SOAP, and gRPC boundary operations without application rules.

## Developer Use Cases

### Story 1: Resolve CDI injection targets

**As a** Jakarta EE application developer
**I want** analysis to use CDI injection metadata
**So that** a graph contains only beans that a declared injection point can select.

**Acceptance Criteria (EARS):**

- [ ] WHEN an `@Inject` field or constructor parameter has one source-visible CDI bean with matching type and qualifiers THE SYSTEM SHALL expand that bean implementation.
- [ ] WHEN a candidate does not declare a CDI bean-defining scope, has a non-matching qualifier, or is a disabled alternative THE SYSTEM SHALL exclude that candidate from the dispatch graph.
- [ ] IF CDI metadata is absent, ambiguous, dynamic, or cannot be proved THEN THE SYSTEM SHALL retain generic dispatch behavior and visible coverage gaps.

### Story 2: Describe Jakarta EE service boundaries

**As a** Jakarta EE application developer
**I want** common framework operations to have exact semantic contracts
**So that** container setup and persistence operations do not hide source-visible business rules.

**Acceptance Criteria (EARS):**

- [ ] WHEN the optional adapter is enabled THE SYSTEM SHALL provide exact contracts for supported JAX-RS response, Bean Validation, JPA, JTA, and JAX-WS factory operations.
- [ ] WHEN a SOAP or gRPC client call has no source-visible implementation THE SYSTEM SHALL preserve an explicit boundary or coverage gap instead of inferring remote business logic.
- [ ] WHEN a supported service signature changes or an unsupported signature is called THE SYSTEM SHALL remain fail-closed.

### Story 3: Verify on a real application

**As a** library maintainer
**I want** a pinned external Jakarta EE application conformance test
**So that** CDI resolution works in a realistic source corpus.

**Acceptance Criteria (EARS):**

- [ ] WHEN the pinned `hantsy/jakartaee-rest-sample` source tree is clean and at the reviewed commit THE SYSTEM SHALL analyze configured CDI-backed REST decisions with the Jakarta EE adapter.
- [ ] THE SYSTEM SHALL keep production code free of Jakarta EE and gRPC binary dependencies.
- [ ] THE SYSTEM SHALL use test-scoped API dependencies to verify each exact catalog signature.

## Constraints and Assumptions

- Create one optional `fachtracing-jakartaee` module with a single adapter responsibility.
- Support CDI field and constructor-parameter injection. Producer resolution, portable extensions,
  XML alternatives, and dynamic `Instance<T>` lookup are out of scope and remain explicit.
- Use only exact binary owners, method names, and JVM descriptors for service contracts.
- Use `hantsy/jakartaee-rest-sample@85da1d6861fea14579b1c6eb76253f0549a8e80f` as a conformance corpus.
- gRPC is an external-service integration, not a Jakarta EE standard.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep each component focused on one responsibility.
- Do not use hardcoded diagrams.
