# Implementation Journal: Jakarta EE CDI and service semantics

## Summary

Version 2 adds three tasks to the completed adapter. Framework selectors now distinguish
abstention from unproved resolution. Source annotations and exact platform contracts add explicit
container, callback, infrastructure, and remote-peer gaps. CDI runtime confirmation stays passive.

## Phase 1 Context Summary

- Config: SpecOps defaults; vertical `library`; specsDir `.specops`; task tracking `none`.
- Context recovery: existing incomplete specs are unrelated to this feature.
- Steering files: product, tech, structure, reference application, dependencies, and repo map loaded.
- Repo map: existing map loaded; source tree is brownfield.
- Memory: loaded completed specification context and recurring design patterns.
- Vertical: library.
- Affected files: engine dispatch analysis, new Jakarta EE adapter, Maven plugin discovery, conformance scripts, and capability documentation.
- Project state: brownfield.
- Coherence check: pass.
- Vocabulary check: pass.
- Plan validation: new module and conformance paths are intentional; existing engine, script, and documentation paths were validated.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
|---|----------|-----------|------|-----------|
| 1 | Add an unresolved selector result | CDI ownership with unknown deployment state must not fall back to generic inclusion. | Task 4 | 2026-08-18T10:48:20Z |
| 2 | Add source-semantic providers | Container annotations can invoke behavior without a source call. | Task 4 | 2026-08-18T10:48:20Z |
| 3 | Keep exact operations and attach boundary gaps | A known local action does not prove callbacks, infrastructure, or remote processing. | Task 4 | 2026-08-18T10:48:20Z |
| 4 | Keep CDI runtime confirmation passive | Active CDI lookup can create contextual instances, invoke producers, and change application state. | Task 5 | 2026-08-18T10:48:20Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
|---|---|---|---|
| No deviation | Implemented the version 2 design | None | Tasks 4-6 |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
|---|---|---|---|
| Sandbox blocked the default Maven repository | Re-ran verification with approved Maven cache access | No implementation impact | Task 6 |
| GitHub integration cannot edit PR metadata | Kept the existing PR title and body; push and check reads remain available | No code or CI impact | Task 6 |

## Documentation Review

- `docs/maven-plugin.md`: updated with proof states, runtime CDI ownership, and boundary gaps.
- `docs/supported-java-constructs.md`: updated with supported and explicit-gap capabilities.
- `docs/java-capabilities.json`: updated with executable contracts for new gaps.
- `.specops/steering/repo-map.md`: refreshed with the new engine and adapter providers.

## Phase 2 Completion Summary

- Requirements: distinguish proven CDI selection, unresolved framework ownership, source-driven
  container behavior, and incomplete exact API boundaries.
- Design: one selector proof state, one source-semantic SPI, and one gap set on exact contracts.
- Tasks: three version 2 tasks with no new dependency.

## Phase 3 Completion Summary

- Tasks implemented: 3 of 3 version 2 tasks.
- Files modified: engine extension points and analyzer, Jakarta EE providers and fixtures, Maven
  discovery, capability documentation, and specification records.
- Deviations: none.
- Local verification: `./scripts/verify.sh` and `./scripts/verify-pr.sh` passed. Mega, Spring
  PetClinic, and Jakarta EE REST conformance passed.

## Session Log

- 2026-08-18T09:25:40Z: Selected and pinned the external `hantsy/jakartaee-rest-sample` corpus at `85da1d6861fea14579b1c6eb76253f0549a8e80f`.
- Task 1 scope: Add one optional dispatch-candidate selector extension. It must preserve generic dispatch when no selector applies and report selector conflicts as explicit gaps.
- Task 1 complete: The engine selector SPI preserves generic dispatch without a selector and records excluded or conflicting framework decisions.
- Task 2 complete: The optional adapter selects scoped CDI field injection targets, excludes unscoped and alternative candidates, and verifies exact Jakarta EE, SOAP, and gRPC signatures without production framework dependencies.
- Task 3 complete: The clean pinned `hantsy/jakartaee-rest-sample` corpus runs in the pull-request gate and proves that `TaskResources.allTasks` reaches `JpaTaskRepository` through CDI dispatch.
- Verification: `./scripts/verify-pr.sh` passed on 2026-08-18.
- Follow-up: Expanded the exact catalog with EJB, Jakarta Security, REST Client, JMS, JSON-B,
  Mail, Servlet, and WebSocket operations. Reflection verification passed for each catalog entry.
- 2026-08-18T10:48:20Z: Reopened the specification as version 2. Added explicit unresolved CDI, source-semantic, external-boundary, and passive runtime confirmation requirements.
- 2026-08-18T11:03:07Z: Full local pull-request gate passed, including all three external conformance corpora.
- 2026-08-18T11:04:00Z: Pushed implementation commit `22ed14c` to pull request 31.
- 2026-08-18T11:08:32Z: GitHub Actions run 116 passed all five jobs for the implementation commit.
