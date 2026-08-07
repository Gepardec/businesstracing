# Implementation Tasks: Developer Graph JSON Schema

## Spec-Level Dependencies

None.

## Task Breakdown

### Task 1: Generate V1 and V2 JSON Schemas

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add the public dependency-free generator and executable V1/V2 contract assertions.

**Implementation Steps:**

1. Add failing test assertions for the complete schema structure and enums.
2. Add `DeveloperGraphJsonSchema` with deterministic V1 and V2 generation.
3. Verify unsupported schema identifiers fail.
4. Run the focused Maven plugin contract.

**Acceptance Criteria:**

- [x] V1 and V2 generation returns valid parsed JSON Schema Draft 2020-12 documents.
- [x] Each schema has the correct data-schema constant and required field sets.
- [x] Node, completeness, and origin enums match the production Java enums.
- [x] Unsupported schema identifiers fail explicitly.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/developer/DeveloperGraphJsonSchema.java` (new)
- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/AnalyzeMojoTest.java`

**Tests Required:**

- [x] Focused `AnalyzeMojoTest`

---

### Task 2: Publish the Matching Maven Schema Artifact

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Write and link the matching schema during developer graph generation, remove stale schema output, and document the frontend handoff.

**Implementation Steps:**

1. Select the V1 or V2 schema from the Maven provenance mode.
2. Write the shared schema file and add its index link.
3. Extend narrow stale-artifact cleanup and the Maven contract assertions.
4. Document the public API, filenames, and frontend use.
5. Run focused and full verification.

**Acceptance Criteria:**

- [x] Maven writes the schema file that matches all generated developer JSON documents.
- [x] `index.md` links to the schema once.
- [x] A run without developer output removes the stale schema and keeps unrelated files.
- [x] Documentation tells a developer which files to give to the frontend and how to generate the schema in Java.

**Files to Modify:**

- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/ProjectGraphGenerator.java`
- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/AnalyzeMojoTest.java`
- `README.md`
- `docs/maven-plugin.md`

**Tests Required:**

- [x] Focused `AnalyzeMojoTest`
- [x] `./scripts/verify.sh`

## Implementation Order

1. Task 1 defines and verifies the public schema generator.
2. Task 2 adds the Maven handoff artifact and documentation.

## Progress Tracking

- Total Tasks: 2
- Completed: 2
- In Progress: 0
- Blocked: 0
- Pending: 0
