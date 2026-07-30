# Feature: Generic Fachtracing Walking Skeleton

## Overview

Java business decisions are difficult to reconstruct because their reasons are embedded in control flow, method calls, and runtime dispatch; this feature proves that one annotation can turn previously unknown code into an explainable decision record without domain-specific tracing code.

## Core Contract

Given a Java method annotated with `@FachTracing`, the system uses static code analysis to acquire the domain structure relevant to its result, derives the reachable business-decision graph, enriches that graph with the path taken during a specific execution, and explains what decision was made, why it was made, and how execution reached it.

The system learns the decision structure from the program. It receives no package allowlist, method catalog, domain vocabulary, or hand-written branch mapping for the analyzed application.

`Gepardec/mega-backend` is the mandatory realistic brownfield reference corpus. It proves that
the same generic extractor works outside synthetic fixtures; it must never shape the extractor
into a Gepardec-specific solution.

## Terms

- **Business-decision graph:** The conditions, result-relevant computations, possible dispatches, and outcomes that can influence an annotated method's returned value.
- **Execution path:** The subset of graph nodes and edges observed during one invocation, including the implementation selected at dynamic dispatch points.
- **Explanation:** A business-facing projection that combines the final result with the evaluated reasons and their order, without exposing Java packages, classes, method signatures, stack frames, or bytecode details.

## Developer Use Cases

### Use Case 1: Derive a graph from an unknown method

**As a** Java developer
**I want** to annotate a decision method once
**So that** Fachtracing derives its decision structure without hand-written tracing statements or application-specific configuration

**Acceptance Criteria (EARS):**

- WHEN compilation discovers a method annotated with `@FachTracing` THE SYSTEM SHALL build a typed, directed graph rooted at that method's returned decision.
- THE SYSTEM SHALL retain conditions, computations, and calls that have a control or data dependency on a returned decision.
- THE SYSTEM SHALL exclude statements that have no control or data dependency on a returned decision, regardless of their package or naming.
- IF a result-relevant language construct cannot be analyzed THEN THE SYSTEM SHALL identify the source location and construct kind as an explicit coverage gap and SHALL NOT label the graph complete.
- WHEN the same analyzer processes a second annotated method from a different fixture domain THE SYSTEM SHALL derive its graph without analyzer code or configuration changes.

**Progress Checklist:**

- [x] Annotated methods produce typed decision graphs.
- [x] Backward relevance removes result-independent operations.
- [x] Unsupported result-relevant constructs appear in coverage diagnostics.
- [x] A structurally different fixture passes without analyzer customization.

### Use Case 2: Correlate the actual runtime path

**As a** Java developer
**I want** the runtime to record only the evidence needed for one decision
**So that** the explanation remains lightweight and precise

**Acceptance Criteria (EARS):**

- WHEN an annotated method executes THE SYSTEM SHALL correlate each observed result-relevant predicate and outcome with an opaque node identifier from the static graph.
- WHEN an interface or abstract call dispatches at runtime THE SYSTEM SHALL record the implementation branch selected for that invocation without exposing its Java type name in the business-facing record.
- THE SYSTEM SHALL capture only values used to render visited predicates or the final result; it SHALL NOT serialize arbitrary receiver objects or call `toString()` on unknown types.
- IF trace capture fails THEN THE SYSTEM SHALL preserve the annotated method's return value and exception behavior and SHALL emit a trace-failure diagnostic outside the business record.

**Progress Checklist:**

- [x] Branch observations correlate with static node identifiers.
- [x] A polymorphic call records the actual branch.
- [x] Captured values are limited to explanation evidence.
- [x] Instrumentation does not change application behavior on capture failure.

### Use Case 3: Explain the decision and its derivation

**As a** non-technical business user
**I want** a record that states the result and the reasons that led to it
**So that** I can answer questions without reading code or asking a developer to debug the request

**Acceptance Criteria (EARS):**

