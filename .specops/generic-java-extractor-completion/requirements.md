# Requirements: Generic Java Extractor Completion

## Overview

This follow-up completes the result-relevant Java coverage that the release candidate reports as
explicit gaps. It must extend the generic extractor and runtime plan. It must not hide missing
logic by changing documentation, lowering completeness rules, or adding application-specific
knowledge. The pinned Mega Backend remains one conformance corpus only.

## Scope Assessment

SpecOps detected all five decomposition signals. The recommended manual split is:

| Proposed spec | Scope | Size | Wave |
| --- | --- | --- | --- |
| `result-relevant-structured-control-flow` | Exceptions, resources, and synchronized blocks | L | 1 |
| `exact-complex-predicate-paths` | Atomic Boolean, ternary, and switch paths | L | 1 |
| `dynamic-and-binary-decision-resolution` | Proxies, reflection, services, source artifacts, and bytecode fallback | L | 2 |
| `automatic-async-trace-propagation` | Executors, stages, platform threads, and virtual threads | M | 1 |
| `owned-external-jpms-sources` | Named and automatic module source ownership | M | 2 |
| `release-ci-and-production-jdbc` | PostgreSQL and pull-request gates | M | 3 |

Codex non-interactive mode keeps one specification. The tasks preserve these boundaries.

## User Story 1: Result-relevant exception flow

**As a** business reviewer  
**I want** decisions that use exception control flow to have a complete graph  
**So that** a recovered or overridden result is explained without Java exception details

### Acceptance Criteria

- WHEN a relevant `try` completes normally THE SYSTEM SHALL connect its result-relevant path to the
  applicable `finally` logic and later decision flow.
- WHEN a relevant thrown value reaches a compatible `catch` THE SYSTEM SHALL model the selected
  recovery path and all predicates or mutations that affect the final result.
- WHEN a relevant `finally` block changes or replaces the result THE SYSTEM SHALL show that business
  change on every applicable path.
- WHEN try-with-resources contains result-relevant resource initialization, use, close failure, or
  recovery logic THE SYSTEM SHALL model the business effect without showing resource mechanics.
- THE SYSTEM SHALL keep exception class names, stack frames, synthetic close calls, and compiler
  suppression mechanics out of business graphs and records.
- IF the analyzer cannot prove which handler receives a result-relevant exception THEN THE SYSTEM
  SHALL produce a source-located actionable coverage gap and keep the graph incomplete.

## User Story 2: Result-relevant synchronized logic

**As a** business reviewer  
**I want** business rules inside synchronized code to remain visible  
**So that** a locking implementation detail does not remove a decision reason

### Acceptance Criteria

- WHEN a synchronized block contains result-relevant predicates, calculations, mutations, calls,
  or returns THE SYSTEM SHALL analyze those statements as ordinary decision logic.
- THE SYSTEM SHALL omit monitor acquisition, monitor release, lock expressions, and Java
  synchronization vocabulary from all business outputs.
- THE SYSTEM SHALL NOT mark a graph incomplete only because a relevant statement is synchronized.

## User Story 3: Exact complex Boolean and choice paths

**As a** business reviewer  
**I want** the record to show each tested fact and selected outcome  
**So that** I can follow why a mixed or nested condition produced its result

### Acceptance Criteria

- WHEN a relevant condition uses nested or mixed `&&`, `||`, or `!` THE SYSTEM SHALL create a
  deterministic atomic predicate plan that preserves Java short-circuit order.
- WHEN an atomic predicate runs THE SYSTEM SHALL record its typed evidence and exact selected edge.
- WHEN short-circuit evaluation skips an atomic predicate THE SYSTEM SHALL not claim that the
  predicate was evaluated.
- WHEN a Boolean ternary expression affects the result THE SYSTEM SHALL record the exact condition
  edge and the selected value path.
- WHEN a switch statement or switch expression affects the result THE SYSTEM SHALL record the exact
  selected case or default edge, including string, enum, integral, and pattern-compatible forms in
  the supported Java 21 subset.
- THE SYSTEM SHALL NOT replace an available exact outcome with only a generic `evaluated`
  observation.
- IF bytecode cannot be correlated to the complete static atomic plan THEN THE SYSTEM SHALL reject
  the partial exact plan and produce a source-located actionable coverage gap.

## User Story 4: Dynamic invocation without guessing

**As a** developer integrating unknown applications  
**I want** generic runtime evidence for dynamic calls  
**So that** a selected implementation is shown only when the system can prove it

### Acceptance Criteria

- WHEN a proxy delegates to a statically known decision candidate THE SYSTEM SHALL use the runtime
  target type or invoked member to select exactly one precomputed candidate edge.
- WHEN `ServiceLoader` returns a provider that matches one statically known candidate THE SYSTEM
  SHALL record that candidate edge.
- WHEN reflection invokes a member that matches one statically known candidate by owner, name, and
  descriptor THE SYSTEM SHALL record that candidate edge.
- WHEN the runtime target is unknown or ambiguous THE SYSTEM SHALL not guess and SHALL add one
  bounded developer diagnostic plus an execution coverage gap that states what evidence is missing.
- THE SYSTEM SHALL keep proxy classes, reflection APIs, service-loader mechanics, and Java member
  identities out of business-facing artifacts.

## User Story 5: Source-unavailable decision logic

**As a** developer  
**I want** the analyzer to use available source or safe bytecode  
**So that** dependency logic is not lost only because it is outside the current source tree

### Acceptance Criteria

- WHERE a configured Maven source artifact contains the called method THE SYSTEM SHALL attribute it
  in the correct compiler context and analyze it as source.
