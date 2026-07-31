# Implementation Journal: Maven Developer Graph Export

## Summary

The Maven plugin now creates an opt-in, revision-pinned developer JSON artifact for each decision.
It links each artifact from the generated index and keeps diagram-only builds Git-free. Export also
proves that every analyzed source is the exact blob in the captured commit. Independent JSON parsing,
ignored-source regression coverage, documentation, and the full repository verification all pass.

## Phase 1 Context Summary

- Config: defaults; no `.specops.json`; `specsDir=.specops`, vertical `library`, task tracking `none`
- Context recovery: no incomplete spec; the merged developer JSON spec is completed
- Steering files: loaded 6 (`dependencies.md`, `product.md`, `reference-application.md`, `repo-map.md`, `structure.md`, `tech.md`)
- Repo map: stale because the merged file-list hash does not match; refresh required at completion
- Memory: loaded 12 decisions from 5 recorded spec IDs and 6 patterns
- Vertical: library
- Affected files: Maven Mojo/generator/test, engine exporter test, Maven guide, README, SpecOps metadata
- Project state: brownfield
- Scope assessment: one coupled follow-up; Maven output, consumer validation, and its guide form one shippable build workflow
- Coherence check: pass; opt-in settings preserve compatibility and strict provenance
- Vocabulary check: pass; library terms use developer use cases and public plugin configuration
- Plan validation: pass; five existing paths resolve and the missing Maven guide is explicitly new
- Dependency introduction: no new dependencies

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
|---|----------|-----------|------|-----------|
| 1 | Make the complete parameter pair the only JSON enablement signal | Existing builds stay Git-free, while configured builds cannot silently omit revision data. | Task 1 | 2026-07-31T09:24:03Z |
| 2 | Verify the source blob at the captured commit | Clean Git status does not include ignored generated files, so blob verification is required for an exact source link. | Task 4 | 2026-07-31T09:24:03Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
|---------|--------|--------|------|
| Add incomplete coverage parsing to the engine test | Parse the incomplete JSON file in the Maven consumer contract | One end-to-end test proves file encoding, JSON validity, Maven wiring, and coverage gaps without duplicating parsers. | Task 2 |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
|---------|------------|--------|------|

## Documentation Review

| Document | Result |
| --- | --- |
| `README.md` | Updated the Maven quick start with the opt-in developer JSON result. |
| `docs/maven-plugin.md` | Added the missing guide with settings, outputs, provenance rules, and data boundaries. |

## Session Log

### Task 1 scope — 2026-07-31

Add the complete Maven parameter pair, optional clean-Git capture, UTF-8 JSON artifacts, index links, and narrow stale-file cleanup. Existing diagram-only generation must stay Git-free and unchanged.

### Task 1 completed — 2026-07-31

Added the two Maven parameters and shared pair validation. The generator captures one clean revision after analysis, writes and links UTF-8 developer JSON, and removes only recognized stale artifacts. Its focused executable contract passes configured, diagram-only, partial-setting, dirty-tree, and cleanup cases.

### Task 2 scope — 2026-07-31

Parse the complete generated document with a separate test-only parser. Verify public topology, revision, source URL, and a non-empty coverage-gap array from an incomplete decision.

### Task 2 completed — 2026-07-31

Added a separate recursive JSON parser to the Maven executable contract. It parses the complete artifact and verifies schema, full commit, nodes, edges, source URL, and a real non-empty coverage-gap array. The focused contract passes.

### Task 3 scope — 2026-07-31

Create the missing linked Maven guide with copyable one-off, lifecycle, strict, skip, and developer JSON configuration. Keep the README summary aligned with the implemented opt-in behavior.

### Task 3 completed — 2026-07-31

Added the missing Maven guide and updated the README summary. The guide documents diagram-only output, lifecycle binding, parent use, strict and skip settings, opt-in developer JSON, its two required parameters, UTF-8 output, clean Git enforcement, and the developer/business data boundary.

### Task 4 scope — 2026-07-31

Verify that each analyzed file exists in the captured commit and that its Git blob fingerprint matches the analysis. Reject ignored generated source instead of creating a false revision link.

### Task 4 completed — 2026-07-31

The exporter reads each source blob from the captured commit and compares its SHA-256 fingerprint
with the analysis manifest. A regression contract proves that an ignored generated source fails when
the captured commit does not contain it. The focused exporter contract and full verification pass.

### Phase 2 completion — 2026-07-31

- Requirements: opt-in Maven JSON files, complete configuration validation, strict provenance, stale cleanup, and consumer parsing.
- Design: capture one engine `SourceRevision` after analysis and before output writes; keep diagram-only builds Git-free.
- Tasks: three ordered tasks for implementation, contract hardening, and documentation.
- Dependencies: no new packages; the completed developer JSON exporter is required.

### Phase 3 completion — 2026-07-31

- Implemented opt-in Maven developer JSON generation, index links, UTF-8 output, and narrow cleanup.
- Added independent full-document parsing and a real incomplete-coverage assertion.
- Added exact captured-commit blob validation for every analyzed source.
- Added the missing Maven guide and updated the README.
- Full verification passed with 0.146% p95 overhead and no errors, mismatches, drops, or contamination.

### Phase 4 completion — 2026-07-31

- Implementation evaluation passed all four dimensions.
- Requirements and task acceptance checks are complete.
- Dependency audit passed with no new dependency.
- Spec metadata, memory, index, repository map, and run log were refreshed.
