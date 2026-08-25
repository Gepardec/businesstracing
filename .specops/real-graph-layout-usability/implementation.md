# Implementation Journal: Real Graph Layout Usability

## Summary

All six tasks are complete. Static graph preview now has an explicit arranging state, separate Reading and Overview modes, route-aware deterministic ELK placement, bounded route-set refinement, persistent static node selection, safe control-aware focus, and a generic file-driven review command. Reading mode keeps the focus and its direct topology at full contrast and fades unrelated presentation geometry without removing graph data. A compact canvas summary identifies the current focus, run step, or whole-graph overview. Both V1 graph formats, all three supplied real graphs, the generated 250-node safety graph, and the graph-only browser suite pass.

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
| 8 | Use two bounded stable ELK profiles and score their completed routes before placement acceptance. | Route crossings, branch containment, and detour are part of layout quality and must influence placement. | Task 3 | 2026-08-25T10:49:16Z |
| 9 | Replace exhaustive spine-path enumeration with memoized longest-forward-path analysis. | The real 45-node graph contains many valid paths. Enumerating all paths made layout exponential and caused the five-second delay. | Task 3 | 2026-08-25T10:20:00Z |
| 10 | Use the direct focus neighborhood as the Reading context and fade unrelated static topology. | Medium graphs stay complete, but unrelated corridor walls must not compete with the local explanation task. | Task 5 | 2026-08-25T10:44:15Z |
| 11 | Fall back to focus-centered 0.86 zoom when a full direct neighborhood cannot fit. | The selected node must stay visible and readable even when direct routes or neighbors extend beyond the canvas. | Task 5 | 2026-08-25T10:37:18Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |
| Keep request cancellation in `layout-client.ts`. | `layout-client.ts` keeps the worker boundary; `FlowCanvas.svelte` owns monotonic presentation request IDs and rejects stale results. | The component owns graph replacement and the visible layout phase. The worker client remains stateless apart from its reusable ELK worker. | Task 2 |
| Reading view shows the local neighborhood over the complete graph at normal contrast. | Reading view keeps the focus and direct neighborhood at full contrast and fades unrelated static nodes, routes, junctions, crossings, and regions. | Real-file screenshot review showed that offscreen routes still formed line walls across the local view. Fading preserves the full model and makes the local explanation legible. | Task 5 |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
| --- | --- | --- | --- |
| The restricted environment blocked the live npm advisory request because it would send dependency metadata to the public registry. | No dependency or lockfile changed. Existing unit, type, build, route review, and browser gates were used. | Dependency audit could not be refreshed. Product verification is unaffected. | Task 6 |
| PostgreSQL and generated dogfood directories were not present. | Ran all graph-only browser journeys, both V1 contracts, real-file upload journeys, the 250-node graph, and run-highlight unit tests. | Five external-fixture browser journeys were skipped. No server, SQL, or run-view behavior changed. | Task 6 |

## Session Log

### Session 1 — Specification from real-graph review (2026-08-25)

- Loaded the SpecOps configuration, steering context, memory, related specifications, and current frontend graph implementation.
- Re-ran all three supplied files through the current browser preview.
- Measured render time, complete-fit zoom, effective node size, layout dimensions, crossings, long routes, branch violations, label collisions, and route detours.
- Confirmed that all files parse and render and that node overlap is zero.
- Confirmed that initial readability, layout progress, route-set quality, selection, and fixed-control safe areas remain unacceptable.
- Created the focused high-severity bugfix specification. No product code changed.

### Session 2 — Implementation (2026-08-25)

