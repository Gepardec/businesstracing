# Implementation Journal: Real Graph Layout Usability

## Summary

The first implementation was rejected during direct user review. Tasks 7 and 8 reopen route selection and viewport presentation. The prior completion claim and visual approval are invalid until the rejected screenshot cases pass.

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
| 12 | Reopen the completed spec after user visual rejection. | The existing metrics certified a visibly wrong route, and the prior review did not prove usability. | Tasks 7 and 8 | 2026-08-25T11:20:33Z |
| 13 | Remove static focus dimming and keep node text present at every zoom. | Navigation must not make the rest of a static graph disappear or turn nodes into empty boxes. | Task 8 | 2026-08-25T11:20:33Z |
| 14 | Rank wrong-way excursion and route length before crossings and outer-corridor preference. | Crossings remain visible and traceable; a hidden long detour makes the edge itself untraceable. | Task 7 | 2026-08-25T11:28:36Z |
| 15 | Keep label search on the route and cap label offset at 24 layout pixels. | A label leader creates a second ambiguous line. Route-relative positions remain attached to the edge. | Task 7 | 2026-08-25T11:28:36Z |
| 16 | Use a reversible readable presentation before layout. | Safe action, rule, guard, and parallel-edge grouping reduces noise while Full detail preserves exact source topology. | Task 9 | 2026-08-25T16:15:00Z |
| 17 | Give Explore an independent local layout. | Complete-graph coordinates made directly connected nodes look far apart and created line walls in a local task. | Task 11 | 2026-08-25T17:40:00Z |
| 18 | Show the first material split in the opening context. | A two-node entry view did not explain the first business choice or how to continue. | Task 11 | 2026-08-25T19:08:00Z |
| 19 | Try compact local spacing before safe standard spacing. | Compact spacing improves label size. Some cyclic neighborhoods need the wider route clearances of the standard profile. | Task 11 | 2026-08-25T19:14:00Z |
| 20 | Use `Path N` for an unlabeled alternative. | This interim choice removed a collision, but user review showed that it invented meaning. Session 6 removes it. | Tasks 7 and 11 | 2026-08-25T19:25:00Z |
| 21 | Use target business labels for unlabeled continuations and keep unlabeled edges unlabeled. | The source graph owns business meaning. The interface must not add a synthetic outcome. | Task 12 | 2026-08-25T19:31:00Z |
| 22 | Pair Explore with a persistent explanation panel. | A local graph alone does not explain grouped steps or all immediate continuations. | Task 12 | 2026-08-25T19:31:00Z |
| 23 | Treat the latest 55-node Overview screenshot as a failed acceptance test. | Safe routes and complete topology do not make a balanced map. The empty center, distant islands, and dominant cross-links prevent visual parsing. | Task 13 | 2026-08-25T19:50:40Z |
| 24 | Add a topology-derived primary skeleton for visual hierarchy only. | Users need one main reading structure while all secondary source links remain available. | Task 13 | 2026-08-25T19:50:40Z |
| 25 | Separate business-branch meaning from primary-skeleton priority. | A `No` or `Yes` alternative remains a first-class decision path even when it is not the spanning-tree parent. | Task 14 | 2026-08-26T07:22:51Z |
| 26 | Replace grouped arrow summaries with one complete member and a remaining count. | A clipped chain repeats text but does not explain the sequence. The panel owns the complete ordered list. | Task 14 | 2026-08-26T07:22:51Z |
| 27 | Restrict quiet dashed styling to directed feedback connections. | A later-parent or convergence connection is still a real business continuation. Dashing it makes the route look detached or unfinished. | Task 15 | 2026-08-26T08:01:31Z |
| 28 | Spread small port groups over a bounded visual span. | Twelve-pixel attachment spacing makes rounded left and right elbows look like duplicate loops. | Task 15 | 2026-08-26T08:01:31Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |
| Keep request cancellation in `layout-client.ts`. | `layout-client.ts` keeps the worker boundary; `FlowCanvas.svelte` owns monotonic presentation request IDs and rejects stale results. | The component owns graph replacement and the visible layout phase. The worker client remains stateless apart from its reusable ELK worker. | Task 2 |
| Reading view shows the local neighborhood over the complete graph at normal contrast. | Reading view keeps the focus and direct neighborhood at full contrast and fades unrelated static nodes, routes, junctions, crossings, and regions. | Real-file screenshot review showed that offscreen routes still formed line walls across the local view. Fading preserves the full model and makes the local explanation legible. | Task 5 |
| Reading view fades unrelated static topology and Overview hides node text below 0.72 zoom. | Explore moves the viewport without static dimming, and node text remains present at all supported zoom levels. | Direct user review found the fade and empty boxes more harmful than useful. | Task 8 |
| Explore reuses complete-graph coordinates and only changes the viewport. | Explore derives and lays out a bounded local graph. Overview uses the cached complete layout. | Reusing global coordinates kept local neighbors far apart and preserved irrelevant route corridors. | Task 11 |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
| --- | --- | --- | --- |
| The restricted environment blocked the live npm advisory request because it would send dependency metadata to the public registry. | No dependency or lockfile changed. Existing unit, type, build, route review, and browser gates were used. | Dependency audit could not be refreshed. Product verification is unaffected. | Task 6 |
| PostgreSQL and generated dogfood directories were not present. | Ran all graph-only browser journeys, both V1 contracts, real-file upload journeys, the 250-node graph, and run-highlight unit tests. | Five external-fixture browser journeys were skipped. No server, SQL, or run-view behavior changed. | Task 6 |

