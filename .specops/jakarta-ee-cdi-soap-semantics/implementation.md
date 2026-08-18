# Implementation Journal: Jakarta EE CDI and service semantics

## Summary

Version 2 extends the completed adapter with a truthful Jakarta EE completeness contract. It does not claim full container emulation.

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

## Deviations from Design

| Planned | Actual | Reason | Task |
|---|---|---|---|

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
|---|---|---|---|

## Documentation Review

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
