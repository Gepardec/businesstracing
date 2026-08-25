# Implementation Tasks: Static Graph Layout Quality

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `frontend-flow-explorer` | This bugfix changes the completed viewer layout while it preserves the graph contracts and shared canvas. | Yes | Completed |

## Task Breakdown

### Task 1: Add generated topology fixtures and layout-quality metrics

**Status:** Pending  
**Estimated Effort:** L  
**Dependencies:** None  
**Priority:** High  
**IssueID:** None  
**Blocker:** None

**Description:**
Create the reusable test builders and objective metrics that define the current defects. Do not add hand-positioned nodes, fixed routes, or product-specific diagrams.

**Implementation Steps:**

1. Create topology builders for the chain, balanced branch, diamond, fan-in, fixed-port detour, long shortcut, duplicate-label, multiple-entry, cycle, non-planar, 250-node, and real generated graph cases.
2. Create `route-quality.ts` with node-overlap, intrusion, collision, crossing, length, bend, backtracking, corridor, and branch-region metrics.
3. Add assertions for every numeric geometry and semantic gate in the design.
4. Capture tests that prove the current fixed-port layout fails the detour, fan-in, crossing, and compactness gates before the layout replacement.
5. Keep the existing two-second 250-node safety limit.

**Acceptance Criteria:**

- [ ] Every fixture in the design matrix is generated from graph nodes and edges.
- [ ] No fixture contains node coordinates, port coordinates, or route points.
- [ ] The metrics give the same result across five repeated layouts.
- [ ] Geometry failures identify the edge, node, or label that caused the failure.
- [ ] The 250-node fixture preserves the existing two-second safety gate.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/graph-fixtures.ts` (create)
- `fachtracing-viewer/src/lib/graph/route-quality.ts` (create)
- `fachtracing-viewer/src/lib/graph/route-quality.test.ts` (create)
- `fachtracing-viewer/src/lib/graph/graph.test.ts`
- `fachtracing-viewer/e2e/visual-fixtures.ts` (create)

**Tests Required:**

- [ ] Fixture determinism tests
- [ ] Layout-quality metric unit tests
- [ ] Current-defect characterization tests
- [ ] 250-node safety test

---

### Task 2: Analyze topology and place structural regions

**Status:** Pending  
**Estimated Effort:** L  
**Dependencies:** Task 1  
**Priority:** High  
**IssueID:** None  
**Blocker:** None

**Description:**
Add one topology-analysis stage and use its result to produce stable ranks, spines, branch regions, convergence groups, components, cycles, duplicate groups, and long-edge corridors. Keep ELK responsible only for node placement.

**Implementation Steps:**

1. Create directed and reverse adjacency indexes and deterministic component analysis.
2. Add Tarjan strongly connected component detection and rank calculation on the collapsed acyclic graph.
3. Select a deterministic structural spine for each component without presenting it as a runtime or primary path.
4. Derive branch regions, convergence groups, long edges, duplicate-label groups, and terminal ranks.
5. Change the ELK input to use topology constraints without fixed north and south edge ports or final ELK routes.
6. Add the deterministic placement correction and compaction pass.
7. Emit visible region metadata only for multi-node cycles and disconnected components without declared entries.

**Acceptance Criteria:**

- [ ] ST-01 through ST-10 pass their defined placement gates.
- [ ] TP-01 through TP-07 pass their defined topology gates.
- [ ] A single child is centered below its parent when no obstacle prevents it.
- [ ] Sibling roots use one rank and remain inside their assigned branch regions.
- [ ] Reachable outcomes use the last reachable rank.
- [ ] A cycle uses one outer cycle corridor and does not reverse the acyclic rank structure.
- [ ] Normal branch regions have no background enclosure; only eligible cycles and disconnected components expose neutral structural enclosures.
- [ ] The same graph produces byte-for-byte equal topology analysis and node positions across five runs.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/topology-analysis.ts` (create)
- `fachtracing-viewer/src/lib/graph/topology-analysis.test.ts` (create)
- `fachtracing-viewer/src/lib/graph/layout-definition.ts`
- `fachtracing-viewer/src/lib/graph/layout-client.ts`
- `fachtracing-viewer/src/lib/graph/layout-engine.ts`
- `fachtracing-viewer/src/lib/graph/graph.test.ts`

**Tests Required:**

- [ ] Components, entries, outcomes, rank, branch-region, and spine tests
- [ ] Strongly connected component and cycle-corridor tests
- [ ] Placement alignment, compaction, balance, and determinism tests
- [ ] Developer graph V1 and business graph V1 mapping tests

---

### Task 3: Select four-side ports and plan local orthogonal routes

**Status:** Pending  
**Estimated Effort:** XL  
**Dependencies:** Tasks 1 and 2  
**Priority:** High  
**IssueID:** None  
**Blocker:** None

**Description:**
Replace fixed north and south routing with a deterministic sparse orthogonal route planner. The planner selects ports and routes. Svelte components only render its output.

