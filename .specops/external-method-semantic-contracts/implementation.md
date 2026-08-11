# Implementation Notes: External Method Semantic Contracts

## Summary

Completed all three tasks. The engine now accepts exact method-level semantic providers, validates
conflicts without priority, and uses one proven contract for result slicing and business labels.
Source-visible code remains authoritative. Existing opaque-boundary and gap behavior remains intact.

## Phase 1 Context Summary

- Config: SpecOps defaults; library vertical; `.specops`; no external task tracking.
- Context recovery: no incomplete specification.
- Conversion source: inline approved plan.
- Steering directory: verified; six always-included files loaded.
- Memory directory: verified.
- Vertical: Java library.
- Affected files: engine analysis API, analyzer, tests, and analysis documentation.
- Project state: brownfield.
- Scope assessment: decomposed into four ordered specifications and pull requests.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Match exact dotted binary owners, method names, and JVM descriptors. | Exact keys prevent a framework adapter from silently applying facts to an overload or unrelated type. | Task 1 | 2026-08-11T09:25:00Z |
| 2 | Represent provider ambiguity as a normal coverage gap. | A priority rule could report an incorrect complete graph when two trusted catalogs disagree. | Task 2 | 2026-08-11T09:29:00Z |

## Phase 2 Completion Summary

- Requirements: exact facts, precedence, conflict behavior, fallback, and isolation.
- Design: separate method reference, contract, provider, registry, and analyzer duties.
- Dependencies: none added.
- Evaluation: all specification dimensions passed.

## Phase 3 Completion Summary

- Tasks completed: 3 of 3.
- Production changes: four generic contract types, immutable request configuration, and analyzer use.
- Tests: exact, absent, conflict, source precedence, mutation, predicate, and caught-exception cases.
- Verification: `./scripts/verify-pr.sh` passed, including both pinned conformance suites and the load test.
- PostgreSQL: skipped locally because no connection was configured; the hosted CI gate remains required.

## Documentation Review

| Document | Status | Result |
| --- | --- | --- |
| `docs/supported-java-constructs.md` | Updated | Defines exact contracts, precedence, conflicts, and the trust boundary. |
| `docs/maven-plugin.md` | Updated | Shows programmatic provider configuration and separates later Maven loading. |
| `README.md` | Up-to-date | The framework-neutral product description remains correct. |

## Session Log

- 2026-08-11T09:17:19Z — Converted the approved plan into the four-spec initiative.
- 2026-08-11T09:25:00Z — Task 1 completed. Added exact immutable contracts and registry tests.
- 2026-08-11T09:29:00Z — Task 2 completed. Applied contracts after source and before opaque fallback.
- 2026-08-11T09:44:03Z — Task 3 completed. Exact catch filtering and full pull-request verification passed.
