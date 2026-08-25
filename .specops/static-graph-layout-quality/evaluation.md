# Evaluation Report: Static Graph Layout Quality

## Spec Evaluation

### Iteration 1

**Evaluated at:** 2026-08-25T07:42:57Z  
**Threshold:** 7/10

### Criteria Testability

**Evidence:** `bugfix.md` defines 67 findings with IDs, severity, current defects, and required corrections. Its Testing Plan separates current, expected, and unchanged behavior. `design.md` defines geometry, semantic, visual, regression, accessibility, deterministic, and two-second safety gates. `tasks.md` maps every finding group to one implementation task and one primary verification method.

**Finding:** Recognition of sequence, branch, cycle, convergence, and terminal patterns cannot be fully reduced to a geometry assertion. The fixed reference matrix and required human approval contain this remaining subjective decision. Implementation must not use human approval as a replacement for the numeric geometry and semantic gates.

**Score:** 9/10 — Pass

### Criteria Completeness

**Evidence:** `bugfix.md` covers structural shape, route choice, branch origins, fan-in, crossings, topology comprehension, duplicate context, node and edge grammar, local reasoning, regression risk, failure behavior, and acceptance. `design.md` defines the full pipeline, internal types, topology algorithms, placement, routing, labels, convergence, crossings, structural regions, accessibility, fixtures, and rollout.

**Finding:** Initial zoom and fit policy are intentionally absent. This is complete for the accepted static-layout scope, but the broader `frontend-visual-design-quality` spec must still own the reading and overview viewport behavior. The current spec must not grow a second viewport policy during implementation.

**Score:** 9/10 — Pass

### Design Coherence

**Evidence:** The pipeline assigns one responsibility to topology analysis, ELK placement, route planning, convergence and crossing projection, label placement, and Svelte presentation. Original graph IDs survive all stages. Junctions, trunks, crossings, corridors, and regions remain presentation-only. The design now states that normal branch regions have no visible box and limits neutral enclosures to multi-node cycles and disconnected components without entries.

**Finding:** A sparse orthogonal router is the most complex new unit. The design contains this risk with a sparse visibility graph, lexicographic scoring, stable routing order, no more than two refinement passes, and the existing 250-node two-second test. Implementation must keep candidate scoring in `route-planner.ts` and metrics in `route-quality.ts`; moving route repair into Svelte would break this design.

**Score:** 9/10 — Pass

### Task Coverage

**Evidence:** Task 1 creates fixtures and metrics. Task 2 implements structural analysis and placement. Task 3 implements ports and routing. Task 4 implements branch and node context. Task 5 implements fan-in and crossings. Task 6 integrates and approves the shared canvas. The final coverage table maps ST, RT, BR, FN, TP, CT, VG, and LR findings without a gap.

**Finding:** Tasks 2, 3, and 5 all modify `layout-definition.ts`, which is an intentional integration boundary but also a change hotspot. Each task must leave only coordination in that file and keep analysis, routing, and metrics in their dedicated modules.

**Score:** 9/10 — Pass

| Dimension | Score | Threshold | Result |
| --- | --- | --- | --- |
| Criteria Testability | 9 | 7 | Pass |
| Criteria Completeness | 9 | 7 | Pass |
| Design Coherence | 9 | 7 | Pass |
| Task Coverage | 9 | 7 | Pass |

**Verdict:** PASS — 4 of 4 dimensions passed.

**Known review points:** Human design approval remains mandatory after all objective gates pass. The viewport remains outside this spec. The custom router must stay isolated from presentation components.

## Implementation Evaluation

### Iteration 1

**Evaluated at:** 2026-08-25T09:23:18Z
**Threshold:** 7/10

### Root Cause Accuracy

**Evidence:** `topology-analysis.ts:258` adds the missing graph-wide analysis stage. `route-planner.ts` owns final four-side route selection, runs two deterministic crossing-refinement passes, and reports remaining candidate improvements instead of forcing the metric to zero. `route-quality.ts:55` and `route-quality.ts:156` measure separation and complete route quality outside Svelte. `GraphJunctions.svelte:45` renders convergence as presentation data.

**Finding:** The route planner is necessarily more complex than the removed fixed-port projection. Its candidate ordering and penalty weights remain the main maintenance risk, even though they are isolated and covered by deterministic fixtures.

**Score:** 9/10 — Pass

### Fix Completeness

**Evidence:** Generated fixtures cover chain, branch, diamond, 12-source fan-in, fixed-port detour, long shortcut, duplicates, multiple entries, cycles, crossings, and 250 nodes. Browser references cover both graph contracts, all node kinds, both themes, forced colors, convergence focus, cycle regions, crossing bridges, duplicates, long corridors, and generated dogfood. Geometry assertions report zero intrusions and label collisions for planar references.

**Finding:** Initial zoom and fit-to-view policy are intentionally outside this bugfix. Wide same-rank fan-in graphs can therefore start at a small scale, although search focus makes the selected node readable.

**Score:** 8/10 — Pass

### Regression Safety

**Evidence:** `npm run check` reports zero errors and warnings. All 43 unit tests pass, including graph-contract parsing, run-highlight ID mapping, privacy behavior, deterministic layout, and 250-node safety. The production build passes. Ten browser journeys pass, including local upload, business V1, developer V1, and dogfood.

**Finding:** Four PostgreSQL-backed run-detail browser journeys could not run because `FACHTRACING_DATABASE_URL` was not available. The affected ID mapping and shared-canvas logic have unit coverage, but the browser integration still needs a database-backed environment.

**Score:** 7/10 — Pass

### Test Verification

**Evidence:** Vitest executed 12 files and 43 tests with no failures. Playwright executed 14 journeys: 10 passed and 4 database-dependent journeys skipped. The generated references include light, dark, forced-color, 250-node focus, real business V1, and generated Fachtracing output. No graph-content masks are configured.

**Finding:** The reference images are generated evidence, not committed pixel baselines. Geometry and semantic gates prevent structural regressions, while final subjective design approval remains a user action.

**Score:** 8/10 — Pass

| Dimension | Score | Threshold | Result |
| --- | --- | --- | --- |
| Root Cause Accuracy | 9 | 7 | Pass |
| Fix Completeness | 8 | 7 | Pass |
| Regression Safety | 7 | 7 | Pass |
| Test Verification | 8 | 7 | Pass |

**Verdict:** PASS — 4 of 4 dimensions passed.
