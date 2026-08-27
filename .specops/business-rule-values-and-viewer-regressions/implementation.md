# Implementation Notes: Business Rule Values and Viewer Regressions

## Phase 1 Context Summary

- SpecOps configuration: defaults; no `.specops.json` exists.
- Context recovery: related semantic and real-layout specs are complete.
- Steering loaded: product, technology, structure, reference application, dependencies, and repo map.
- Memory loaded: prior graph-generation and viewer decisions.
- Vertical: full stack.
- Project state: brownfield.
- Affected areas: Java static analysis, business projection, Svelte node presentation, and route labels.
- Scope: one regression because the same generated graphs are the final acceptance evidence.

## Decision Log

| # | Decision | Rationale | Task | Date |
| --- | --- | --- | --- | --- |
| 1 | Keep scalar evidence internal and keep business JSON V1. | The current contract can carry the improved text without a format migration. | 2 | 2026-08-26 |
| 2 | Omit ambiguous values. | A missing value is safer than a false business rule. | 2 | 2026-08-26 |
| 3 | Use one shared node size for CSS and layout. | Route geometry must match the visible node box. | 3 | 2026-08-26 |

## Progress

Implementation completed after the defects were reproduced and checked on both supplied graphs.

## Completion Summary

- Added `semantic.sourceValue` as internal evidence. The resolver follows source-visible method and
  implementation calls. It accepts a configuration value only when the rule reads the matching
  field or key, and only when one distinct value remains.
- Appended the proved value to a business predicate without changing business JSON V1. The real
  rule is now `svnr ist unter altersgrenze (18)`.
- Added generic positive, ambiguous-value, and unrelated-constant contracts. No application name,
  insurance term, or literal threshold exists in production logic.
- Increased graph cards to 112 px, allowed four label lines, reduced selection to one border and
  shadow, and kept edge labels at least 30 layout pixels from endpoints.
- Observed responsive overlay size changes and fitted compact local context above the bottom guide.

## Deviations

| Planned | Actual | Reason |
| --- | --- | --- |
| Collect values from relevant immutable initializers. | Require a field-name or string-key match between the source method and initializer. | This prevents an unrelated numeric constant from entering a business rule. |
| A 0.5 compact context floor. | Use 0.48 only for compact safe rectangles. | The real seven-node neighborhood fits at 0.497. A strict 0.5 comparison incorrectly fell back to one-node framing and overlapped the bottom guide. |

## Verification

- Analyzer executable contract: pass, including one proved value, ambiguous values, and an
  unrelated constant.
- Maven compile and test phases: pass for the API and engine reactor.
- Viewer unit tests: 18 files and 87 tests pass.
- Svelte diagnostics: 0 errors and 0 warnings.
- Production build: pass; only existing third-party XYFlow and D3 warnings remain.
- Real graph review: both supplied V1 files pass with zero overlaps, node intrusions, label
  collisions, detached labels, wrong-way exits, avoidable crossings, and avoidable detours.
- Playwright production review: both real graphs pass Explore, selection, branches, Overview, Full
  detail, dark mode, and narrow responsive geometry.
- Manual screenshot review confirms one selection border, complete labels, visible `(18)`, clear
  outcome routes, and all declared result nodes.