- Recovered the evaluated specification and verified that the required static-layout dependency is completed.
- Passed the review, task-tracking, cycle, and dependency-introduction gates with no new dependency.
- Started Task 1 for generic layout metrics and the file-driven review harness.
- Completed Task 1. Added candidate-relative detour and density metrics, deterministic quality diagnostics, generated topology profiles, and `npm run review:graphs`.
- Verified both V1 contracts with tests. Verified the three optional evidence files by SHA-256. The command correctly returned a failing status for the current layouts and named their crossing, branch, and detour defects.
- Started Task 2 for explicit layout state and separate Reading and Overview viewport modes.
- Completed Task 2 with explicit idle, arranging, ready, and failed presentation states; accessible busy feedback; stale-result rejection; Reading and Overview controls; and pure safe-viewport calculations.
- Completed Task 3 with full ELK coordinate preservation, stable `network-simplex-balanced` and `coffman-graham-wide` profiles, route-aware lexicographic placement selection, and memoized topology-spine analysis.
- Completed Task 4 with candidate-relative detour metrics, direction-pruned ports, bounded internal lanes, three stable crossing-refinement passes, branch-region checks, shared-corridor density, and named acceptance failures.
- Completed Task 5 with exact-ID and label selection, stable duplicate occurrence feedback, non-destructive no-match behavior, focus-centered Reading fallback, direct-context emphasis, and control-safe viewport updates after resize and theme changes.
- Completed Task 6 with the generic review command, real-file SHA-256 verification, light and dark references, both V1 contracts, and production-build browser review.
- Corrected a selected-node visibility defect found during screenshot inspection and strengthened the browser assertion to require full canvas containment plus 16-pixel control clearance.
- Final objective results: search users 45/77 at 371 ms with 17 crossings; determine journey warnings 55/89 at 306 ms with 22 crossings; validate journey direction 19/29 at 49 ms with 3 crossings. Every graph has zero overlaps, intrusions, label collisions, avoidable crossings, branch violations, corridor violations, and avoidable detours.

### Session 3 — Completion audit (2026-08-25)

- Re-inspected Reading, selected Reading, and Overview screenshots for all three supplied graphs at full resolution.
- Added a data-driven canvas summary that names the focused node and direct-neighbor count, the current run step, or the whole-graph node count.
- Corrected the summary to use plain visible text so it does not duplicate the existing screen-reader status channel.
- Rebuilt the production application and reran the complete browser suite. Eleven applicable journeys pass, and five unavailable external-fixture journeys skip.

## Phase 3 Completion Summary

- Completed six tasks across metrics, presentation state, viewport policy, placement, routing, static selection, and acceptance review.
- Added no dependency and changed no graph contract, SQL, HTTP, storage, or CI file.
- Kept all graph positions, routes, groups, and focus state derived from the supplied topology.
- Used local optional evidence by file path and hash only. No supplied graph ID, node ID, label, coordinate, route, or diagram entered production or test fixtures.
- Verified 58 unit tests, zero Svelte diagnostics, the production build, and 11 applicable full-suite browser journeys, including both optional-evidence journeys.

## Phase 4 Verification Summary

- `npm test`: pass; 15 files and 58 tests.
- `npm run check`: pass; 0 errors and 0 warnings.
- `npm run build`: pass; only existing third-party XYFlow and D3 warnings.
- `npm run review:graphs -- <three supplied files>`: pass for all files; 49 to 371 ms per layout in Node 24.18 on the local acceptance machine.
- Playwright production build with real evidence: 11 passed and 5 external PostgreSQL or generated-dogfood journeys skipped.
- Manual visual review: approved Reading, selected Reading, and Overview for all three files in light and dark themes. Reading now isolates direct topology; Overview preserves the full shape.
- Source scan: no supplied graph IDs, business labels, absolute evidence paths, fixed positions, fixed routes, or hard-coded diagrams exist in frontend source, scripts, or browser tests.

## Documentation Review

| File | Status | Review result |
| --- | --- | --- |
| `fachtracing-viewer/README.md` | Updated | Documents Reading, Overview, selection, topology level of detail, the generic review command, and the local POC timing boundary. |
| `README.md` | Up to date | Root build, API, and storage behavior did not change. |
| `docs/` | Up to date | No public graph, SQL, HTTP, Java, or persistence contract changed. |
| `AGENTS.md` | Up to date | The change keeps single responsibilities and contains no hard-coded diagram. |
| `.specops/steering/repo-map.md` | Updated | Adds placement, viewport, layout status, and review modules and refreshes the source-list hash. |
