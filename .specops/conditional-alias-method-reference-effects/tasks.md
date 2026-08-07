# Implementation Tasks: Conditional Alias and Method-Reference Effects

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `jdk-mutation-alias-effect-correctness` | Provides the effect and alias model. | Yes | Completed |

## Dependency Resolution Log

| Blocker | Resolution Type | Resolution Detail | Date |
| --- | --- | --- | --- |
| None | — | — | — |

## Task Breakdown

### Task 1: Freeze both false-complete regressions

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add independent fixtures and assertions for conditional alias reassignment and a mutating bound
method-reference callback. Run the focused test before production edits.

**Implementation Steps:**

1. Add both source forms to `ResultSlicePolicy`.
2. Add focused completeness, gap, and graph-label assertions.
3. Run the static analyzer contract and record the expected failures.

**Acceptance Criteria:**

- [x] The conditional alias fixture fails because it is falsely complete.
- [x] The method-reference mutation fixture fails because the transfer is absent.

**Files to Modify:**

- `fachtracing-engine/src/test/resources/fixtures/slicing/ResultSlicePolicy.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`

**Tests Required:**

- [x] `StaticDecisionAnalyzerTest` runs and reproduces both defects.

---

### Task 2: Preserve branch and method-reference effects

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Carry alias certainty through `if` merges and classify bound member-reference callback receivers
with existing mutation contracts.

**Implementation Steps:**

1. Add proved and possible alias-root resolution and branch merge support.
2. Apply alias certainty in both effect-index consumers.
3. Resolve bound callback member references and reuse platform/source effect contracts.
4. Run the full static analyzer contract.

**Acceptance Criteria:**

- [x] Conditional aliases cannot create a false `COMPLETE` graph.
- [x] `accepted::add` preserves the transfer from candidates to accepted state.
- [x] Existing direct alias, detached alias, lambda, and predicate-reference contracts pass.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/LocalAliasResolver.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DependencyGraphBuilder.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`

**Tests Required:**

- [x] `StaticDecisionAnalyzerTest` passes.

---

### Task 3: Verify, document, and publish

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 2
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Update the support boundary, run the full pull-request checks, complete the spec record, commit,
push, open the pull request, and verify hosted CI.

**Implementation Steps:**

1. Review and update capability documentation.
2. Run the standard pull-request verification and any focused checks.
3. Complete SpecOps evaluation, memory, metrics, and documentation review.
4. Commit, push, open a draft pull request, and wait for required CI.

**Acceptance Criteria:**

- [x] Documentation states the supported alias and callback boundary.
- [x] Local pull-request verification passes.
- [x] The branch is committed, pushed, and has a pull request.
- [x] Required hosted CI checks pass.

**Files to Modify:**

- `docs/supported-java-constructs.md`
- `.specops/conditional-alias-method-reference-effects/`
- `.specops/index.json`
- `.specops/memory/`
- `.specops/runs/`

**Tests Required:**

- [x] `scripts/verify-pr.sh` passes.
- [x] Required GitHub Actions checks pass.

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
