# Implementation Journal: Business Graph Terminal Semantics

## Phase 1 Context Summary

- Config: defaults; library vertical; review and external task tracking disabled
- Context: completed generic extraction and Mermaid specs loaded from memory
- Affected files: static analyzer, graph/probe builder, renderer snapshots/tests, Mega oracles/artifacts, documentation
- Scope assessment: one cohesive graph semantics change; no decomposition
- Dependency gate: both required specs are completed; no new dependency

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Reuse one Stop node with multiple outcome probes | Static topology stays business-simple while every bytecode return still records the typed result. | 1 | 2026-07-24T11:18:08Z |
| 2 | Strip only standalone id/ids tokens | This removes technical persistence vocabulary generically without guessing domain synonyms. | 1 | 2026-07-24T11:18:08Z |
| 3 | Translate result-relevant null comparisons to absent/exists | Optionality can be a business reason, but Java's null spelling is an implementation detail. | 1 | 2026-07-24T11:18:08Z |

## Phase 2 Completion Summary

- Requirements: one Start/Stop, convergent terminals, explicit returned values, generic identifier cleanup
- Design: shared Stop ID with multiple probes and terminal-edge statements
- Task: one implementation/verification unit
- Dependencies: completed graph and Mermaid features; no external packages

## Phase 3 Completion Summary

- Implemented one shared Start/Stop topology with per-return probes and explicit return edges.
- Routed root and statically expanded callee failures to Stop without normal continuation.
- Removed standalone identifier vocabulary and translated meaningful null comparisons to absent/exists.
- Updated snapshots, generic fixtures, immutable Mega oracles, generated PlantUML/Mermaid, and documentation.
- `./scripts/verify.sh` passed at 1,000 RPS with 5,000 captures, 0.112% p95 overhead, and zero errors.
- `./scripts/verify-mega-backend.sh` passed all five exact graphs and the real polymorphic runtime path.
