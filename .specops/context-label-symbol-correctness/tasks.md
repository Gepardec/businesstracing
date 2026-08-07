# Implementation Tasks: Context label symbol correctness

## Spec-Level Dependencies

No required spec dependency exists.

## Task Breakdown

### Task 1: Add regression contracts

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add exact source and graph contracts for static utility targets, inferred `var` types, and
identifier shadowing.

**Implementation Steps:**

1. Extend the generic context-label fixture with all three source forms.
2. Add exact expected and forbidden label assertions.
3. Run the focused contract and confirm that it fails before production changes.

**Acceptance Criteria:**

- [x] Static sort and fill contracts name their first argument as the mutation target.
- [x] The inferred calendar contract names `gregorian calendar` and not `item`.
- [x] The shadowing contract does not apply the block-local subject to the field receiver.

**Files to Modify:**

- `fachtracing-engine/src/test/resources/fixtures/labels/ContextAwareLabelPolicy.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`

**Tests Required:**

- [x] Focused static analyzer contract fails on the reported behavior before the fix.

### Task 2: Use attributed mutation subjects

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Use attributed symbols and types for receiver subjects, and align static utility labels with
existing effect targets.

**Implementation Steps:**

1. Key stored subjects by compiler element.
2. Derive variable subjects from attributed type mirrors with a syntax fallback.
3. Share static utility mutation classification between effect and label code.
4. Use the first argument as the receiver of a supported static utility mutation.

**Acceptance Criteria:**

- [x] Static utility labels name the actual changed object.
- [x] `var` declarations use their inferred type.
- [x] Equal source spellings in different scopes resolve to different elements.
- [x] Existing context-aware labels remain unchanged.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`

**Tests Required:**

- [x] Complete static analyzer executable contract passes.

### Task 3: Verify and prepare publication

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 2
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:**
Run repository checks, complete the bug-fix records, and prepare the branch update for publication.

**Implementation Steps:**

1. Run focused module tests and the pull-request gate.
2. Review public documentation for impact.
3. Complete SpecOps evaluation, memory, metrics, and run-log records.
4. Prepare the completed change for commit, push, and hosted CI confirmation.

**Acceptance Criteria:**

- [x] Focused tests pass.
- [x] The exact pull-request gate passes.
- [x] Documentation impact is recorded.
- [x] The completed change is ready for commit and push.

**Files to Modify:**

- `docs/supported-java-constructs.md`
- `.specops/context-label-symbol-correctness/`
- `.specops/index.json`
- `.specops/memory/`
- `.specops/runs/context-label-symbol-correctness-20260807-121753.log.md`

**Tests Required:**

- [x] Focused engine verification.
- [x] Pull-request verification workflow.

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
