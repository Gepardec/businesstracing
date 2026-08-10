# Implementation Journal: Explicit opaque library boundaries

## Summary

Completed one task. Dependency JARs are fail-closed by default. Users can declare exact resolved technical-library artifacts, while unselected dependencies, direct binary Boolean decisions, and application class directories remain incomplete. Focused contracts, real Hogarama validation, and the full pull-request gate pass.

## Phase 1 Context Summary

- Config: SpecOps defaults; vertical `library`; specsDir `.specops`; task tracking `none`.
- Context recovery: the completed `fix-hogarama-aggregate-completeness` spec inferred trust from all dependency JARs; user review identified a false-complete risk.
- Steering files: loaded product, tech, structure, reference application, dependency context, and fresh repo map.
- Memory: loaded completed-spec context, decisions, and recurring external-library patterns.
- Vertical: library.
- Project state: brownfield with 384 tracked files.
- Affected files: analyzer boundary and origin resolver, Maven goals and generation adapter, executable contracts, and public Maven and Java-capability documentation.
- Scope: one coupled explicit-trust boundary; no decomposition.

## Phase 2 Completion Summary

- Requirement: dependency code stays decision-bearing unless the user selects its exact resolved artifact as a technical library.
- Design: Maven maps coordinates to exact paths; the engine applies immutable path membership.
- Tasks: one medium implementation and verification task.
- Dependencies: no new dependency; the current direct dependency inventory was verified earlier on 2026-08-07.

## Phase 3 Completion Summary

- Added immutable `OpaqueLibraryBoundary` engine options without changing existing method contracts.
- Extended binary origin resolution with the exact first classpath location.
- Added exact Maven `groupId:artifactId` resolution for compile-classpath JARs.
- Passed the boundary through both Maven goals and the generation adapter.
- Proved empty, unrelated, exact, direct Boolean, and directory behavior in executable contracts.
- Proved that strict Hogarama analysis fails without selection and passes with Morphia, Commons Collections, and Commons Lang selected.
- Passed Java capabilities, self-tracing, external release, five Mega graphs, and the full pull-request gate.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
|---|----------|-----------|------|-----------|
| 1 | Make opaque dependency trust empty by default. | Packaging in a JAR does not prove that code is free of business decisions. | Task 1 | 2026-08-07T12:42:10Z |
| 2 | Resolve Maven coordinates to exact compile-classpath archive paths. | Maven owns artifact identity, while the engine must remain build-tool independent. | Task 1 | 2026-08-07T12:42:10Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
|---------|--------|--------|------|

No deviations.

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
|---------|------------|--------|------|

The sandbox blocked Maven writes to the user repository during the full gate. The approved full-gate run completed outside that filesystem restriction. No product change was required.

## Documentation Review

- `README.md`: checked; it links to the detailed Maven setup and needs no duplicate option list.
- `docs/maven-plugin.md`: updated with the default, XML and command-line syntax, validation rules, and trust warning.
- `docs/supported-java-constructs.md`: updated with the explicit boundary and fail-closed cases.
- `docs/java-capabilities.json`: updated to bind the capability to its executable contract.

## Session Log

- 2026-08-07T12:41:09Z: Started the safety remediation after user review.
- 2026-08-07T12:42:10Z: Defined the explicit fail-closed boundary and its verification plan.
- 2026-08-07T12:48:00Z: Focused engine and Maven contracts passed.
- 2026-08-07T12:50:42Z: Real strict Hogarama analysis failed without an opaque-library selection as required.
- 2026-08-07T12:50:56Z: Real strict Hogarama analysis passed with the three required technical artifacts selected.
- 2026-08-07T12:55:00Z: The full pull-request gate passed with five complete Mega graphs and zero load correctness failures.
