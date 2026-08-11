# Implementation Notes: Spring PetClinic Business Conformance

## Summary

Implemented three complete PetClinic business workflows with an annotation-only overlay. The harness now writes business Mermaid, PlantUML, and JSON first. It keeps exact structure artifacts as technical developer output.

The implementation adds reusable contextual contracts for application-declared Spring Data page queries. It also refines generic loop projection and result labels without changing the exact Mega graphs.

## Phase 1 Context Summary

- Config: SpecOps defaults; library vertical; no task tracking.
- Context recovery: from-plan conversion.
- Conversion source: inline approved plan.
- Steering directory: verified.
- Memory directory: verified.
- Vertical: Java conformance harness.
- Affected files: PetClinic overlay, harness, oracles, report, isolation test, generic projection, and Spring contracts.
- Project state: brownfield.
- Scope assessment: final conformance pull request after all generic capabilities.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Treat Spring binding and `@Valid` results as method inputs. | These operations run before the annotated controller method. | 1 | 2026-08-11T11:09:17Z |
| 2 | Match derived page queries only for proven Spring Data repository subtypes. | Application repository methods need general Spring semantics without PetClinic names. | 2 | 2026-08-11T11:09:17Z |
| 3 | Move a folded loop rule to its aggregate predicate in the business projection. | Business order must follow short-circuit meaning while the exact graph stays unchanged. | 2 | 2026-08-11T11:09:17Z |
| 4 | Commit reviewed business JSON only. | Generated diagrams stay reproducible build output; JSON equality protects business meaning. | 2 | 2026-08-11T11:09:17Z |

## Verification

- `./scripts/verify.sh` passed.
- `./scripts/verify-pr.sh` passed, including the five unchanged Mega graphs and three complete PetClinic graphs.
- All three PetClinic JSON files matched their reviewed oracles and passed schema validation.
- PostgreSQL 18.4 passed locally in a temporary container: `POSTGRES_JDBC_OK`.
- All four stacked draft pull requests passed the hosted PR and PostgreSQL gates.
