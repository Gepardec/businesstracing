# Implementation Tasks: Real Graph Layout Usability

## Spec-Level Dependencies

| Dependent spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `static-graph-layout-quality` | This follow-up tightens the completed topology-first placement and routing implementation. | Yes | Completed |

## Task Breakdown

### Task 1: Add generic real-graph review metrics and evidence harness

**Status:** Completed
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

- [x] All RB-04 metrics have pure deterministic tests.
- [x] The command accepts both supported V1 graph formats.
- [x] Missing optional evidence does not fail generic tests.
- [x] No production or test source contains fixed node positions, route points, or a copied supplied diagram.
- [x] A failed gate returns a non-zero status and names the failed metric.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/route-quality.ts`
- `fachtracing-viewer/src/lib/graph/route-quality.test.ts`
- `fachtracing-viewer/src/lib/graph/graph-fixtures.ts`
- `fachtracing-viewer/scripts/review-graphs.ts` (create)
- `fachtracing-viewer/package.json`

**Tests Required:**

- [x] Metric unit tests
- [x] Review-command success and failure tests
- [x] Graph-contract compatibility tests

---

### Task 2: Add explicit layout and viewport state

**Status:** Completed
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

- [x] RB-01, RB-02, RB-06, and the first two RB-08 criteria pass.
- [x] Reading view starts at an effective 12-pixel business-label floor.
- [x] Overview keeps all original nodes and edges loaded.
- [x] Pending and failed states are accessible.
- [x] Replacing a graph cannot show a stale prior layout.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/FlowCanvas.svelte`
- `fachtracing-viewer/src/lib/graph/layout-client.ts`
- `fachtracing-viewer/src/lib/graph/FitGraph.svelte`
- `fachtracing-viewer/src/lib/graph/FocusCurrent.svelte`
- `fachtracing-viewer/src/lib/graph/graph-viewport.ts` (create)
- `fachtracing-viewer/src/lib/graph/graph-viewport.test.ts` (create)
- `fachtracing-viewer/src/lib/graph/GraphLayoutStatus.svelte` (create)

**Tests Required:**

- [x] Layout state transition tests
- [x] Stale-result cancellation test
- [x] Reading and Overview viewport tests
- [x] Safe-control rectangle tests

---

### Task 3: Preserve and select balanced ELK placement profiles

**Status:** Completed
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

- [x] RB-03 passes for all generated profiles.
- [x] Placement is byte-for-byte deterministic across five runs.
- [x] The 45-node evidence no longer uses the 888-by-7,112 flat-rank placement when available.
- [x] The 55-node evidence no longer uses the 7,104-pixel global rank row when available.
- [x] Node overlap, forward-order violations, and unrelated-node intrusion remain zero.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/placement-profiles.ts` (create)
- `fachtracing-viewer/src/lib/graph/placement-profiles.test.ts` (create)
- `fachtracing-viewer/src/lib/graph/layout-definition.ts`
- `fachtracing-viewer/src/lib/graph/layout-engine.ts`
- `fachtracing-viewer/src/lib/graph/layout-client.ts`
- `fachtracing-viewer/src/lib/graph/topology-analysis.ts`

**Tests Required:**

- [x] Placement profile determinism tests
- [x] Aspect-ratio and chain-exception tests
- [x] Component and cycle packing tests
- [x] Deep and wide topology regression tests

---

### Task 4: Refine route sets and dense shared corridors

**Status:** Completed
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

- [x] All RB-04 criteria pass.
- [x] RG-MEGA-WARNINGS has zero avoidable crossings, zero branch violations, zero label collisions, and no route above the defined detour limit.
- [x] Each supplied graph stays at or below 0.5 crossing bridges per edge.
- [x] Shared routes preserve every original edge ID and accessible endpoint description.
- [x] Route output is deterministic across five runs.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/route-planner.ts`
- `fachtracing-viewer/src/lib/graph/route-planner.test.ts`
- `fachtracing-viewer/src/lib/graph/route-quality.ts`
- `fachtracing-viewer/src/lib/graph/layout-definition.ts`
- `fachtracing-viewer/src/lib/graph/GraphJunctions.svelte`
- `fachtracing-viewer/src/lib/graph/BusinessEdge.svelte`

**Tests Required:**

- [x] Candidate-relative detour tests
- [x] Route-set refinement tests
- [x] Dense convergence and cycle-corridor tests
- [x] Edge-identity and accessibility tests

---

### Task 5: Add static selection and readable local focus

**Status:** Completed
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

