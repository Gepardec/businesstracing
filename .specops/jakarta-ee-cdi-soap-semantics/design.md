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

## Version 2 Architecture

### Decision 5: Separate abstention from unresolved framework selection

**Decision:** Extend the selector result with `UNRESOLVED`. `ABSTAIN` means that a selector does not own the receiver. `UNRESOLVED` means that the selector recognizes the framework injection point but cannot prove its runtime bean set.

**Rationale:** Generic inclusion is safe only when no framework owns the receiver. It is not safe after CDI ownership is known.

### Decision 6: Attach incompleteness to exact external contracts

**Decision:** An external method contract can list coverage-gap facts. The analyzer keeps the known local operation and adds each declared gap at the call site.

**Rationale:** For example, `EntityManager.persist` is a known local action, but entity listeners, callbacks, database rules, and lazy behavior are not reconstructed.

### Decision 7: Add a source-semantic provider SPI

**Decision:** A source-semantic provider inspects one reachable source method and returns deterministic gap descriptions. The Jakarta EE provider identifies container-driven annotations and meta-annotations by binary name.

**Rationale:** Interceptors, validation, events, lifecycle callbacks, security, and timers do not require an explicit method call in application source.

### Decision 8: Keep runtime CDI verification passive

**Decision:** The CDI container creates the contextual reference or proxy. Fachtracing confirms the edge only when execution enters a proven candidate implementation. It does not resolve or instantiate CDI beans.

**Rationale:** Active lookup can invoke producers, create contextual instances, change application state, and disagree with the real injection point.

## Version 2 Data Flow

1. Source analysis resolves receiver origins.
2. The CDI selector returns include, exclude, abstain, conflict, or unresolved evidence.
3. The source-semantic provider adds gaps for reachable container-driven annotations.
4. Exact platform contracts add the known local operation and any incomplete-boundary gaps.
5. At runtime, the agent records the dispatch expectation and candidate method entry.
6. A missing candidate entry produces the existing runtime coverage gap.

## Version 2 Failure Modes

- Recognized CDI with unproved deployment state: exclude the unproved candidate path and add a framework-selection gap.
- Custom scope or stereotype with source-visible meta-annotations: treat it as a bean-defining annotation.
- Annotation metadata that can invoke container behavior: add a provider-specific gap before the method flow.
- Exact API call with hidden callbacks or a remote peer: retain the direct operation and add one or more contract gaps.
- `failOnIncomplete=true`: fail through the current graph completeness gate.
