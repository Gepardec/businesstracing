# Implementation Tasks: Generic Fachtracing Walking Skeleton

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| — | Walking skeleton has no predecessor | — | — |

## Dependency Resolution Log

| Blocker | Resolution Type | Resolution Detail | Date |
| --- | --- | --- | --- |
| — | — | — | — |

## Task Breakdown

### Task 1: Create the public API and immutable model

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Create the minimal Maven module structure, `@FachTracing`, immutable graph and execution contracts, extensible decision values, completeness types, and storage port.

**Implementation Steps:**

1. Create the root Maven build and Java 21 module descriptors.
2. Create the framework-neutral annotation and extension interfaces.
3. Create sealed/record-based graph, execution, observation, explanation, and diagnostic models.
4. Create the in-memory decision-record repository behind the storage port.

**Acceptance Criteria:**

- [x] Public API modules compile on Java 21 without Quarkus, Spring, or CDI dependencies.
- [x] Boolean, number, category, and string results round-trip through the value contract.
- [x] An unknown object value is rejected unless an adapter is registered.
- [x] Graph and execution records expose opaque IDs but no source or bytecode coordinates.

**Files to Modify:**

- `pom.xml`
- `fachtracing-api/pom.xml`
- `fachtracing-api/src/main/java/at/gepardec/fachtracing/api/FachTracing.java`
- `fachtracing-api/src/main/java/at/gepardec/fachtracing/api/DecisionValueAdapter.java`
- `fachtracing-api/src/main/java/at/gepardec/fachtracing/api/DecisionValueRedactor.java`
- `fachtracing-engine/pom.xml`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/model/BusinessDecisionGraph.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/model/DecisionExecution.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/model/DecisionExplanation.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/store/DecisionRecordRepository.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/store/InMemoryDecisionRecordRepository.java`

**Tests Required:**

- [x] Public API dependency-boundary test.
- [x] Built-in and custom decision-value adapter tests.
- [x] Unknown-value and redaction tests.

---

### Task 2: Derive result-relevant decision graphs

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Use attributed Java 21 ASTs to discover annotated methods, build control/data dependencies, slice backwards from returns, and produce graph and coverage artifacts without domain-specific configuration.

**Implementation Steps:**

1. Create an analysis request that accepts sources and an explicit compilation classpath.
2. Create AST visitors for the supported walking-skeleton constructs.
3. Build control and data dependencies and compute the backward result slice.
4. Link direct calls, represent dynamic dispatch, assign opaque IDs, and emit developer-only source mappings.
5. Emit visible diagnostics for unsupported result-relevant constructs.

**Acceptance Criteria:**

- [x] Result-independent logging, metrics, and temporary computations are excluded by dependency analysis rather than names.
- [x] Direct and nested supported decisions produce complete graph topology.
- [x] Interface calls produce dispatch nodes and candidate edges without selecting a runtime target.
- [x] Unsupported relevant constructs produce incomplete graphs with source diagnostics.
- [x] Two fixture domains pass without analyzer code or configuration changes.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisRequest.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DependencyGraphBuilder.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/BackwardDecisionSlicer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DecisionGraphBuilder.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisManifest.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `fachtracing-engine/src/test/resources/fixtures/eligibility/EligibilityPolicy.java`
- `fachtracing-engine/src/test/resources/fixtures/pricing/PricingPolicy.java`

**Tests Required:**

- [x] AST construct parameterized tests.
- [x] Backward-slice inclusion and exclusion tests.
- [x] Direct-call and dynamic-dispatch graph tests.
- [x] Coverage-gap tests.

---

### Task 3: Instrument and capture actual execution paths

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 2
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Create a Java agent that injects non-throwing probes for analyzed nodes and a runtime collector that isolates ordered observations and records the selected polymorphic implementation edge.

**Implementation Steps:**

1. Create the agent module with the approved ASM dependency.
2. Validate class fingerprints against the analysis manifest before transformation.
3. Inject entry, predicate, dispatch, return, and failure-safe probes.
4. Create thread-isolated invocation contexts and an ordered collector.
5. Add a fixture with two strategy implementations and verify the selected target edge.

**Acceptance Criteria:**

- [x] Injected probes correlate observations with opaque static node IDs.
- [x] The selected implementation edge is recorded for two different runtime strategy objects.
- [x] Capture failures preserve application return values and exceptions.
- [x] Thirty-two concurrent traces contain no observations from another invocation.
- [x] Runtime capture performs no filesystem or database I/O on the invoking thread.

**Files to Modify:**

- `fachtracing-agent/pom.xml`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingAgent.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingTransformer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/TraceRuntime.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/InvocationContext.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeCollector.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/runtime/RuntimeCollectorTest.java`
- `fachtracing-engine/src/test/resources/fixtures/strategy/DecisionStrategy.java`
- `fachtracing-engine/src/test/resources/fixtures/strategy/StrategyDecisionService.java`

