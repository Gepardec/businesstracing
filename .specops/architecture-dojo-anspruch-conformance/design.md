# Design: Architecture Dojo Anspruch conformance

## Architecture

Add one external conformance adapter. A shell command creates a detached worktree for the pinned
commit, builds it with Java 21, and passes its sources and classpath to a Java harness. The harness
selects two exact application entry points, projects the source analysis to business graphs, and
writes Mermaid, PlantUML, JSON, and the shared JSON Schema under `target/generated`.

## Responsibilities

- `scripts/verify-architecture-dojo-anspruch.sh` owns checkout validation, build orchestration, and
  harness launch.
- `ArchitectureDojoAnspruchConformanceTest` owns source selection, entry-point selection, business
  artifact checks, schema checks, and disposable output.
- The conformance README owns the pin, branch reason, commands, and output description.
- The root README links the explicit external command.

## Entry points

- `AnspruchWebCheck.pruefe(String)` with the business label `check benefit entitlement`.
- `ErstelleAuMeldungService.erstelleAuMeldung(String, LocalDate)` with the business label
  `submit incapacity notification`.

These identifiers select source roots only. They do not provide nodes, edges, branches, results, or
diagram layout.

## Data and security

The generated graph data is Internal. The output contains business labels and topology. The
business artifact guard must reject developer-only source data and Java implementation details.
No credentials, production data, or personal data is used.

## Dependencies

No production or test dependency is added. The harness reuses the existing analyzer, projector,
renderers, exporter, and dependency-free schema validator.

## Verification

1. Build and test the pinned external repository.
2. Run the new conformance command.
3. Validate both JSON files and the schema.
4. Run the viewer contract checks with the generated files.
5. Run the Fachtracing pull-request gate.