- [x] RB-05 and RB-06 pass.
- [x] Exact-ID search visibly identifies one exact node.
- [x] Duplicate labels report the stable occurrence count.
- [x] Selection is visually separate from keyboard focus and all run or status states.
- [x] The selected node stays outside fixed-control rectangles.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/FlowCanvas.svelte`
- `fachtracing-viewer/src/lib/graph/BusinessNode.svelte`
- `fachtracing-viewer/src/lib/graph/flow-types.ts`
- `fachtracing-viewer/src/lib/graph/graph-viewport.ts`
- `fachtracing-viewer/src/app.css`

**Tests Required:**

- [x] Exact-ID, label, duplicate, and no-match tests
- [x] Static selection state test
- [x] Local-neighborhood framing test
- [x] Keyboard focus precedence test

---

### Task 6: Verify the complete static graph acceptance slice

**Status:** Completed
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

- [x] RB-01 through RB-08 pass.
- [x] All three supplied graphs pass the objective review gates when available.
- [x] Manual visual review approves Reading and Overview for all three available graphs.
- [x] Both V1 graph formats keep exact graph counts and IDs.
- [x] The 19-to-55-node layouts meet the four-second local POC gate.
- [x] No CI, SQL, HTTP, persistence, or graph-contract file changes.

**Files to Modify:**

- `fachtracing-viewer/e2e/decision-explorer.spec.ts`
- `fachtracing-viewer/e2e/visual-fixtures.ts`
- `fachtracing-viewer/src/lib/graph/graph.test.ts`
- `fachtracing-viewer/src/lib/runs/run-highlight.test.ts`
- `fachtracing-viewer/README.md`

**Tests Required:**

- [x] Svelte diagnostics
- [x] Graph unit tests
- [x] Graph-only browser journeys
- [x] Real-file review command
- [x] Manual screenshot approval

---

### Task 7: Correct route direction, detour proof, and label attachment

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 4
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Replace the false route-quality proof with a candidate baseline that cannot hide shorter routes. Keep outer corridors as fallbacks and keep branch labels attached to their edges.

**Implementation Steps:**

1. Add route-direction and full-candidate detour metrics.
2. Make route length and directional progress rank before crossing avoidance for collision-free candidates.
3. Stop topology rank span from forcing an outer corridor.
4. Keep label placement on the route or within 24 layout pixels and remove detached leaders.
5. Re-run the supplied direction graph and inspect the three branches from its first choice node.

**Acceptance Criteria:**

- [x] The rejected 665-pixel outer loop uses a bounded in-layout route.
- [x] The review report fails wrong-way boundary exits and uses the full valid candidate baseline.
- [x] No visible edge label uses a detached leader.
- [x] Existing node-intrusion, endpoint, cycle, crossing, and edge-identity behavior stays correct.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/route-planner.ts`
- `fachtracing-viewer/src/lib/graph/route-quality.ts`
- `fachtracing-viewer/src/lib/graph/route-planner.test.ts`
- `fachtracing-viewer/src/lib/graph/route-quality.test.ts`
- `fachtracing-viewer/src/lib/graph/BusinessEdge.svelte`
- `fachtracing-viewer/src/lib/graph/layout-review.ts`

**Tests Required:**

- [x] Directional-route unit tests
- [x] Honest detour-baseline unit tests
- [x] Generic graph-route regression tests
- [x] Supplied direction-graph route inspection

---

### Task 8: Remove destructive focus and empty zoom states

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 7
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Keep navigation useful without fading the graph or hiding node text. Remove the focus-summary badge and validate Explore plus Overview on all supplied graphs.

**Implementation Steps:**

1. Rename Reading to Explore and keep the existing readable viewport anchor.
2. Remove static context opacity from nodes, edges, junctions, crossings, and regions.
3. Remove the focus-summary badge.
4. Keep node kind and business-label text present at every supported zoom.
5. Keep route labels present and show a fixed-size node readout below normal reading zoom.
6. Use a secondary quiet solid style for long return or reference routes and a solid tinted boundary for cycle regions.
7. Update browser checks and inspect complete screenshots at normal, selected, and overview states.

**Acceptance Criteria:**

