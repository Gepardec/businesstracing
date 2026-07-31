# Implementation Tasks: Developer Graph JSON Export

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| generic-tracing-walking-skeleton | Supplies graph/source manifest correlation | Yes | completed |

## Task Breakdown

### Task 1: Implement deterministic developer graph export

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add the developer-only JSON projection, strict Git revision capture, relative source normalization, revision-pinned link generation, and executable contract coverage.

**Implementation Steps:**

1. Create `DeveloperGraphExporter` with nested immutable source-revision metadata.
2. Capture clean Git revision metadata through argument-safe `ProcessBuilder` commands.
3. Render deterministic schema-v1 JSON from graph topology, coverage, source mappings, and fingerprints.
4. Export the developer package from the engine module.
5. Extend the analyzer executable contract with export, Git cleanliness, escaping, containment, and synthetic-node cases.

**Acceptance Criteria:**

- [x] JSON contains the versioned graph contract and stable opaque IDs.
- [x] Source-backed nodes contain relative coordinates, fingerprints, and commit-pinned URLs.
- [x] Export verifies that source files still match the analysis fingerprints.
- [x] Synthetic nodes omit source data.
- [x] Dirty Git state and out-of-root mappings fail before export.
- [x] JSON output contains no absolute workspace path and is deterministic.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/developer/DeveloperGraphExporter.java` (new)
- `fachtracing-engine/src/main/java/module-info.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`

**Tests Required:**

- [x] Existing executable contracts pass.
- [x] New exporter and Git provenance assertions pass.
- [x] Stale analysis fingerprint assertions pass.

---

### Task 2: Document external visualization and code navigation

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 1
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:**
Document existing presentation exports, the new JSON interchange contract, and a commit-pinned source-link example.

**Implementation Steps:**

1. Add the JSON export to the integration flow.
2. Show how an external graph tool reads nodes/edges and opens `source.url`.
3. State the clean-repository and developer-only provenance constraints.

**Acceptance Criteria:**

- [x] README distinguishes Mermaid/PlantUML presentation exports from JSON interchange.
- [x] README includes a working Java usage example and source-link behavior.
- [x] Documentation preserves the business/developer data boundary.

**Files to Modify:**

- `README.md`

**Tests Required:**

- [x] Documented API names compile as part of the implementation contracts.

## Implementation Order

1. Task 1 establishes the export contract and tests.
2. Task 2 documents the verified API.

## Progress Tracking

- Total Tasks: 2
- Completed: 2
- In Progress: 0
- Blocked: 0
- Pending: 0
