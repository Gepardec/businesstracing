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

## Session Log

- 2026-08-05: Created the follow-up feature specification from the release review. No gap is closed
  by documentation alone. Each cluster has independent executable contracts and the full original
  generality, Mega, external activation, PostgreSQL, CI, and performance gates remain mandatory.

## Deviations

- None.

## Blockers

- None.

## Summary

Implementation has not started.

## Documentation Review

- Pending until implementation identifies the exact supported subsets and configuration surface.

