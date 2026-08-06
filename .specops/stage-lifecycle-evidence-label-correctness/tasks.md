# Tasks: Stage Lifecycle, Evidence, and Label Correctness

## Task 1: Freeze the five regressions

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Acceptance Criteria:**

- [x] Each attached review example has an independent executable contract.
- [x] Tests fail against the current faulty behavior.

**Files to Modify:**

- `fachtracing-agent/src/test/java/at/gepardec/fachtracing/agent/FachtracingTransformerTest.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/BusinessLabelNormalizerTest.java`

**Tests Required:**

- [x] Agent regression tests
- [x] Engine analysis and label tests

## Task 2: Correct async lifecycle and callback positions

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Acceptance Criteria:**

- [x] Skipped stage callbacks release their exact reservation once.
- [x] Cancel probes cover unselected methods in fingerprinted classes.
- [x] Every catalog callback position can be wrapped without changing call behavior.

**Files to Modify:**

- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/AsyncInvocationCatalog.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingTransformer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeCollector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/TraceRuntime.java`

**Tests Required:**

- [x] Agent and runtime module tests

## Task 3: Correct receiver evidence and business labels

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Acceptance Criteria:**

- [x] Direct parameter receivers create exact evidence targets.
- [x] Unsupported explicit receivers create source-located gaps.
- [x] Legitimate validator labels remain unchanged.
- [x] Technical validation helper labels remain business-only without a domain dictionary.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/BusinessLabelNormalizer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/BusinessArtifactGuard.java`
- `conformance/mega-backend/src/test/resources/oracles/`

**Tests Required:**

- [x] Engine analysis and label tests
- [x] Mega conformance tests

## Task 4: Run release conformance and close the specification

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Tasks 2 and 3
**Priority:** High
**IssueID:** None
**Blocker:** None

**Acceptance Criteria:**

- [x] Standard, external, Mega, and long-load checks pass.
- [x] Spec, docs, memory, and release evidence match the implementation.

**Files to Modify:**

- `docs/`
- `.specops/stage-lifecycle-evidence-label-correctness/`
- `.specops/memory/`
- `.specops/index.json`

**Tests Required:**

- [x] `./scripts/verify.sh`
- [x] 600-second 1,000-RPS release gate

## Progress Tracking

- Total Tasks: 4
- Completed: 4
- In Progress: 0
- Blocked: 0
- Pending: 0
