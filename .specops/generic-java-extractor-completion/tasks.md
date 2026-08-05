# Tasks: Generic Java Extractor Completion

## Task 1: Freeze independent capability contracts

**Status:** In Progress
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add independent generic fixtures and machine-readable capability entries for every
new supported and controlled-fallback construct. Correct the Activation V3 JavaDoc.

**Acceptance Criteria:**

- [ ] Exception, resource, synchronized, complex Boolean, ternary, switch, dynamic invocation,
  binary fallback, automatic async, and owned-module fixtures each run independently.
- [ ] Every capability entry names one executable contract.
- [ ] Current explicit-gap contracts fail against the new expected complete subset before implementation.
- [ ] `RuntimeActivationBundle` JavaDoc states V3 and V2 read compatibility.

**Files to Modify:**

- `fachtracing-engine/src/test/resources/fixtures/**`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `fachtracing-agent/src/test/java/**`
- `docs/java-capabilities.json`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeActivationBundle.java`

**Tests Required:**

- [ ] One failing-before-change contract per capability cluster.

---

## Task 2: Extract structured exception and synchronized flow

**Status:** Pending
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add explicit control-flow exit kinds, compatible catch routing, finally transforms,
resource semantics, and transparent synchronized-block scanning.

**Acceptance Criteria:**

- [ ] Supported try/catch/finally and try-with-resources fixtures are complete.
- [ ] Runtime executions select normal, caught, and finally-overridden result paths exactly.
- [ ] Synchronized fixtures retain business predicates and mutations with no monitor vocabulary.
- [ ] Unproven implicit exceptions remain source-located actionable gaps.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DependencyGraphBuilder.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DecisionGraphBuilder.java`
- `fachtracing-engine/src/test/**`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingTransformer.java`

**Tests Required:**

- [ ] Normal try, matching catch, multi-catch, finally mutation, finally return, resource failure,
  nested try, and synchronized mutation contracts.

---

## Task 3: Lower complex Boolean expressions into exact atomic paths

**Status:** Pending
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Replace compound evaluated-only probes with atomic graph topology and all-or-none
bytecode correlation for mixed and nested Boolean logic and Boolean ternaries.

**Acceptance Criteria:**

- [ ] Mixed `&&` and `||`, nested grouping, and `!` preserve Java evaluation order.
- [ ] Each evaluated atom records typed evidence and one exact edge.
- [ ] Skipped atoms have no observation.
- [ ] Ternary conditions and chosen value paths are exact.
- [ ] Partial bytecode correlation produces an actionable gap, not a legacy evaluated observation.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisManifest.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DecisionGraphBuilder.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingTransformer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeActivationBundle.java`
- `fachtracing-engine/src/test/**`
- `fachtracing-agent/src/test/**`

**Tests Required:**

- [ ] Truth-table executions for mixed, nested, negated, short-circuited, lambda, and ternary forms.

---

## Task 4: Record exact switch choices

**Status:** Pending
**Estimated Effort:** L
**Dependencies:** Task 3
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add choice-target manifests and exact table, lookup, string, enum, and supported
pattern switch correlation.

**Acceptance Criteria:**

