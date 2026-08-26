# Implementation Tasks: Real Graph Layout Usability

## Spec-Level Dependencies

| Dependent spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `static-graph-layout-quality` | This follow-up tightens the completed topology-first placement and routing implementation. | Yes | Completed |

## Task Breakdown

### Task 1: Add generic real-graph review metrics and evidence harness

**Status:** Pending  
**Estimated Effort:** M  
**Dependencies:** None  
**Priority:** High  
**IssueID:** None  
**Blocker:** None

**Description:**
Extend the pure layout-quality model and add a file-driven local review command. Keep graph contents outside production code and generated test fixtures free of fixed coordinates or routes.

**Implementation Steps:**

1. Add candidate-relative detour, crossing-density, parallel-corridor-density, and avoidable-detour metrics.
2. Add generated deep, wide, cyclic, dense convergence, duplicate-label, and multi-entry profiles.
3. Create `review:graphs` with arbitrary file arguments and JSON plus table output.
4. Add objective gate diagnostics that identify the offending edge, label, region, or crossing.
5. Record the expected hashes for optional local evidence without copying its topology into a fixture.

**Acceptance Criteria:**

- [ ] All RB-04 metrics have pure deterministic tests.
- [ ] The command accepts both supported V1 graph formats.
- [ ] Missing optional evidence does not fail generic tests.
- [ ] No production or test source contains fixed node positions, route points, or a copied supplied diagram.
- [ ] A failed gate returns a non-zero status and names the failed metric.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/route-quality.ts`
- `fachtracing-viewer/src/lib/graph/route-quality.test.ts`
- `fachtracing-viewer/src/lib/graph/graph-fixtures.ts`
- `fachtracing-viewer/scripts/review-graphs.ts` (create)
- `fachtracing-viewer/package.json`

**Tests Required:**

- [ ] Metric unit tests
- [ ] Review-command success and failure tests
- [ ] Graph-contract compatibility tests

---

### Task 2: Add explicit layout and viewport state

**Status:** Pending  
**Estimated Effort:** M  
**Dependencies:** Task 1  
**Priority:** High  
**IssueID:** None  
**Blocker:** None

**Description:**
Make asynchronous layout visible and separate Reading view from Overview without changing the loaded topology.

**Implementation Steps:**

1. Add idle, arranging, ready, and failed layout states with request IDs.
2. Render the arranging surface and accessible busy message in the canvas.
3. Ignore stale worker results after graph replacement.
4. Add `reading` and `overview` mode controls.
5. Move viewport calculations into a pure `graph-viewport.ts` module.
6. Add safe-rectangle measurement for fixed controls.

**Acceptance Criteria:**

- [ ] RB-01, RB-02, RB-06, and the first two RB-08 criteria pass.
- [ ] Reading view starts at an effective 12-pixel business-label floor.
- [ ] Overview keeps all original nodes and edges loaded.
- [ ] Pending and failed states are accessible.
- [ ] Replacing a graph cannot show a stale prior layout.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/FlowCanvas.svelte`
- `fachtracing-viewer/src/lib/graph/layout-client.ts`
- `fachtracing-viewer/src/lib/graph/FitGraph.svelte`
- `fachtracing-viewer/src/lib/graph/FocusCurrent.svelte`
- `fachtracing-viewer/src/lib/graph/graph-viewport.ts` (create)
- `fachtracing-viewer/src/lib/graph/graph-viewport.test.ts` (create)
- `fachtracing-viewer/src/lib/graph/GraphLayoutStatus.svelte` (create)

**Tests Required:**

- [ ] Layout state transition tests
- [ ] Stale-result cancellation test
- [ ] Reading and Overview viewport tests
- [ ] Safe-control rectangle tests

---

### Task 3: Preserve and select balanced ELK placement profiles

**Status:** Pending  
**Estimated Effort:** L  
**Dependencies:** Task 1  
**Priority:** High  
**IssueID:** None  
**Blocker:** None

**Description:**
Stop reconstructing flat global ranks. Preserve full ELK placement candidates, normalize components, and select the best topology-safe placement with one deterministic score.

**Implementation Steps:**

1. Define the bounded placement profiles from the design.
2. Preserve full ELK coordinates and normalize them per component.
3. Keep topology rank as ordering validation instead of direct row assignment.
4. Pack disconnected components and compact cycle regions.
5. Score each profile with provisional routes and the lexicographic placement score.
6. Keep the chosen profile and metric report in immutable layout output.

**Acceptance Criteria:**

