# Implementation Tasks: CI Business Projection Regression

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| compact-graph-reading-and-business-phrasing | Defines aggregate role output | Yes | completed |
| business-graph-semantic-explanation | Defines semantic reduction | Yes | completed |

## Task Breakdown

### Task 1: Remove call syntax from aggregate qualifiers

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Keep qualifier evidence and move all punctuation to the renderer.

**Implementation Steps:**

1. Add failing renderer assertions for neutral qualifier punctuation.
2. Change qualifier extraction and renderer input.
3. Run the focused analyzer contract.
4. Review changed Mega source paths and refresh only the normalized inventories that gained exact
   aggregate or return evidence.

**Acceptance Criteria:**

- [x] Qualified aggregate labels contain all source roles and no parentheses.
- [x] Unqualified aggregate labels do not gain a trailing separator.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AggregateBusinessLabelRenderer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`

**Tests Required:**

- [x] Static analyzer executable contract.
- [x] Mega conformance after integration.

### Task 2: Restore explicit failure results

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Remove the owner-suffix terminal filter and add a focused regression test.

**Implementation Steps:**

1. Make the existing failure source use a controller owner.
2. Confirm the current projector contract fails.
3. Remove the terminal filter and rerun the contract.
4. Give controller-owned rules and actions direct evidence precedence over owner suffixes.
5. Keep clear normalized labels and remove duplicate logical-complement wrappers.

**Acceptance Criteria:**

- [x] An infrastructure-owned exact failure edge creates a business result.
- [x] Success and correction results remain present.
- [x] Controller-owned rules and actions keep clear business labels without duplicate wrappers.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphProjector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessSemanticReducer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/business/BusinessGraphProjectionTest.java`
- `conformance/spring-petclinic/src/test/java/at/gepardec/fachtracing/conformance/SpringPetClinicConformanceTest.java`
- `conformance/spring-petclinic/src/test/resources/oracles/*`

**Tests Required:**

- [x] Business projection executable contract.
- [x] Spring PetClinic conformance.

### Task 3: Align release proof and verify CI

**Status:** In Progress
**Estimated Effort:** M
**Dependencies:** Task 2
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Verify technical-choice reduction at the correct artifacts and run all release
gates.

**Implementation Steps:**

1. Assert the choice in exact structure and its reduction in projection audit.
2. Run self-tracing and viewer dogfood.
3. Run the complete local pull-request gate.
4. Commit, push, and wait for every required GitHub check.
5. If a combined job exceeds the hard budget, move independent work to a parallel job and rerun.

**Acceptance Criteria:**

- [x] Self-tracing proves discovery and intentional technical-choice removal.
- [x] Mega, PetClinic, viewer dogfood, and pull-request gates pass.
- [ ] All required GitHub checks pass on the pushed commit.

**Files to Modify:**

- `scripts/verify-self-tracing.sh`
- `.github/workflows/verify.yml`
- `scripts/test-release-workflow-budget.sh`
- `scripts/test-fast-pr-workflow.sh`
- `.specops/ci-business-projection-regression/*`
- `.specops/index.json`
- `.specops/memory/context.md`
- `.specops/memory/decisions.json`
- `.specops/memory/patterns.json`

**Tests Required:**

- [x] Self-tracing verification.
- [x] Viewer dogfood verification.
- [x] Full pull-request verifier.
- [ ] GitHub pull-request checks.

## Implementation Order

1. Task 1.
2. Task 2.
3. Task 3.

## Progress Tracking

- Total Tasks: 3
- Completed: 2
- In Progress: 1
- Blocked: 0
- Pending: 0