- WHEN an invocation completes THE SYSTEM SHALL produce an explanation containing the final result, the ordered visited reasons, the values evaluated for those reasons, and the chosen path.
- THE SYSTEM SHALL support final results represented as Boolean, number, category, or string values through a tagged value contract.
- WHERE a custom result-value adapter is registered THE SYSTEM SHALL support additional result types without changing the decision graph schema.
- THE SYSTEM SHALL render comparisons in business language, including transformations such as `age < 24` to “age was below 24” with the observed value.
- THE SYSTEM SHALL NOT include Java package names, class names, method signatures, stack traces, bytecode offsets, or agent terminology in the business-facing explanation.

**Progress Checklist:**

- [x] Explanations state what, why, and how.
- [x] Four required result kinds are supported.
- [x] Custom result kinds use an extension contract.
- [x] Business-facing output contains no prohibited Java details.

### Use Case 4: Generate structural and execution PlantUML

**As a** business analyst
**I want** PlantUML representations of the possible decision graph and a specific execution
**So that** I can review both the rule structure and why one result occurred

**Acceptance Criteria (EARS):**

- WHEN static analysis succeeds THE SYSTEM SHALL generate PlantUML source representing every supported result-relevant node and edge in the business-decision graph.
- WHEN an execution record is supplied THE SYSTEM SHALL generate PlantUML source that visually distinguishes the visited path from unvisited alternatives.
- IF coverage gaps exist THEN THE SYSTEM SHALL render a visible incomplete-analysis marker and list the affected graph locations.
- THE SYSTEM SHALL generate diagram labels from business expressions and values, not Java type or method names.

**Progress Checklist:**

- [x] Structural graph PlantUML is generated.
- [x] Execution-path PlantUML is generated.
- [x] Coverage gaps remain visible.
- [x] Diagram labels are business-facing.

### Use Case 5: Prove generic extraction on `mega-backend`

**As a** Fachtracing adopter
**I want** the unchanged extractor validated against a realistic brownfield Java system
**So that** synthetic fixtures cannot hide application-scale analysis defects

**Acceptance Criteria (EARS):**

- WHEN the conformance suite checks out the pinned `Gepardec/mega-backend` revision THE SYSTEM
  SHALL analyze at least three test-annotated business-decision entry points across at least two
  distinct business areas, including one path with interface or abstract dispatch.
- FOR EACH selected entry point THE SYSTEM SHALL generate a complete business-decision graph
  whose result-relevant conditions, computations, calls, dispatch alternatives, outcomes, and
  edges match an independently reviewed code-derived oracle; missing or extra business topology
  fails conformance.
- WHEN a selected Mega decision executes THE SYSTEM SHALL enrich the graph with the actual
  visited path, observed business values, selected polymorphic edge where applicable, and final
  typed result, and SHALL generate the corresponding explanation and PlantUML.
- THE SYSTEM SHALL use the same published analyzer, agent, value protocol, and renderer used by
  non-Mega applications; the production modules and generic configuration SHALL contain no Mega
  package, class, method, source-path, or business-vocabulary special cases.
- IF Mega exposes a missing Java construct THEN its support SHALL be implemented and tested as a
  construct-level generic capability before the Mega conformance oracle is accepted; trimming,
  naming heuristics, or branch mappings specific to Mega SHALL NOT satisfy this criterion.
- THE conformance suite SHALL rerun at least two structurally different non-Mega fixture domains
  with the identical extractor artifact and generic settings to guard against reference-driven
  overfitting.
- The test harness MAY contain a pinned revision, test-only annotation overlay, entry-point
  selection, invocation data, and expected graph artifacts, but the extractor SHALL NOT consume
  those artifacts as analysis hints.

**Definition of Done:**

- [x] A pinned Mega revision and selected entry points are recorded with their selection rationale.
- [x] At least three independently reviewed Mega graph oracles pass exact semantic topology comparison.
- [x] At least one Mega runtime execution explains the actual polymorphic path and final result.
- [x] Generated Mega explanations and PlantUML are business-facing and contain no prohibited Java details.
- [x] A production-source/configuration guard proves there are no Mega-specific implementation hints.
- [x] The unchanged artifact still passes at least two non-Mega domains and the complete regression suite.
- [x] A checked-in conformance report links the source revision, oracles, generated graphs, runtime evidence, and review result.