- WHEN source is unavailable but a compiled method fits the controlled bytecode subset THE SYSTEM
  SHALL reconstruct constants, parameters, fields, simple calculations, comparisons, conditional
  branches, and returns into a developer-proven decision fragment.
- THE SYSTEM SHALL verify class fingerprints before it uses a bytecode fallback result.
- THE SYSTEM SHALL label bytecode-derived business facts only from safe metadata or existing graph
  vocabulary and SHALL not expose local-slot numbers, opcodes, descriptors, or class names.
- IF a relevant binary method uses unsupported bytecode, invokedynamic that is not a supported
  lambda, native code, or unavailable metadata THEN THE SYSTEM SHALL keep the graph incomplete and
  produce a source-located actionable coverage gap.

## User Story 6: Automatic asynchronous propagation

**As an** application developer  
**I want** standard asynchronous calls to keep trace context automatically  
**So that** I do not have to wrap each callback by hand

### Acceptance Criteria

- WHEN instrumented application code submits a `Runnable` or `Callable` to a standard `Executor` or
  `ExecutorService` THE SYSTEM SHALL capture and restore the current trace context automatically.
- WHEN instrumented application code registers standard `CompletionStage` functions, consumers,
  runnables, or bi-functions THE SYSTEM SHALL propagate context without a manual wrapper.
- WHEN application code starts a platform or virtual thread through supported Java 21 factories THE
  SYSTEM SHALL propagate context to the new task and clear it after completion.
- WHEN no trace is active THE SYSTEM SHALL preserve the original task or callback behavior.
- WHEN one carrier executes concurrent tasks from different traces THE SYSTEM SHALL prevent
  cross-trace observations and clear restored state after every task.
- IF an asynchronous API is unsupported THEN THE SYSTEM SHALL add a precise execution coverage gap
  rather than silently joining or dropping observations.

## User Story 7: Owned external sources in mixed JPMS builds

**As a** developer of a modular application  
**I want** external source inputs to declare module ownership  
**So that** reachable dependency source can join the valid compiler context

### Acceptance Criteria

- WHERE an external source input declares a named module THE SYSTEM SHALL add its sources and module
  descriptor to that module's source context.
- WHERE an external source input declares automatic-module ownership THE SYSTEM SHALL pair its
  sources with the Maven binary module identity and keep the named entry module valid.
- WHEN Maven provides a valid build model across named and automatic modules THE SYSTEM SHALL
  resolve reachable source logic without a false module-descriptor error.
- IF ownership is absent, conflicting, or not readable from the entry module THEN THE SYSTEM SHALL
  fail before extraction with an actionable input diagnostic.
- THE SYSTEM SHALL include ownership and source checksums in the boundary fingerprint and developer
  provenance only, never in business output.

## User Story 8: Pull-request and production-database evidence

**As a** release reviewer  
**I want** automated generic, brownfield, storage, and load gates  
**So that** extractor coverage cannot regress without a failed pull request check

### Acceptance Criteria

- WHEN a pull request opens or changes THE SYSTEM SHALL run repository integrity, Java capability,
  generic verification, external activation integration, pinned Mega conformance, PostgreSQL JDBC
  integration, and the 600-second 1,000-RPS gate.
- THE PostgreSQL contract SHALL test schema creation, exact idempotency, both durable-key conflicts,
  lookup, correlation search, retention, and statement timeout behavior against a supported server.
- THE workflow SHALL use read-only repository permissions, pinned or current official action major
  versions, Maven caching, and a supported PostgreSQL 18 minor image.
- THE release gate SHALL report zero result changes, cross-trace contamination, silent accepted
  record loss, or unclassified accepted records.

## Compatibility and Generality Requirements

- THE SYSTEM SHALL keep Activation V2 readable and SHALL write Activation V3 until a schema meaning
  change requires a new version.
- THE SYSTEM SHALL preserve manual context wrappers as supported public APIs.
- THE SYSTEM SHALL keep the five pinned Mega graphs semantically equal to their reviewed oracles.
- THE SYSTEM SHALL use the same analyzer, runtime plan, and agent artifacts for Mega and all generic
  fixtures.
- THE SYSTEM SHALL contain no production package, class, method, vocabulary, or rule hint derived
  from Mega Backend.
- THE SYSTEM SHALL keep unsupported result-relevant logic incomplete with a source-located,
  actionable diagnostic.

## Non-functional Requirements

- The application-thread probe path SHALL remain bounded and SHALL not perform storage I/O.
- The 1,000-RPS gate SHALL complete 600,000 enabled decisions with less than 10% p95 overhead.
- Runtime diagnostics and unresolved dynamic identities SHALL use bounded memory.
- Runtime instrumentation SHALL preserve all return values, thrown objects, callback identities when
  no wrapping is needed, interruption behavior, and application-visible ordering.
- All new capability entries SHALL name one independent executable contract in
  `docs/java-capabilities.json`.

## Definition of Done

- [x] Every required construct has an independent generic fixture and executable contract.
- [x] Supported constructs produce complete business graphs and exact runtime paths.
- [x] Unsupported variants produce source-located actionable gaps.
- [x] Mega Backend produces the five reviewed graphs with no production Mega hints.
- [x] External activation integration passes without runtime source analysis.
- [x] PostgreSQL integration passes in local opt-in mode and required GitHub CI.
- [x] The clean-clone 600-second release gate passes at 1,000 RPS.
- [x] GitHub CI runs the standard verification and conformance gates for each pull request.
- [x] `RuntimeActivationBundle` JavaDoc states Activation V3.

