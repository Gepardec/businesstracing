# Implementation Tasks: Maven Developer Graph Export

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| developer-graph-json-export | Supplies the JSON exporter and strict revision contract | Yes | completed |

## Task Breakdown

### Task 1: Generate developer JSON from Maven

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add the parameter pair, optional generator configuration, UTF-8 JSON files, index links, strict failure propagation, and stale artifact cleanup.

**Implementation Steps:**

1. Add repository and source-template Maven parameters.
2. Validate that both parameters are present or absent.
3. Capture Git once before output writes when JSON is enabled.
4. Write and link one deterministic `*-developer.json` file per decision.
5. Remove stale generated JSON without deleting unrelated files.

**Acceptance Criteria:**

- [x] Configured builds write and link commit-pinned JSON.
- [x] Diagram-only builds do not require Git.
- [x] Partial settings and unsafe Git state fail explicitly.
- [x] JSON uses UTF-8 and deterministic collision-safe names.
- [x] Stale JSON cleanup preserves unrelated files.

**Files to Modify:**

- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/AnalyzeMojo.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/ProjectGraphGenerator.java`
- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/AnalyzeMojoTest.java`

**Tests Required:**

- [x] Maven generator contract covers configured JSON, compatibility, validation, dirty state, and cleanup.

---

### Task 2: Strengthen the JSON consumer contract

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Parse the full Maven artifact independently and verify that an incomplete graph exports non-empty coverage gaps.

**Implementation Steps:**

1. Add a test-only recursive JSON parser.
2. Parse the generated Maven artifact and assert its public fields.
3. Use an incomplete generated Maven artifact to verify parsed coverage gaps.

**Acceptance Criteria:**

- [x] The complete generated document passes an independent parser.
- [x] Parsed fields include schema, commit, topology, and source URL.
- [x] Parsed incomplete output contains a coverage-gap entry.

**Files to Modify:**

- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/AnalyzeMojoTest.java`

**Tests Required:**

- [x] Focused Maven plugin executable contract passes.

---

### Task 3: Complete Maven integration documentation

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 1
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:**
Create the missing Maven guide and document diagram-only and developer-JSON usage.

**Implementation Steps:**

1. Add the linked Maven plugin guide.
2. Document one-off, lifecycle, strict coverage, skip, and developer JSON settings.
3. State the clean Git and source-data boundaries.

**Acceptance Criteria:**

- [x] The README link resolves to a tracked guide.
- [x] The guide contains copyable developer JSON configuration.
- [x] The guide states that JSON is opt-in and requires a clean repository.

**Files to Modify:**

- `docs/maven-plugin.md` (new)
- `README.md`

**Tests Required:**

- [x] Documentation references match the implemented parameter and artifact names.

---

### Task 4: Prove source exists in the captured commit

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Close the review finding where an ignored generated source can pass clean-worktree validation even though the captured commit does not contain it.

**Implementation Steps:**

1. Read each source blob from the captured commit through argument-safe Git execution.
2. Compare the blob SHA-256 with the analysis fingerprint.
3. Reject a missing or mismatched committed blob.
4. Add an ignored-source regression contract.

**Acceptance Criteria:**

- [x] Export proves that every analyzed source exists in the captured commit.
- [x] Export proves that committed blob content matches the analysis fingerprint.
- [x] Ignored generated source fails instead of producing a false link.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/developer/DeveloperGraphExporter.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`

**Tests Required:**

- [x] Focused engine exporter contract passes.
- [x] Full repository verification passes.

## Implementation Order

1. Task 1 adds the output path.
2. Task 2 verifies the complete consumer contract.
3. Task 3 documents the verified behavior.
4. Task 4 closes the committed-source review finding.

## Progress Tracking

- Total Tasks: 4
- Completed: 4
- In Progress: 0
- Blocked: 0
- Pending: 0