## Session Log

### Session 9 — Rejected edge integrity (2026-08-26)

- Reopened the specification after direct screenshots showed a floating-looking dashed continuation, unclear outcome termination, and adjacent rounded elbows that looked like double loops.
- Invalidated the 5/5 acceptance claim. The prior browser checks proved presence and contrast for branches but did not prove visual endpoint ownership for all business continuations.
- Added RB-13 and started Task 15 before product-code changes.
- Confirmed that both V1 JSON contracts remain unchanged and that no dependency or CI change is needed.
- Exposed directed feedback classification separately from spanning-tree priority. Forward later-parent and convergence links now remain solid.
- Increased the bounded port span for small connection groups. Two bottom ports now use a 56-pixel separation instead of 12 pixels.
- Added explicit target ownership to shared convergence segments and browser checks for source boundary, target boundary, and outcome arrow termination.
- Re-rendered and reviewed all three supplied graphs in Readable, Full detail Explore, and complete Overview. The rejected floating line, double-loop, and missing-termination patterns are no longer present.
- Passed 85 unit tests, clean Svelte diagnostics, the production build, the graph-only browser suite, the optional three-real-graph browser journey, and all three objective graph reviews.

### Session 8 — Rejected branch comprehension (2026-08-26)

- Reopened the completed specification after direct review found missing supplied outcomes and unclear grouped actions.
- Added RB-12 and started Task 14 before product-code changes.
- Defined a five-part comprehension rubric. Every supplied graph must pass orientation, decision alternatives, route ownership, node meaning, and explanation recovery at normal reading zoom.
- Kept graph contracts, source topology, active-run behavior, dependencies, server behavior, and CI out of scope.
- Added a pure edge-presentation policy so that only non-branch cross-links become quiet references.
- Added explicit branch data to rendered routes and kept supplied branch labels visible at normal contrast.
- Replaced grouped arrow sentences with the first complete step plus a grammatically correct remaining count.
- Exposed every grouped member in the Overview pointer panel as well as the selected Explore panel.
- Reviewed normal-reading-zoom screenshots for all three supplied graphs. Each graph passed the five-part comprehension rubric.
- Passed 83 unit tests, clean Svelte diagnostics, the production build, and the optional-real-graph Chromium journey.

### Session 7 — Rejected complete-map composition (2026-08-25)

