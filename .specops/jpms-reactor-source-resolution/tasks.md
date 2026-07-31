# Implementation Tasks: JPMS Reactor Source Resolution

## Task Breakdown

### Task 1: Exclude module descriptors from analyzer sources

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Reproduce the JPMS reactor failure, filter module descriptors at the Maven boundary, and verify all affected behavior.

**Implementation Steps:**

1. Add module descriptors to both reactor fixture modules and reproduce the failure.
2. Exclude `module-info.java` in `AnalyzeMojo.sourceFiles`.
3. Run focused and full verification.

**Acceptance Criteria:**

- [x] The JPMS reactor build generates one current-module graph.
- [x] Both sibling implementations remain dispatch candidates.
- [x] Current-module root isolation remains active.
- [x] Source-empty and single-module behavior remains unchanged.

**Files to Modify:**

- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/AnalyzeMojo.java`
- `fachtracing-maven-plugin/src/test/resources/it/reactor/decision-entry/src/main/java/module-info.java`
- `fachtracing-maven-plugin/src/test/resources/it/reactor/decision-implementations/src/main/java/module-info.java`
- `fachtracing-maven-plugin/src/test/resources/it/reactor/decision-implementations/src/main/java/example/reactor/impl/ReactorDecisionRules.java`
- `scripts/verify.sh`

**Tests Required:**

- [x] JPMS reactor integration passes.
- [x] Maven executable contracts pass.
- [x] Full repository verifier passes.

## Progress Tracking

- Total Tasks: 1
- Completed: 1
- In Progress: 0
- Blocked: 0
- Pending: 0
