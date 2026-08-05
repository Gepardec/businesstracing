# Implementation Tasks: Generic Application Readiness

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `generic-tracing-walking-skeleton` | Supplies the product and conformance baseline. | Yes | completed |
| `reactor-wide-implementation-resolution` | Supplies source-role isolation. | Yes | completed |
| `runtime-decision-path-capture` | Supplies exact runtime edge capture. | Yes | completed |
| `maven-developer-graph-export` | Supplies build-time JSON and source provenance. | Yes | completed |

## Task Breakdown

### Task 1: Restore repository and specification integrity

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None

**Description:**
Restore all reviewed Mega and Maven specification artifacts, reconcile SpecOps references, and add a
fresh-clone integrity gate.

**Implementation Steps:**

1. Recover the preserved Mega and `maven-project-analysis` files without overwriting newer content.
2. Verify Mega oracle hashes and generated evidence against the pinned source revision.
3. Reconcile the SpecOps index, initiative members, dependency references, and completed claims.
4. Add a checker for public links and files required by completed tasks.
5. Run the checker in the main verification workflow from a clean temporary clone.

**Acceptance Criteria:**

- [x] All required conformance and specification files are tracked.
- [x] No initiative or dependency points to a missing spec.
- [x] No completed task points to a missing required artifact.
- [x] Mega conformance runs without stash or workspace-only files.

**Files to Modify:**

- `conformance/mega-backend/**`
- `.specops/maven-project-analysis/**`
- `.specops/index.json`
- `.specops/initiatives/generic-java-fachtracing.json`
- `scripts/verify-repository-integrity.sh` (new)
- `scripts/verify.sh`
- `README.md`

**Tests Required:**

- [x] Fresh-clone repository integrity test.
- [x] Pinned Mega conformance test.

---

### Task 2: Define the application source-boundary API

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None

**Description:**
Replace the flat source universe with project-aware entry and resolution source roles while keeping
the engine independent of Maven.

**Implementation Steps:**

1. Add immutable project, compiler-model, source-role, dependency-edge, and boundary records.
2. Add compatibility conversion from the current `AnalysisRequest` constructors.
3. Extend source mappings and diagnostics with developer-only project identity.
4. Add boundary fingerprints and duplicate/ambiguity validation.
5. Add engine contracts for entry isolation and cross-project symbol resolution.

**Acceptance Criteria:**

