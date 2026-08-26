# Tasks: CDI dispatch selection correctness

## Task 1: Resolve constructor injection points

**Status:** Completed
**Priority:** High
**Effort:** M
**Dependencies:** None
**IssueID:** None

**Description:** Index direct injected-constructor assignments and provide the parameter element to
dispatch selectors.

**Files to Modify:**
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DynamicDispatchTargetSelector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`

**Acceptance Criteria:**
- [x] Constructor parameter qualifiers reach the CDI selector.
- [x] Field injection and generic dispatch keep their current behavior.

**Tests Required:**
- [x] Engine and adapter dispatch tests pass.

## Task 2: Apply exact CDI qualifier rules

**Status:** Completed
**Priority:** High
**Effort:** M
**Dependencies:** Task 1
**IssueID:** None

**Description:** Implement implicit `@Default`, binding member comparison, and `@Nonbinding`.

**Files to Modify:**
- `fachtracing-jakartaee/src/main/java/at/gepardec/fachtracing/jakartaee/CdiDispatchTargetSelector.java`
- `fachtracing-jakartaee/src/test/resources/fixtures/CdiWorkflow.java`
- `fachtracing-jakartaee/src/test/java/at/gepardec/fachtracing/jakartaee/JakartaEeMethodContractProviderTest.java`

**Acceptance Criteria:**
- [x] Default-qualified injection excludes custom-only beans.
- [x] Binding values must match.
- [x] Nonbinding values do not affect matching.

**Tests Required:**
- [x] Add and run focused CDI regression tests.

## Task 3: Verify and publish

**Status:** Completed
**Priority:** High
**Effort:** S
**Dependencies:** Task 2
**IssueID:** None

**Description:** Run the PR verification, review the diff, commit, push, and confirm CI.

**Files to Modify:**
- `.specops/cdi-dispatch-selection-correctness/*`
- `.specops/index.json`
- `.specops/memory/*`

**Acceptance Criteria:**
- [x] Local PR verification passes.
- [x] PR 31 contains the fix commit.
- [x] GitHub Actions passes.