- [x] Selecting or searching does not fade unrelated static topology.
- [x] Zooming out does not create empty node boxes or remove route labels.
- [x] A node at small zoom exposes its complete label in a fixed-size hover readout.
- [x] The canvas toolbar has no focus-summary badge.
- [x] Long routes are secondary until inspection and cycle boundaries do not look like routes.
- [x] Explore opens at a readable anchor and Overview still fits the complete topology.
- [x] All three supplied graphs pass direct full-resolution visual review.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/FlowCanvas.svelte`
- `fachtracing-viewer/src/lib/graph/BusinessNode.svelte`
- `fachtracing-viewer/src/lib/graph/BusinessEdge.svelte`
- `fachtracing-viewer/src/lib/graph/GraphJunctions.svelte`
- `fachtracing-viewer/src/lib/graph/flow-types.ts`
- `fachtracing-viewer/e2e/decision-explorer.spec.ts`
- `fachtracing-viewer/README.md`

**Tests Required:**

- [x] Svelte diagnostics
- [x] Browser zoom-text and hover-readout check
- [x] Browser no-static-dimming check
- [x] Long-route secondary-style check
- [x] Supplied graph screenshots at Explore and Overview scales

---

### Task 9: Derive a reversible readable business map

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 8
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Reduce visual noise without changing the JSON contract. Group straight action sequences and parallel connections for the default map. Preserve original IDs and provide Full detail.

**Implementation Steps:**

1. Add a pure graph-presentation transform with forward and reverse ID mappings.
2. Group safe unlabelled computation chains, convergent rule chains, and shared-exit guard chains.
3. Group parallel presentation edges and combine their displayed outcomes.
4. Map search, selection, accessibility, and run highlights through the presentation.
5. Add Readable and Full detail controls and state the reduction.
6. Verify the transform on fixtures and the three supplied graphs.

**Acceptance Criteria:**

- [x] The readable map has no more nodes or connections than the source graph.
- [x] Full detail restores exact source node and edge counts.
- [x] No material predicate, choice, outcome, entry, labelled transition, branch, or merge is hidden in a sequence.
- [x] Search and run highlights resolve every original ID.
- [x] The transform is deterministic and has no graph-specific data.

**Tests Required:**

- [x] Pure presentation-transform tests
- [x] Search and highlight mapping tests
- [x] Browser Readable and Full detail tests
- [x] Supplied graph reduction report and screenshots

---

### Task 10: Explain each graph in three sentences

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 9
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add a generic short explanation that helps a user identify the start, main paths, work, and possible results before reading the map.

**Acceptance Criteria:**

- [x] The explanation has at most three actual sentences.
- [x] It names supplied entry, branch, and outcome labels when they exist.
- [x] It does not invent a domain fact.
- [x] It remains useful for multiple entries, chains, branches, cycles, and incomplete graphs.
- [x] The graph preview displays the explanation before the canvas.

**Tests Required:**

- [x] Summary unit tests for all topology fixtures
- [x] Browser summary semantics and responsive layout checks
- [x] Human review on the three supplied graphs

---

### Task 11: Lay out Explore as an independent local graph

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Tasks 8 and 9
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Stop complete-graph coordinates from separating local neighbors. Build and lay out a bounded local graph for Explore while keeping the complete layout ready for Overview.

**Implementation Steps:**

1. Derive an opening context that follows a bounded straight path to the first material split.
2. Derive direct predecessor-and-successor context after selection.
3. Run the local graph through compact ELK spacing and retry with standard spacing when compact routing is not safe.
4. Fit all local nodes inside the safe canvas and state how the user continues.
5. Preserve a pending Overview request when the complete worker result arrives.

**Acceptance Criteria:**

- [x] Complete-graph coordinates do not create empty corridors inside Explore.
- [x] The initial Explore view shows the first material split when it is within the bounded opening path.
- [x] A selected node shows its direct local topology.
- [x] Every supplied default local view has zero node intrusions and label collisions, and each visible card is at least 160 by 60 CSS pixels at the tested desktop size.
- [x] Selecting Overview during layout cannot leave the canvas empty.

**Tests Required:**

- [x] Opening-context and bounded-chain unit tests
- [x] Real-file local geometry checks
- [x] Pending-Overview browser regression tests
- [x] Light and dark screenshots for the three supplied files

## Requirement Coverage

| Requirement | Tasks |
| --- | --- |
| RB-01 | 2, 6 |
| RB-02 | 2, 6, 11 |
| RB-03 | 3, 6 |
| RB-04 | 1, 4, 6 |
| RB-05 | 5, 6 |
| RB-06 | 2, 5, 6 |
| RB-07 | 1, 6 |
| RB-08 | 2, 3, 4, 6 |
| RB-09 | 9, 10, 11 |
| Rejected route evidence | 7 |
| Rejected focus and zoom evidence | 8 |

## Cross-Spec Blockers

None. The required `static-graph-layout-quality` dependency is completed.

## Progress Tracking

- Total Tasks: 11
- Completed: 11
- In Progress: 0
- Blocked: 0
- Pending: 0
