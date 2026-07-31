# Implementation Tasks: Reactor-wide Implementation Resolution

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `maven-project-analysis` | Existing Maven adapter behavior is the compatibility baseline. | No | completed record in project memory |

## Task Breakdown

### Task 1: Separate graph roots from the source universe

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None
**Documentation Required:** No
**Breaking Change:** No

**Description:**
Extend the analysis request with an explicit graph-root source set while preserving current callers.

**Implementation Steps:**

1. Add and validate `rootSourceFiles` in `AnalysisRequest`.
2. Restrict annotated root discovery in `StaticDecisionAnalyzer` to the root set.
3. Add an engine contract fixture and assertions for source-universe resolution and root isolation.

**Acceptance Criteria:**

- [x] Existing analysis request construction treats all sources as graph roots.
- [x] Explicit root sources select graph entries without limiting implementation discovery.
- [x] Invalid root scopes fail before compiler analysis.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisRequest.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `fachtracing-engine/src/test/resources/fixtures/reactor/DecisionEntry.java`
- `fachtracing-engine/src/test/resources/fixtures/reactor/LocalDecisionRule.java`
- `fachtracing-engine/src/test/resources/fixtures/reactor/RegionalDecisionRule.java`

**Tests Required:**

- [x] Engine executable contracts pass.

---

### Task 2: Supply reactor-wide Maven inputs

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None
**Documentation Required:** Yes
**Breaking Change:** No

**Description:**
Use all active reactor projects for implementation source resolution while each Mojo execution keeps its current-module entry roots and output.

**Implementation Steps:**

1. Inject reactor projects and collect stable source/classpath unions with a current-project fallback.
2. Pass current-module roots and reactor-wide sources through `ProjectGraphGenerator`.
3. Add Maven generator coverage and document reactor behavior.

**Acceptance Criteria:**

- [x] Sibling reactor implementations are available to current-module analysis.
- [x] Only current-module annotations generate current-module diagrams.
- [x] Single-module builds retain existing output and skip behavior.
- [x] Source and classpath inputs are stable and duplicate-free.

**Files to Modify:**

- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/AnalyzeMojo.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/ProjectGraphGenerator.java`
- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/AnalyzeMojoTest.java`
- `fachtracing-maven-plugin/src/test/resources/it/reactor/pom.xml`
- `fachtracing-maven-plugin/src/test/resources/it/reactor/decision-entry/`
- `fachtracing-maven-plugin/src/test/resources/it/reactor/decision-implementations/`
- `scripts/verify.sh`
- `README.md`
- `docs/supported-java-constructs.md`

**Tests Required:**

- [x] Maven generator contracts pass.
- [x] Full repository verification passes.

---

### Task 3: Address review findings

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 2
**Priority:** High
**IssueID:** None
**Blocker:** None
**Documentation Required:** Yes
**Breaking Change:** No

**Description:**
Make source-empty parent modules skip before reactor classpath resolution and complete the Maven reactor documentation.

**Implementation Steps:**

1. Move the empty-root decision before reactor source and classpath collection.
2. Bind the integration fixture in its parent POM and verify output isolation for the parent and both children.
3. Document active-reactor implementation resolution and JPMS descriptor handling in the Maven guide.

**Acceptance Criteria:**

- [x] A source-empty parent module skips without reactor classpath resolution.
- [x] Only the decision-entry child writes a graph in an inherited parent-POM lifecycle.
- [x] The Maven guide explains reactor and JPMS behavior.

**Tests Required:**

- [x] Full repository verification passes.

## Implementation Order

1. Task 1 establishes the analysis contract.
2. Task 2 adapts Maven and verifies the complete flow.
3. Task 3 resolves review findings before publication.

## Progress Tracking

- Total Tasks: 3
- Completed: 3
- In Progress: 0
- Blocked: 0
- Pending: 0
