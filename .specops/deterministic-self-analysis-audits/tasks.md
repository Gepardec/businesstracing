# Implementation Tasks: Deterministic Self-Analysis Audit Graphs

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `self-runtime-tracing` | Supplies the production self-analysis gate | Yes | Completed |
| `explainable-relevance-and-polymorphic-dispatch` | Supplies analysis decisions | Yes | Completed |
| `generic-business-graph-projection` | Supplies the projector and business graph | Yes | Completed |

## Task Breakdown

### Task 1: Record and Render Generic Audit Decisions

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add the projection audit model, record final projector classifications, preserve readable excluded
subjects, and render both audit types from their data.

**Implementation Steps:**

1. Add an immutable business projection result and stable action and reason enums.
2. Make the projector record one final decision for every exact node and terminal result.
3. Add a bounded source subject for excluded analysis constructs.
4. Add one deterministic Mermaid renderer for analysis and projection audits.
5. Add focused tests for classification, relations, exclusions, and deterministic input-driven
   rendering.

**Acceptance Criteria:**

- [x] Every exact node receives one final projection decision.
- [x] Projection actions and reasons come from the projector, not the renderer.
- [x] Analysis audit uses `AnalysisManifest.analysisDecisions()`.
- [x] Excluded source has a bounded readable subject and no exact-node relation.
- [x] The renderer contains no project-specific or method-specific diagram content.
- [x] Equivalent input produces identical Mermaid output.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphProjection.java` (new)
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphProjector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisDecisionAuditor.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/developer/DecisionAuditMermaidRenderer.java` (new)
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/business/BusinessGraphProjectionTest.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/developer/DecisionAuditMermaidRendererTest.java` (new)

**Tests Required:**

- [x] Engine executable contracts pass
- [x] Projection audit covers kept, removed, replaced, and unreachable decisions
- [x] Analysis audit covers included, excluded, and gap decisions

---

### Task 2: Export and Prove the Self-Generated Audits

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Write both audit files for every analysis, add them to the output index and cleanup lifecycle, and
make the self-tracing gate prove the production method classifications.

**Implementation Steps:**

1. Use `projectWithAudit` in `ProjectGraphGenerator`.
2. Write and index analysis and projection audit Mermaid files.
3. Remove stale audit files with the other generated artifacts.
4. Check the self-generated audit content in `verify-self-tracing.sh`.
5. Replace checked-in manual diagrams in the guide with generated-file inspection commands.
6. Run focused and full verification.

**Acceptance Criteria:**

- [x] Every analyzed decision receives two audit Mermaid files.
- [x] `index.md` links both audit files as technical developer artifacts.
- [x] Stale audit files are removed without deleting application files.
- [x] The self-trace proves structural, technical-calculation, business-rule, and terminal-result
  decisions from generated output.
- [x] Documentation does not embed a manually maintained self algorithm diagram.

**Files to Modify:**

- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/ProjectGraphGenerator.java`
- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/AnalyzeMojoTest.java`
- `scripts/verify-self-tracing.sh`
- `docs/self-tracing.md`

**Tests Required:**

- [x] Maven-plugin executable contracts pass
- [x] `FACHTRACING_SKIP_PROJECT_BUILD=true ./scripts/verify-self-tracing.sh`
- [x] `./scripts/verify.sh`

## Implementation Order

1. Task 1 creates and verifies the engine audit data and renderer.
2. Task 2 connects the generic output to Maven and proves it on the project itself.

## Progress Tracking

- Total Tasks: 2
- Completed: 2
- In Progress: 0
- Blocked: 0
- Pending: 0
