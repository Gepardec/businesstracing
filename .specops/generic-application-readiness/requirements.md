# Feature: Generic Application Readiness

## Overview

Fachtracing has a working Java 21 walking skeleton, but a fresh clone cannot reproduce all claimed
evidence and the Maven source boundary does not cover many normal application layouts. This feature
makes the repository self-contained and makes the generic integration usable for realistic Maven
applications. It adds controlled source expansion, module-safe analysis, actionable runtime
diagnostics, durable high-rate records, and a release-grade integration path.

The feature does not use application names, packages, methods, or business vocabulary as analyzer
hints. `Gepardec/mega-backend` remains one black-box conformance corpus.

## User Stories

### Story 1: Reproduce every completed claim from a fresh clone

**As a** maintainer
**I want** every required specification and conformance artifact in version control
**So that** a clean checkout can prove the published Definition of Done

**Acceptance Criteria (EARS):**

- WHEN a fresh clone checks out the repository THE SYSTEM SHALL contain every Mega overlay, oracle,
  conformance test, generated review artifact, report, and runner input referenced by a completed
  specification or public document.
- WHEN the SpecOps index and initiative files are audited THE SYSTEM SHALL contain every referenced
  specification or SHALL remove the invalid reference with a recorded replacement rationale.
- THE SYSTEM SHALL restore the `maven-project-analysis` specification from its preserved source and
  SHALL reconcile its status with the implemented Maven behavior.
- IF a completed specification references a missing file, test, dependency, or linked specification
  THEN THE SYSTEM SHALL fail the repository conformance gate.
- WHEN the complete verification runs from a fresh clone THE SYSTEM SHALL run the generic suite and
  the pinned Mega suite without unpublished workspace files.

**Progress Checklist:**

- [ ] Fresh-clone verification has no hidden local input.
- [ ] Mega conformance files and reports are tracked.
- [ ] SpecOps index, initiatives, dependencies, and completed claims are consistent.

### Story 2: Analyze one selected Maven application with one command

**As a** developer with an arbitrary Maven application
**I want** one aggregate analysis command after I add the annotation
**So that** I do not need a custom launcher or repeated module analysis

**Acceptance Criteria (EARS):**

- WHEN a developer runs `mvn compile ...:analyze-reactor` THE SYSTEM SHALL analyze the active Maven
  reactor once after compilation and SHALL generate one deterministic aggregate index.
- WHEN Maven uses `-pl`, `-am`, resume, or reactor exclusions THE SYSTEM SHALL use the effective
  active project set as the default application boundary.
- WHERE include or exclude module coordinates are configured THE SYSTEM SHALL narrow the default
  boundary deterministically and SHALL list the effective boundary in the developer build log.
- WHEN the existing `analyze` goal runs THE SYSTEM SHALL preserve its per-module behavior and output
  contract.
- IF output labels collide across modules THEN THE SYSTEM SHALL use stable graph IDs and module
  provenance to prevent file replacement.
- WHEN the aggregate goal runs in a reactor with no annotated entry THE SYSTEM SHALL remove only its
  stale generated files and SHALL report a successful skip.

**Progress Checklist:**

- [ ] One aggregate goal analyzes one selected reactor once.
- [ ] Maven selection and explicit boundary controls work.
- [ ] The existing goal remains compatible.

### Story 3: Resolve source-visible implementations outside sibling modules

**As a** developer
**I want** controlled ways to add implementation source
**So that** normal dependency boundaries do not create unexplained incomplete dispatch nodes

**Acceptance Criteria (EARS):**

- THE SYSTEM SHALL distinguish entry sources from resolution-only sources.
- WHERE `additionalSourceRoots` are configured THE SYSTEM SHALL add their Java sources as
  resolution-only sources by default.
- WHERE `additionalEntrySourceRoots` are configured THE SYSTEM SHALL permit annotated methods in
  those roots to become graph entries.
- WHERE exact `sourceDependencies` coordinates are configured THE SYSTEM SHALL resolve only those
  Maven `sources` artifacts and SHALL add them as resolution-only sources.
- THE SYSTEM SHALL NOT discover or download all dependency source artifacts by default.
- WHILE Maven offline mode is active THE SYSTEM SHALL use only locally available named source
  artifacts and SHALL give an actionable failure for an absent artifact.
- IF a source archive contains an absolute path, parent traversal, link escape, duplicate entry, or
  configured size-limit violation THEN THE SYSTEM SHALL reject the archive before analysis.
- IF a compatible implementation is not source-visible THEN THE SYSTEM SHALL keep the business
  graph incomplete and SHALL emit a developer diagnostic that names the contract, member, call site,
  and searched source boundary.
