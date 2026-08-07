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

---

### Task 5: Make processor fixture compilation deterministic

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 4
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Stop Java 21 from discovering the fixture processor before its implementation class is compiled.

**Implementation Steps:**

1. Reproduce the clean fixture failure with Maven on Java 21.
2. Disable processing only while the processor module compiles itself.
3. Verify that the application still loads and executes the completed processor.
4. Run the complete pull-request verifier.

**Acceptance Criteria:**

- [x] The processor module compiles from a clean state on Java 21.
- [x] The application still generates `GeneratedApprovalPolicy.java`.
- [x] Per-module and aggregate Fachtracing graphs still contain the generated decision.
- [x] The complete pull-request verifier passes on Java 21.

**Files to Modify:**

- `fachtracing-maven-plugin/src/test/resources/it/annotation-processor/processor/pom.xml`
- `.specops/annotation-processor-output-analysis/*`

**Tests Required:**

- [x] Run the clean annotation-processor fixture with Maven on Java 21.
- [x] Run `./scripts/verify-pr.sh` with Maven on Java 21.

---

### Task 6: Preserve Maven source and target semantics

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 5
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Stop converting Maven `source` and `target` into the stricter `javac --release` option.

**Implementation Steps:**

1. Add a compiler-model language-selection mode with release-compatible constructors.
2. Make the Maven resolver distinguish explicit `release` from `source` and `target`.
3. Add an executable analyzer regression for generated Java that uses a newer JDK API.
4. Run focused and pull-request verification.

**Acceptance Criteria:**

- [x] Maven `source` and `target` produce `-source` and `-target` analyzer options.
- [x] Maven `release` still produces `--release` analyzer options.
- [x] Generated Java that imports `javax.annotation.processing.Generated` can be attributed for a
  Java 8 source/target project on Java 21.
- [x] Existing flat and JPMS compiler-model contracts pass.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/ApplicationSourceBoundary.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `docs/maven-plugin.md`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/MavenCompilerModelResolver.java`
- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/AnalyzeMojoTest.java`

**Tests Required:**

- [x] Run the focused engine and Maven plugin executable contracts.
- [x] Run `./scripts/verify-pr.sh` with Maven on Java 21.

## Implementation Order

1. Task 1 freezes the defect.
2. Task 2 implements and tests the fix.
3. Task 3 documents and verifies the general boundary.
4. Task 4 closes the two review gaps before release.
5. Task 5 fixes the Java 21 CI-only fixture bootstrap failure.
6. Task 6 preserves the language-selection mode used by the successful Maven compile.

## Progress Tracking

- Total Tasks: 6
- Completed: 6
- In Progress: 0
- Blocked: 0
- Pending: 0
