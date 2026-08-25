# Implementation Journal: Static Graph Layout Quality

## Summary

All six implementation tasks are complete. The viewer now uses deterministic topology analysis, compact ranked placement, four-side orthogonal routing, collision-aware branch labels, presentation-only convergence and crossing geometry, and a distinct static node grammar. The graph contracts, stored payloads, and run semantics did not change. Objective geometry, unit, build, upload, dogfood, and browser gates pass. Final user approval of the visual references and the PostgreSQL-backed run-detail browser journeys remain deferred environment or review gates.

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
| 6 | Preserve the source and target port leads during point simplification. | Removing a collinear reversal can move a route through a node and can make the route start on the wrong side. | Task 3 | 2026-08-25T09:12:18Z |
| 7 | Reserve cycle corridors for edges whose source and target are in the same multi-node strongly connected component. | Undefined cycle identifiers compared as equal and incorrectly classified ordinary edges as cycle edges. | Task 3 | 2026-08-25T09:12:18Z |
| 8 | Place labels in one global pass against all nodes, prior labels, and unrelated routes. | A label can be clear on its own route and still hide another route or branch label. | Task 4 | 2026-08-25T09:12:18Z |
| 9 | Let normal edges use only internal graph corridors. Use an outer corridor only for a proven long or cycle edge, or as a marked last-resort fallback. | Unrestricted outer candidates caused local branches to leave the graph and return from a remote side. | Task 3 | 2026-08-25T09:12:18Z |
| 10 | Give each fan-in feeder a stable slot on one convergence lane and render one shared trunk after the junction. | Routing every feeder to one point created a dense comb and lost individual traceability. | Task 5 | 2026-08-25T09:12:18Z |
| 11 | Penalize near-parallel congestion, not only exact path overlap. | Routes that differ by a few pixels can still look like one thick or ambiguous connection. | Task 3 | 2026-08-25T09:12:18Z |
| 12 | Remove color transitions from shared buttons. | A theme change made the foreground switch before the background and caused a short low-contrast state in visual references. | Task 6 | 2026-08-25T09:12:18Z |
| 13 | Run two deterministic crossing-refinement passes and report any remaining single-edge improvement. | Setting the avoidable-crossing metric to zero after routing would hide a route that a valid candidate can still improve. | Tasks 3 and 6 | 2026-08-25T09:22:00Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |
| Build a general sparse rectilinear visibility graph. | Built a bounded deterministic family of orthogonal candidates from port leads, obstacle sides, prior-route lanes, and reserved corridors, followed by two stable crossing-refinement passes. | The bounded candidate family meets the obstacle, separation, refinement, determinism, and two-second gates with less POC complexity. Remaining candidate-space intersections receive explicit crossing bridges. | Task 3 |
| Store approved pixel baselines for all references. | Generate light, dark, and forced-color reference images during Playwright runs and enforce geometry plus semantic assertions. | Final pixel approval is a user review action. Generated artifacts give current evidence without treating assistant inspection as human approval. | Task 6 |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
| --- | --- | --- | --- |
| The live npm advisory request could not run in the restricted environment. | Reused the prior passing audit for the unchanged lockfile and recorded the limitation in `dependency-audit.md`. | No dependency is added by this specification. | Spec |
| PostgreSQL was not available for the four run-detail Playwright journeys. | Ran all graph-only, upload, contract, privacy, 250-node, and dogfood browser tests. Kept the four database-dependent journeys as explicit deferred tests. | Static graph implementation is verified. The shared run-detail browser integration still needs a database-backed review environment. | Task 6 |

## Documentation Review

| File | Status | Review result |
| --- | --- | --- |
| `fachtracing-viewer/README.md` | Updated | Documents the topology-first layout, four-side routing, convergence, crossings, duplicate context, graph upload, and local verification commands. |
| `README.md` | Up to date | No root setup, contract, or release behavior changed. |
| `docs/` | Up to date | No JSON, SQL, HTTP, storage, or Java capability contract changed. |
| `AGENTS.md` | Up to date | The implementation keeps one responsibility per module and contains no hard-coded diagrams. |

## Phase 3 Completion Summary

- Completed six tasks across topology analysis, ranked placement, four-side routing, labels and node grammar, convergence and crossing projection, and shared-canvas integration.
- Added focused modules for topology, route planning, route quality, generated fixtures, and presentation-only junctions.
- Preserved exact original node and edge IDs through layout and presentation.
- Changed no dependency, public JSON schema, SQL schema, HTTP contract, or stored payload.
- Verified 43 unit tests, zero Svelte diagnostics, a production build, and the graph-only browser suite.

## Phase 4 Verification Summary

- `npm run check`: pass with 0 errors and 0 warnings.
- `npm test`: pass; 12 files and 43 tests.
- `npm run build`: pass; only existing dependency warnings from XYFlow and D3.
- Playwright: 10 passed and 4 PostgreSQL-backed run-detail journeys skipped.
- Visual review: generated business V1, dogfood, branching, 12-source fan-in, cycle, duplicate, long-route, crossing, node-grammar, 250-node focus, light, dark, and forced-color states inspected.
- Geometry review: zero unrelated-node intrusion, zero label-to-node collision, zero label-to-label collision, and zero unrelated route-to-label collision for the planar references.
- Performance review: the generated 250-node layout remains below the existing two-second unit-test gate.

## Session Log

### Session 1 — Static graph layout specification (2026-08-25)

- Reviewed the four supplied large-graph screenshots and the accepted visual audit.
- Confirmed that active-run state does not apply to the static graph view.
- Inspected the graph layout pipeline, graph contracts, Svelte Flow presentation, unit tests, and Playwright journeys.
- Created the high-severity static graph layout bugfix specification. No product code changed.

### Session 2 — Implementation (2026-08-25)

- Recovered the approved static-only scope and all six pending tasks.
- Confirmed a clean working tree before implementation.
- Loaded the frontend steering context, repository map, project memory, graph implementation, contracts, and current tests.
- Started Task 1 with no external task tracker and no new dependency.
- Completed Task 1 with generated graph builders, deterministic topology fixtures, layout-quality metrics, and 17 focused passing tests.
- Started Task 2 with strongly connected component analysis, directed ranks, structural spines, branch regions, convergence groups, duplicate groups, and compact rank placement.
- Completed Task 2 with compact component columns, stable rank rows, centered chains and siblings, terminal outcome ranks, and presentation-only cycle regions.
- Completed Task 3 with four-side port selection, stable port slots, orthogonal candidate scoring, obstacle clearance, long-edge corridors, crossing detection, and shared convergence targets.
- Started Task 4 for branch labels, node grammar, duplicate context, and accessible edge descriptions.
- Completed Task 4 with source-adjacent branch labels, `Yes` and `No` display mapping, exact accessible outcomes, four-side hidden handles, duplicate occurrence context, and distinct node-kind silhouettes.
- Completed Task 5 with slotted convergence lanes, one shared trunk, independently focusable feeders, crossing bridges, shared focus projection, and unchanged semantic graph counts.
- Completed Task 6 with shared-canvas integration, reduced dense-grid contrast, generated light, dark, and forced-color references, a theme-transition contrast correction, upload privacy verification, business V1 verification, and generated Fachtracing dogfood.
- Corrected route simplification, ordinary-edge cycle classification, label-versus-route collisions, outer-corridor leakage, near-parallel congestion, fan-in slot convergence, and avoidable-crossing verification during adversarial visual review.
- Final verification passed: Svelte check, 43 unit tests, production build, 10 browser journeys, and complete static graph reference inspection. Four PostgreSQL-backed run-detail journeys remain deferred.
