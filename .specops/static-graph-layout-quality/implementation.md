# Implementation Journal: Static Graph Layout Quality

## Summary

Specification only. No product implementation has started.

## Phase 1 Context Summary

- Config: defaults because `.specops.json` does not exist; `specsDir=.specops`; task tracking none; evaluation enabled
- Context recovery: started a focused high-severity bugfix spec; `frontend-flow-explorer` is completed
- Scope boundary: static graph topology and presentation only; run selection, current step, executed path, and evidence are excluded
- Steering files: loaded `product.md`, `tech.md`, `structure.md`, `reference-application.md`, `dependencies.md`, and `repo-map.md`
- Repo map: fresh; the stored source hash matches the current source tree
- Memory: loaded completed-spec context, decisions, and recurring patterns; no production learning file exists
- Vertical: frontend
- Affected files: graph topology analysis, node placement, route planning, node and edge presentation, canvas composition, graph tests, browser tests, and viewer documentation
- Project state: brownfield
- Existing dependencies: Svelte 5, SvelteKit, Svelte Flow, ELK, Tailwind CSS v4, shadcn-svelte, Bits UI, Lucide, Vitest, and Playwright
- Audit evidence: four current large-graph screenshots, the prior visual audit, current graph implementation, current graph contracts, and current unit and Playwright tests
- Current root causes: fixed north and south ports, one source and target side, no topology-analysis stage, no branch-region model, no convergence or crossing model, and route tests that do not measure structural quality
- Vocabulary check: pass
- Plan validation: pass; existing paths resolve, and new focused files are marked `(create)` in tasks

## Phase 2 Completion Summary

- Defined 67 accepted findings for structural shape, route choice, branch meaning, fan-in, topology comprehension, duplicate context, visual grammar, and local reasoning.
- Excluded the seven active-run findings because the user identified this screen as a static graph view.
- Defined a topology-first layout pipeline with ELK node placement and a deterministic four-side orthogonal router.
- Defined presentation-only junction, shared-trunk, crossing, region, and route metadata without a graph-contract change.
- Defined numeric geometry, semantic, visual, accessibility, regression, and human-approval gates.
- Split implementation into six ordered, single-responsibility tasks.
- Approved no new dependency.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Create a focused static-graph bugfix spec instead of adding more requirements to the broad visual-design spec. | The user excluded run state, and the repository requires one responsibility per change. | Spec | 2026-08-25T07:29:57Z |
| 2 | Keep ELK for ranked node placement and remove final route ownership from ELK. | ELK supplies mature layered ordering. The current defect needs explicit topology, ports, routing, convergence, and crossing control. | Tasks 2 and 3 | 2026-08-25T07:39:57Z |
| 3 | Use all four node sides and compare every valid orthogonal route candidate. | Fixed north and south ports cause remote starts, avoidable hooks, and long routes. | Task 3 | 2026-08-25T07:39:57Z |
| 4 | Add junctions, trunks, regions, and crossings as presentation-only data. | The layout must explain geometry without changing the supplied graph topology. | Task 5 | 2026-08-25T07:39:57Z |
| 5 | Keep the shared canvas and treat run highlighting as regression behavior only. | Static preview and run detail must use the same topology, but this bugfix adds no run-state requirement. | Task 6 | 2026-08-25T07:39:57Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
| --- | --- | --- | --- |
| The live npm advisory request could not run in the restricted environment. | Reused the prior passing audit for the unchanged lockfile and recorded the limitation in `dependency-audit.md`. | No dependency is added by this specification. | Spec |

## Documentation Review

Specification phase only. Viewer documentation changes are part of Task 6.

## Session Log

### Session 1 — Static graph layout specification (2026-08-25)

- Reviewed the four supplied large-graph screenshots and the accepted visual audit.
- Confirmed that active-run state does not apply to the static graph view.
- Inspected the graph layout pipeline, graph contracts, Svelte Flow presentation, unit tests, and Playwright journeys.
- Created the high-severity static graph layout bugfix specification. No product code changed.

