# Implementation Tasks: Generic Call-Specific Business Flow

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `generic-business-graph-projection` | Business-only graph contract. | Yes | Completed |
| `runtime-decision-path-capture` | Observed exact path contract. | Yes | Completed |
| `configured-endpoint-business-tracing` | Automatic endpoint file output. | Yes | Completed |

## Dependency Resolution Log

| Blocker | Resolution Type | Resolution Detail | Date |
| --- | --- | --- | --- |
| — | — | — | — |

## Task Breakdown

### Task 1: Preserve generic projection traceability

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add an immutable traceability result and make the business projector expose every exact path represented by a generated business edge.

**Implementation Steps:**

1. Write failing synthetic contracts for exact-node, terminal-edge, and collapsed-edge mappings.
2. Add `BusinessGraphProjection` with defensive validation.
3. Refactor `BusinessGraphProjector` so the compatible graph method delegates to one traceable projection.
4. Keep all current projection snapshots and guards valid.

**Acceptance Criteria:**

- [x] Each projected business node records its exact source node where one exists.
- [x] Each business result records its exact terminal edge.
- [x] Each business edge records all alternative exact edge sequences that it represents.
- [x] Existing business graph output remains deterministic and compatible.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphProjection.java` (new)
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphProjector.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/business/BusinessGraphProjectionTest.java`

**Tests Required:**

- [x] Focused business projection executable contract.

---

### Task 2: Summarize graphs and select one execution

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add graph-semantic summary and an execution projector that emits only the business flow proved by one call.

**Implementation Steps:**

1. Write failing synthetic contracts for gap regions, equivalent states, two branches, semantic mutation, and version mismatch.
2. Add `BusinessGraphSummarizer` with deterministic gap and equivalence rewrites.
3. Add `BusinessExecutionGraphProjector` using exact visited-edge evidence and traceability.
4. Apply summary to the build-time graph and the selected runtime subgraph.

**Acceptance Criteria:**

