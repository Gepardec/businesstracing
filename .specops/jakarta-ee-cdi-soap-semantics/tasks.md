# Implementation Tasks: Jakarta EE CDI and service semantics

## Task Breakdown

### Task 1: Add the generic dispatch-candidate selector SPI

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**

Add the focused engine extension point and apply it to dynamic source dispatch without changing
behavior when no selector is enabled.

**Implementation Steps:**

1. Define immutable request and selection types in the analysis package.
2. Add selectors to `AnalysisRequest`.
3. Apply selection before the analyzer emits dynamic dispatch candidates.
4. Add engine tests for abstention, exclusion, and no-selector compatibility.

**Acceptance Criteria:**

- [x] Generic dynamic dispatch remains unchanged without a selector.
- [x] A selector can exclude one candidate without changing other candidates.
- [x] Conflicting selector decisions create a visible gap.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisRequest.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/` (new selector SPI)
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`

**Tests Required:**

- [x] Focused engine selector tests

---

### Task 2: Add the optional Jakarta EE adapter

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**

Create the optional Jakarta EE module, CDI selector, service registration, and exact framework
contract catalog.

**Implementation Steps:**

1. Add `fachtracing-jakartaee` to the reactor.
2. Implement CDI field and constructor injection selection with scopes, qualifiers, and alternatives.
3. Implement exact Jakarta EE, JAX-WS/SOAP, and gRPC boundary contracts.
4. Add reflection and analysis tests against real test APIs.

**Acceptance Criteria:**

- [x] Production adapter code imports no Jakarta EE or gRPC types.
- [x] A matching CDI bean is selected and a non-bean candidate is excluded.
- [x] Unsupported CDI and remote-call cases remain fail-closed.
- [x] Every catalog entry matches a real API signature.

**Files to Modify:**

- `pom.xml`
- `fachtracing-jakartaee/**` (new)
- `docs/maven-plugin.md`
- `docs/java-capabilities.json`
- `docs/supported-java-constructs.md`

**Tests Required:**

- [x] Adapter unit and signature tests
- [x] Maven plugin provider discovery test

---

### Task 3: Add Jakarta EE external conformance

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 2
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**

Add a clean, pinned, externally sourced Jakarta EE conformance harness and include it in the
repository verification gate.

**Implementation Steps:**

1. Add selection and corpus documentation.
2. Add the harness and isolated shell command.
3. Analyze the configured CDI-backed endpoint and assert the selected repository implementation.
4. Run the harness against the pinned external checkout.

**Acceptance Criteria:**

- [x] The harness rejects a dirty or unpinned corpus checkout.
- [x] The conformance graph includes the CDI-selected JPA repository implementation.
- [x] The conformance harness contains no production analysis hints.

**Files to Modify:**

- `conformance/jakartaee-rest/**` (new)
- `scripts/verify-jakartaee-rest.sh`
- `scripts/verify-pr.sh`
- `scripts/verify-repository-integrity.sh`
- `.github/workflows/**`

**Tests Required:**

- [x] Pinned Jakarta EE conformance command
- [x] Full pull-request verification gate

## Implementation Order

1. Task 1
2. Task 2
3. Task 3

## Progress Tracking

- Total Tasks: 3
- Completed: 3
- In Progress: 0
- Blocked: 0
- Pending: 0
