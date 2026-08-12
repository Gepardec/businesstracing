# Implementation Tasks: Configured Endpoint Business Tracing

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| — | — | — | — |

## Dependency Resolution Log

| Blocker | Resolution Type | Resolution Detail | Date |
| --- | --- | --- | --- |
| — | — | — | — |

## Task Breakdown

### Task 1: Add exact configured graph roots

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add the framework-neutral entry-point contract and resolve it in source attribution without changing annotation behavior.

**Implementation Steps:**

1. Add the validated entry-point value.
2. Extend `AnalysisRequest` with compatible constructors and a selection helper.
3. Resolve exact owner, method, and optional parameter types in root sources.
4. Add focused analyzer tests for success and failure paths.

**Acceptance Criteria:**

- [x] Unannotated configured methods produce labeled graphs.
- [x] Missing and ambiguous roots fail before graph generation.
- [x] Annotation roots and configured roots remain compatible and deduplicated.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/BusinessEntryPoint.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisRequest.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`

**Tests Required:**

- [x] Focused analyzer executable contract.

---

### Task 2: Expose configured roots through Maven

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add one Maven configuration type and pass selections through both graph-generation goals.

**Implementation Steps:**

1. Add the Maven configuration bean.
2. Pass selections through `ProjectGraphGenerator`, `AnalyzeMojo`, and `AnalyzeReactorMojo`.
3. Add executable configuration and generation tests.
4. Document the XML contract.

**Acceptance Criteria:**

- [x] Both Maven goals accept the same endpoint configuration.
- [x] Existing Maven calls and generated artifacts remain compatible.
- [x] Documentation includes exact configuration and overload guidance.

**Files to Modify:**

- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/BusinessEntryPointConfiguration.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/ProjectGraphGenerator.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/AnalyzeMojo.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/AnalyzeReactorMojo.java`
- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/AnalyzeMojoTest.java`
- `docs/maven-plugin.md`

**Tests Required:**

- [x] Maven plugin executable contract.

---

### Task 3: Write business artifacts for called endpoints

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add safe completion for arbitrary endpoint results, business path rendering, and opt-in automatic agent file output.

**Implementation Steps:**

1. Preserve completed traces for null and unsupported results.
2. Add the business execution Mermaid renderer.
3. Add strict agent option parsing and the daemon file sink.
4. Configure redacted runtime capture when agent arguments are present.
5. Add focused runtime and agent tests.

**Acceptance Criteria:**

- [x] Null and unsupported endpoint results create incomplete or complete executions as specified.
- [x] Text and Mermaid files contain business statements and no Java provenance.
- [x] Agent file output is redacted and does not perform I/O on endpoint threads.
- [x] Programmatic agent configuration remains compatible.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeCollector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/explain/BusinessExecutionMermaidRenderer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/runtime/RuntimeCollectorTest.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/explain/DecisionExplanationProjectorTest.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/AgentOptions.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/BusinessTraceFileSink.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingAgent.java`
- `fachtracing-agent/src/test/java/at/gepardec/fachtracing/agent/FachtracingTransformerTest.java`
- `docs/runtime-integration.md`

**Tests Required:**

- [x] Runtime collector executable contract.
- [x] Explanation and Mermaid executable contract.
- [x] Agent executable contract.

---

### Task 4: Add Mega and Keycloak conformance examples

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Tasks 1, 2, 3
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:** Move Mega to annotation-free selections and add a pinned Keycloak endpoint workflow.

**Implementation Steps:**

1. Remove the Mega annotation overlay and select its five methods in the harness.
2. Keep the reviewed Mega graphs and runtime path unchanged.
3. Add the pinned Keycloak endpoint selection, command, and usage guide.
4. Update repository integrity, top-level guidance, and CI only where the time budget permits.

**Acceptance Criteria:**

- [x] Mega produces all reviewed graphs without source edits.
- [x] The Keycloak guide selects and explains the user-search endpoint.
- [x] External names remain absent from production code.
- [x] All required repository checks pass.

**Files to Modify:**

- `conformance/mega-backend/annotation-overlay.patch`
- `conformance/mega-backend/src/test/java/at/gepardec/fachtracing/conformance/MegaBackendConformanceTest.java`
- `conformance/mega-backend/README.md`
- `conformance/mega-backend/selection.md`
- `conformance/mega-backend/conformance-report.md`
- `scripts/verify-mega-backend.sh`
- `conformance/keycloak/README.md`
- `conformance/keycloak/selection.md`
- `scripts/verify-keycloak.sh`
- `scripts/verify-repository-integrity.sh`
- `README.md`

**Tests Required:**

- [x] Mega external conformance gate.
- [x] Keycloak external conformance gate.
- [x] Full pull-request verification.

## Implementation Order

1. Task 1 establishes the root-selection contract.
2. Task 2 exposes the contract to builds.
3. Task 3 produces per-call business output.
4. Task 4 proves the complete external workflow.

## Progress Tracking

- Total Tasks: 4
- Completed: 4
- In Progress: 0
- Blocked: 0
- Pending: 0
