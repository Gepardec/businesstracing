# Implementation Journal: Generic Java Extractor Completion

## Phase 1 Context Summary

- Config status: `.specops.json` is absent; SpecOps 1.8.0 defaults apply with `.specops`, library
  vertical, no task tracker, no review pause, conservative dependency safety, and no auto-commit.
- Context recovery: no incomplete specification exists. The release candidate and remediation
  version 4 are complete on branch `codex/generic-application-readiness`.
- Steering loaded: dependency safety, product, reference application, structure, technology, and
  the refreshed generated repository map.
- Repo map: stale by source-list hash at session start; refreshed for 153 non-SpecOps project files.
- Memory loaded: 27 decisions from eight specs and recurring runtime-correlation,
  source-provenance, and Maven-adapter patterns. No production learnings file exists.
- Detected vertical: library.
- Project state: brownfield Maven multi-module Java 21 library.
- Affected areas: analyzer control flow, manifest and activation models, bytecode transformer,
  runtime collector, async carrier API, Maven source and JPMS inputs, JDBC tests, CI scripts,
  capability contracts, external integration, Mega conformance, documentation, and load evidence.
- Scope assessment: all five decomposition signals are present. Non-interactive SpecOps mode keeps
  one specification; `requirements.md` records the six-spec manual proposal.

## Implementation Plan

1. Freeze capability contracts and correct the V3 JavaDoc.
2. Implement source control flow, exact predicates, and exact switches.
3. Implement proven dynamic invocation and controlled bytecode fallback.
4. Implement automatic async propagation and owned JPMS sources.
5. Add PostgreSQL and pull-request CI.
6. Run generic, external, Mega, PostgreSQL, and long-load release evidence.

## Regression Risk Analysis

| Behavior to preserve | Risk | Gate |
| --- | --- | --- |
| One Start and one Stop with explicit returned value | Must-Test | Existing analyzer and Mega oracles |
| Honest incomplete status for unsupported relevant logic | Must-Test | Negative capability matrix |
| Activation V2 read and V3 source-free runtime | Must-Test | API and external release integration |
| Exact simple branches and overload-safe descriptors | Must-Test | Analyzer and transformer contracts |
| No result change or exception replacement | Must-Test | Agent contracts and load gate |
| Multi-graph, nested, and concurrent isolation | Must-Test | Runtime and agent concurrency contracts |
| Project-aware flat, JPMS, and mixed compilation | Must-Test | Maven and analyzer compiler fixtures |
| Strict durable identity and honest delivery counters | Must-Test | H2, PostgreSQL, and protocol contracts |
| Business artifacts contain no Java or Mega details | Must-Test | Privacy and forbidden-reference scans |
| Manual async wrappers remain valid | Must-Test | Existing explicit propagation contract |

## Dependency Audit Summary

- Existing production dependencies stay unchanged.
- Proposed pgJDBC 42.7.13 is test scope only. The official pgJDBC download page lists it as the
  current Java 8+ release. OSV returned no advisory for this Maven coordinate on 2026-08-05.
- PostgreSQL 18.4 is supported through November 2030. It contains the May 2026 security fixes.
- Official GitHub actions use checkout V7 and setup-java V5. No third-party action is introduced.

## Decision Log

| # | Decision | Rationale | Task | Date |
| --- | --- | --- | --- | --- |
| 1 | Lower compound Boolean expressions to atomic graph nodes | Exact runtime paths require one edge per evaluated business fact, including short-circuit skips. | 3 | 2026-08-05 |
| 2 | Use source artifacts before a fail-closed ASM fallback | Source keeps business vocabulary and semantics; bytecode is safe only in a small proven subset. | 6 | 2026-08-05 |
| 3 | Inject async wrappers at application call sites | This avoids bootstrap instrumentation and removes manual wrapping for standard APIs. | 7 | 2026-08-05 |
| 4 | Make module ownership an explicit source input | A file path alone cannot be assigned safely to a named or automatic module. | 8 | 2026-08-05 |
| 5 | Test JDBC against PostgreSQL without a production driver dependency | A test-scope driver proves production database behavior while the adapter stays vendor-neutral. | 9 | 2026-08-05 |
| 6 | Retain each proven atomic path and add a gap for each unproven atom | Exact evidence stays useful, but the runtime never infers an edge for an atom that bytecode correlation cannot prove. | 3 | 2026-08-05 |
| 7 | Model structured exits before graph projection | Normal, return, throw, catch, and finally effects need one control-flow model before business-only rendering. | 2 | 2026-08-05 |
| 8 | Select dynamic targets only from static candidates with runtime evidence | A proxy, provider, or reflected member can select known logic, but an unknown or ambiguous target must stay a gap. | 5 | 2026-08-05 |
| 9 | Limit bytecode extraction to fingerprinted side-effect-free methods | A small ASM subset can prove result-relevant binary logic without exposing bytecode vocabulary or claiming unsupported behavior. | 6 | 2026-08-05 |
| 10 | Keep the production JDBC adapter driver-neutral | PostgreSQL proof uses a test-scope driver and an explicit integration command, while applications continue to supply a standard data source. | 9 | 2026-08-05 |

