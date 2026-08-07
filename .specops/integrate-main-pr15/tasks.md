# Tasks: Integrate Current Main into PR #15

## Task 1: Merge and resolve

**Status:** In Progress
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Acceptance Criteria:**

- [ ] Current `origin/main` is merged without unresolved files.
- [ ] Analyzer, test, documentation, and SpecOps changes from both sides are retained.
- [ ] The two pre-existing unsupported cases are not changed.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `docs/java-capabilities.json`
- `docs/supported-java-constructs.md`
- `.specops/index.json`
- `.specops/memory/context.md`
- `.specops/memory/decisions.json`
- `.specops/memory/patterns.json`
- `.specops/steering/repo-map.md`

## Task 2: Verify and publish

**Status:** Pending
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Acceptance Criteria:**

- [ ] The full local pull-request gate passes.
- [ ] The merge commit is pushed to PR #15.
- [ ] Required hosted checks pass.

## Progress

- Total: 2
- Completed: 0
- In Progress: 1
- Pending: 1
- Blocked: 0
