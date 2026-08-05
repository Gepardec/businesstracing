# Tasks: Generic Java Extractor Completion

## Task 1: Freeze independent capability contracts

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add independent generic fixtures and machine-readable capability entries for every
new supported and controlled-fallback construct. Correct the Activation V3 JavaDoc.

**Acceptance Criteria:**

- [x] Exception, resource, synchronized, complex Boolean, ternary, switch, dynamic invocation,
  binary fallback, automatic async, and owned-module fixtures each run independently.
- [x] Every capability entry names one executable contract.
- [x] Current explicit-gap contracts fail against the new expected complete subset before implementation.
- [x] `RuntimeActivationBundle` JavaDoc states V3 and V2 read compatibility.

**Files to Modify:**

- `fachtracing-engine/src/test/resources/fixtures/**`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `fachtracing-agent/src/test/java/**`
- `docs/java-capabilities.json`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeActivationBundle.java`

**Tests Required:**

- [x] One failing-before-change contract per capability cluster.

---

## Task 2: Extract structured exception and synchronized flow

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add explicit control-flow exit kinds, compatible catch routing, finally transforms,
resource semantics, and transparent synchronized-block scanning.

**Acceptance Criteria:**

- [x] Supported try/catch/finally and try-with-resources fixtures are complete.
- [x] Runtime executions select normal, caught, and finally-overridden result paths exactly.
- [x] Synchronized fixtures retain business predicates and mutations with no monitor vocabulary.
- [x] Unproven implicit exceptions remain source-located actionable gaps.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DependencyGraphBuilder.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DecisionGraphBuilder.java`
- `fachtracing-engine/src/test/**`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingTransformer.java`

**Tests Required:**

- [x] Normal try, matching catch, multi-catch, finally mutation, finally return, resource failure,
  nested try, and synchronized mutation contracts.

---

## Task 3: Lower complex Boolean expressions into exact atomic paths

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Replace compound evaluated-only probes with atomic graph topology and fail-closed
correlation for each atom in mixed and nested Boolean logic and Boolean ternaries.

**Acceptance Criteria:**

- [x] Mixed `&&` and `||`, nested grouping, and `!` preserve Java evaluation order.
- [x] Each evaluated atom records typed evidence and one exact edge.
- [x] Skipped atoms have no observation.
- [x] Ternary conditions and chosen value paths are exact.
- [x] Partial bytecode correlation produces an actionable gap, not a legacy evaluated observation.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisManifest.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DecisionGraphBuilder.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingTransformer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeActivationBundle.java`
- `fachtracing-engine/src/test/**`
- `fachtracing-agent/src/test/**`

**Tests Required:**

- [x] Truth-table executions for mixed, nested, negated, short-circuited, lambda, and ternary forms.

---

## Task 4: Record exact switch choices

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 3
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add choice-target manifests and exact table, lookup, string, enum, and supported
pattern switch correlation.

**Acceptance Criteria:**

