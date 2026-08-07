# Implementation Tasks: Context-aware operation labels

## Spec-Level Dependencies

No required spec dependency exists.

## Task Breakdown

### Task 1: Add a failing label contract

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add one generic Java fixture and exact assertions for the unclear local and setter labels.

**Implementation Steps:**

1. Add a fixture that creates a one-letter `Calendar` local and calls `set(property, value)`.
2. Assert the required business labels.
3. Assert that raw `c` and `evaluate set` are absent and rejected by the artifact guard.

**Acceptance Criteria:**

- [x] The test fails with the current production label logic.
- [x] The test names the expected receiver, property, and value.

**Files to Modify:**

- `fachtracing-engine/src/test/resources/fixtures/labels/ContextAwareLabelPolicy.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`

**Tests Required:**

- [x] Static analyzer executable contract reproduces the defect.

### Task 2: Generate context-aware operation labels

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Use the local declaration and generic setter operands to produce useful labels.

**Implementation Steps:**

1. Record a useful local subject when the scanner visits a variable.
2. Use the subject in derivation and receiver labels.
3. Render an exact two-argument `set` call with its receiver, property, and value.
4. Extend the artifact guard for the two unclear fallback forms.

**Acceptance Criteria:**

- [x] `Calendar c` produces `calendar`.
- [x] `c.set(Calendar.HOUR_OF_DAY, hour)` produces `set calendar hour of day to hour`.
- [x] Meaningful existing local and named-setter labels remain unchanged.
- [x] The artifact guard rejects `c` and `evaluate set`.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/BusinessArtifactGuard.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`

**Tests Required:**

- [x] Static analyzer executable contracts pass.

### Task 3: Audit the complete application graph

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 2
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:**
Regenerate the Hogajama graph, find other context-free labels from the same root cause, and extend
the generic label rules for those forms.

**Implementation Steps:**

1. Regenerate the complete Hogajama graph with the changed analyzer.
2. Audit unique labels for short locals and context-free operations.
3. Cover type abbreviations, generic collection names, and collection additions.

**Acceptance Criteria:**

- [x] The complete graph has no `c`, `comp`, `list`, `evaluate set`, or `evaluate add` node.
- [x] Generic regression contracts pass.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/BusinessArtifactGuard.java`
- `fachtracing-engine/src/test/resources/fixtures/labels/ContextAwareLabelPolicy.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`

**Tests Required:**

- [x] Static analyzer executable contracts.

### Task 4: Verify and close the change

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 3
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:**
Run repository checks, review documentation impact, and complete the SpecOps records.

**Implementation Steps:**

1. Run focused engine and capability verification.
2. Run the repository verification workflow.
3. Review affected documentation and complete the spec records.

**Acceptance Criteria:**

- [x] Focused verification passes.
- [x] Repository verification passes.
- [x] Documentation impact is recorded.

**Files to Modify:**

- `docs/java-capabilities.json`
- `docs/supported-java-constructs.md`
- `.specops/context-aware-operation-labels/`
- `.specops/index.json`
- `.specops/memory/`

**Tests Required:**

- [x] Focused engine and capability contracts.
- [x] Repository verification workflow.

## Implementation Order

1. Task 1
2. Task 2
3. Task 3
4. Task 4

## Progress Tracking

- Total Tasks: 4
- Completed: 4
- In Progress: 0
- Blocked: 0
- Pending: 0
