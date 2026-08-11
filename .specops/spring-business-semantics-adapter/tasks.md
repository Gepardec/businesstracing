# Implementation Tasks: Spring Business Semantics Adapter

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `external-method-semantic-contracts` | Supplies the provider interface. | Yes | completed |
| `generic-business-graph-projection` | Consumes the business semantics supplied by the adapter. | Yes | completed |

## Task Breakdown

### Task 1: Add the optional adapter module

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Required specifications
**Priority:** High
**IssueID:** None

**Description:**
Add the module, service registration, and one Spring contract provider.

**Implementation Steps:**

1. Add the module to the reactor.
2. Add the provider and exact signature catalog.
3. Register the provider with Java service metadata.

**Acceptance Criteria:**

- [x] Production code imports no Spring types.
- [x] All catalog entries use exact method keys.
- [x] Provider discovery is deterministic.

**Files to Modify:**

- `pom.xml`
- `fachtracing-spring/**` (new)

**Tests Required:**

- [x] Module and provider-loading tests.

---

### Task 2: Verify general Spring behavior

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None

**Description:**
Test validation, page cardinality, persistence, caught integrity failures, and flash mutations with
general fixtures.

**Implementation Steps:**

1. Add real Spring test dependencies.
2. Add framework-level controller fixtures with no PetClinic code.
3. Assert complete projected business behavior and unmatched-call gaps.

**Acceptance Criteria:**

- [x] Supported Spring calls produce their declared semantics.
- [x] Unsupported calls stay incomplete.
- [x] The adapter has no application vocabulary.

**Files to Modify:**

- `fachtracing-spring/src/test/**`
- `docs/maven-plugin.md`

**Tests Required:**

- [x] Utility, validation, page, repository, exception, redirect, and isolation tests.
- [x] Pull-request verification.
