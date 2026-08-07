# Implementation Tasks: Explicit opaque library boundaries

## Task Breakdown

### Task 1: Replace inferred JAR trust with explicit library selection

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None
**Documentation Required:** Update Maven setup, the capability contract, and the supported-construct guide.
**Breaking Change:** No; existing analyzer and Maven behavior becomes safely fail-closed unless the new option is set.

**Description:**

Add one engine boundary value, exact binary-location matching, Maven artifact resolution, focused contracts, public configuration documentation, and real Hogarama verification.

**Implementation Steps:**

1. Change the archive fixture to prove fail-closed default behavior.
2. Add `OpaqueLibraryBoundary` and carry it through analyzer overloads.
3. Return exact paths from `BinaryTypeOriginResolver` and require boundary membership.
4. Add `OpaqueLibraryArtifactResolver` for resolved compile-classpath artifacts.
5. Pass the new setting through both Maven goals and `ProjectGraphGenerator`.
6. Add engine and Maven resolver contracts.
7. Update public documentation and capability metadata.
8. Verify strict Hogarama failure without selection and success with selection.
9. Run all repository and pull-request gates.

**Acceptance Criteria:**

- [x] Unselected archive operations are incomplete.
- [x] Selected reference operations are complete and keep source predicates.
- [x] Direct Boolean and application directory cases stay incomplete.
- [x] Maven selection resolves exact compile JARs and rejects unsafe inputs.
- [x] Both Maven goals pass the explicit boundary.
- [x] Hogarama and repository gates pass.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/OpaqueLibraryBoundary.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/BinaryTypeOriginResolver.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/OpaqueLibraryArtifactResolver.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/AnalyzeMojo.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/AnalyzeReactorMojo.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/ProjectGraphGenerator.java`
- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/AnalyzeMojoTest.java`
- `docs/maven-plugin.md`
- `docs/supported-java-constructs.md`
- `docs/java-capabilities.json`

**Tests Required:**

- [x] `StaticDecisionAnalyzerTest`
- [x] `AnalyzeMojoTest`
- [x] Real Hogarama strict aggregate analysis
- [x] Full pull-request verification

## Implementation Order

1. Task 1

## Progress Tracking

- Total Tasks: 1
- Completed: 1
- In Progress: 0
- Blocked: 0
- Pending: 0