- WHEN a graph uses sources from more than one origin THE SYSTEM SHALL export developer provenance per
  source origin: Git revision data for repository sources, Maven coordinate plus artifact checksum for
  source artifacts, and a fingerprint without a false commit URL for generated sources.
- WHEN source is added for the missing implementation THE SYSTEM SHALL resolve the implementation
  without an analyzer code change or application-specific mapping.

**Progress Checklist:**

- [ ] Reactor, local-root, and exact source-artifact inputs use explicit roles.
- [ ] Source archive handling is bounded and safe.
- [ ] Missing source stays honest and actionable.

### Story 4: Preserve Java project boundaries during analysis

**As a** developer of a modular or mixed Java reactor
**I want** source attribution to respect each Maven project's compiler model
**So that** valid applications do not become one invalid synthetic compilation

**Acceptance Criteria (EARS):**

- WHEN active projects use JPMS THE SYSTEM SHALL analyze their source declarations without placing
  several unrelated module descriptors in one flat compiler task.
- WHEN separate projects contain the same fully qualified class name THE SYSTEM SHALL preserve their
  project identity and SHALL NOT fail only because the aggregate boundary contains both definitions.
- WHEN projects use different source encodings, release levels, compiler arguments, generated source
  roots, or annotation-processor outputs THE SYSTEM SHALL use the effective Maven compiler model for
  each project or SHALL report the unsupported difference before graph extraction.
- WHEN reachable generated source is not present in Git THE SYSTEM SHALL retain its graph topology and
  SHALL mark its developer provenance as generated instead of claiming a committed source location.
- IF a reachable call crosses project indexes THEN THE SYSTEM SHALL resolve it through attributed
  symbol identity and declared project dependencies, not through simple class-name matching.
- IF two applicable source definitions remain ambiguous in the selected deployable boundary THEN THE
  SYSTEM SHALL fail with a developer diagnostic and SHALL NOT choose one silently.
- THE SYSTEM SHALL keep framework-neutral graph extraction independent of Maven APIs.

**Progress Checklist:**

- [ ] JPMS and duplicate-type fixtures remain valid.
- [ ] Per-project compiler settings are explicit.
- [ ] Cross-project dispatch remains generic and deterministic.

### Story 5: Explain unresolved runtime dispatch safely

**As a** developer operating an instrumented application
**I want** a precise diagnostic when the runtime target has no static edge
**So that** I know which source boundary to correct

**Acceptance Criteria (EARS):**

- WHEN a runtime dispatch target matches a registered implementation THE SYSTEM SHALL record only the
  existing opaque selected edge in the business execution.
- IF a runtime target has no registered static edge THEN THE SYSTEM SHALL emit a developer-only
  diagnostic with graph ID, dispatch node ID, runtime class, and a stable reason code.
- THE SYSTEM SHALL NOT copy runtime class names, source paths, Maven coordinates, proxy names, or
  diagnostic text into `DecisionExecution`, `DecisionExplanation`, Mermaid, or PlantUML.
- WHILE unresolved targets repeat at high rate THE SYSTEM SHALL deduplicate by graph, node, runtime
  target, and graph version and SHALL store diagnostics in a bounded non-blocking structure.
- IF a target is a generated proxy or subclass THEN THE SYSTEM SHALL try registered assignable target
  mappings without claiming a static candidate that does not exist.
- IF reflection, a service loader, a lambda, or a proxy prevents reliable static expansion THEN THE
  SYSTEM SHALL preserve an explicit dynamic-boundary coverage gap and SHALL record only validated
  runtime evidence.
- WHEN several graph manifests are active in one JVM THE SYSTEM SHALL select the correct graph and
  class fingerprint without replacing another active definition.
- WHERE a decision continues through a supported JDK executor or completion-stage boundary THE SYSTEM
  SHALL propagate the invocation context without cross-trace contamination.
- WHERE an integration uses the framework-neutral context-carrier SPI THE SYSTEM SHALL permit a
  framework adapter to capture and restore context without adding framework types to the core API.
- IF execution crosses an unsupported asynchronous or reactive boundary THEN THE SYSTEM SHALL emit a
  developer diagnostic and SHALL mark the execution evidence incomplete instead of joining unrelated
  observations.

**Progress Checklist:**

- [ ] Unknown runtime targets become actionable developer evidence.
- [ ] Diagnostic volume is bounded and business output remains technical-detail free.
- [ ] Proxies and dynamic targets never produce invented business topology.
- [ ] Multi-graph and supported asynchronous execution keep the correct invocation context.

### Story 6: Define and verify the supported Java contract

