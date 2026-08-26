# SpecOps Run: Business Graph Semantic Explanation

- **Started:** 2026-08-26T09:31:16Z
- **SpecOps:** 1.8.0
- **Mode:** Specification only
- **Status:** Draft created

## Context

- Read project steering and repository map.
- Read the current exact-to-business projector, language normalizer, label normalizer, summarizer,
  artifact guard, JSON exporter, model, and relevant analyzer label paths.
- Inspected both supplied business graph JSON documents.
- Confirmed that both graphs are structurally complete and semantically poor.
- Recorded the supplied labels and the lost child-subject example.

## Scope Decision

Use one bugfix specification for the semantic projection pipeline. Do not change viewer layout,
runtime storage, JSON V1 shape, CI configuration, or production dependencies.

## Artifacts

- `bugfix.md`
- `design.md`
- `tasks.md`
- `implementation.md`
- `dependency-audit.md`
- `evaluation.md`
- `spec.json`

## Product Changes

None. Implementation has not started.

## User Correction — Domain Neutrality

- Removed the proposed project-vocabulary path from the solution.
- Required the two insurance examples to work without an insurance adapter, glossary, contract
  provider, configuration file, label map, or expected graph shape.
- Required the same attributed-symbol, ownership, call-binding, dataflow, polarity, materiality, and
  reduction algorithms for unrelated domains.
- Kept exact semantic contracts only for their existing source-unavailable method boundary.
- Required an honest semantic gap when generic analysis cannot prove a complete statement.
