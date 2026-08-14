# Tasks: Keycloak Live Business Output Correctness

## Task 1: Preserve Predicate Outcomes Across Gaps

**Status:** Completed
**Priority:** High
**Effort:** M
**Dependencies:** None
**IssueID:** None

**Acceptance Criteria:**

- [x] Boolean frontier outcomes stay bindable when they enter a coverage gap.
- [x] Non-Boolean frontier outcomes keep the existing unresolved behavior.
- [x] Focused engine and pinned Keycloak conformance tests pass.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `conformance/keycloak/src/test/java/at/gepardec/fachtracing/conformance/KeycloakConformanceTest.java`

## Task 2: Protect Automatic Business Output

**Status:** Completed
**Priority:** High
**Effort:** M
**Dependencies:** Task 1
**IssueID:** None

**Acceptance Criteria:**

- [x] Successful unsupported results appear as `Completed` without result access.
- [x] Automatic text and Mermaid files contain only business-safe coverage wording.
- [x] Exact developer explanations stay unchanged.
- [x] Focused agent tests pass.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/explain/BusinessExplanationProjector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/explain/BusinessExplanationTextRenderer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/explain/DecisionExplanationProjectorTest.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/BusinessTraceFileSink.java`
- `fachtracing-agent/src/test/java/at/gepardec/fachtracing/agent/FachtracingTransformerTest.java`

## Task 3: Prove the Live Flow and Close the Fix

**Status:** Completed
**Priority:** High
**Effort:** M
**Dependencies:** Tasks 1 and 2
**IssueID:** None

**Acceptance Criteria:**

- [x] The real Keycloak HTTP proof shows the observed input decisions.
- [x] The generated files pass the technical-language and privacy checks.
- [x] Complete local checks pass. Hosted checks run after push.
- [x] SpecOps records and user documentation match the verified behavior.

**Files to Modify:**

- `docs/runtime-integration.md`
- `conformance/keycloak/README.md`
- `conformance/keycloak/selection.md`
- `.specops/keycloak-live-business-output-correctness/`
- `.specops/memory/`
- `.specops/steering/repo-map.md`
- `.specops/index.json`

## Progress

- Total: 3
- Completed: 3
- In Progress: 0
- Pending: 0
- Blocked: 0