**As a** Fachtracing adopter
**I want** an exact compatibility and coverage contract
**So that** “generic” does not imply unsupported or guessed behavior

**Acceptance Criteria (EARS):**

- THE SYSTEM SHALL publish a machine-readable Java capability matrix for source constructs, compiler
  versions, bytecode producers, dynamic mechanisms, and fallback behavior.
- WHEN a result-relevant Java 21 source construct is in the supported matrix THE SYSTEM SHALL create
  the specified topology and SHALL have a construct-level executable contract.
- IF a result-relevant construct is not supported THEN THE SYSTEM SHALL create a source-located
  coverage gap and SHALL NOT mark the graph complete.
- WHEN a new JDK or compiler is claimed as supported THE SYSTEM SHALL pass its complete compiler and
  agent compatibility matrix before release.
- THE SYSTEM SHALL test exception flow, try-with-resources, pattern matching, records, sealed types,
  nested classes, generic dispatch, method references, lambdas, loops, switch forms, and supported
  short-circuit shapes as independent generic constructs.
- THE SYSTEM SHALL define dynamic source-unavailable behavior as an honest boundary; it SHALL NOT
  claim that reflection or arbitrary bytecode can always be reconstructed as source business logic.

**Progress Checklist:**

- [ ] Supported Java behavior has executable construct contracts.
- [ ] Unsupported behavior has visible gaps.
- [ ] Release claims match the verified matrix.

### Story 7: Persist and retrieve decision records at production load

**As a** business operations user
**I want** completed decisions stored and retrievable by stable business lookup fields
**So that** later questions can be answered without access to application logs

**Acceptance Criteria (EARS):**

- THE SYSTEM SHALL define a versioned, forward-readable decision-record envelope with graph version,
  execution ID, timestamps, status, typed final result, ordered evidence, selected edges, completeness,
  and redaction metadata.
- THE SYSTEM SHALL provide a storage-neutral asynchronous delivery interface and one JDBC reference
  adapter with migrations for save and retrieval.
- WHEN a caller searches by execution ID THE SYSTEM SHALL return the immutable record or an explicit
  not-found result.
- WHERE application correlation keys are configured THE SYSTEM SHALL store only redacted indexed
  values and SHALL support time-range plus correlation-key retrieval.
- WHILE the application runs at 1,000 completed decisions per second for ten minutes THE SYSTEM SHALL
  perform no database or filesystem I/O on the decision thread and SHALL lose no accepted record.
- IF the persistence queue reaches its configured bound THEN THE SYSTEM SHALL apply an explicit
  fail-open, block, or reject-new-trace policy and SHALL publish counters; the default SHALL preserve
  application behavior and SHALL not block the decision thread.
- IF the repository is unavailable THEN THE SYSTEM SHALL retry outside the application thread with
  bounded backoff and SHALL expose accepted, saved, retried, rejected, and dropped counters.
- THE SYSTEM SHALL define retention, deletion, schema migration, transaction, and sensitive-data
  rules without coupling the core graph model to one database product.

**Progress Checklist:**

- [ ] Records have a stable versioned protocol.
- [ ] JDBC save and retrieval work through the storage port.
- [ ] Backpressure, retries, metrics, and retention are explicit.
- [ ] The 1,000-RPS durability gate passes.

### Story 8: Make installation release-grade

**As a** developer of a project outside this repository
**I want** copyable released coordinates and startup instructions
**So that** annotation, Maven analysis, runtime capture, and storage do not depend on a local checkout

**Acceptance Criteria (EARS):**

- WHEN a clean external fixture uses the documented released coordinates THE SYSTEM SHALL resolve the
  API, Maven plugin, engine, agent, and optional JDBC adapter from the configured artifact repository.
- WHEN the fixture contains one `@FachTracing` method THE SYSTEM SHALL generate Mermaid, PlantUML,
  developer JSON, a runtime activation bundle, and an aggregate index with the documented commands.
- THE SYSTEM SHALL generate a runtime bundle that binds graph and class fingerprints and SHALL give a
  copyable standard `-javaagent` option; the build SHALL fail rather than hide a missing required
  runtime setup step.
- WHEN the application executes the decision THE SYSTEM SHALL capture, persist, retrieve, and explain
  the result without a project-specific analyzer extension.
- THE SYSTEM SHALL publish non-snapshot versioning, compatibility, upgrade, and rollback guidance.

**Progress Checklist:**

- [ ] A clean external project works without a local Fachtracing build.
- [ ] Static and runtime integration instructions are complete.
- [ ] Release and compatibility policies are documented and tested.

## Non-Functional Requirements

## PR 5 Remediation Requirements

