# Implementation Journal: Compact Graph Reading and Business Phrasing

## Summary

The compact viewer now selects a bounded topology slice before layout. It keeps every outgoing
alternative and uses spare capacity for stable incoming context. Generated aggregate rules now use
source-derived role punctuation instead of mixed-language sentence filler.

## Phase 1 Context Summary

- Config: defaults; no `.specops.json`; full-stack vertical; no task tracker.
- Context recovery: the two related correction specs are complete. This is a focused follow-up.
- Steering files: loaded six files, including product, technology, structure, dependencies,
  reference application, and repo map.
- Repo map: loaded. Its affected module declarations are current for this change.
- Memory: loaded prior decisions and recurring graph-viewer patterns.
- Affected files: `graph-viewport.ts`, `FlowCanvas.svelte`, viewer tests,
  `StaticDecisionAnalyzer.java`, and analyzer tests.
- Project state: brownfield.
- Scope assessment: two code domains are present, but they form one acceptance slice on the same
  generated graphs. Splitting would permit readable cards with unclear text, or clear text with an
  unreadable compact view. One correction is used.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Keep all direct successors in compact view. | The viewer must not hide a decision alternative. | 2 | 2026-08-26T16:12:51Z |
| 2 | Add stable predecessors only after all successors and within a three-card budget. | This limits card count without hiding an alternative. | 2 | 2026-08-26T16:12:51Z |
| 3 | Use punctuation, not a language or domain dictionary. | Source terms stay exact and the code remains generic. | 3 | 2026-08-26T16:12:51Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |
| Keep label anchors within 32 layout pixels. | Permit a bounded 48-pixel leader line. | One dense real overview had no collision-free label position inside 32 pixels. The 48-pixel bound removes the collision and remains attached. | 4 |

## Blockers Encountered

The first Maven compile found an invalid method reference in the nested scanner. It was replaced
with an explicit lambda. The next compile and executable contracts passed.

## Documentation Review

- `README.md`: up to date; no public setup or workflow changed.
- `docs/`: up to date; no API, JSON, storage, or user workflow changed.
- `AGENTS.md`: checked and followed; no update required.
- CI files: not changed, as requested.

## Session Log

- 2026-08-26: Reproduced the compact zoom and mixed-language aggregate label from the prior real
  graph evidence. Started implementation.
- 2026-08-26: Added compact topology selection, neutral aggregate rendering, and a source-independent
  renderer contract.
- 2026-08-26: Regenerated both supplied graphs. The geometry review found one notification label
  collision; bounded label placement removed it.
- 2026-08-26: Completed Maven, executable engine, viewer unit, Svelte, build, graph-review, and full
  browser gates. Inspected the final wide, compact, and overview screenshots.
