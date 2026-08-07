# Implementation Tasks: Self-Dogfood Business Tracing

## Spec-Level Dependencies

None.

## Task Breakdown

### Task 1: Refresh Existing Dependencies

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Update the two stale direct build dependencies and verify the selected dependency set.

**Implementation Steps:**

1. Upgrade Plexus Utils to the newest compatible stable release, 3.6.1.
2. Upgrade Maven JAR Plugin to 3.5.1.
3. Build and test the affected modules.
4. Record the release-age and vulnerability checks.

**Acceptance Criteria:**

- [x] All direct dependency and build-plugin versions are the newest compatible stable releases published on or before 2026-08-04.
- [x] Exact OSV checks report no advisory for the selected direct dependency versions.
- [x] The affected Maven modules build and test successfully.

**Files to Modify:**

- `fachtracing-agent/pom.xml`
- `fachtracing-maven-plugin/pom.xml`
- `.specops/self-dogfood-business-tracing/dependency-audit.md`

**Tests Required:**

- [x] Affected Maven module tests
- [x] Resolved dependency version check

---

### Task 2: Generate and Verify the Project's Own Graph

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Annotate the Maven plugin's developer-export policy and add a repository gate that analyzes the reactor with its own plugin.

**Implementation Steps:**

1. Add the public Fachtracing annotation to the selected production method.
2. Add a shell gate that installs the current plugin, runs aggregate analysis, and checks the generated artifacts and paths.
3. Call the self-tracing gate from the normal verifier after the reactor install.
4. Protect the new gate with the repository integrity check.

**Acceptance Criteria:**

- [x] One production method has the business label `enable developer graph export`.
- [x] The gate generates Mermaid, PlantUML, index, and activation artifacts under `target/fachtracing`.
- [x] The gate verifies the enabled and disabled result paths.
- [x] The normal verifier calls the gate without a second project install.

**Files to Modify:**

- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/ProjectGraphGenerator.java`
- `scripts/verify-self-tracing.sh`
- `scripts/verify.sh`
- `scripts/verify-repository-integrity.sh`

**Tests Required:**

- [x] `./scripts/verify-self-tracing.sh`
- [x] Focused Maven plugin tests

---

### Task 3: Explain the Self-Trace

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 2
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:**
Add a short dogfood guide that shows the actual generated graph and link it from the README.

**Implementation Steps:**

1. Copy the generated graph semantics into a Markdown-native Mermaid example.
2. Explain the decision, outcomes, coverage, and regeneration command.
3. Link the guide from the README and protect it with the repository integrity check.

**Acceptance Criteria:**

- [x] The guide shows the actual graph produced by Task 2.
- [x] The guide explains what the project does in this example.
- [x] The README links to the guide.

**Files to Modify:**

- `docs/self-tracing.md`
- `README.md`
- `scripts/verify-repository-integrity.sh`

**Tests Required:**

- [x] Repository integrity verification
- [x] README link verification

## Implementation Order

1. Task 1 refreshes and verifies existing dependencies.
2. Task 2 generates and verifies the source graph.
3. Task 3 documents the verified output.

## Progress Tracking

- Total Tasks: 3
- Completed: 3
- In Progress: 0
- Blocked: 0
- Pending: 0
