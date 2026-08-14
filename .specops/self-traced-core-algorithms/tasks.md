# Implementation Tasks: Self-Traced Core Algorithms

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `deterministic-self-analysis-audits` | Supplies generated audit output and the self-proof gate | Yes | Completed |
| `generic-business-graph-projection` | Supplies the authoritative exact-node classifier | Yes | Completed |
| `reactor-wide-implementation-resolution` | Supplies project-aware source roles and closure behavior | Yes | Completed |

## Task Breakdown

### Task 1: Trace the Production Exact-Node Classifier

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Mark the current `projectNode` method as a traced decision and prove that it stays the method used by
the business graph projector.

**Implementation Steps:**

1. Extract and annotate the final reason classifier from `projectNode`.
2. Keep all current keep, remove, loop, technical, business, and gap projection rules unchanged.
3. Extend the projection executable contract for the annotation and current classifications.

**Acceptance Criteria:**

- [x] The traced method is the method called by `projectWithAudit`.
- [x] Existing final projection reasons stay unchanged.
- [x] The focused projection contract passes.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphProjector.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/business/BusinessGraphProjectionTest.java`

**Tests Required:**

- [x] Business projection executable contract passes

---

### Task 2: Extract and Trace Production Source Selection

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Move project-aware request selection into one traced component and make the static analyzer consume
its result.

**Implementation Steps:**

1. Add `AnalysisSourceSelector` with one immutable selection result.
2. Move empty-entry, connected-project, modular-source, external-source, classpath, entry-source,
   and character-set selection into it.
3. Replace local source-selection logic in `StaticDecisionAnalyzer` with the selector result.
4. Add focused flat, modular, and empty-entry selection tests.

**Acceptance Criteria:**

- [x] One component owns the complete source-selection algorithm.
- [x] `StaticDecisionAnalyzer` has no duplicate source-selection block.
- [x] Entry, resolution, external, classpath, flat, and modular behavior is tested.
- [x] Existing analyzer behavior stays compatible.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisSourceSelector.java` (new)
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`

**Tests Required:**

- [x] Static analyzer executable contract passes
- [x] Engine compilation passes

---

### Task 3: Generate and Prove Both Algorithm Graph Sets

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 2
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Extend the existing self-analysis gate and guide for the node projection and source-selection
algorithms.

**Implementation Steps:**

1. Require all generated files for both new decision labels.
2. Check current source-derived branches and decisions in the new files.
3. Compare new audit file checksums after repeated analysis.
4. Document the production method, call site, source roles, and generated files without a manual
   diagram body.
5. Run focused, self-tracing, and full repository checks.

**Acceptance Criteria:**

- [x] The self gate fails if either traced algorithm is absent.
- [x] The self gate checks current content and deterministic output for both decisions.
- [x] Documentation points to generated graphs and authoritative code.
- [x] No hardcoded algorithm diagram enters production or documentation.
- [x] Full repository verification passes.

**Files to Modify:**

- `scripts/verify-self-tracing.sh`
- `docs/self-tracing.md`

**Tests Required:**

- [x] `FACHTRACING_SKIP_PROJECT_BUILD=true ./scripts/verify-self-tracing.sh`
- [x] `./scripts/verify.sh`

## Implementation Order

1. Trace the existing exact-node classifier.
2. Extract and trace source selection.
3. Generate, inspect, document, and verify both graph sets.

## Progress Tracking

- Total Tasks: 3
- Completed: 3
- In Progress: 0
- Blocked: 0
- Pending: 0
