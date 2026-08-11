# Implementation Tasks: External Method Semantic Contracts

## Task Breakdown

### Task 1: Define exact semantic contracts

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None

**Description:**
Add immutable method references, semantic facts, providers, and one conflict-validating registry.

**Implementation Steps:**

1. Add the public contract records and enums.
2. Add provider validation and exact-key resolution.
3. Add compatibility-safe request configuration.

**Acceptance Criteria:**

- [x] Exact keys and immutable facts are public.
- [x] Duplicate provider matches fail closed.
- [x] Existing analysis request constructors work unchanged.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/**`
- `fachtracing-engine/src/main/java/module-info.java`

**Tests Required:**

- [x] Exact, absent, invalid, and conflicting provider contracts.
- [x] Analysis-request compatibility.

---

### Task 2: Apply contracts during source analysis

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None

**Description:**
Resolve contracts after source analysis and before opaque-library or coverage-gap handling.

**Implementation Steps:**

1. Resolve exact attributed method keys at relevant call sites.
2. Apply predicate, mutation, return, and exception facts.
3. Emit deterministic source-located gaps for conflicts.

**Acceptance Criteria:**

- [x] Source analysis has precedence.
- [x] Contract facts can complete supported compiled calls.
- [x] Opaque and unmatched behavior stays fail-closed.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DecisionGraphBuilder.java`

**Tests Required:**

- [x] Source precedence, exact match, unmatched call, conflict, mutation, and caught-exception tests.

---

### Task 3: Verify and document the extension point

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 2
**Priority:** Medium
**IssueID:** None

**Description:**
Document the trust boundary and run the project verification gates.

**Implementation Steps:**

1. Document exact contract configuration and precedence.
2. Run focused analyzer tests and the pull-request gate.

**Acceptance Criteria:**

- [x] Documentation explains that enabled providers are trusted method-level facts.
- [x] Existing runtime and developer graph tests pass.

**Files to Modify:**

- `docs/supported-java-constructs.md`
- `docs/maven-plugin.md`

**Tests Required:**

- [x] Focused engine tests.
- [x] Pull-request verification.