**Tests Required:**

- [x] Agent transformation and fingerprint mismatch tests.
- [x] Polymorphic dispatch integration tests.
- [x] Failure-transparency characterization tests.
- [x] Concurrent context-isolation test.

---

### Task 4: Generate business explanations and PlantUML

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1, Task 2, Task 3
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Project static graphs and runtime observations into deterministic explanations that state what, why, and how, then render structural and execution PlantUML with visible coverage status.

**Implementation Steps:**

1. Create deterministic templates for supported predicate and outcome kinds.
2. Apply value adapters and redaction before constructing explanations.
3. Construct an ordered explanation tree from the visited graph path.
4. Create PlantUML renderers for the full graph and highlighted execution path.
5. Add a prohibited-technical-term assertion over all business-facing snapshots.

**Acceptance Criteria:**

- [x] Each completed explanation states the typed final result and ordered evaluated reasons.
- [x] Predicate labels include observed values and read as business statements.
- [x] Structural PlantUML includes all supported graph nodes and edges.
- [x] Execution PlantUML distinguishes visited and unvisited edges.
- [x] Incomplete analysis is visible in both text and PlantUML.
- [x] Business snapshots contain no package, class, method-signature, stack-frame, or bytecode-offset strings.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/explain/DecisionExplanationProjector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/explain/BusinessStatementRenderer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/plantuml/PlantUmlRenderer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/explain/DecisionExplanationProjectorTest.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/plantuml/PlantUmlRendererTest.java`
- `fachtracing-engine/src/test/resources/snapshots/eligibility-explanation.txt`
- `fachtracing-engine/src/test/resources/snapshots/eligibility-structure.puml`
- `fachtracing-engine/src/test/resources/snapshots/eligibility-execution.puml`

**Tests Required:**

- [x] Explanation snapshot tests for all built-in value kinds.
- [x] Redaction and missing-adapter tests.
- [x] Structural and execution PlantUML snapshot tests.
- [x] Technical-detail exclusion test.

---

### Task 5: Prove the complete walking skeleton

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1, Task 2, Task 3, Task 4
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Compile, analyze, instrument, invoke, explain, save, retrieve, and render multiple unknown fixture decisions through one public orchestration API.

**Implementation Steps:**

1. Create a Fachtracing engine facade that composes analysis, collection, explanation, rendering, and storage ports.
2. Run three fixture domains through the unchanged pipeline.
3. Verify stored and retrieved records preserve graph and execution version correlation.
4. Add negative scenarios for incomplete analysis and runtime capture failure.

**Acceptance Criteria:**

- [x] Three different fixture domains complete the same end-to-end pipeline without analyzer customization.
- [x] A saved record can be retrieved by ID and reproduces the same explanation and PlantUML.
- [x] A polymorphic fixture explains the actual implementation path selected in each invocation.
- [x] Analysis and runtime failures remain explicit without changing application behavior.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/FachtracingEngine.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/FachtracingEngineIT.java`
- `fachtracing-engine/src/test/resources/fixtures/eligibility/EligibilityPolicy.java`
- `fachtracing-engine/src/test/resources/fixtures/pricing/PricingPolicy.java`
- `fachtracing-engine/src/test/resources/fixtures/strategy/StrategyDecisionService.java`

**Tests Required:**

- [x] End-to-end tests for three fixture domains.
- [x] Persistence round-trip test.
- [x] Polymorphic explanation test.
- [x] Incomplete-analysis and capture-failure tests.

---

### Task 6: Verify performance and document the public flow

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 5
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:**
Create a reproducible 1,000-RPS benchmark, document the supported construct boundary, and add PlantUML sources for the library's own business logic and integration flow.

**Implementation Steps:**

1. Create a benchmark harness with instrumented-disabled and instrumented-enabled modes.
2. Run 1,000 completed invocations per second for ten minutes with concurrent callers.
3. Record throughput, p50/p95 latency, errors, result mismatches, dropped traces, and cross-trace contamination.
4. Create user documentation and checked-in PlantUML matching the design flows.

**Acceptance Criteria:**

- [x] Benchmark sustains 1,000 completed invocations per second for ten minutes.
- [x] Tracing causes zero application errors, result mismatches, and cross-trace observations.
- [x] Enabled p95 latency is no more than 10% above the instrumented-disabled baseline.
- [x] Documentation states supported constructs, explicit gaps, failure behavior, and data-redaction requirements.
- [x] Checked-in PlantUML covers analysis, runtime correlation, explanation construction, and the record model.