- [x] One connected gap region becomes one visible gap with preserved external paths.
- [x] Only behaviorally equivalent nodes merge.
- [x] Two branch executions create different selected graphs and named results.
- [x] A semantic graph mutation changes output.
- [x] Identity mismatch fails closed.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphSummarizer.java` (new)
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessExecutionGraphProjector.java` (new)
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphProjector.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/business/BusinessGraphProjectionTest.java`

**Tests Required:**

- [x] Focused summary and execution-selection executable contracts.

---

### Task 3: Use one selected model for automatic files

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 2
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Render automatic text and Mermaid from the same call-specific business graph on the daemon sink thread.

**Implementation Steps:**

1. Write a failing agent contract for two calls with different branches.
2. Add the business execution text renderer.
3. Replace the automatic exact-graph explanation path with the selected business graph.
4. Verify private and technical values remain absent and unsupported results remain untouched.

**Acceptance Criteria:**

- [x] Text and Mermaid contain the same selected business rules and result.
- [x] Each call omits its unselected branch.
- [x] Projection and file work remain on the sink thread.
- [x] Existing programmatic explanation APIs remain compatible.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessExecutionTextRenderer.java` (new)
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/BusinessTraceFileSink.java`
- `fachtracing-agent/src/test/java/at/gepardec/fachtracing/agent/FachtracingTransformerTest.java`
- `docs/runtime-integration.md`

**Tests Required:**

- [x] Focused agent automatic-output executable contract.
- [x] Existing explanation executable contract.

---

### Task 4: Prove generic and brownfield conformance

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Tasks 1, 2, 3
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add integrity checks and black-box conformance evidence without giving production code any external-project facts.

**Implementation Steps:**

1. Extend repository integrity checks for forbidden external-project production references and fixed topology.
2. Make Keycloak conformance assert overview size, generated traceability, and a selected-path fixture derived from its analyzed graph.
3. Keep Mega runtime and static conformance valid.
4. Document the generated overview, evaluated flow, and manual non-Java review rubric.
5. Run focused, full, Mega, Keycloak, and hosted CI checks.

**Acceptance Criteria:**

- [x] Synthetic unknown-project proof runs before external checks.
- [x] Production contains no Keycloak or Mega rule.
- [x] Keycloak and Mega use only generated production behavior.
- [x] The manual review rubric tests the stated definition of done.
- [x] Pull-request CI passes.

**Files to Modify:**

- `scripts/verify-repository-integrity.sh`
- `conformance/keycloak/src/test/java/at/gepardec/fachtracing/conformance/KeycloakConformanceTest.java`
- `conformance/keycloak/README.md`
- `conformance/keycloak/selection.md`
- `conformance/mega-backend/src/test/java/at/gepardec/fachtracing/conformance/MegaBackendConformanceTest.java`

**Tests Required:**

- [x] Repository integrity gate.
- [x] Keycloak external conformance gate.
- [x] Mega external conformance gate.
- [x] Full pull-request verification and hosted CI.

---

### Task 5: Replace duplicate call gaps with generic boundary rules

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 4
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add target-neutral rules for source-visible call boundaries, nested binary owners, lazy callback actions, and explicit caught outcomes. Keep one gap when no rule proves the missing behavior.

**Implementation Steps:**

1. Add failing synthetic contracts for each new rule and for the direct-decision counterexample.
2. Add one source-unavailable call boundary classifier and use JVM binary names for bytecode lookup.
3. Apply the classifier to calls, callbacks, and caught paths without changing exact runtime evidence contracts.
4. Regenerate the Keycloak graph and require no more than three justified visible gap regions.
5. Run two live Keycloak calls and verify connected, non-contradictory selected diagrams.
6. Run repository integrity, focused tests, all external conformance gates, the full pull-request gate, and hosted CI.

**Acceptance Criteria:**

- [x] Synthetic unknown-project fixtures prove all boundary rules before Keycloak runs.
- [x] A direct source-unavailable decision still creates a coverage gap.
- [x] Nested binary types resolve from the configured classpath.
- [x] Lazy callbacks state what is configured without claiming runtime evaluation.
- [x] The Keycloak overview has at most three visible, justified gap regions.
- [x] Two live Keycloak diagrams are connected and contain no contradictory rule outcomes.
- [x] Production code contains no Keycloak, Mega, reviewed label, method, or topology rule.
- [x] Pull-request CI passes.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/SourceUnavailableCallClassifier.java` (new)
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/ObservedBusinessSegmentConnector.java` (new)
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessExecutionGraphProjector.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/business/BusinessGraphProjectionTest.java`
- `fachtracing-engine/src/test/resources/fixtures/analysis/SourceBoundaryPolicy.java` (new)
- `fachtracing-engine/src/test/resources/fixtures/analysis/SourceBoundaryBinaryRules.java` (new)
- `fachtracing-spring/src/test/java/at/gepardec/fachtracing/spring/SpringMethodContractProviderTest.java`
- `conformance/keycloak/src/test/java/at/gepardec/fachtracing/conformance/KeycloakConformanceTest.java`
- `conformance/keycloak/selection.md`
- `docs/supported-java-constructs.md`
- `scripts/verify-repository-integrity.sh`

**Tests Required:**

- [x] Focused static analyzer contract.
- [x] Repository integrity gate.
- [x] Keycloak static and live conformance proof.
- [x] Mega and PetClinic external gates.
- [x] Full pull-request verification and hosted CI.

## Implementation Order

1. Task 1 creates the traceability contract.
2. Task 2 uses that contract for summary and selection.
3. Task 3 moves automatic output to the selected model.
4. Task 4 supplies black-box proof and final review evidence.
5. Task 5 removes duplicate call gaps and completes the live definition-of-done proof.

## Progress Tracking

- Total Tasks: 5
- Completed: 5
- In Progress: 0
- Blocked: 0
- Pending: 0
