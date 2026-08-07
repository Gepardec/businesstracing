# Implementation Tasks: Spring PetClinic Conformance

## Spec-Level Dependencies

None.

## Task Breakdown

### Task 1: Add the Pinned Corpus Harness

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add the annotation overlay, executable conformance test, reviewed semantic oracles, and local verification script.

**Implementation Steps:**

1. Add the three annotation-only source changes as a patch.
2. Add a plain-Java conformance test with exact decision, completeness, graph, and business-artifact assertions.
3. Add the three reviewed normalized semantic oracles.
4. Add a script that builds and verifies the pinned clean PetClinic checkout.

**Acceptance Criteria:**

- [x] The overlay changes annotations and imports only.
- [x] The analyzer finds exactly the three selected decisions.
- [x] Two graphs are complete and the application workflow has explicit coverage gaps.
- [x] Generated semantics equal all reviewed oracles.

**Files to Modify:**

- `conformance/spring-petclinic/annotation-overlay.patch`
- `conformance/spring-petclinic/src/test/java/at/gepardec/fachtracing/conformance/SpringPetClinicConformanceTest.java`
- `conformance/spring-petclinic/src/test/resources/oracles/*.txt`
- `scripts/verify-spring-petclinic.sh`

**Tests Required:**

- [x] `./scripts/verify-spring-petclinic.sh`

---

### Task 2: Add Repository and CI Gates

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Protect the new harness and run it in pull-request and release verification.

**Implementation Steps:**

1. Add required-file, oracle-hash, and generated-artifact checks to repository integrity.
2. Add PetClinic setup and caching to the GitHub workflow.
3. Run the new suite from fast and release gates.
4. Update the shell contract tests for the new wiring.

**Acceptance Criteria:**

- [x] Repository integrity protects all immutable conformance inputs.
- [x] Pull-request and release workflows prepare the exact pinned source.
- [x] Fast and release gates run the same local script.

**Files to Modify:**

- `.github/workflows/verify.yml`
- `scripts/verify-pr.sh`
- `scripts/verify-release.sh`
- `scripts/verify-release-gates.sh`
- `scripts/verify-repository-integrity.sh`
- `scripts/test-fast-pr-workflow.sh`

**Tests Required:**

- [x] `./scripts/verify-repository-integrity.sh`
- [x] `./scripts/test-fast-pr-workflow.sh`

---

### Task 3: Explain and Link the Results

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 2
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:**
Add the selection rationale, reproduction guide, and graph-based report, then link them from the project README.

**Implementation Steps:**

1. Document the pinned source and selection rationale.
2. Add a conformance report that shows and explains the reviewed graphs.
3. Link the suite from the root README.

**Acceptance Criteria:**

- [x] The report explains complete and incomplete analysis in plain language.
- [x] One documented command reproduces the suite.
- [x] The root README links to the harness and report.

**Files to Modify:**

- `conformance/spring-petclinic/README.md`
- `conformance/spring-petclinic/selection.md`
- `conformance/spring-petclinic/conformance-report.md`
- `conformance/spring-petclinic/src/test/resources/oracles/README.md`
- `README.md`

**Tests Required:**

- [x] Repository integrity and README link checks
- [x] Full pull-request gate

## Implementation Order

1. Task 1 establishes the executable contract.
2. Task 2 makes the contract mandatory.
3. Task 3 explains the verified result.

## Progress Tracking

- Total Tasks: 3
- Completed: 3
- In Progress: 0
- Blocked: 0
- Pending: 0
