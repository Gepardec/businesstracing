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

## Version 2: Jakarta EE completeness contract

### Story 4: Do not hide unsupported CDI resolution

**As a** Jakarta EE application developer
**I want** every unproved CDI selection to make the graph incomplete
**So that** the graph cannot show an impossible bean path as complete.

**Acceptance Criteria (EARS):**

- [x] WHEN one recognized CDI injection point has a provable source-visible bean set THE SYSTEM SHALL include only matching beans.
- [x] WHEN CDI uses multiple origins, `Instance<T>`, `Provider<T>`, a producer, a portable extension, an XML-selected alternative, or other unproved resolution THE SYSTEM SHALL add a visible coverage gap.
- [x] WHEN an alternative has `@Priority` THE SYSTEM SHALL treat it as enabled, subject to type and qualifier matching.
- [x] WHEN a bean uses a custom scope or stereotype THE SYSTEM SHALL identify its bean-defining annotation through source-visible meta-annotations.
- [x] THE SYSTEM SHALL keep runtime CDI ownership in the container and SHALL use observed implementation entry to confirm the selected static edge.
- [x] WHEN runtime execution does not enter a proven candidate THE SYSTEM SHALL retain the existing unresolved-dispatch runtime gap.

### Story 5: Expose container-driven Jakarta EE behavior

**As a** graph consumer
**I want** container callbacks and cross-process boundaries to be explicit
**So that** a graph cannot claim full Jakarta EE coverage when the analyzer has only the direct Java call.

**Acceptance Criteria (EARS):**

- [x] WHEN a reachable method or class uses interceptor bindings, transactions, security, asynchronous execution, timers, CDI events, producer or disposer methods, decorators, lifecycle callbacks, or validation constraints THE SYSTEM SHALL add a visible coverage gap unless a provider proves that behavior.
- [x] WHEN an exact Jakarta EE API contract can trigger application callbacks or external processing THE SYSTEM SHALL retain its known local action and SHALL add a boundary-specific coverage gap.
- [x] THE SYSTEM SHALL cover JPA lifecycle and lazy behavior, Bean Validation validators, JMS consumers, REST and SOAP remote services, mail delivery, WebSocket peers, and gRPC peers with explicit incomplete-boundary facts.
- [x] WHEN `failOnIncomplete` is enabled ANY new Jakarta EE coverage gap SHALL fail graph generation through the existing incomplete-graph gate.

### Story 6: Publish a truthful coverage matrix

**As a** maintainer
**I want** a capability matrix and regression fixtures
**So that** support and incomplete areas are reviewable before release.

**Acceptance Criteria (EARS):**

- [x] THE SYSTEM SHALL document static CDI proof, runtime edge confirmation, annotation-driven gaps, and external-boundary gaps separately.
- [x] THE SYSTEM SHALL test custom CDI scopes, stereotypes, enabled alternatives, unresolved CDI lookup, container annotations, and incomplete API contracts.
- [x] THE SYSTEM SHALL keep production adapter code free of Jakarta EE and gRPC binary dependencies.

## Version 2 Limits

- The adapter does not reproduce a CDI container, an application server, a database, or a remote service.
- Runtime resolution stays passive. The agent does not call `CDI.current()`, `BeanManager.getReference()`, or application producers.
- A source-visible proof can remove candidates. Any mechanism that needs deployment metadata or runtime extension state stays incomplete.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep each component focused on one responsibility.
- Do not use hardcoded diagrams.