- [x] Current callers remain source and binary compatible where promised.
- [x] Entry and resolution-only source roles are explicit.
- [x] Project identity survives indexing without entering business output.
- [x] Boundary ambiguity fails before graph completion.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisRequest.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisManifest.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/**`

**Tests Required:**

- [x] Compatibility-constructor contract.
- [x] Project-aware root and resolution contract.
- [x] Duplicate and ambiguous symbol contract.

---

### Task 3: Add safe external source inputs

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 2
**Priority:** High
**IssueID:** None

**Description:**
Add resolution-only local source roots and exact Maven source dependencies with safe archive handling.

**Implementation Steps:**

1. Add Maven parameters for additional source roots, entry roots, exact coordinates, and limits.
2. Resolve named sources under Maven repository and offline rules.
3. Extract archives into a content-addressed build directory with traversal and size checks.
4. Add searched-boundary diagnostics for absent implementations.
5. Add developer provenance V2 for Git, Maven source artifact, and generated-source origins.
6. Document privacy, credentials, cleanup, cache, and provenance behavior.

**Acceptance Criteria:**

- [x] Local and exact artifact sources resolve a previously incomplete dispatch.
- [x] Dependency sources never create entries unless explicitly configured.
- [x] No unnamed source artifact is downloaded.
- [x] Offline and malicious-archive failures are deterministic and safe.
- [x] Missing source keeps the graph incomplete with an actionable diagnostic.
- [x] External and generated sources never receive a false Git commit URL.

**Files to Modify:**

- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/**`
- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/**`
- `fachtracing-maven-plugin/src/test/resources/it/source-boundary/**` (new)
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/developer/DeveloperGraphExporter.java`
- `docs/maven-plugin.md`

**Tests Required:**

- [x] Additional root and entry-root matrix.
- [x] Exact source dependency and offline matrix.
- [x] Traversal, link, duplicate, and archive-size rejection matrix.
- [x] Developer provenance V1 compatibility and V2 multi-origin matrix.

---

### Task 4: Add one aggregate Maven analysis goal

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Tasks 2 and 3
**Priority:** High
**IssueID:** None

**Description:**
Run project-aware analysis once for the effective reactor and create deterministic aggregate output.

**Implementation Steps:**

1. Add the direct `analyze-reactor` aggregator Mojo.
2. Read the effective Maven project selection and apply explicit include/exclude coordinates.
3. Build each effective compiler model after reactor compilation.
4. Generate one collision-safe aggregate index and activation bundle.
5. Preserve and regression-test the per-module `analyze` goal.

**Acceptance Criteria:**

- [x] The aggregate goal invokes analysis once per reactor command.
- [x] `-pl`, `-am`, exclusions, and explicit boundary controls select expected candidates.
- [x] Duplicate labels cannot replace files.
- [x] Existing one-off and lifecycle module behavior passes unchanged.

**Files to Modify:**

- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/AnalyzeReactorMojo.java` (new)
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/**`
- `fachtracing-maven-plugin/src/test/resources/it/reactor/**`
- `scripts/verify.sh`
- `docs/maven-plugin.md`

**Tests Required:**

- [x] Direct aggregate-goal black-box test.
- [x] Reactor-selection and output-collision matrix.
- [x] Per-module compatibility suite.

---

### Task 5: Make compiler and JPMS analysis project-safe

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Tasks 2 and 4
**Priority:** High
**IssueID:** None

**Description:**
Replace flat union attribution with per-project compiler contexts and validated cross-project links.

**Implementation Steps:**

1. Preserve module descriptors and effective compiler settings in project models.
2. Attribute each project in a valid compiler context.
3. Create stable cross-project symbol keys and dependency-aware linking.
4. Detect unsupported compiler-model differences and ambiguous definitions.
5. Add JPMS, duplicate-class, generated-source, encoding, and release fixtures.

**Acceptance Criteria:**

- [x] Multiple JPMS modules analyze without discarding their module identity.
- [x] Duplicate fully qualified names in isolated projects do not collide.
- [x] Cross-project calls follow declared dependencies.
- [x] Unsupported model differences give a diagnostic before extraction.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/**`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/**`
- `fachtracing-maven-plugin/src/test/resources/it/compiler-models/**` (new)
- `docs/supported-java-constructs.md`

**Tests Required:**

- [x] JPMS, duplicate-name, generated-source, encoding, and release matrix.
- [x] Cross-project dependency-direction contract.

---

### Task 6: Add bounded runtime dispatch diagnostics and context propagation

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 2
**Priority:** High
**IssueID:** None

**Description:**
Report unmatched or ambiguous runtime targets, support several active graphs, and preserve context
across explicit in-JVM asynchronous boundaries without changing business records or application flow.

**Implementation Steps:**

1. Add exact and unique-most-specific assignable dispatch indexes.
2. Add stable mismatch reason codes and developer diagnostic data.
3. Add bounded deduplication, capacity, overflow counters, and polling.
4. Replace the single active-manifest assumption with a graph-version and class-fingerprint registry.
5. Add immutable context tokens, JDK executor/completion-stage wrappers, and a framework-neutral SPI.
6. Keep proxy and runtime class details outside business models and renderers.
7. Add load pressure, multi-graph, asynchronous, and concurrency contracts.

**Acceptance Criteria:**

- [x] Known targets keep exact opaque edge behavior.
- [x] Unknown and ambiguous targets emit one deduplicated diagnostic per key.
- [x] Diagnostic memory use is bounded.
- [x] Business outputs contain no technical runtime target data.
- [x] Several graph manifests remain active without replacement or cross-correlation.
- [x] Supported asynchronous continuations preserve context and always clear restored state.
- [x] Unsupported boundaries produce incomplete evidence, not false correlation.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeCollector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/TraceRuntime.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/runtime/**`
- `fachtracing-agent/src/test/java/**`
- `fachtracing-api/src/main/java/at/gepardec/fachtracing/api/TraceContextCarrier.java` (new)

**Tests Required:**

- [x] Exact, proxy, unknown, ambiguous, deduplication, overflow, multi-graph, and privacy matrix.
- [x] Executor, completion-stage, SPI, cleanup, and unsupported-boundary matrix.
- [x] Concurrent 1,000-RPS diagnostic and propagation pressure test.

---

### Task 7: Create the verified Java capability contract

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Tasks 2, 5, and 6
**Priority:** High
**IssueID:** None

**Description:**
Turn Java support claims into a machine-readable matrix and close construct-level gaps for the
declared Java 21 baseline.

**Implementation Steps:**

1. Define the versioned capability schema and documentation projection.
2. Inventory all result-relevant Java 21 source constructs and current fallback behavior.
3. Add one generic executable contract for each supported or explicit-gap entry.
4. Implement missing generic source-visible constructs required for baseline completeness.
5. Add compiler and bytecode-producer release gates.

**Acceptance Criteria:**

- [x] Every published support claim has an executable contract.
- [x] Every unsupported relevant construct produces a located gap.
- [x] Dynamic mechanisms have explicit static and runtime semantics.
- [x] Documentation and machine-readable capability data cannot drift.

**Files to Modify:**

- `docs/java-capabilities.json` (new)
- `docs/supported-java-constructs.md`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/**`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/**`
- `fachtracing-engine/src/test/resources/fixtures/java21/**` (new)
- `scripts/verify-java-capabilities.sh` (new)

**Tests Required:**

- [x] Complete Java 21 capability matrix.
- [x] Documentation drift check.
- [x] Unsupported-construct completeness checks.

---

### Task 8: Define the durable decision-record protocol

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 6
**Priority:** High
**IssueID:** None

**Description:**
Add a versioned record envelope and bounded asynchronous delivery without application-thread I/O.

**Implementation Steps:**

1. Add the V1 envelope and deterministic serialization contract.
2. Add redacted correlation keys and query types.
3. Add bounded admission, retry, shutdown, and delivery-counter policies.
4. Extend the repository port with save and retrieval capabilities without breaking current callers.
5. Add serialization, concurrency, outage, and recovery contracts.

**Acceptance Criteria:**

- [x] V1 records round-trip deterministically and preserve typed values and path order.
- [x] The application thread performs no repository I/O.
- [x] Queue and retry policies are bounded and observable.
- [x] Existing in-memory callers remain compatible.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/model/**`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/store/**`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/**`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/store/**`
- `docs/decision-record-protocol.md` (new)

**Tests Required:**

- [x] V1 round-trip and backward-reader tests.
- [x] Queue capacity, retry, shutdown, privacy, and recovery matrix.

---

### Task 9: Add a JDBC persistence and retrieval adapter

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 8
**Priority:** High
**IssueID:** None

**Description:**
Provide one production-shaped JDBC adapter with migrations, indexed retrieval, retention, and
fault-injection tests.

**Implementation Steps:**

1. Select the first reference database after dependency safety review.
2. Create a separate JDBC adapter module and versioned migrations.
3. Implement idempotent save, execution lookup, correlation/time query, and retention deletion.
4. Add transaction, retry classification, and schema upgrade behavior.
5. Add database fault and high-rate integration tests.

**Acceptance Criteria:**

- [x] A saved record is retrievable by execution ID.
- [x] Redacted correlation and time-range queries use indexes.
- [x] Migrations are repeatable or fail safely according to their documented contract.
- [x] Outage and recovery do not change application decisions.
- [x] Retention deletes only eligible records.

**Files to Modify:**

- `fachtracing-storage-jdbc/**` (new)
- `pom.xml`
- `docs/jdbc-storage.md` (new)
- `scripts/verify.sh`

**Tests Required:**

- [x] Migration, save, idempotency, lookup, query, retention, outage, and recovery suite.
- [x] Dependency and license audit for the test/reference database.

---

### Task 10: Prove released external-project integration

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Tasks 4, 7, 8, and 9
**Priority:** High
**IssueID:** None

**Description:**
Make the documented integration work from a clean project that has no source checkout or local Maven
installation of Fachtracing.

**Implementation Steps:**

1. Define non-snapshot artifact, compatibility, upgrade, and rollback policy.
2. Publish a release candidate to an isolated test repository.
3. Build a clean external Maven fixture from only published coordinates.
4. Generate static assets and activation bundle, run with the agent, persist, retrieve, and explain.
5. Replace all copyable documentation placeholders with verified commands.

**Acceptance Criteria:**

- [x] External coordinates resolve without `mvn install` in this repository.
- [x] One annotation and the documented Maven command generate the complete static bundle.
- [x] The documented `-javaagent` setup produces and persists one retrievable explanation.
- [x] Upgrade and rollback compatibility tests pass.

**Files to Modify:**

- `distribution/**` or CI release configuration
- `fachtracing-maven-plugin/src/test/resources/it/external-release/**` (new)
- `README.md`
- `docs/maven-plugin.md`
- `docs/runtime-integration.md` (new)

**Tests Required:**

- [x] Isolated artifact-repository resolution test.
- [x] Static-to-persisted external fixture test.
- [x] Upgrade and rollback matrix.

---

### Task 11: Run the final generic release gate

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Tasks 1 through 10
**Priority:** High
**IssueID:** None

**Description:**
Run one clean, reproducible release gate that proves every requirement and prevents partial completion.

**Implementation Steps:**

1. Create a clean temporary clone with an empty local artifact repository.
2. Run repository integrity, generic, capability, Maven, compiler-model, runtime, JDBC, and external
   release tests.
3. Run pinned Mega conformance and at least two non-Mega same-artifact domains.
4. Run the 600-second, 1,000-RPS persistence-enabled workload with fault windows.
5. Scan production source and configuration for reference-specific hints and business-output leaks.
6. Publish the evidence manifest with commands, versions, hashes, and results.

**Acceptance Criteria:**

- [x] Every requirement checklist item has executable evidence.
- [x] All tests pass from a fresh clone without stash or unpublished inputs.
- [x] The long gate has zero trace-caused errors, result mismatches, contamination, and silently lost
  accepted records, with p95 overhead below 10%.
- [x] Mega and non-Mega exact semantic checks pass with the same artifacts.
- [x] The specification remains non-completed until this task passes.

**Files to Modify:**

- `scripts/verify-release.sh` (new)
- `docs/release-evidence.md` (new)
- `.specops/generic-application-readiness/implementation.md`
- `.specops/generic-application-readiness/evaluation.md`

**Tests Required:**

- [x] Complete fresh-clone release gate.

## Implementation Order

1. Task 1 repairs the evidence baseline.
2. Tasks 2 and 3 define the complete source boundary.
3. Tasks 4 and 5 make Maven and compiler analysis safe and efficient.
4. Tasks 6 and 7 close runtime diagnostic and Java support claims.
5. Tasks 8 and 9 add durable records and retrieval.
6. Task 10 proves external installation.
7. Task 11 is the only completion gate.

### Task 12: Prove real external runtime tracing

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 10
**Priority:** High
**IssueID:** None

**Acceptance Criteria:**

- [x] The external fixture invokes `ExternalDecision.approve` under a configured transformer.
- [x] The persisted execution contains injected input, predicate-edge, and result observations.
- [x] Retrieval produces a business explanation from the actual execution.

### Task 13: Complete aggregate source-boundary inputs

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Tasks 3 and 4
**Priority:** High
**IssueID:** None

**Acceptance Criteria:**

- [x] Aggregate local resolution and entry roots have distinct entry behavior.
- [x] Exact source artifacts are resolution-only and work in offline mode from local cache.
- [x] Aggregate multi-origin developer provenance remains accurate.

### Task 14: Preserve the effective Maven compiler model

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 5
**Priority:** High
**IssueID:** None

**Acceptance Criteria:**

- [x] Effective release or source/target, encoding, arguments, generated roots, module path, and
  processor path are represented or rejected before extraction.
- [x] JPMS analysis uses a valid per-project compiler context and does not silently remove module
  descriptors.

### Task 15: Enforce strict concurrent diagnostic capacity

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 6
**Priority:** High
**IssueID:** None

**Acceptance Criteria:**

- [x] Concurrent unique mismatch publication never retains more than the configured capacity.
- [x] Overflow and deduplication counters remain correct under pressure.

### Task 16: Make delivery shutdown loss-accountable

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 8
**Priority:** High
**IssueID:** None

**Acceptance Criteria:**

- [x] `close()` does not return while the worker remains active.
- [x] Each accepted record is saved or counted as dropped after shutdown.
- [x] Interrupted and timed-out retry paths have deterministic counter contracts.

### Task 17: Rerun corrected release evidence

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Tasks 12 through 16
**Priority:** High
**IssueID:** None

**Acceptance Criteria:**

- [x] Main verification and pinned Mega conformance pass.
- [x] The full clean-clone 600-second release gate passes with corrected runtime evidence.

## Progress Tracking

- Total Tasks: 17
- Completed: 17
- In Progress: 0
- Blocked: 0
- Pending: 0

## PR 5 Remediation Tasks — Iteration 3

### Task 18: Generate and consume a complete runtime activation bundle

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Tasks 10 and 12
**Priority:** High

**Acceptance Criteria:**

- [x] The aggregate bundle round-trips graphs, manifests, boundary data, and class fingerprints.
- [x] The agent supports all manifests in one bundle and verifies each original class once.
- [x] The external release fixture traces and explains its decision without source or compiler use.

### Task 19: Use the JPMS compiler context for extraction

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 14
**Priority:** High

**Acceptance Criteria:**

- [x] Modular extraction includes all descriptors and the effective module path in its extraction task.
- [x] The JPMS reactor graph still resolves both source implementations.
- [x] Incompatible modular compiler models or external unnamed sources fail before extraction.

### Task 20: Bound delivery shutdown and JDBC statements

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 16
**Priority:** High

**Acceptance Criteria:**

- [x] A repository operation that ignores interruption cannot keep `close()` blocked.
- [x] The delivery worker stops within the configured deadline and accounts for the in-flight record.
- [x] JDBC statements use the configured timeout in save, query, migration, and retention paths.

### Task 21: Add independent Java construct contracts

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 7
**Priority:** High

**Acceptance Criteria:**

- [x] Try-with-resources, pattern matching, sealed types, nested classes, and method references each
  have one matrix entry and one focused executable contract.
- [x] Supported constructs assert topology; unsupported constructs assert an explicit gap.

### Task 22: Rerun all corrected release evidence

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Tasks 18 through 21
**Priority:** High

**Acceptance Criteria:**

- [x] Main verification and pinned Mega conformance pass.
- [x] The clean-clone 600-second release gate passes with source-free activation and bounded shutdown.

## Iteration 3 Progress Tracking

- Total Tasks: 22
- Completed: 22
- In Progress: 0
- Blocked: 0
- Pending: 0

## PR 5 Remediation Tasks — Iteration 4

### Task 23: Make execution IDs and idempotency collision-safe

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 22
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add a restart-safe execution namespace and reject duplicate storage keys that contain different
decision records.

**Acceptance Criteria:**

- [x] Two new collectors generate different first execution IDs.
- [x] One collector keeps unique ordered IDs under concurrent load.
- [x] Exact envelope retries remain idempotent in memory and JDBC.
- [x] A different envelope with a reused execution ID or record ID fails and keeps the first record.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeCollector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/store/DecisionRecordRepository.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/store/InMemoryDecisionRecordRepository.java`
- `fachtracing-storage-jdbc/src/main/java/at/gepardec/fachtracing/storage/jdbc/JdbcDecisionRecordRepository.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/runtime/RuntimeCollectorTest.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/store/DecisionRecordProtocolTest.java`
- `fachtracing-storage-jdbc/src/test/java/at/gepardec/fachtracing/storage/jdbc/JdbcDecisionRecordRepositoryTest.java`

**Tests Required:**

- [x] Runtime identity contract.
- [x] In-memory and H2 duplicate-collision contracts.

---

### Task 24: Make timed-out delivery outcomes honest and bounded

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 23
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Record an unknown storage outcome when an operation can finish after its wait times out. Stop the
delivery circuit after this state so that only one uncooperative operation can remain detached.

**Acceptance Criteria:**

- [x] A timed-out uncooperative save increments `unknown` and does not increment `dropped`.
- [x] The delivery worker stops, rejects later offers, and leaves no unresolved accepted count.
- [x] A later commit does not change the honest unknown counter.
- [x] Queued records that never started are counted as dropped.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/DecisionRecordDelivery.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/store/DecisionRecordProtocolTest.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/performance/FachtracingLoadTest.java`
- `docs/decision-record-protocol.md`
- `docs/jdbc-storage.md`

**Tests Required:**

- [x] Timeout, shutdown, late-commit, queue-drain, and accounting contracts.

---

### Task 25: Bind overloaded methods by JVM descriptor

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 24
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Carry JVM descriptors from attributed source into activation data and use them for agent method and
lambda binding.

**Acceptance Criteria:**

- [x] Analyzer manifests contain descriptors for normal, dispatch, branch, and lambda bindings.
- [x] Activation V3 round-trips descriptors and reads legacy V2 bundles.
- [x] Two annotated overloads instrument and execute as two independent graphs.
- [x] Lambda targets from overloaded methods do not cross-bind.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisManifest.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DecisionGraphBuilder.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeActivationBundle.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingTransformer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `fachtracing-agent/src/test/java/at/gepardec/fachtracing/agent/FachtracingTransformerTest.java`
- `fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java`
- `docs/runtime-integration.md`

**Tests Required:**

- [x] Descriptor extraction, activation compatibility, overload, and overloaded-lambda contracts.

---

### Task 26: Support mixed modular and non-modular reactors

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 25
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Select JPMS mode from the graph-entry project and partition mixed connected sources without a false
module-descriptor requirement.

**Acceptance Criteria:**

- [x] A non-modular annotated library stays complete when a modular project depends on it.
- [x] A modular entry does not require unrelated connected non-modular projects to define modules.
- [x] Existing all-modular and all-flat compiler contracts remain unchanged.
- [x] Unavailable cross-mode source logic produces a coverage gap, not a false compiler claim.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `docs/supported-java-constructs.md`

**Tests Required:**

- [x] Mixed-boundary, all-modular, and all-flat compiler contracts.

---

### Task 27: Rerun all iteration 4 release evidence

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Tasks 23, 24, 25, 26
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Run the full generic, external, Mega, and ten-minute release gates. Update the specification,
decision journal, documentation, and evidence only after all gates pass.

**Acceptance Criteria:**

- [x] `./scripts/verify.sh` passes.
- [x] External release integration invokes and stores an instrumented annotated overload-safe method.
- [x] Pinned Mega conformance returns five complete graphs with no production Mega hints.
- [x] The 600,000-decision gate passes at 1,000 RPS with less than 10% p95 overhead and no unresolved accepted records.
- [x] Repository integrity, documentation, memory, index, initiative, and clean-worktree checks pass.

**Files to Modify:**

- `.specops/generic-application-readiness/*`
- `.specops/index.json`
- `.specops/initiatives/generic-java-fachtracing.json`
- `.specops/initiatives/generic-java-fachtracing-log.md`
- `.specops/memory/context.md`
- `.specops/memory/decisions.json`
- `README.md`
- `docs/*.md`

**Tests Required:**

- [x] Focused contract suites.
- [x] Full generic verifier.
- [x] External release fixture.
- [x] Mega conformance.
- [x] Ten-minute persistence-enabled load gate.

## Iteration 4 Progress Tracking

- Total Tasks: 27
- Completed: 27
- In Progress: 0
- Blocked: 0
- Pending: 0
