# Feature: Spring PetClinic Conformance

## Overview

The project has one large brownfield conformance corpus. A reader cannot yet use a small, familiar Spring application to learn what Fachtracing does by adding annotations and inspecting the generated graphs.

## Developer Use Cases

### Use Case 1: Generate PetClinic Graphs

**As a** Java developer evaluating Fachtracing
**I want** selected Spring PetClinic methods to receive only `@FachTracing` annotations
**So that** I can see the graphs that the generic analyzer produces without application-specific configuration

**Acceptance Criteria (EARS):**

- WHEN a maintainer runs the PetClinic conformance gate THE SYSTEM SHALL apply an annotation-only overlay to the pinned canonical Spring PetClinic source.
- THE SYSTEM SHALL generate one simple entity-state graph, one domain lookup graph, and one application workflow graph.
- THE SYSTEM SHALL classify unsupported result-relevant behavior as explicit coverage gaps.

**Progress Checklist:**

- [x] The overlay changes imports and annotations only.
- [x] Exactly three selected decisions produce Mermaid, PlantUML, and normalized semantic output.
- [x] Complete graphs and the intentionally incomplete application graph have their expected completeness states.

### Use Case 2: Detect Conformance Drift

**As a** Fachtracing maintainer
**I want** reviewed graph topologies and isolation checks
**So that** analyzer or corpus drift cannot silently change the product explanation

**Acceptance Criteria (EARS):**

- WHEN a generated semantic graph differs from its reviewed oracle THE SYSTEM SHALL fail the gate.
- IF PetClinic-specific knowledge enters production source or generic configuration THEN THE SYSTEM SHALL fail the gate.
- THE SYSTEM SHALL keep all generated graphs under ignored `target/` output.

**Progress Checklist:**

- [x] Immutable semantic oracles cover all three decisions.
- [x] Repository integrity checks protect the harness and oracle hashes.
- [x] No generated PetClinic artifact is tracked.

### Use Case 3: Explain the Tool

**As a** reader
**I want** a short report with the generated graphs and plain explanations
**So that** I can understand what Fachtracing discovers and where it stops

**Acceptance Criteria (EARS):**

- WHEN a reader opens the conformance report THE SYSTEM SHALL explain complete extraction and explicit incomplete analysis with PetClinic examples.
- WHEN a reader follows the reproduction instructions THE SYSTEM SHALL provide one command for the full conformance run.
- WHEN CI runs a pull-request or release gate THE SYSTEM SHALL prepare and verify the pinned PetClinic corpus.

**Progress Checklist:**

- [x] The report shows the reviewed Mermaid graphs.
- [x] The README links to the PetClinic harness and command.
- [x] Pull-request and release CI run the suite with a cached pinned checkout.

## Library Quality Requirements

- The change shall add no production dependency.
- PetClinic-specific packages, method names, and vocabulary shall remain in the conformance harness.
- The harness shall use the same public annotation, static analyzer, graph model, and renderers as other applications.
- Shell scripts shall use focused responsibilities and fail on the first invalid prerequisite or result.

## Constraints & Assumptions

- The canonical corpus is `spring-projects/spring-petclinic` at commit `88e37c15cf6fc8490b01bc3e8e2c800cec1ac272`.
- The three decisions are `BaseEntity.isNew`, `Owner.getPet(String, boolean)`, and `PetController.processCreationForm`.
- The application workflow is intentionally expected to be incomplete because result-relevant calls cross compiled Spring and persistence boundaries.
- Runtime instrumentation is outside this conformance suite.

## Dependencies & Blockers

No spec-level dependency or blocker exists.

## Success Metrics

- Three annotated decisions are found and compared exactly with reviewed semantic oracles.
- The report distinguishes two complete graphs from one graph with explicit coverage gaps.
- Local and CI verification use the same pinned source revision.

## Out of Scope

- Changes to Spring PetClinic upstream.
- Analyzer remediation for gaps exposed by the application workflow.
- Runtime requests against a running Spring Boot application.
- More than three annotated PetClinic decisions.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Use the single-responsibility principle.
- Use plain Java, POSIX shell, and the existing Maven build.