**Files to Modify:**

- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/performance/FachtracingLoadTest.java`
- `README.md`
- `docs/supported-java-constructs.md`
- `docs/plantuml/extraction-flow.puml`
- `docs/plantuml/runtime-correlation.puml`
- `docs/plantuml/explanation-flow.puml`
- `docs/plantuml/decision-record-model.puml`

**Tests Required:**

- [x] Ten-minute 1,000-RPS load test.
- [x] Documentation-to-supported-construct consistency test.
- [x] PlantUML source snapshot consistency test.

---

### Task 7: Prove generic brownfield conformance on `mega-backend`

**Status:** Completed
**Estimated Effort:** XL
**Dependencies:** Tasks 1-6
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Validate the unchanged Fachtracing artifacts against a pinned `Gepardec/mega-backend` revision.
Produce independently reviewed correct business-decision graphs and actual-path evidence while
preventing Mega-specific knowledge from entering production implementation or generic settings.

**Implementation Steps:**

1. Pin and verify the Mega revision; record at least three decision entry points across two
   business areas, including one polymorphic path, with selection rationale.
2. Create a test-only annotation/invocation overlay that does not alter analyzer behavior or
   provide graph hints.
3. Independently derive semantic graph oracles from the pinned source: result-relevant business
   conditions, computations, calls, dispatch alternatives, outcomes, and edges.
4. Run the published analyzer artifact and compare semantic topology exactly, ignoring opaque IDs
   and developer-only source coordinates.
5. Execute at least one selected decision, correlate the actual polymorphic path and typed result,
   and generate business explanation plus structural/execution PlantUML.
6. For every missing construct exposed by Mega, first implement a generic construct-level fixture
   and regression test; do not introduce repository/package/class/method/vocabulary special cases.
7. Add a forbidden-reference guard over production source and generic configuration, then rerun
   at least two non-Mega domains and the complete existing verification suite with the same build.
8. Publish a conformance report linking the pinned source, selection, reviewed oracles, generated
   artifacts, runtime evidence, gaps resolved generically, and reviewer outcome.

**Acceptance Criteria:**

- [x] At least three selected Mega entry points across two business areas produce complete graphs.
- [x] Generated semantic topology exactly matches independently reviewed code-derived oracles.
- [x] A Mega polymorphic execution records and explains the actual selected implementation edge.
- [x] Mega explanation and PlantUML artifacts are business-facing and contain no prohibited Java details.
- [x] Production source and generic configuration contain no Mega-specific hints or identifiers.
- [x] Each Mega-discovered gap has a domain-neutral construct fixture and implementation.
- [x] The identical artifact passes at least two non-Mega domains and all existing regressions.
- [x] The checked-in report contains sufficient evidence to reproduce and review conformance.

**Files to Modify:**

- `pom.xml`
- `conformance/mega-backend/README.md`
- `conformance/mega-backend/selection.md`
- `conformance/mega-backend/annotation-overlay.patch`
- `conformance/mega-backend/src/test/java/at/gepardec/fachtracing/conformance/MegaBackendConformanceTest.java`
- `conformance/mega-backend/src/test/java/at/gepardec/fachtracing/conformance/ForbiddenReferenceTest.java`
- `conformance/mega-backend/src/test/resources/oracles/`
- `conformance/mega-backend/generated/`
- `conformance/mega-backend/conformance-report.md`
- `scripts/verify-mega-backend.sh`
- Generic analyzer/runtime files and construct fixtures only where a target-neutral capability gap is proven

**Tests Required:**

- [x] Pinned checkout and annotation-overlay reproducibility test.
- [x] Exact semantic graph-oracle tests for all selected entry points.
- [x] Mega runtime-path, explanation, and PlantUML integration test.
- [x] Forbidden Mega-reference scan over production implementation and generic configuration.
- [x] Generic construct regression for every Mega-discovered capability gap.
- [x] Same-artifact non-Mega generality regression.

## Implementation Order

1. Task 1 establishes contracts.
2. Task 2 produces static graph definitions.
3. Task 3 correlates runtime paths.
4. Task 4 projects explanations and diagrams.
5. Task 5 proves the full path.
6. Task 6 verifies scale and documents the result.
7. Task 7 proves that the unchanged generic artifact works on a realistic Mega brownfield corpus.

## Progress Tracking

- Total Tasks: 7
- Completed: 7
- In Progress: 0
- Blocked: 0
- Pending: 0
