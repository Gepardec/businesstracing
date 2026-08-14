# Implementation Tasks: Explainable Generated Mermaid Audits

## Task 1: Record Final Projection Decisions

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Record final exact-to-business decisions while preserving runtime traceability and
map those decisions through business summary.

**Acceptance Criteria:**

- [x] Every exact node has one final keep or remove decision.
- [x] Every terminal result has one replacement decision.
- [x] Runtime node, terminal, and edge-path mappings keep their current contract.
- [x] Merged summary nodes have an original-to-final mapping.
- [x] Final decisions point only to nodes in the final business graph.
- [x] Focused projection and summary tests pass.

**Files:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphProjection.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphAudit.java` (new)
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphProjector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphSummarizer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/business/BusinessGraphProjectionTest.java`

## Task 2: Render and Export Compact Audit Mermaid

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Render compact analysis and projection audits and add them to the Maven output
lifecycle.

**Acceptance Criteria:**

- [x] Analysis audit groups current included, excluded, and gap decisions.
- [x] Projection audit groups current kept, removed, and replaced decisions.
- [x] Counts and bounded examples come from current input.
- [x] Equivalent input gives identical Mermaid.
- [x] Renderer production code contains no application-specific diagram content.
- [x] Maven output writes, indexes, and removes both audit file types.
- [x] Focused engine and Maven tests pass.

**Files:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisDecisionAuditor.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/developer/DecisionAuditMermaidRenderer.java` (new)
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/developer/DecisionAuditMermaidRendererTest.java` (new)
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/ProjectGraphGenerator.java`
- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/AnalyzeMojoTest.java`
- `docs/maven-plugin.md`

## Task 3: Prove the Audits on Keycloak

**Status:** In Progress
**Estimated Effort:** M
**Dependencies:** Task 2
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Generate both audits from the pinned Keycloak source and run all release gates.

**Acceptance Criteria:**

- [ ] Keycloak conformance writes both audit Mermaid files.
- [ ] Generated audits contain the required actual decision categories and source identity.
- [ ] Two Keycloak runs produce equal audit hashes.
- [ ] Production guards reject application-specific renderer or projector rules.
- [ ] Repository integrity, full verification, and Keycloak conformance pass.
- [ ] Hosted CI passes.

**Files:**

- `conformance/keycloak/src/test/java/at/gepardec/fachtracing/conformance/KeycloakConformanceTest.java`
- `conformance/keycloak/README.md`
- `scripts/verify-repository-integrity.sh`

## Order

1. Task 1 creates reliable decision data.
2. Task 2 formats and exports that data.
3. Task 3 proves the feature on real source.

## Progress

- Total Tasks: 3
- Completed: 2
- In Progress: 1
- Blocked: 0
- Pending: 0