## Session Log

- 2026-08-05: Created the follow-up feature specification from the release review. No gap is closed
  by documentation alone. Each cluster has independent executable contracts and the full original
  generality, Mega, external activation, PostgreSQL, CI, and performance gates remain mandatory.
- 2026-08-05: Tasks 1 through 4 completed. Independent contracts now cover structured exception
  flow, safe resource closure, synchronized business logic, atomic Boolean short circuits,
  ternaries, and integral, string, enum, and pattern switches. Runtime observations select one
  typed edge for each evaluated atom or one exact switch choice.
- 2026-08-05: Tasks 5 and 6 completed. Runtime evidence selects proven proxy, service-provider, and
  reflected candidates. A fingerprinted ASM fallback handles only simple parameters, configured
  fields, calculations, comparisons, branches, and returns. Unknown targets and unsafe bytecode
  stay source-located coverage gaps.
- 2026-08-05: Tasks 7 and 8 completed. The agent injects idempotent context wrappers for standard
  executors, completion stages, platform threads, and virtual threads. External sources can declare
  named or automatic module ownership, and Maven places them in the correct compiler context.
- 2026-08-05: Tasks 9 through 11 completed. PostgreSQL 18.4 passed migration, idempotency, conflict,
  query, retention, rollback, retry, and timeout tests. Pull-request CI runs standard, external,
  Mega, PostgreSQL, and long-load checks. Capability and integration documents state the supported
  subsets and the controlled gaps.
- 2026-08-05: Task 12 completed. Standard verification passed with 0.160% p95 overhead. The pinned
  Mega run produced five complete graphs from 420 sources. The clean-clone release gate completed
  600,000 decisions at 1,000 RPS with 6.126% p95 overhead and zero errors, result mismatches,
  dropped records, or trace contamination.

## Deviations

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |
| Reject a whole exact plan when one compound atom cannot be correlated | Retain every proven atom and add an execution gap for each unproven atom | This gives exact available evidence without guessing the missing edge. It also removes the legacy generic `evaluated` observation. | 3 |

## Blockers

- None.

## Summary

All 12 tasks are complete. The analyzer now handles the required generic exception, synchronized,
Boolean, ternary, switch, dynamic invocation, binary fallback, asynchronous, and mixed-JPMS
subsets. Unsupported variants remain precise gaps. The implementation uses no Mega-specific
production logic. PostgreSQL 18.4, five pinned Mega graphs, the source-free external integration,
and the 600-second 1,000-RPS release gate all passed.

## Documentation Review

| File | Status | Notes |
| --- | --- | --- |
| `README.md` | Updated | Names exact path, dynamic, bytecode, asynchronous, and module-ownership support. |
| `docs/supported-java-constructs.md` | Updated | Defines each complete subset and each controlled gap without broad Java claims. |
| `docs/runtime-integration.md` | Updated | Documents automatic standard asynchronous propagation and gap behavior. |
| `docs/maven-plugin.md` | Updated | Shows named and automatic external module ownership. |
| `docs/jdbc-storage.md` | Updated | Documents the PostgreSQL integration command and driver-neutral production contract. |
| `docs/release-evidence.md` | Updated | Records PostgreSQL, external, Mega, and 600-second release evidence. |
| `docs/java-capabilities.json` | Updated | Binds every new capability to an independent executable contract. |
