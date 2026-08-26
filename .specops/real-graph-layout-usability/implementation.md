# Implementation Journal: Real Graph Layout Usability

## Summary

Specification complete. No product code has been changed.

## Phase 1 Context Summary

- Config: defaults because `.specops.json` does not exist; `specsDir=.specops`; task tracking none; evaluation enabled
- Context recovery: `frontend-visual-design-quality` remains a broad draft; this work creates a focused follow-up to completed `static-graph-layout-quality`
- Scope boundary: static graph preview only; active run, current step, path highlighting, evidence, dashboard, server, database, HTTP, CI, and production benchmarking are excluded
- Steering files: loaded `dependencies.md`, `product.md`, `reference-application.md`, `repo-map.md`, `structure.md`, and `tech.md`
- Repo map: fresh on the same clean source tree and current day
- Memory: loaded 52 indexed specs, 131 decisions, and 19 recurring patterns; no production-learnings file exists
- Vertical: frontend
- Project state: brownfield
- Affected files: graph topology, placement, routing, quality metrics, worker lifecycle, viewport controls, node and edge presentation, graph preview tests, and viewer documentation
- Existing dependencies: Svelte 5, SvelteKit, Svelte Flow, ELK, Tailwind CSS v4, shadcn-svelte, Bits UI, Lucide, Vitest, and Playwright
- Evidence: three complete business graph V1 files with 19, 45, and 55 nodes; current browser measurements and layout metrics recorded in `bugfix.md`
- Scope assessment: one cohesive acceptance slice; no decomposition because all deliverables share the same static graph pipeline and cannot meet the user outcome independently
- Coherence check: pass; the 12-pixel reading floor maps to minimum zoom 0.86, Overview explicitly permits lower zoom with topology-level detail, and the four-second limit is labelled as a local POC gate
- Dependency decision: no new dependency
- Plan validation: pass; existing file paths resolve and all new files are marked `(create)` in `tasks.md`

## Phase 2 Completion Summary

- Defined eight requirement groups for layout state, Reading and Overview, balanced placement, route simplicity, selection, safe controls, generic evidence, and POC responsiveness.
- Recorded reproducible hashes and current measurements for the three supplied real graphs without copying their topology into source files.
- Designed a deterministic placement-profile stage that preserves full ELK coordinates and a route-set refinement stage with candidate-relative detour.
- Defined a generic file-driven review command and topology-equivalent generated tests so acceptance does not depend on one local directory.
- Split work into six ordered single-responsibility tasks.
- Excluded active-run behavior and CI as requested.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Create a focused real-graph usability bugfix instead of reopening the completed static-layout spec. | The completed spec established the architecture. The supplied real graphs expose measurable acceptance gaps that require a follow-up. | Spec | 2026-08-25T09:48:51Z |
| 2 | Keep the broad visual-design draft related but not required. | This work is limited to static graph behavior and must not pull in run, dashboard, or mobile-sheet redesign. | Spec | 2026-08-25T09:48:51Z |
| 3 | Separate Reading view from Overview. | A complete fit cannot make a 7,112-pixel graph readable. Both user tasks must exist explicitly. | Task 2 | 2026-08-25T09:48:51Z |
| 4 | Preserve full ELK placement candidates instead of reconstructing flat global ranks. | The current reconstruction causes the extreme wide and tall layouts in the evidence set. | Task 3 | 2026-08-25T09:48:51Z |
| 5 | Measure detour against the shortest valid obstacle-aware route candidate. | Straight-line distance is not a fair route bound, while the current local crossing metric accepts extreme detours. | Tasks 1 and 4 | 2026-08-25T09:48:51Z |
| 6 | Keep real files optional and identify them by content hash. | Real evidence must be reviewable without hard-coding product diagrams or local absolute paths into production. | Tasks 1 and 6 | 2026-08-25T09:48:51Z |
| 7 | Add no dependency and no GSAP. | The problem is layout policy and routing, not missing animation infrastructure. | Spec | 2026-08-25T09:48:51Z |

## Session Log

### Session 1 — Specification from real-graph review (2026-08-25)

- Loaded the SpecOps configuration, steering context, memory, related specifications, and current frontend graph implementation.
- Re-ran all three supplied files through the current browser preview.
- Measured render time, complete-fit zoom, effective node size, layout dimensions, crossings, long routes, branch violations, label collisions, and route detours.
- Confirmed that all files parse and render and that node overlap is zero.
- Confirmed that initial readability, layout progress, route-set quality, selection, and fixed-control safe areas remain unacceptable.
- Created the focused high-severity bugfix specification. No product code changed.