- [ ] RB-03 passes for all generated profiles.
- [ ] Placement is byte-for-byte deterministic across five runs.
- [ ] The 45-node evidence no longer uses the 888-by-7,112 flat-rank placement when available.
- [ ] The 55-node evidence no longer uses the 7,104-pixel global rank row when available.
- [ ] Node overlap, forward-order violations, and unrelated-node intrusion remain zero.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/placement-profiles.ts` (create)
- `fachtracing-viewer/src/lib/graph/placement-profiles.test.ts` (create)
- `fachtracing-viewer/src/lib/graph/layout-definition.ts`
- `fachtracing-viewer/src/lib/graph/layout-engine.ts`
- `fachtracing-viewer/src/lib/graph/layout-client.ts`
- `fachtracing-viewer/src/lib/graph/topology-analysis.ts`

**Tests Required:**

- [ ] Placement profile determinism tests
- [ ] Aspect-ratio and chain-exception tests
- [ ] Component and cycle packing tests
- [ ] Deep and wide topology regression tests

---

### Task 4: Refine route sets and dense shared corridors

**Status:** Pending  
**Estimated Effort:** L  
**Dependencies:** Tasks 1 and 3  
**Priority:** High  
**IssueID:** None  
**Blocker:** None

**Description:**
Use candidate-relative detour and route-set scoring to remove corridor walls, remaining avoidable crossings, branch violations, and extreme detours.

**Implementation Steps:**

1. Retain the shortest valid candidate for each edge.
2. Add the three-pass stable route-set refinement from the design.
3. Evaluate shared trunks for repeated convergence and cycle corridors.
4. Keep feeder identity and accessible edge semantics on shared presentation geometry.
5. Emit crossing bridges only after the final set-level refinement.
6. Enforce the RB-04 gate set in the layout report and review command.

**Acceptance Criteria:**

- [ ] All RB-04 criteria pass.
- [ ] RG-MEGA-WARNINGS has zero avoidable crossings, zero branch violations, zero label collisions, and no route above the defined detour limit.
- [ ] Each supplied graph stays at or below 0.5 crossing bridges per edge.
- [ ] Shared routes preserve every original edge ID and accessible endpoint description.
- [ ] Route output is deterministic across five runs.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/route-planner.ts`
- `fachtracing-viewer/src/lib/graph/route-planner.test.ts`
- `fachtracing-viewer/src/lib/graph/route-quality.ts`
- `fachtracing-viewer/src/lib/graph/layout-definition.ts`
- `fachtracing-viewer/src/lib/graph/GraphJunctions.svelte`
- `fachtracing-viewer/src/lib/graph/BusinessEdge.svelte`

**Tests Required:**

- [ ] Candidate-relative detour tests
- [ ] Route-set refinement tests
- [ ] Dense convergence and cycle-corridor tests
- [ ] Edge-identity and accessibility tests

---

### Task 5: Add static selection and readable local focus

**Status:** Pending  
**Estimated Effort:** M  
**Dependencies:** Tasks 2 and 3  
**Priority:** High  
**IssueID:** None  
**Blocker:** None

**Description:**
Turn search focus into persistent static selection and frame the selected node's local topology inside the safe Reading viewport.

**Implementation Steps:**

1. Return structured exact-ID and label search matches.
2. Store `selectedNodeId` separately from run highlight state.
3. Apply Svelte Flow selected state and a neutral selected style.
4. Calculate predecessor-and-successor neighborhood bounds.
5. Keep no-match behavior non-destructive.
6. Preserve selection and view mode across theme and size changes.

**Acceptance Criteria:**

- [ ] RB-05 and RB-06 pass.
- [ ] Exact-ID search visibly identifies one exact node.
- [ ] Duplicate labels report the stable occurrence count.
- [ ] Selection is visually separate from keyboard focus and all run or status states.
- [ ] The selected node stays outside fixed-control rectangles.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/FlowCanvas.svelte`
- `fachtracing-viewer/src/lib/graph/BusinessNode.svelte`
- `fachtracing-viewer/src/lib/graph/flow-types.ts`
- `fachtracing-viewer/src/lib/graph/graph-viewport.ts`
- `fachtracing-viewer/src/app.css`

**Tests Required:**

- [ ] Exact-ID, label, duplicate, and no-match tests
- [ ] Static selection state test
- [ ] Local-neighborhood framing test
- [ ] Keyboard focus precedence test

---

### Task 6: Verify the complete static graph acceptance slice

**Status:** Pending  
**Estimated Effort:** M  
**Dependencies:** Tasks 1 through 5  
**Priority:** High  
**IssueID:** None  
**Blocker:** None

**Description:**
Run objective and human review for the three supplied graphs, both graph contracts, responsive viewports, shared canvas regressions, and POC responsiveness. Do not add CI configuration.

**Implementation Steps:**

1. Add browser journeys for pending layout, Reading, Overview, selection, no match, resize, and failure.
2. Run the generic review command on the three hashed evidence files when available.
3. Capture Reading and Overview references for each available graph in light and dark themes.
4. Verify the complete acceptance decision tasks from `bugfix.md`.
5. Verify all Must-Test behavior without adding active-run features.
6. Update the viewer README with the view modes and review command.

**Acceptance Criteria:**

- [ ] RB-01 through RB-08 pass.
- [ ] All three supplied graphs pass the objective review gates when available.
- [ ] A human approves Reading and Overview for all three available graphs.
- [ ] Both V1 graph formats keep exact graph counts and IDs.
- [ ] The 19-to-55-node layouts meet the four-second local POC gate.
- [ ] No CI, SQL, HTTP, persistence, or graph-contract file changes.

**Files to Modify:**

- `fachtracing-viewer/e2e/decision-explorer.spec.ts`
- `fachtracing-viewer/e2e/visual-fixtures.ts`
- `fachtracing-viewer/src/lib/graph/graph.test.ts`
- `fachtracing-viewer/src/lib/runs/run-highlight.test.ts`
- `fachtracing-viewer/README.md`

**Tests Required:**

- [ ] Svelte diagnostics
- [ ] Graph unit tests
- [ ] Graph-only browser journeys
- [ ] Real-file review command
- [ ] Manual screenshot approval

## Requirement Coverage

| Requirement | Tasks |
| --- | --- |
| RB-01 | 2, 6 |
| RB-02 | 2, 6 |
| RB-03 | 3, 6 |
| RB-04 | 1, 4, 6 |
| RB-05 | 5, 6 |
| RB-06 | 2, 5, 6 |
| RB-07 | 1, 6 |
| RB-08 | 2, 3, 4, 6 |

## Cross-Spec Blockers

None. The required `static-graph-layout-quality` dependency is completed.

