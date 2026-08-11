# Implementation Notes: Generic Business Graph Projection

## Summary

Implemented a separate business-only model, deterministic projection, strict vocabulary guard,
format-specific renderers, JSON V1 export, and Draft 2020-12 schema. Maven now writes the business
artifacts first and keeps the exact structure and optional developer JSON artifacts.

## Phase 1 Context Summary

- Config: SpecOps defaults; library vertical; no task tracking.
- Context recovery: from-plan conversion.
- Conversion source: inline approved plan.
- Steering directory: verified.
- Memory directory: verified.
- Vertical: Java library and Maven plugin.
- Affected files: engine models, business projection and exporters, Maven graph generation, tests, and docs.
- Project state: brownfield.
- Scope assessment: one focused projection pull request after the contract dependency.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Keep terminal results as distinct nodes for each exact terminal edge. | This preserves each successful, correction, and failure path even when two paths have the same label. | 1 | 2026-08-11T10:03:47Z |
| 2 | Collapse loop bodies into one existence rule. | Business readers need the searched condition, not iteration variables or back edges. | 1 | 2026-08-11T10:03:47Z |
| 3 | Use one class per renderer, exporter, guard, and projector. | This keeps format and policy responsibilities separate. | 2 | 2026-08-11T10:03:47Z |
| 4 | Keep structure artifacts and write business artifacts additively. | Runtime correlation and current developer consumers keep their exact graph contract. | 3 | 2026-08-11T10:03:47Z |

## Verification

- Business projection executable contracts passed.
- Maven generator and JSON parser contracts passed.
- `./scripts/verify-pr.sh` passed, including Mega and Spring PetClinic conformance.
- Local PostgreSQL verification was skipped because no connection was configured.
