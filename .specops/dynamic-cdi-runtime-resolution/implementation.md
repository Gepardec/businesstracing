# Implementation Journal: Dynamic CDI runtime resolution

## Summary

The implementation adds one runtime-observable selector state. Dynamic CDI keeps compatible
candidate probes while other unresolved CDI mechanisms stay fail-closed. A real Weld SE container
proves two qualifier-selected paths through the Java agent.

## Phase 1 Context Summary

- Config: SpecOps defaults; vertical `library`; specsDir `.specops`; task tracking `none`.
- Context recovery: `release-gate-timeout-budget` is incomplete and unrelated.
- Steering files: product, technology, structure, and repository map loaded.
- Repo map: existing brownfield map loaded.
- Memory: completed feature patterns loaded.
- Affected files: engine dispatch analysis, Jakarta EE tests, PR verification, documentation, and specification records.
- Project state: brownfield.
- Scope assessment: one cohesive feature; no decomposition needed.
- Coherence check: requirements and design use the same passive runtime contract.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Retain a conservative candidate superset for unresolved CDI | Runtime cannot confirm an implementation that has no target probe. | Task 1 | 2026-08-18T12:00:56Z |
| 2 | Observe the real container decision | A second CDI lookup could change state or disagree with the application call. | Task 2 | 2026-08-18T12:00:56Z |

## Session Log

- 2026-08-18T12:00:56Z: Started the stacked follow-up from pull request 31.
- 2026-08-18T12:00:56Z: Selected Weld SE 6.0.4.Final for CDI 4.1 and Jakarta EE 11 test conformance.
- 2026-08-18T12:05:32Z: Weld selected the EU and US beans and produced two isolated complete traces.
- 2026-08-18T12:07:00Z: The pull-request gate passed Mega, PetClinic, Jakarta EE REST, and the new dynamic CDI conformance.
- 2026-08-18T12:09:00Z: Pinned Keycloak conformance passed with a complete 128-node exact graph.

## Deviations from Design

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |
| Reuse `UNRESOLVED` for retained candidates | Added `RUNTIME_OBSERVABLE` | A focused test showed that reuse also retained XML-selected alternatives. The separate state keeps the change limited to dynamic lookup. | Task 1 |

## Documentation Review

- `README.md`: updated with the dynamic CDI conformance command.
- `docs/maven-plugin.md`: updated with passive dynamic selection and remaining static limits.
- `docs/supported-java-constructs.md`: updated with the runtime-observable CDI contract.
- `docs/java-capabilities.json`: updated with the executable conformance contract.
- `.specops/steering/repo-map.md`: refreshed for the new conformance source.

## Phase 2 Completion Summary

- Requirements: six acceptance criteria cover static candidates, runtime edge confirmation, real CDI, CI, and dependency scope.
- Design: one selector state, existing implementation-entry probes, and one real-container harness.
- Coherence and vocabulary checks: pass.

## Phase 3 Completion Summary

- Tasks completed: 3 of 3.
- Local verification: focused CDI assertions, dynamic Weld conformance, repository verification,
  the full pull-request gate, and pinned Keycloak conformance passed.
- Publication: commit `819b061` opened stacked pull request 32 against the pull request 31 branch.
- GitHub Actions: run 32135897003 passed all five jobs, including the Weld-enabled core gate.