- Reopened the completed specification after the user rejected the 55-node Overview screenshot.
- Invalidated the prior visual approval. Existing hard metrics do not detect the empty center, distant branch islands, or the visual dominance of long secondary routes.
- Added RB-11 and started Task 13 before product-code changes.
- Kept the scope in the existing frontend layout pipeline. No dependency, graph contract, server, database, HTTP, or CI change is required.
- Derived a deterministic primary spanning forest from declared entries. Later incoming links, feedback edges, and cross-branch links remain complete but become secondary presentation links.
- Added a primary-edge ELK profile and scored candidate layouts by label safety, corridor density, internal empty bands, and primary-edge span.
- Reduced complete-layout vertical spacing from 96 to 72 pixels after real-file review. The 55-node readable map fell from 4,436 to 3,908 layout pixels.
- Hid secondary route labels until route inspection and rendered secondary links as quiet dashed references. The semantic edge list still exposes every connection.
- Full-resolution browser review approved all three supplied Readable Overview maps. The 55-node primary skeleton has zero crossings, while all 55 nodes and 89 edges remain present.

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

### Session 4 — Rejected visual result and remediation (2026-08-25)

- Reopened the spec after the user rejected the supplied direction-graph screenshot and the focus interaction.
- Confirmed that the `operation failed` edge uses a 665-pixel outer loop, leaves above the layout, and reports a false 1.0 detour ratio.
- Confirmed that `Branch 2` is 72 layout pixels from its route anchor and uses a detached dashed leader.
- Confirmed that static focus reduces unrelated topology to 10 to 12 percent opacity and Overview removes node text below 0.72 zoom.
- Task 7 scope: correct the detour baseline, make outer corridors fallback-only, rank bounded route length before crossings, and keep labels attached. Acceptance requires the supplied direction graph to lose its wrong-way loop without regressing safe endpoint geometry.
- Completed Task 7. The direction graph branch no longer leaves the layout, all three supplied graphs report a 1.0 maximum detour, and all report zero detached labels, wrong-way boundary exits, node intrusions, and label collisions.
- Task 8 scope: remove static focus opacity and the focus summary, rename Reading to Explore, keep node text present while zooming, and validate the three supplied graphs at Explore and Overview scales.

### Session 5 — Readable presentation and independent Explore layout (2026-08-25)

- Added a reversible presentation transform. It groups safe action chains, convergent rule chains, shared-exit guard chains, and parallel presentation connections.
- Added Readable and Full detail modes. Full detail restores each source node and edge. Search, accessibility, and run-highlight mappings retain original IDs.
- Added a generic summary with no more than three actual sentences. It uses supplied entry, branch, work, cycle, and outcome data only.
- Fixed feedback-edge detection to use directed depth-first traversal from entries instead of lexical node-ID order.
- Replaced the global-coordinate Explore viewport with an independently laid-out local graph.
- Added an opening context that follows a bounded setup path to the first material split. Selected nodes use direct predecessor-and-successor context.
- Added compact local spacing with a safe standard-spacing retry for cyclic or dense local neighborhoods.
- Fixed a race where Overview selected during pending layout could produce an empty canvas.
- Shortened unlabeled alternative labels from `Branch N` to `Path N`; all three complete real-file reviews now have zero label collisions.
- Readable reductions are 45 to 22 nodes and 77 to 29 connections, 55 to 39 nodes and 89 to 63 connections, and 19 to 15 nodes and 29 to 20 connections.

### Session 6 — Explanation workspace (2026-08-25)

- Reopened the completed geometry result after direct user review showed that it was not an acceptable explanation.
- Removed invented `Path N` labels. Unlabelled edges remain unlabelled, and the panel names each continuation with its supplied target business label.
- Added a pure explanation model and a persistent guide for the current step, type, complete sequence members, incoming context, and all immediate continuations.
- Made continuation rows navigate to the selected target and rebuild its local Explore graph.
- Replaced the count-based summary with three source-derived sentences for the declared start, first material alternatives, and possible results.
- Removed the duplicate summary above the canvas. Explore now uses the graph and explanation panel as one workspace.
- Defined Overview as a topology map. Hover shows the full node label, and selecting a node returns to Explore.
- Found a 41-pixel narrow-screen overlap during browser review. Reserved a bottom safe area and allowed a 0.5 local-context fit in constrained viewports.
- Kept run views unchanged by enabling the guide only for the static `/graphs` preview.
- Changed no Java code and no graph JSON contract.