**Implementation Steps:**

1. Add immutable layout-port, route, obstacle, corridor, and route-candidate types.
2. Generate exact north, east, south, and west port slots with required corner and slot clearance.
3. Build a sparse rectilinear visibility graph from ports, expanded obstacles, labels, regions, junction lanes, and outer corridors.
4. Evaluate all valid source and target port pairs with the lexicographic score in the design.
5. Route local spine, local branch, convergence, long, and cycle edges in stable order.
6. Run at most two deterministic crossing-refinement passes.
7. Return selected ports, complete route points, length, bends, and presentation references for every original edge.

**Acceptance Criteria:**

- [ ] RT-01 through RT-12 and RT-14 pass.
- [ ] LR-01, LR-02, LR-04, and LR-05 pass.
- [ ] Fixed north and south are not the only port options.
- [ ] The fixed-port detour uses east or west and no equal-quality candidate is more than 8 pixels shorter.
- [ ] Planar fixtures have no route through an unrelated node and no avoidable crossing.
- [ ] Distinct parallel routes have 12-pixel clearance unless they use an explicit shared trunk.
- [ ] Every non-port segment is at least 16 pixels long.
- [ ] Route output is deterministic across five repeated layouts.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/route-planner.ts` (create)
- `fachtracing-viewer/src/lib/graph/route-planner.test.ts` (create)
- `fachtracing-viewer/src/lib/graph/layout-definition.ts`
- `fachtracing-viewer/src/lib/graph/flow-types.ts`
- `fachtracing-viewer/src/lib/graph/graph.test.ts`

**Tests Required:**

- [ ] Four-side port and stable slot tests
- [ ] Candidate rejection and optimality tests
- [ ] Clearance, bend, backtracking, corridor, and parallel-route tests
- [ ] Long-edge and cycle-route tests
- [ ] 250-node route-quality and safety tests

---

### Task 4: Render branch meaning and static node context

**Status:** Pending  
**Estimated Effort:** L  
**Dependencies:** Tasks 2 and 3  
**Priority:** High  
**IssueID:** None  
**Blocker:** None

**Description:**
Make branch origins, repeated nodes, and node kinds clear in the static graph. Do not add run-state color, current-step state, or execution evidence.

**Implementation Steps:**

1. Update edge-label mapping for Boolean, `next`, blank, neutral branch, and business outcomes.
2. Place every multi-branch label near its source before the first crossing or convergence.
3. Expose matching hidden Svelte Flow handles for every selected four-side port.
4. Apply the static node silhouettes, icons, labels, accents, selection border, and keyboard focus ring.
5. Remove the predicate diamond marker.
6. Add stable occurrence markers and technical or accessible node IDs only for duplicate kind-plus-label groups.
7. Add complete accessible names for every original edge and node.

**Acceptance Criteria:**

- [ ] BR-01 through BR-08 pass.
- [ ] CT-01 through CT-05 pass.
- [ ] VG-01 through VG-04 and VG-06 through VG-08 pass.
- [ ] LR-06 and TP-08 pass.
- [ ] Every source with two or more outgoing edges has one visible branch label per edge.
- [ ] `true` and `false` display as `Yes` and `No` while accessible text retains the raw outcome.
- [ ] Repeated kind-plus-label nodes show stable `n of total` markers without putting opaque IDs in the main label.
- [ ] Selection, focus, node kind, and outcome semantics use distinct visual roles.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/edge-label.ts`
- `fachtracing-viewer/src/lib/graph/edge-label.test.ts`
- `fachtracing-viewer/src/lib/graph/BusinessNode.svelte`
- `fachtracing-viewer/src/lib/graph/BusinessEdge.svelte`
- `fachtracing-viewer/src/lib/graph/flow-types.ts`
- `fachtracing-viewer/src/app.css`

**Tests Required:**

- [ ] Edge-label mapping and placement tests
- [ ] Duplicate-label order and accessibility tests
- [ ] Node grammar light, dark, and monochrome visual tests
- [ ] Keyboard focus and selection-state tests

---

### Task 5: Render convergence and crossings without changing topology

**Status:** Pending  
**Estimated Effort:** XL  
**Dependencies:** Tasks 1 through 4  
**Priority:** High  
**IssueID:** None  
**Blocker:** None

**Description:**
Add presentation-only junctions, shared trunks, and crossing gaps. Preserve one semantic item and one exact ID for each original edge.

**Implementation Steps:**

1. Create convergence-lane and shared-trunk geometry for target in-degree of four or more.
2. Keep every feeder separate and independently focusable until the junction.
3. Detect route intersections and classify real shared geometry separately from crossings.
4. Render a 10-pixel gap or bridge on the lower-priority route at each unavoidable crossing.
5. Add focus and hover projection for feeder-plus-trunk routes.
6. Add accessible convergence counts and original edge membership without adding semantic graph nodes.

