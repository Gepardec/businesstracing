# Implementation Journal: Spring PetClinic Conformance

## Summary

All three tasks are complete. The project now uses a pinned canonical Spring PetClinic checkout as a small teaching and conformance corpus. Three annotation-only changes produce two complete graphs and one intentionally incomplete application graph with five explicit proof gaps. Immutable semantic oracles, repository integrity, pull-request CI, release CI, and plain-language documentation protect and explain the result.

The full pull-request gate passed. It included all project tests, external release simulation, short performance verification, the 420-file Mega corpus, and the 30-file PetClinic corpus.

## Phase 1 Context Summary

- Config: defaults; `specsDir=.specops`, vertical `library`, task tracking `none`
- Context recovery: no incomplete spec found
- Steering files: loaded 6 files (`dependencies.md`, `product.md`, `reference-application.md`, `repo-map.md`, `structure.md`, `tech.md`)
- Repo map: existing map loaded; it will be refreshed after implementation
- Memory: loaded completed project context, decisions, and recurring patterns
- Vertical: library
- Affected files: new PetClinic conformance harness, verification scripts, GitHub workflow, root README, integrity checks, and SpecOps artifacts
- Project state: brownfield
- Scope assessment: one independent conformance deliverable across harness, gates, and explanation; no decomposition needed
- Vocabulary check: pass; application-specific knowledge stays under the conformance boundary
- Plan validation: pass; existing Mega harness and verification scripts supply the implementation pattern

## Phase 2 Completion Summary

- Requirements: annotate three PetClinic decisions, compare reviewed graphs, show explicit gaps, and run the suite in CI.
- Design: use a pinned clean external checkout, an annotation-only overlay, normalized semantic oracles, and disposable output.
- Tasks: three sequential tasks cover the harness, mandatory gates, and explanation.
- Dependencies: no new Fachtracing dependency; PetClinic remains external conformance input.
- Dependency safety: pass; no project dependency changes are planned.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
|---|----------|-----------|------|-----------|
| 1 | Use three PetClinic decisions with increasing analysis depth | The sequence teaches simple predicates, domain control flow, and explicit framework-boundary gaps | 1 | 2026-08-07T12:19:57Z |

## Session Log

### 2026-08-07 — Task 1 anchor

- Objective: add the pinned annotation overlay, reviewed graphs, executable test, and local gate.
- Files: `conformance/spring-petclinic/` and `scripts/verify-spring-petclinic.sh`.
- Checks: exact decision set, semantic oracles, completeness, coverage gaps, and artifact guard.
- Safety: no production implementation or dependency change.

### 2026-08-07 — Task 1 complete

- Added the annotation-only overlay for three selected PetClinic methods.
- Added exact graph, completeness, terminal, gap, business-artifact, and isolation checks.
- Added three normalized semantic oracles and the local verification script.
- The standalone gate passed: 30 source files, three decisions, two complete graphs, and one graph with five explicit gaps.

### 2026-08-07 — Task 2 anchor

- Objective: make the PetClinic contract mandatory in repository integrity, pull-request, and release gates.
- Files: GitHub workflow and focused verification scripts.
- Checks: shell workflow contracts, tracked inputs, immutable hashes, and ignored output.
- Safety: reuse one pinned cached source checkout; do not add external credentials or mutable references.

### 2026-08-07 — Task 2 complete

- Added a cached pinned PetClinic checkout to pull-request CI.
- Added exact pinned checkout preparation to the clean release gate.
- Connected the PetClinic script to both verification paths.
- Added tracked-file, ignored-output, and immutable oracle-hash checks.
- Repository integrity, workflow contracts, and release-budget contracts passed.

### 2026-08-07 — Task 3 complete

- Added the selection rationale, reproduction guide, independent oracle review method, and graph report.
- The report shows the verified Mermaid topology for all three decisions.
- Updated the root README and persistent project context for the second external corpus.
- The full pull-request gate passed with both external corpora.

## Phase 3 Completion Summary

- Tasks completed: 3 of 3.
- Files modified: PetClinic harness, local and CI verification, documentation, steering context, and SpecOps artifacts.
- Deviations: none.
- Tests: standalone PetClinic, repository integrity, workflow contracts, release budget, and full pull-request gate passed.

## Documentation Review

| File | Status | Review result |
|------|--------|---------------|
| `README.md` | Updated | Adds the PetClinic command and graph-report link. |
| `conformance/spring-petclinic/README.md` | Added | Defines the pin, output, and one-command use. |
| `conformance/spring-petclinic/selection.md` | Added | Explains the three increasing analysis levels. |
| `conformance/spring-petclinic/conformance-report.md` | Added | Shows and explains all three verified Mermaid graphs. |
| `docs/supported-java-constructs.md` | Up-to-date | No production support claim changed. |
| `docs/maven-plugin.md` | Up-to-date | Maven plugin behavior did not change. |
| `.specops/steering/product.md` | Updated | Records both external corpora as validation inputs. |
| `.specops/steering/tech.md` | Updated | Records the PetClinic stack and test duty. |
| `.specops/steering/reference-application.md` | Updated | Records the pin, purpose, and proof boundary. |

## Review Remediation

### 2026-08-07 — Complete production isolation coverage

- Review found that the isolation test did not scan the JDBC storage module.
- Added `fachtracing-storage-jdbc/src/main` to the guarded production paths.
- The focused PetClinic and repository integrity gates passed after the correction.
