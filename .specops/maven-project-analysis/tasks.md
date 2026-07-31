# Tasks: Maven Project Analysis

### Task 1: Implement the Maven analysis goal

**Status:** Completed
**Priority:** High
**Estimated Effort:** M
**Dependencies:** generic-tracing-walking-skeleton, mermaid-diagram-rendering
**IssueID:** None

**Description:** Add a standard Maven plugin module that adapts Maven project metadata to the
existing generic analyzer and renderers.

**Implementation Steps:**

1. Add the Maven plugin reactor module and approved plugin-development dependencies.
2. Implement source/classpath discovery, module skipping, strict coverage, deterministic naming,
   stale-output cleanup, diagram writes, and Markdown index generation.
3. Add focused executable contracts for naming, discovery, output, skipping, and strict coverage.

**Files to Modify:**

- `pom.xml`
- `fachtracing-maven-plugin/pom.xml`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/AnalyzeMojo.java`
- `fachtracing-maven-plugin/src/main/java/at/gepardec/fachtracing/maven/ProjectGraphGenerator.java`
- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/AnalyzeMojoTest.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`

**Acceptance Criteria:**

- [x] Maven metadata is converted into a generic `AnalysisRequest` without domain hints.
- [x] Deterministic Mermaid, PlantUML, and index artifacts are generated.
- [x] Empty/unannotated modules skip and incomplete graphs obey strict mode.

**Tests Required:**

- [x] Plugin unit contract passes.
- [x] Existing Java contracts pass.

### Task 2: Prove the copy-paste Maven workflow

**Status:** Completed
**Priority:** High
**Estimated Effort:** M
**Dependencies:** Task 1
**IssueID:** None

**Description:** Verify a clean external-style Maven fixture can add the annotation and plugin,
run one lifecycle command, and receive usable diagrams.

**Implementation Steps:**

1. Add a standalone Maven fixture using the public annotation and plugin configuration.
2. Run the fixture through both a fully qualified one-off goal and configured `mvn process-classes`.
3. Document installation, POM setup, command, outputs, multi-module behavior, and static/runtime boundary.
4. Re-run pinned Mega conformance to prove target neutrality.

**Files to Modify:**

- `fachtracing-maven-plugin/src/test/resources/it/basic/pom.xml`
- `fachtracing-maven-plugin/src/test/resources/it/basic/pom-command.xml`
- `fachtracing-maven-plugin/src/test/resources/it/basic/src/main/java/example/ApprovalPolicy.java`
- `scripts/verify.sh`
- `README.md`
- `docs/maven-plugin.md`

**Acceptance Criteria:**

- [x] A standalone Maven fixture generates both diagram formats and an index with `mvn process-classes`.
- [x] The same fixture works through a fully qualified Maven goal without plugin POM configuration.
- [x] Documentation contains a complete copyable setup with no classpath assembly.
- [x] Generic and Mega verification remain green.

**Tests Required:**

- [x] `./scripts/verify.sh` passes including the Maven fixture.
- [x] `./scripts/verify-mega-backend.sh` passes unchanged.