- WHEN the isolated external release fixture runs with `-javaagent` THE SYSTEM SHALL configure the
  agent with the generated manifest and class fingerprint, invoke the annotated method, collect its
  actual execution, persist it, retrieve it, and project its business explanation.
- WHEN `analyze-reactor` receives additional resolution roots, additional entry roots, or exact
  source dependencies THE SYSTEM SHALL add them to the aggregate application boundary with the same
  entry and provenance rules as the module goal.
- WHEN Maven supplies an effective compiler model THE SYSTEM SHALL preserve its encoding, release or
  source/target level, compiler arguments, generated roots, module descriptor, module path, and
  processor path, or SHALL reject an unsupported setting before graph extraction.
- WHILE concurrent runtime mismatches exceed diagnostic capacity THE SYSTEM SHALL keep the retained
  diagnostic-key count at or below the configured capacity.
- WHEN delivery shutdown returns THE SYSTEM SHALL have terminated its worker and SHALL account for
  each accepted record as saved or dropped, including an interrupted or timed-out in-flight retry.

- **Performance:** Static aggregate analysis SHALL parse each effective project source model no more
  than once per reactor run. Runtime tracing and asynchronous delivery SHALL stay within 10% p95
  application latency overhead at 1,000 completed decisions per second for ten minutes.
- **Reliability:** The long run SHALL report zero trace-caused application errors, result mismatches,
  cross-trace contamination, and silently lost accepted records.
- **Security:** Archive extraction SHALL prevent traversal and resource exhaustion. Repository
  credentials, source details, runtime types, and database credentials SHALL not enter business
  records or normal business diagrams.
- **Privacy:** Redaction SHALL run before values enter queues, metrics labels, persistence records, or
  diagnostic payloads. Unknown value types remain denied by default.
- **Determinism:** Identical source, boundary, configuration, and graph version SHALL produce identical
  graph topology, opaque IDs, output names, capability reports, and protocol bytes.
- **Observability:** Build-time gaps and runtime delivery failures SHALL use stable reason codes and
  bounded developer channels.

## Constraints and Assumptions

- Java 21 remains the first mandatory baseline. Broader Java versions require explicit matrix entries
  and tests; this feature does not use the phrase “all Java” without a verified boundary.
- Source business meaning is derived only from attributed source. Bytecode can correlate runtime
  events but cannot invent source-level business semantics.
- Exact source dependency coordinates and Maven's repository policy define external source access.
- Frontend work is outside this feature. Mermaid, PlantUML, JSON, and repository queries are the
  integration outputs.
- The preserved pre-pull stash can help recover repository artifacts, but no final test or release may
  depend on that stash.

## Dependencies and Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `generic-tracing-walking-skeleton` | Defines the graph, record, agent, Mega, and load baselines. | Yes | completed |
| `reactor-wide-implementation-resolution` | Supplies entry-source isolation and reactor source discovery. | Yes | completed |
| `runtime-decision-path-capture` | Supplies exact edges, failure state, and nested dispatch correlation. | Yes | completed |
| `maven-developer-graph-export` | Supplies Maven JSON output and source provenance. | Yes | completed |

### Cross-Spec Blockers

| Blocker | Blocking Spec | Resolution Type | Resolution Detail | Status |
| --- | --- | --- | --- | --- |
| Missing tracked `maven-project-analysis` artifacts | `maven-project-analysis` | interface_defined | Recover and audit the preserved specification before implementation claims completion. | open |
| Missing tracked Mega conformance directory | `generic-tracing-walking-skeleton` | interface_defined | Restore reviewed artifacts and verify hashes from the pinned source before use. | open |

## Success Metrics

- A fresh clone passes the generic verifier, exact Mega conformance, SpecOps integrity audit, external
  Maven fixture, JPMS/duplicate-type fixtures, JDBC persistence tests, and the long 1,000-RPS gate.
- The incomplete dispatch fixture becomes complete after an exact source root or source artifact is
  added, without production code changes.
- An unmatched runtime implementation produces one bounded developer diagnostic and zero technical
  terms in business output.
- The aggregate goal performs one analysis run for the selected reactor and produces collision-safe
  output.
- Every public compatibility claim maps to a machine-readable capability entry and executable test.

## Out of Scope

- A browser or business-user frontend.
- Domain-specific label dictionaries or per-application rule maps.
- Decompilation that claims to recover source business meaning.
- Automatic download of every dependency source artifact.
- Distributed execution correlation across separate services; this feature covers one JVM, JDK
  executor and completion-stage propagation, and a framework-neutral context-carrier SPI.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Do not use subagents.
- Keep technical provenance and diagnostics outside business records.
- Keep Mega-specific knowledge inside conformance artifacts only.
