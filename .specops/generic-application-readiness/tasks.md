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

- [ ] All required conformance and specification files are tracked.
- [ ] No initiative or dependency points to a missing spec.
- [ ] No completed task points to a missing required artifact.
- [ ] Mega conformance runs without stash or workspace-only files.

**Files to Modify:**

- `conformance/mega-backend/**`
- `.specops/maven-project-analysis/**`
- `.specops/index.json`
- `.specops/initiatives/generic-java-fachtracing.json`
- `scripts/verify-repository-integrity.sh` (new)
- `scripts/verify.sh`
- `README.md`

**Tests Required:**

- [ ] Fresh-clone repository integrity test.
- [ ] Pinned Mega conformance test.

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

- [ ] Current callers remain source and binary compatible where promised.
- [ ] Entry and resolution-only source roles are explicit.
- [ ] Project identity survives indexing without entering business output.
- [ ] Boundary ambiguity fails before graph completion.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisRequest.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisManifest.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/**`

**Tests Required:**

- [ ] Compatibility-constructor contract.
- [ ] Project-aware root and resolution contract.
- [ ] Duplicate and ambiguous symbol contract.

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

- [ ] Local and exact artifact sources resolve a previously incomplete dispatch.
- [ ] Dependency sources never create entries unless explicitly configured.
- [ ] No unnamed source artifact is downloaded.
- [ ] Offline and malicious-archive failures are deterministic and safe.
- [ ] Missing source keeps the graph incomplete with an actionable diagnostic.
- [ ] External and generated sources never receive a false Git commit URL.

**Files to Modify:**

- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/**`
- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/**`
- `fachtracing-maven-plugin/src/test/resources/it/source-boundary/**` (new)
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/developer/DeveloperGraphExporter.java`
- `docs/maven-plugin.md`

**Tests Required:**

- [ ] Additional root and entry-root matrix.
- [ ] Exact source dependency and offline matrix.
- [ ] Traversal, link, duplicate, and archive-size rejection matrix.
- [ ] Developer provenance V1 compatibility and V2 multi-origin matrix.

---

### Task 4: Add one aggregate Maven analysis goal

**Status:** In Progress
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

- [ ] The aggregate goal invokes analysis once per reactor command.
- [ ] `-pl`, `-am`, exclusions, and explicit boundary controls select expected candidates.
- [ ] Duplicate labels cannot replace files.
- [ ] Existing one-off and lifecycle module behavior passes unchanged.

**Files to Modify:**

- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/AnalyzeReactorMojo.java` (new)
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/**`
- `fachtracing-maven-plugin/src/test/resources/it/reactor/**`
- `scripts/verify.sh`
- `docs/maven-plugin.md`

**Tests Required:**

- [ ] Direct aggregate-goal black-box test.
- [ ] Reactor-selection and output-collision matrix.
- [ ] Per-module compatibility suite.

---

### Task 5: Make compiler and JPMS analysis project-safe

**Status:** Pending
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

- [ ] Multiple JPMS modules analyze without discarding their module identity.
- [ ] Duplicate fully qualified names in isolated projects do not collide.
- [ ] Cross-project calls follow declared dependencies.
- [ ] Unsupported model differences give a diagnostic before extraction.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/**`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/**`
- `fachtracing-maven-plugin/src/test/resources/it/compiler-models/**` (new)
- `docs/supported-java-constructs.md`

**Tests Required:**

- [ ] JPMS, duplicate-name, generated-source, encoding, and release matrix.
- [ ] Cross-project dependency-direction contract.

---

### Task 6: Add bounded runtime dispatch diagnostics and context propagation

**Status:** Pending
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

- [ ] Known targets keep exact opaque edge behavior.
- [ ] Unknown and ambiguous targets emit one deduplicated diagnostic per key.
- [ ] Diagnostic memory use is bounded.
- [ ] Business outputs contain no technical runtime target data.
- [ ] Several graph manifests remain active without replacement or cross-correlation.
- [ ] Supported asynchronous continuations preserve context and always clear restored state.
- [ ] Unsupported boundaries produce incomplete evidence, not false correlation.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeCollector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/TraceRuntime.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/runtime/**`
- `fachtracing-agent/src/test/java/**`
- `fachtracing-api/src/main/java/at/gepardec/fachtracing/api/TraceContextCarrier.java` (new)

**Tests Required:**

- [ ] Exact, proxy, unknown, ambiguous, deduplication, overflow, multi-graph, and privacy matrix.
- [ ] Executor, completion-stage, SPI, cleanup, and unsupported-boundary matrix.
- [ ] Concurrent 1,000-RPS diagnostic and propagation pressure test.

---

### Task 7: Create the verified Java capability contract

**Status:** Pending
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

- [ ] Every published support claim has an executable contract.
- [ ] Every unsupported relevant construct produces a located gap.
- [ ] Dynamic mechanisms have explicit static and runtime semantics.
- [ ] Documentation and machine-readable capability data cannot drift.

**Files to Modify:**

- `docs/java-capabilities.json` (new)
- `docs/supported-java-constructs.md`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/**`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/**`
- `fachtracing-engine/src/test/resources/fixtures/java21/**` (new)
- `scripts/verify-java-capabilities.sh` (new)

**Tests Required:**

- [ ] Complete Java 21 capability matrix.
- [ ] Documentation drift check.
- [ ] Unsupported-construct completeness checks.

---

### Task 8: Define the durable decision-record protocol

**Status:** Pending
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

- [ ] V1 records round-trip deterministically and preserve typed values and path order.
- [ ] The application thread performs no repository I/O.
- [ ] Queue and retry policies are bounded and observable.
- [ ] Existing in-memory callers remain compatible.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/model/**`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/store/**`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/**`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/store/**`
- `docs/decision-record-protocol.md` (new)

**Tests Required:**

- [ ] V1 round-trip and backward-reader tests.
- [ ] Queue capacity, retry, shutdown, privacy, and recovery matrix.

---

### Task 9: Add a JDBC persistence and retrieval adapter

**Status:** Pending
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

- [ ] A saved record is retrievable by execution ID.
- [ ] Redacted correlation and time-range queries use indexes.
- [ ] Migrations are repeatable or fail safely according to their documented contract.
- [ ] Outage and recovery do not change application decisions.
- [ ] Retention deletes only eligible records.

**Files to Modify:**

- `fachtracing-storage-jdbc/**` (new)
- `pom.xml`
- `docs/jdbc-storage.md` (new)
- `scripts/verify.sh`

**Tests Required:**

- [ ] Migration, save, idempotency, lookup, query, retention, outage, and recovery suite.
- [ ] Dependency and license audit for the test/reference database.

---

### Task 10: Prove released external-project integration

**Status:** Pending
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

- [ ] External coordinates resolve without `mvn install` in this repository.
- [ ] One annotation and the documented Maven command generate the complete static bundle.
- [ ] The documented `-javaagent` setup produces and persists one retrievable explanation.
- [ ] Upgrade and rollback compatibility tests pass.

**Files to Modify:**

- `distribution/**` or CI release configuration
- `fachtracing-maven-plugin/src/test/resources/it/external-release/**` (new)
- `README.md`
- `docs/maven-plugin.md`
- `docs/runtime-integration.md` (new)

**Tests Required:**

- [ ] Isolated artifact-repository resolution test.
- [ ] Static-to-persisted external fixture test.
- [ ] Upgrade and rollback matrix.

---

### Task 11: Run the final generic release gate

**Status:** Pending
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

- [ ] Every requirement checklist item has executable evidence.
- [ ] All tests pass from a fresh clone without stash or unpublished inputs.
- [ ] The long gate has zero trace-caused errors, result mismatches, contamination, and silently lost
  accepted records, with p95 overhead below 10%.
- [ ] Mega and non-Mega exact semantic checks pass with the same artifacts.
- [ ] The specification remains non-completed until this task passes.

**Files to Modify:**

- `scripts/verify-release.sh` (new)
- `docs/release-evidence.md` (new)
- `.specops/generic-application-readiness/implementation.md`
- `.specops/generic-application-readiness/evaluation.md`

**Tests Required:**

- [ ] Complete fresh-clone release gate.

## Implementation Order

1. Task 1 repairs the evidence baseline.
2. Tasks 2 and 3 define the complete source boundary.
3. Tasks 4 and 5 make Maven and compiler analysis safe and efficient.
4. Tasks 6 and 7 close runtime diagnostic and Java support claims.
5. Tasks 8 and 9 add durable records and retrieval.
6. Task 10 proves external installation.
7. Task 11 is the only completion gate.

## Progress Tracking

- Total Tasks: 11
- Completed: 2
- In Progress: 1
- Blocked: 0
- Pending: 8
