# Implementation Tasks: Spring PetClinic Business Conformance

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `external-method-semantic-contracts` | Supplies exact compiled-method facts. | Yes | completed |
| `generic-business-graph-projection` | Supplies business graph files. | Yes | completed |
| `spring-business-semantics-adapter` | Supplies general Spring semantics. | Yes | completed |

## Task Breakdown

### Task 1: Select and annotate three business workflows

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Required specifications
**Priority:** High
**IssueID:** None

**Description:**
Replace technical examples with owner search, visit booking, and pet registration.

**Implementation Steps:**

1. Update the annotation-only overlay.
2. Update selection and harness expectations.
3. Keep the pinned source revision and isolation boundary.

**Acceptance Criteria:**

- [x] The overlay contains only annotation imports and annotations.
- [x] Exactly three intended controller methods are graph roots.

**Files to Modify:**

- `conformance/spring-petclinic/annotation-overlay.patch`
- `conformance/spring-petclinic/selection.md`
- `conformance/spring-petclinic/src/test/**`

**Tests Required:**

- [x] Overlay and decision-selection assertions.

---

### Task 2: Add reviewed business artifacts

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None

**Description:**
Require complete graphs and compare generated business JSON with reviewed oracles.

**Implementation Steps:**

1. Generate business Mermaid, PlantUML, and JSON.
2. Review and commit the three JSON oracles.
3. Update the report and oracle documentation.

**Acceptance Criteria:**

- [x] All three graphs are complete.
- [x] Every expected result path is present.
- [x] Business artifacts contain no prohibited vocabulary.

**Files to Modify:**

- `conformance/spring-petclinic/src/test/resources/oracles/**`
- `conformance/spring-petclinic/conformance-report.md`
- `conformance/spring-petclinic/README.md`

**Tests Required:**

- [x] Exact JSON oracle comparison.
- [x] JSON schema validation.

---

### Task 3: Verify isolation and complete CI

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 2
**Priority:** High
**IssueID:** None

**Description:**
Run repository, PetClinic, Maven, and PostgreSQL verification and enforce generic production code.

**Implementation Steps:**

1. Extend production isolation to the Spring adapter.
2. Run the PetClinic and pull-request gates.
3. Run hosted PostgreSQL verification when CI credentials are available.

**Acceptance Criteria:**

- [x] Production modules contain no PetClinic knowledge.
- [x] All local verification gates pass.
- [x] CI, including configured PostgreSQL verification, passes.

**Files to Modify:**

- `conformance/spring-petclinic/src/test/java/at/gepardec/fachtracing/conformance/SpringPetClinicIsolationTest.java`
- `scripts/verify-spring-petclinic.sh`
- `.github/workflows/**`

**Tests Required:**

- [x] PetClinic gate.
- [x] Pull-request gate.
- [x] Hosted PostgreSQL CI gate.
