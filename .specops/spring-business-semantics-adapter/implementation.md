# Implementation Notes: Spring Business Semantics Adapter

## Summary

Implemented an optional Java service provider with exact Spring method signatures. Production code
depends only on the generic engine. Tests use Spring Framework 7.0.8 and Spring Data 4.1.0 to verify
each symbolic method reference and general controller behavior.

## Phase 1 Context Summary

- Config: SpecOps defaults; library vertical; no task tracking.
- Context recovery: from-plan conversion.
- Conversion source: inline approved plan.
- Steering directory: verified.
- Memory directory: verified.
- Vertical: Java integration library.
- Affected files: parent reactor, new Spring adapter, adapter tests, and Maven documentation.
- Project state: brownfield.
- Scope assessment: one optional adapter pull request after the generic contracts and projection.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Load providers from the Maven plugin realm. | A normal plugin dependency can enable the adapter without a Spring dependency in the engine or plugin. | 1 | 2026-08-11T10:18:03Z |
| 2 | Catalog inherited symbolic owners where Maven or Java can emit them. | Exact owner, name, and descriptor matching must cover valid Page, repository, and BindingResult invocation owners. | 1 | 2026-08-11T10:18:03Z |
| 3 | Use possible persistence exceptions on save contracts. | Compatible catch paths become explicit without trusting the persistence implementation. | 2 | 2026-08-11T10:18:03Z |
| 4 | Use contract labels inside negated and comparison predicates. | Page cardinality and negated utility calls must keep the reusable business label. | 2 | 2026-08-11T10:18:03Z |

## Verification

- Every catalog entry resolved against real Spring test APIs.
- Supported general Spring fixtures were complete; one unmatched method stayed incomplete.
- Maven plugin-realm service discovery passed.
- `./scripts/verify-pr.sh` passed, including Mega and Spring PetClinic conformance.
- Local PostgreSQL verification was skipped because no connection was configured.