- [x] Switch statements and expressions record one exact case or default edge.
- [x] Integral, string, enum, and supported Java 21 pattern fixtures are complete.
- [x] Compiler-lowered helper comparisons do not appear as business nodes.
- [x] Incomplete case correlation produces an actionable gap.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisManifest.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeActivationBundle.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingTransformer.java`
- `fachtracing-engine/src/test/**`
- `fachtracing-agent/src/test/**`

**Tests Required:**

- [x] One execution for every case and default in each supported switch form.

---

## Task 5: Resolve dynamic invocation from proven candidates

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Tasks 3 and 4
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add call-site candidate plans and runtime handling for proxies, `ServiceLoader`, and
reflection without guessing.

**Acceptance Criteria:**

- [x] Exact proxy, service provider, and reflected member evidence selects one static candidate.
- [x] Unknown and ambiguous cases record bounded diagnostics and execution gaps.
- [x] Raw runtime classes and Java invocation mechanics stay out of business output.
- [x] Multi-graph and concurrent executions do not cross-select candidates.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisManifest.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeCollector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/InvocationContext.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/TraceRuntime.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingTransformer.java`
- `fachtracing-engine/src/test/**`
- `fachtracing-agent/src/test/**`

**Tests Required:**

- [x] Proxy subclass, JDK proxy, service provider, reflection, unknown, ambiguous, bounded-memory,
  multi-graph, and concurrent isolation contracts.

---

## Task 6: Add the controlled bytecode decision fallback

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Analyze fingerprinted binary methods in a fail-closed ASM subset only after source
artifact resolution fails.

**Acceptance Criteria:**

- [x] Simple binary constants, parameters, fields, calculations, comparisons, branches, and returns
  produce a proven fragment.
- [x] Class fingerprint mismatches prevent use.
- [x] Unsupported calls, monitors, exception tables, native code, and invokedynamic remain gaps.
- [x] Binary implementation details never enter business labels.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/BytecodeDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/ApplicationSourceBoundary.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/test/**`
- `docs/supported-java-constructs.md`

**Tests Required:**

- [x] Accepted subset matrix, rejected opcode matrix, missing metadata, fingerprint mismatch, and
  source-preferred-over-binary contracts.

---

## Task 7: Propagate context automatically across standard async calls

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add idempotent functional wrappers and inject them at supported application call
sites for executors, stages, platform threads, and virtual threads.

**Acceptance Criteria:**

- [x] Application code uses no manual tracing wrapper in automatic-propagation fixtures.
- [x] Executor, submit, stage callback, platform-thread, and virtual-thread observations join the
  correct execution.
- [x] Inactive contexts preserve callback identity.
- [x] Concurrent traces have zero contamination and restored state is always cleared.

**Files to Modify:**

- `fachtracing-api/src/main/java/at/gepardec/fachtracing/api/TraceContextCarrier.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeCollector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/TraceRuntime.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingTransformer.java`
- `fachtracing-engine/src/test/**`
- `fachtracing-agent/src/test/**`

**Tests Required:**

- [x] Executor, executor service, completion stage, platform thread, virtual thread, inactive trace,
  nested trace, exception cleanup, and 1,000-concurrent-trace isolation contracts.

---

## Task 8: Attribute owned external JPMS sources

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 6
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add named and automatic module ownership to external resolution sources and Maven
configuration, then place owned sources in the valid compiler context.

**Acceptance Criteria:**

- [x] Named external source joins its declared source module.
- [x] Automatic-module source is paired with its Maven binary identity and resolves reachable logic.
- [x] Missing, conflicting, or unreadable ownership fails before extraction.
- [x] Ownership changes the boundary fingerprint and developer provenance only.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/ApplicationSourceBoundary.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/SourceInputResolver.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/AnalyzeMojo.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/AnalyzeReactorMojo.java`
- `fachtracing-maven-plugin/src/test/**`

**Tests Required:**

- [x] Named source module, automatic module, Maven source artifact, invalid ownership, unreadable
  module, mixed reactor, all-flat, and all-modular contracts.

---

## Task 9: Add PostgreSQL integration

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add a test-only PostgreSQL driver and an environment-controlled integration suite
against PostgreSQL 18.4 while retaining H2.

**Acceptance Criteria:**

- [x] PostgreSQL tests cover schema, save, exact retry, both key conflicts, lookup, correlation,
  retention, rollback, and timeout behavior.
- [x] The driver is test scope only and production modules remain vendor-neutral.
- [x] Local verification skips PostgreSQL only when no explicit connection is configured.

**Files to Modify:**

- `fachtracing-storage-jdbc/pom.xml`
- `fachtracing-storage-jdbc/src/test/java/**`
- `scripts/verify.sh`
- `docs/jdbc-storage.md`

**Tests Required:**

- [x] H2 regression and PostgreSQL 18.4 integration.

---

## Task 10: Add required pull-request CI

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 9
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add a read-only GitHub workflow that runs standard, external, Mega, PostgreSQL, and
long-load evidence for every pull request.

**Acceptance Criteria:**

- [x] Workflow triggers on pull requests and manual dispatch.
- [x] Workflow uses Java 21, Maven cache, PostgreSQL 18.4, and read-only permissions.
- [x] Standard verification, pinned Mega, external activation, PostgreSQL, and 600-second load run.
- [x] The workflow has no Mega or database credentials in production configuration.

**Files to Modify:**

- `.github/workflows/verify.yml`
- `scripts/verify-release.sh`
- `docs/release-evidence.md`

**Tests Required:**

- [x] Workflow syntax check and command parity check.

---

## Task 11: Update supported-capability and integration documentation

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Tasks 2 through 10
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:** Document the new complete subsets, exact paths, controlled fallbacks, configuration,
automatic async behavior, module ownership, PostgreSQL test, and CI without overstating coverage.

**Acceptance Criteria:**

- [x] Documentation names each complete subset and each remaining precise unsupported variant.
- [x] Maven examples show owned named and automatic source inputs.
- [x] Runtime examples need no manual wrappers for supported async calls.
- [x] Business-output privacy and no-guessing rules remain explicit.

**Files to Modify:**

- `README.md`
- `docs/supported-java-constructs.md`
- `docs/runtime-integration.md`
- `docs/maven-plugin.md`
- `docs/jdbc-storage.md`
- `docs/release-evidence.md`
- `docs/java-capabilities.json`

**Tests Required:**

- [x] Documentation-to-capability verifier.

---

## Task 12: Run full generic release evidence

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Tasks 2 through 11
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Run all focused, standard, external, PostgreSQL, pinned Mega, forbidden-reference,
clean-clone, and 600-second performance gates. Complete SpecOps memory and release evidence only
after all gates pass.

**Acceptance Criteria:**

- [x] Every independent capability contract passes.
- [x] `./scripts/verify.sh` and external activation integration pass.
- [x] PostgreSQL 18.4 integration passes.
- [x] Mega produces five reviewed complete graphs with no production hints.
- [x] The 600,000-decision gate passes below 10% p95 overhead with zero correctness, contamination,
  silently lost, or unresolved accepted-record failures.
- [x] Repository integrity, documentation, memory, index, initiative, and clean-worktree checks pass.

**Files to Modify:**

- `.specops/generic-java-extractor-completion/*`
- `.specops/index.json`
- `.specops/initiatives/generic-java-fachtracing*`
- `.specops/memory/*`
- `docs/release-evidence.md`

**Tests Required:**

- [x] Focused contract suites.
- [x] Full generic verifier and external release fixture.
- [x] PostgreSQL integration.
- [x] Pinned Mega conformance and forbidden-reference scan.
- [x] Clean-clone ten-minute persistence-enabled load gate.

## Progress Tracking

- Total Tasks: 12
- Completed: 12
- In Progress: 0
- Blocked: 0
- Pending: 0
