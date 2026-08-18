# Tasks: Generated Keycloak Diagram Correctness

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `configured-endpoint-business-tracing` | It introduced the artifact corrected here. | Yes | Completed |

## Dependency Resolution Log

| Blocker | Resolution Type | Resolution Detail | Date |
| --- | --- | --- | --- |
| — | — | — | — |

## Task Breakdown

### Task 1: Generate the Keycloak Reader Diagram

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Replace the fixed Keycloak graph with the existing generic business projection and add an
executable repository guard against manual Keycloak diagrams.

**Implementation Steps:**

1. Add the repository integrity regression and confirm that it fails on the current source.
2. Render `fullBusinessGraph` in the Keycloak harness.
3. Remove the manual graph, node, and edge construction.
4. Assert reviewed business anchors against generated projection data.

**Acceptance Criteria:**

- [x] The integrity regression fails before the fix and passes after it.
- [x] `search-users-business.mmd` is rendered from `fullBusinessGraph`.
- [x] The harness contains no manually constructed business graph.
- [x] The exact activation graph and manifest inputs do not change.

**Files to Modify:**

- `conformance/keycloak/src/test/java/at/gepardec/fachtracing/conformance/KeycloakConformanceTest.java`
- `scripts/verify-repository-integrity.sh`

**Tests Required:**

- [x] `./scripts/verify-repository-integrity.sh`
- [x] `mvn -q test`

---

### Task 2: Verify and Document the Generated Proof

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Remove the embedded manual diagram from the guide and verify the disposable generated artifact
against the pinned Keycloak source.

**Implementation Steps:**

1. Update the Keycloak guide to describe the generated graph without embedding a fixed flowchart.
2. Run the complete repository gate.
3. Run the pinned Keycloak conformance command.
4. Inspect the generated diagram for business-only content and actual generated topology.
5. If inspection finds technical projection text, add generic projection and guard regressions and
   correct the generic output.

**Acceptance Criteria:**

- [x] No fixed Keycloak flowchart remains in tracked documentation.
- [x] The full repository gate passes.
- [x] The pinned Keycloak command writes a generated business diagram and exact activation bundle.
- [x] The generated diagram contains business rules, an explicit gap, and no Java identifiers.

**Files to Modify:**

- `conformance/keycloak/README.md`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphProjector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessLogicArtifactGuard.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/business/BusinessGraphProjectionTest.java`
- `.specops/generated-keycloak-diagram-correctness/`
- `.specops/memory/`
- `.specops/steering/repo-map.md`
- `.specops/index.json`

**Tests Required:**

- [x] `./scripts/verify.sh`
- [x] `FACHTRACING_SKIP_PROJECT_BUILD=true ./scripts/verify-keycloak.sh`

## Implementation Order

1. Task 1
2. Task 2

## Progress Tracking

- Total Tasks: 2
- Completed: 2
- In Progress: 0
- Blocked: 0
- Pending: 0
