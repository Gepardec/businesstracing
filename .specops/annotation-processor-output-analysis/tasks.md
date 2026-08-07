# Implementation Tasks: Analyze Annotation-Processor Output

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `generic-application-readiness` | It introduced the resolver corrected here. | Yes | Completed |

## Dependency Resolution Log

| Blocker | Resolution Type | Resolution Detail | Date |
| --- | --- | --- | --- |
| — | — | — | — |

## Task Breakdown

### Task 1: Freeze the annotation-processor regression

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add resolver and Maven integration contracts that reproduce the rejection and prove generated-source
discovery.

**Implementation Steps:**

1. Replace the rejection-only unit contract with an analysis-sanitization contract.
2. Create a generic two-module processor fixture.
3. Add the failing fixture invocation and output checks to standard verification.

**Acceptance Criteria:**

- [x] The resolver contract fails against current production behavior.
- [x] The integration fixture generates an annotated Java decision during Maven compile.
- [x] The aggregate verification fails at the current processor-model rejection.

**Files to Modify:**

- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/AnalyzeMojoTest.java`
- `fachtracing-maven-plugin/src/test/resources/it/annotation-processor/pom.xml` (new)
- `fachtracing-maven-plugin/src/test/resources/it/annotation-processor/processor/pom.xml` (new)
- `fachtracing-maven-plugin/src/test/resources/it/annotation-processor/processor/src/main/java/example/processor/DecisionProcessor.java` (new)
- `fachtracing-maven-plugin/src/test/resources/it/annotation-processor/processor/src/main/resources/META-INF/services/javax.annotation.processing.Processor` (new)
- `fachtracing-maven-plugin/src/test/resources/it/annotation-processor/application/pom.xml` (new)
- `fachtracing-maven-plugin/src/test/resources/it/annotation-processor/application/src/main/java/example/application/GenerateDecision.java` (new)
- `fachtracing-maven-plugin/src/test/resources/it/annotation-processor/application/src/main/java/example/application/DecisionRequest.java` (new)
- `scripts/verify.sh`

**Tests Required:**

- [x] Run the focused resolver contract and record the expected failure.
- [x] Run the annotation-processor Maven fixture and record the expected failure.

---

### Task 2: Sanitize annotation-processing settings

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Accept processor-enabled Maven models while keeping the Fachtracing compiler task processor-free.

**Implementation Steps:**

1. Remove configuration-level processor rejection.
2. Filter processor-only compiler arguments and their value tokens.
3. Preserve the existing validation of unrelated analysis-owned settings.
4. Run focused unit and integration contracts.

**Acceptance Criteria:**

- [x] Processor paths, names, and Maven `proc` mode do not reject analysis.
- [x] Processor-only compiler arguments are absent from the analysis compiler model.
- [x] Safe arguments remain ordered and unique.
- [x] Unsupported unrelated compiler controls still fail.

**Files to Modify:**

- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/MavenCompilerModelResolver.java`
- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/AnalyzeMojoTest.java`

**Tests Required:**

- [x] Run the focused resolver contract.
- [x] Run the annotation-processor Maven fixture.

---

### Task 3: Document and verify the support boundary

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 2
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:**
Document post-compile generated-source analysis and verify the complete repository.

**Implementation Steps:**

1. Update Maven setup and supported-construct documentation.
2. Run standard repository verification.
3. Complete the SpecOps evaluation, memory, repository map, and documentation records.

**Acceptance Criteria:**

- [x] Documentation states that Fachtracing consumes output and never executes processors.
- [x] Documentation distinguishes source-generating processors from AST-only transformations.
- [x] Standard repository verification passes.

**Files to Modify:**

- `docs/maven-plugin.md`
- `docs/supported-java-constructs.md`
- `.specops/annotation-processor-output-analysis/*`
- `.specops/index.json`
- `.specops/memory/*`
- `.specops/steering/repo-map.md`

**Tests Required:**

- [x] Run `./scripts/verify.sh`.

---

### Task 4: Correct review gaps in processor sanitization and provenance

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 3
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:**
Remove the remaining official processor-only module option and classify configured generated source
roots independently of their location.

**Implementation Steps:**

1. Extend processor argument sanitization for `--default-module-for-created-files`.
2. Centralize generated source root discovery and use it for developer provenance.
3. Add focused resolver and provenance contracts, then run repository verification.

**Acceptance Criteria:**

- [x] Both forms of `--default-module-for-created-files` are absent from the analysis model.
- [x] A configured generated source root outside the build directory is classified as generated.
- [x] Both Maven goals use the same generated source root discovery.
- [x] Standard repository verification passes.

**Files to Modify:**

- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/MavenCompilerModelResolver.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/AnalyzeMojo.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/AnalyzeReactorMojo.java`
- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/AnalyzeMojoTest.java`
- `.specops/annotation-processor-output-analysis/*`

**Tests Required:**

- [x] Run the focused Maven plugin executable contract.
- [x] Run `./scripts/verify.sh`.

## Implementation Order

1. Task 1 freezes the defect.
2. Task 2 implements and tests the fix.
3. Task 3 documents and verifies the general boundary.
4. Task 4 closes the two review gaps before release.

## Progress Tracking

- Total Tasks: 4
- Completed: 4
- In Progress: 0
- Blocked: 0
- Pending: 0