## Phase 3 Completion Summary

- Completed twelve tasks across metrics, presentation state, viewport policy, placement, routing, static selection, readable presentation, graph explanation, independent Explore layout, the explanation workspace, and acceptance review.
- Added no dependency and changed no graph contract, SQL, HTTP, storage, or CI file.
- Kept all graph positions, routes, groups, and focus state derived from the supplied topology.
- Used local optional evidence by file path and hash only. No supplied graph ID, node ID, label, coordinate, route, or diagram entered production or test fixtures.
- Verified 71 unit tests, zero Svelte diagnostics, the production build, and 11 applicable full-suite browser journeys, including all three optional-evidence graphs.

## Phase 4 Verification Summary

- `npm test`: pass; 15 files and 58 tests.
- `npm run check`: pass; 0 errors and 0 warnings.
- `npm run build`: pass; only existing third-party XYFlow and D3 warnings.
- `npm run review:graphs -- <three supplied files>`: pass for all files; 49 to 371 ms per layout in Node 24.18 on the local acceptance machine.
- Playwright production build with real evidence: 11 passed and 5 external PostgreSQL or generated-dogfood journeys skipped.
- Manual visual review: approved Reading, selected Reading, and Overview for all three files in light and dark themes. Reading now isolates direct topology; Overview preserves the full shape.
- Source scan: no supplied graph IDs, business labels, absolute evidence paths, fixed positions, fixed routes, or hard-coded diagrams exist in frontend source, scripts, or browser tests.

## Phase 5 Verification Summary

- `npm test`: pass; 16 files and 71 tests.
- `npm run check`: pass; 0 errors and 0 warnings.
- `npm run build`: pass; only existing third-party XYFlow and D3 warnings.
- `npm run review:graphs -- <three supplied files>`: pass for all files. Complete layouts have zero overlaps, node intrusions, label collisions, detached labels, wrong-way exits, avoidable crossings, branch violations, and avoidable detours.
- Playwright production build with real evidence: 11 applicable journeys pass and 5 external PostgreSQL or generated-dogfood journeys skip.
- Browser geometry checks on every default real-file Explore view find zero node intrusions, node-label collisions, label-label collisions, and route-label collisions. Every visible card is at least 160 by 60 CSS pixels in the tested desktop viewport.
- Full-resolution visual review approves the opening, selected, Overview, light, and dark states for all three supplied files.
- No Java or JSON contract change was necessary. The current JSON includes sufficient topology and supplied business labels for the generic summary and reversible presentation.

## Phase 6 Verification Summary

- `npm test`: pass; 17 files and 76 tests.
- `npm run check`: pass; 0 errors and 0 warnings.
- `npm run build`: pass; only existing third-party XYFlow unused-import and D3 circular-dependency warnings.
- `npm run review:graphs -- <three supplied files>`: pass for all files. Results are 368 milliseconds, 3,037 milliseconds, and 84 milliseconds, with zero hard layout defects.
- Playwright production suite: 11 applicable journeys pass and 5 external PostgreSQL or generated-dogfood journeys skip.
- Real-file browser review covers initial Explore, selected continuation, Full detail, Overview, light, dark, and narrow states.
- The narrow browser assertion proves that the bottom explanation sheet does not overlap graph nodes.
- No supplied graph ID, node ID, business label, coordinate, route, or diagram is hardcoded in frontend source or tests.

## Documentation Review

| File | Status | Review result |
| --- | --- | --- |
| `fachtracing-viewer/README.md` | Updated | Documents Readable, Full detail, independent Explore layout, the explanation panel, Overview, selection, the review command, and the local POC timing boundary. |
| `README.md` | Up to date | Root build, API, and storage behavior did not change. |
| `docs/` | Up to date | No public graph, SQL, HTTP, Java, or persistence contract changed. |
| `AGENTS.md` | Up to date | The change keeps single responsibilities and contains no hard-coded diagram. |
| `.specops/steering/repo-map.md` | Updated | Adds placement, viewport, layout status, and review modules and refreshes the source-list hash. |
