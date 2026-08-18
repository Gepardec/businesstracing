# Design: Jakarta EE CDI and service semantics

## Architecture Overview

The engine keeps generic dispatch as its safe default. It gains an optional dispatch-candidate
selector SPI. The Jakarta EE module implements that SPI from compiler annotation metadata and
supplies exact external method contracts. The module uses binary names only; it does not import
Jakarta EE or gRPC types in production code.

## Technical Decisions

### Decision 1: Add a focused dispatch-candidate selector SPI

**Decision:** The analyzer asks enabled selectors whether one source candidate is valid for a
dynamic call. A selector can include, exclude, or abstain. An abstention keeps generic dispatch.

**Rationale:** CDI selection needs the injection-point element, qualifiers, and candidate type.
The existing external-method contract SPI has only a called method key and cannot own this work.

### Decision 2: Resolve only provable CDI injection

**Decision:** The Jakarta selector handles `@Inject` fields and constructor parameters. It accepts
source-visible, assignable classes with a CDI scope and matching qualifier annotations. It excludes
unselected alternatives. It abstains for unsupported CDI mechanisms.

**Rationale:** This improves precision without pretending to implement a CDI container.

### Decision 3: Keep remote calls as boundaries

**Decision:** Add exact contracts for JAX-RS response construction, Bean Validation checks, JPA
persistence actions, JTA actions, JAX-WS service setup, and common gRPC channel/stub setup.
Unknown SOAP or gRPC client operations stay source boundaries.

**Rationale:** Local framework operations have known semantics. Remote implementations do not.

### Decision 4: Use an external Jakarta EE conformance corpus

**Decision:** Add a pinned harness for `hantsy/jakartaee-rest-sample` and configure its
`TaskResources.allTasks` endpoint as a graph root.

**Rationale:** The application contains CDI-injected `TaskRepository` with an
`@ApplicationScoped` JPA implementation. It validates CDI selection in an unchanged application.

## Component Design

### Dispatch candidate selector SPI

**Responsibility:** Pass one dynamic-call receiver, injection point, contract type, and candidate
type to an optional framework selector. It does not interpret framework annotations.

### Jakarta EE dispatch selector

**Responsibility:** Decide whether CDI can select a source candidate from `@Inject`, scope,
qualifier, and alternative metadata. It does not load application classes or parse deployment XML.

### Jakarta EE method contract provider

**Responsibility:** Supply exact method semantics for supported APIs. It does not select CDI beans
or infer remote service behavior.

### Jakarta EE conformance harness

**Responsibility:** Build a clean pinned external source tree, run configured analysis, and assert
the selected CDI repository appears without non-bean source candidates.

## Failure Modes

- Missing, ambiguous, or unsupported CDI metadata: selector abstains; generic dispatch and its
  normal coverage behavior stay active.
- No matching CDI bean: selector excludes no generic candidate and records an explicit gap.
- Source-unavailable SOAP or gRPC service implementation: the method stays an explicit boundary
  or coverage gap.

## Testing Strategy

- Test CDI selector behavior with fixtures for field injection, constructor injection, qualifier,
  alternative, and abstention cases.
- Validate all contract signatures by reflection against test-scoped Jakarta EE and gRPC APIs.
- Run the pinned external CDI conformance command and existing conformance commands.

### Dependency Decisions

| Package | Version | Ecosystem | Decision | Rationale |
| --- | --- | --- | --- | --- |
| `jakarta.platform:jakarta.jakartaee-api` | 11.0.0 | Maven test | Approved | Supplies real CDI, JAX-RS, JPA, JTA, Bean Validation, and JAX-WS signatures without production coupling. |
| `io.grpc:grpc-api` | current compatible test version | Maven test | Approved | Supplies real gRPC setup and status signatures without production coupling. |