**Acceptance Criteria:**

- [ ] FN-01 through FN-08 and RT-13 pass.
- [ ] LR-03 passes.
- [ ] A twelve-to-one fan-in has one junction, one trunk, and twelve focusable feeders.
- [ ] A two-way reconverging diamond has no convergence junction.
- [ ] A crossing has no filled shape and creates no graph connectivity.
- [ ] The semantic graph count equals the original node and edge count before and after layout.
- [ ] Focusing any member edge highlights its feeder and the shared trunk.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/GraphJunctions.svelte` (create)
- `fachtracing-viewer/src/lib/graph/BusinessEdge.svelte`
- `fachtracing-viewer/src/lib/graph/FlowCanvas.svelte`
- `fachtracing-viewer/src/lib/graph/flow-types.ts`
- `fachtracing-viewer/src/lib/graph/layout-definition.ts`
- `fachtracing-viewer/src/lib/graph/route-quality.test.ts`

**Tests Required:**

- [ ] Fan-in threshold, feeder, junction, and shared-trunk tests
- [ ] Avoidable and unavoidable crossing tests
- [ ] Crossing-versus-junction accessibility tests
- [ ] Edge focus and exact ID mapping tests

---

### Task 6: Integrate the shared canvas and approve static graph quality

**Status:** Pending  
**Estimated Effort:** L  
**Dependencies:** Tasks 1 through 5  
**Priority:** High  
**IssueID:** None  
**Blocker:** None

**Description:**
Integrate the immutable layout with the read-only Svelte Flow canvas. Prove the complete static graph correction and preserve existing preview and run-view behavior.

**Implementation Steps:**

1. Map positioned nodes, routes, junctions, trunks, crossings, regions, and metrics to focused presentation components.
2. Reduce grid contrast for dense graphs and preserve node, edge, label, and arrow contrast in both themes.
3. Add graph-only Playwright references for every required state at 1,440 by 1,000.
4. Run the static graph semantic, geometry, visual, keyboard, privacy, and read-only journeys.
5. Run the existing upload and run-detail journeys to prove shared-canvas regression safety.
6. Run generated Fachtracing dogfood and review the real generated graph in both themes.
7. Record human approval of the complete reference set.

**Acceptance Criteria:**

- [ ] VG-05 and all remaining design gates pass.
- [ ] All findings ST-01 through LR-06 have passed through their mapped task.
- [ ] Planar references have zero avoidable crossings, zero node intrusion, and zero label collision.
- [ ] The image threshold is `0.2` and `maxDiffPixelRatio` is `0.005` with no graph-content masks.
- [ ] Developer graph V1 and business graph V1 render without contract changes.
- [ ] The browser-only upload remains local and read-only.
- [ ] Existing run highlights still map by original node and edge ID; no new run behavior is added.
- [ ] The complete light and dark reference set has human design approval.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/FlowCanvas.svelte`
- `fachtracing-viewer/src/lib/graph/BusinessNode.svelte`
- `fachtracing-viewer/src/lib/graph/BusinessEdge.svelte`
- `fachtracing-viewer/src/lib/graph/GraphJunctions.svelte`
- `fachtracing-viewer/src/app.css`
- `fachtracing-viewer/e2e/decision-explorer.spec.ts`
- `fachtracing-viewer/e2e/visual-fixtures.ts`
- `fachtracing-viewer/playwright.config.ts`
- `fachtracing-viewer/README.md`

**Tests Required:**

- [ ] `npm run check`
- [ ] `npm test`
- [ ] `npm run build`
- [ ] Graph-only Playwright tests in light and dark
- [ ] Existing upload and run-detail Playwright journeys
- [ ] Generated Fachtracing dogfood verification

## Finding-to-Task Coverage

| Finding group | Implementation task | Primary verification |
| --- | --- | --- |
| ST-01 through ST-10 | Task 2 | Placement and component geometry tests |
| RT-01 through RT-12, RT-14 | Task 3 | Route candidate and quality tests |
| RT-13 | Task 5 | Crossing-versus-junction tests |
| BR-01 through BR-08 | Task 4 | Port-slot, label, and branch visual tests |
| FN-01 through FN-08 | Task 5 | Fan-in, trunk, feeder, and crossing tests |
| TP-01 through TP-07 | Task 2 | Topology-analysis and placement tests |
| TP-08 | Task 4 | Static pattern recognition visual tests |
| CT-01 through CT-05 | Task 4 | Duplicate-context and accessibility tests |
| VG-01 through VG-04, VG-06 through VG-08 | Task 4 | Node and edge grammar tests |
| VG-05 | Task 6 | Dense-graph theme visual tests |
| LR-01, LR-02, LR-04, LR-05 | Task 3 | Direct-neighbor and corridor tests |
| LR-03 | Task 5 | Convergence focus tests |
| LR-06 | Task 4 | Edge accessible-name tests |