- [ ] Switch statements and expressions record one exact case or default edge.
- [ ] Integral, string, enum, and supported Java 21 pattern fixtures are complete.
- [ ] Compiler-lowered helper comparisons do not appear as business nodes.
- [ ] Incomplete case correlation produces an actionable gap.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisManifest.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeActivationBundle.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingTransformer.java`
- `fachtracing-engine/src/test/**`
- `fachtracing-agent/src/test/**`

**Tests Required:**

- [ ] One execution for every case and default in each supported switch form.

---

## Task 5: Resolve dynamic invocation from proven candidates

**Status:** Pending
**Estimated Effort:** L
**Dependencies:** Tasks 3 and 4
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add call-site candidate plans and runtime handling for proxies, `ServiceLoader`, and
reflection without guessing.

**Acceptance Criteria:**

- [ ] Exact proxy, service provider, and reflected member evidence selects one static candidate.
- [ ] Unknown and ambiguous cases record bounded diagnostics and execution gaps.
- [ ] Raw runtime classes and Java invocation mechanics stay out of business output.
- [ ] Multi-graph and concurrent executions do not cross-select candidates.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisManifest.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeCollector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/InvocationContext.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/TraceRuntime.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingTransformer.java`
- `fachtracing-engine/src/test/**`
- `fachtracing-agent/src/test/**`

**Tests Required:**

- [ ] Proxy subclass, JDK proxy, service provider, reflection, unknown, ambiguous, bounded-memory,
  multi-graph, and concurrent isolation contracts.

---

## Task 6: Add the controlled bytecode decision fallback

**Status:** Pending
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Analyze fingerprinted binary methods in a fail-closed ASM subset only after source
artifact resolution fails.

**Acceptance Criteria:**

- [ ] Simple binary constants, parameters, fields, calculations, comparisons, branches, and returns
  produce a proven fragment.
- [ ] Class fingerprint mismatches prevent use.
- [ ] Unsupported calls, monitors, exception tables, native code, and invokedynamic remain gaps.
- [ ] Binary implementation details never enter business labels.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/BytecodeDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/ApplicationSourceBoundary.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/test/**`
- `docs/supported-java-constructs.md`

**Tests Required:**

- [ ] Accepted subset matrix, rejected opcode matrix, missing metadata, fingerprint mismatch, and
  source-preferred-over-binary contracts.

---

## Task 7: Propagate context automatically across standard async calls

**Status:** Pending
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add idempotent functional wrappers and inject them at supported application call
sites for executors, stages, platform threads, and virtual threads.

**Acceptance Criteria:**

- [ ] Application code uses no manual tracing wrapper in automatic-propagation fixtures.
- [ ] Executor, submit, stage callback, platform-thread, and virtual-thread observations join the
  correct execution.
- [ ] Inactive contexts preserve callback identity.
- [ ] Concurrent traces have zero contamination and restored state is always cleared.

**Files to Modify:**

- `fachtracing-api/src/main/java/at/gepardec/fachtracing/api/TraceContextCarrier.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeCollector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/TraceRuntime.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingTransformer.java`
- `fachtracing-engine/src/test/**`
- `fachtracing-agent/src/test/**`

**Tests Required:**

- [ ] Executor, executor service, completion stage, platform thread, virtual thread, inactive trace,
  nested trace, exception cleanup, and 1,000-concurrent-trace isolation contracts.

---

## Task 8: Attribute owned external JPMS sources

**Status:** Pending
**Estimated Effort:** L
**Dependencies:** Task 6
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add named and automatic module ownership to external resolution sources and Maven
configuration, then place owned sources in the valid compiler context.

**Acceptance Criteria:**

- [ ] Named external source joins its declared source module.
- [ ] Automatic-module source is paired with its Maven binary identity and resolves reachable logic.
- [ ] Missing, conflicting, or unreadable ownership fails before extraction.
- [ ] Ownership changes the boundary fingerprint and developer provenance only.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/ApplicationSourceBoundary.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/SourceInputResolver.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/AnalyzeMojo.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/AnalyzeReactorMojo.java`
- `fachtracing-maven-plugin/src/test/**`

**Tests Required:**

- [ ] Named source module, automatic module, Maven source artifact, invalid ownership, unreadable
  module, mixed reactor, all-flat, and all-modular contracts.

---

## Task 9: Add PostgreSQL integration

**Status:** Pending
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add a test-only PostgreSQL driver and an environment-controlled integration suite
against PostgreSQL 18.4 while retaining H2.

**Acceptance Criteria:**

- [ ] PostgreSQL tests cover schema, save, exact retry, both key conflicts, lookup, correlation,
  retention, rollback, and timeout behavior.
- [ ] The driver is test scope only and production modules remain vendor-neutral.
- [ ] Local verification skips PostgreSQL only when no explicit connection is configured.

**Files to Modify:**

- `fachtracing-storage-jdbc/pom.xml`
- `fachtracing-storage-jdbc/src/test/java/**`
- `scripts/verify.sh`
- `docs/jdbc-storage.md`

**Tests Required:**

- [ ] H2 regression and PostgreSQL 18.4 integration.

---

## Task 10: Add required pull-request CI

**Status:** Pending
**Estimated Effort:** M
**Dependencies:** Task 9
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add a read-only GitHub workflow that runs standard, external, Mega, PostgreSQL, and
long-load evidence for every pull request.

**Acceptance Criteria:**

- [ ] Workflow triggers on pull requests and manual dispatch.
- [ ] Workflow uses Java 21, Maven cache, PostgreSQL 18.4, and read-only permissions.
- [ ] Standard verification, pinned Mega, external activation, PostgreSQL, and 600-second load run.
- [ ] The workflow has no Mega or database credentials in production configuration.

**Files to Modify:**

- `.github/workflows/verify.yml`
- `scripts/verify-release.sh`
- `docs/release-evidence.md`

**Tests Required:**

- [ ] Workflow syntax check and command parity check.

---

## Task 11: Update supported-capability and integration documentation

**Status:** Pending
**Estimated Effort:** M
**Dependencies:** Tasks 2 through 10
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:** Document the new complete subsets, exact paths, controlled fallbacks, configuration,
automatic async behavior, module ownership, PostgreSQL test, and CI without overstating coverage.

**Acceptance Criteria:**

- [ ] Documentation names each complete subset and each remaining precise unsupported variant.
- [ ] Maven examples show owned named and automatic source inputs.
- [ ] Runtime examples need no manual wrappers for supported async calls.
- [ ] Business-output privacy and no-guessing rules remain explicit.

**Files to Modify:**

- `README.md`
- `docs/supported-java-constructs.md`
- `docs/runtime-integration.md`
- `docs/maven-plugin.md`
- `docs/jdbc-storage.md`
- `docs/release-evidence.md`
- `docs/java-capabilities.json`

**Tests Required:**

- [ ] Documentation-to-capability verifier.

---

## Task 12: Run full generic release evidence

**Status:** Pending
**Estimated Effort:** L
**Dependencies:** Tasks 2 through 11
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Run all focused, standard, external, PostgreSQL, pinned Mega, forbidden-reference,
clean-clone, and 600-second performance gates. Complete SpecOps memory and release evidence only
after all gates pass.

**Acceptance Criteria:**

- [ ] Every independent capability contract passes.
- [ ] `./scripts/verify.sh` and external activation integration pass.
- [ ] PostgreSQL 18.4 integration passes.
- [ ] Mega produces five reviewed complete graphs with no production hints.
- [ ] The 600,000-decision gate passes below 10% p95 overhead with zero correctness, contamination,
  silently lost, or unresolved accepted-record failures.
- [ ] Repository integrity, documentation, memory, index, initiative, and clean-worktree checks pass.

**Files to Modify:**

- `.specops/generic-java-extractor-completion/*`
- `.specops/index.json`
- `.specops/initiatives/generic-java-fachtracing*`
- `.specops/memory/*`
- `docs/release-evidence.md`

**Tests Required:**

- [ ] Focused contract suites.
- [ ] Full generic verifier and external release fixture.
- [ ] PostgreSQL integration.
- [ ] Pinned Mega conformance and forbidden-reference scan.
- [ ] Clean-clone ten-minute persistence-enabled load gate.

## Progress Tracking

- Total Tasks: 12
- Completed: 0
- In Progress: 1
- Blocked: 0
- Pending: 11
