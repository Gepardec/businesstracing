# Tasks: Outcome Evidence, Cancellation Reach, Slice, and Label Correctness

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `stage-lifecycle-evidence-label-correctness` | Supplies the contracts corrected by this review. | Yes | Completed |

## Task 1: Freeze the four regressions

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Acceptance Criteria:**

- [x] Runtime execution and explanation expose a direct return receiver fact.
- [x] A separate controller cancellation is required by activation and agent tests.
- [x] An ignored read-only call is absent and an unknown reference effect is a gap.
- [x] Proven helper and non-helper validation labels are distinct.

**Files to Modify:**

- `fachtracing-agent/src/test/java/at/gepardec/fachtracing/agent/FachtracingTransformerTest.java`
- `fachtracing-agent/src/test/java/agentfixture/ExternalCancellationController.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `fachtracing-engine/src/test/resources/fixtures/slicing/ResultSlicePolicy.java`
- `fachtracing-maven-plugin/src/test/resources/it/external-release/src/main/java/example/ExternalController.java`
- `fachtracing-maven-plugin/src/test/resources/it/external-release/src/main/java/example/ExternalRuntime.java`

**Tests Required:**

- [x] Focused analyzer and transformer tests fail before production changes.

## Task 2: Preserve terminal evidence and cancellation reach

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Acceptance Criteria:**

- [x] Stop evidence is stored with the final result and appears as a business reason.
- [x] Only compiled application graph owners and supported cancellation callers are fingerprinted.
- [x] A separate controller releases the exact reservation once without changing cancel behavior.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeCollector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/explain/DecisionExplanationProjector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/explain/BusinessStatementRenderer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/CancellationBoundaryScanner.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingTransformer.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/ClassFingerprintResolver.java`

**Tests Required:**

- [x] Runtime, agent, Maven plugin, and external release tests pass.

## Task 3: Correct the result slice and role labels

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Acceptance Criteria:**

- [x] Only proven writes enter the backward slice.
- [x] Unknown effects on result-dependent references create source-located gaps.
- [x] JDK and source-proven mutations that affect a return remain in the graph.
- [x] The global `validate` rule is absent; helper roles and other receivers retain correct meaning.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DependencyGraphBuilder.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/BackwardDecisionSlicer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `conformance/mega-backend/src/test/resources/oracles/`

**Tests Required:**

- [x] Analyzer contracts and Mega conformance pass.

## Task 4: Verify, document, commit, and push

**Status:** In Progress
**Estimated Effort:** L
**Dependencies:** Tasks 2 and 3
**Priority:** High
**IssueID:** None
**Blocker:** None

**Acceptance Criteria:**

- [ ] Standard, external, Mega, and 600-second gates pass.
- [ ] Specs, docs, generated graphs, memory, and release evidence match the code.
- [ ] All changes are committed and pushed to PR #5.

**Files to Modify:**

- `docs/`
- `.specops/outcome-evidence-cancellation-slice-label-correctness/`
- `.specops/memory/`
- `.specops/index.json`

**Tests Required:**

- [ ] `./scripts/verify.sh`
- [ ] Mega Backend conformance
- [ ] `./scripts/release-gate.sh` or the repository's clean long-gate command

## Progress Tracking

- Total Tasks: 4
- Completed: 3
- In Progress: 1
- Blocked: 0
- Pending: 0