## API Design Principles

- One `@FachTracing` annotation selects an entry point; no manual trace call is required inside its method body.
- Static graph definitions are immutable and reusable across invocations.
- Runtime evidence is append-only per invocation and references static nodes by opaque identifiers.
- Business-facing records omit technical provenance; optional developer diagnostics remain a separate artifact.
- Analysis completeness is measurable. Unsupported constructs never disappear silently.

## Compatibility Requirements

- The walking skeleton targets Java 21 source and bytecode because the first validation corpus uses Java 21.
- Public annotation and model contracts must not depend on Quarkus, Spring, Jakarta CDI, or another application framework.
- The analyzer must work from a normal Maven compilation environment and an explicit source/class path.
- Later initiative specs may widen Java-version support; this spec must not claim compatibility beyond Java 21.

## Library Quality Requirements

- At 1,000 completed invocations per second for ten minutes, tracing SHALL introduce no trace-caused application errors and no lost final results in the benchmark fixture.
- At the same load, synchronous capture work SHALL add no more than 10% to p95 method latency compared with an instrumented-but-disabled baseline.
- Runtime capture SHALL perform no database or filesystem I/O on the invoking thread.
- The runtime SHALL isolate invocation context across at least 32 concurrent threads with zero cross-trace node observations.
- All public APIs SHALL include Javadoc describing stability and failure behavior.

## Data Handling

- Decision evidence can contain confidential or restricted business data.
- Unknown object types are denied by default and require an explicit value adapter.
- A value-redaction hook runs before evidence enters the business record.
- Tests use synthetic identities and values only.

## Scope Boundary

### Ships in this walking skeleton

- Public annotation and immutable graph/trace/result contracts.
- Java 21 attributed-AST analysis for annotated methods using `if`/`else`, switch statements or expressions, Boolean operators, comparisons, local assignments, method calls, and returns.
- Backward program slicing from return values to exclude unrelated technical operations.
- Runtime capture for predicates, outcomes, and one polymorphic interface-dispatch flow.
- Deterministic business explanation and PlantUML source generation.
- In-memory persistence port used by end-to-end tests.
- A 1,000-invocation-per-second benchmark for the supported walking-skeleton flow.
- Brownfield conformance against selected decision entry points from a pinned
  `Gepardec/mega-backend` revision, using test-only integration artifacts and no analyzer hints.

### Deferred to later initiative specs

- Exhaustive Java-language and framework construct coverage.
- Production database adapters, retention, querying, and schema migration.
- Distributed traces across processes or services.
- Complete automated coverage of every `mega-backend` business entry point beyond the mandatory
  representative conformance set.
- A graphical frontend or interactive graph viewer.

## Dependencies & Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| — | Walking skeleton has no predecessor | — | — |

### Cross-Spec Blockers

| Blocker | Blocking Spec | Resolution Type | Resolution Detail | Status |
| --- | --- | --- | --- | --- |
| — | — | — | — | — |

## Success Metrics

- Three annotated fixture methods from distinct synthetic domains produce graphs and explanations without analyzer customization.
- One fixture includes at least two implementations of an interface, and its execution record identifies the implementation branch actually selected.
- Every supported result-relevant source construct maps to a graph node or edge; every unsupported relevant construct maps to a coverage diagnostic.
- Generated business records and PlantUML contain zero raw Java package, class, method-signature, stack-frame, or bytecode-offset strings.
- The benchmark sustains 1,000 completed invocations per second for ten minutes within the stated latency and correctness limits.
- A reviewed `mega-backend` conformance set produces correct complete graphs and actual execution
  explanations while production implementation and configuration remain reference-agnostic.

## Team Conventions

No project-specific team conventions are configured. The implementation follows the framework-neutral, minimal-dependency boundaries in this specification.
