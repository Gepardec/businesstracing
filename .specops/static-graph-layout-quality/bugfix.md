# Bug Fix: Static Graph Layout Quality

## Problem Statement

The static graph viewer preserves the graph topology, but it does not turn that topology into a readable diagram. Large generated graphs drift across the canvas, use remote branches, create dense edge combs, hide branch meaning, and make crossings look like merges. A user must trace individual pixels instead of recognizing the graph structure.

This specification corrects the static graph view. It does not define run selection, an executed path, a current step, or runtime evidence. Those states belong to the run view.

## Scope Boundary

**In scope:** Static topology analysis, node placement, orthogonal routing, branch labels, fan-in, crossings, duplicate-node context, graph node grammar, and graph-layout verification.

**Out of scope:** Initial zoom, fit-to-view policy, run-path highlighting, current-step state, execution evidence, run explanations, graph editing, saved positions, server persistence, new JSON versions, and production benchmarking.

The shared canvas must continue to support the run view, but this specification adds no new run-state behavior.

## Root Cause Analysis

The layout pipeline sends a layered, top-to-bottom graph to ELK with one fixed north target port and one fixed south source port per edge. It accepts the returned route if the route avoids unrelated nodes. The renderer receives only points and a label position, so it cannot explain port choice, shared routing, crossings, or merge structure.

The pipeline has no topology-analysis stage. It does not identify a stable structural spine, sibling branches, convergence regions, strongly connected components, repeated labels, or unavoidable crossings before it places nodes. ELK can therefore produce a valid geometric result that has poor local reasoning and a large bounding box.

The tests verify determinism, node count, a two-second 250-node safety case, distinct parallel paths, and node intrusion. They do not verify route optimality, compactness, branch semantics, edge traceability, crossings, fan-in quality, or the relationship between a source and its direct destinations.

**Affected Components:**

- `layout-definition.ts`, which builds the ELK input and accepts its routes
- `layout-client.ts` and `layout-engine.ts`, which run the same layout in browser and tests
- `FlowCanvas.svelte`, which maps layout results to Svelte Flow
- `BusinessNode.svelte`, which exposes only top and bottom handles
- `BusinessEdge.svelte`, which cannot distinguish crossings, shared trunks, or branch origins
- Edge label and route helpers
- Graph unit tests and graph-preview Playwright tests
- The graph preview and the run view, which share `FlowCanvas`

## Impact Assessment

- **Severity:** High
- **Users Affected:** Every user who inspects a non-trivial static graph
- **Frequency:** Often for branching graphs and always for graphs with large fan-in or long return edges
- **Business Impact:** The viewer can display correct data while failing to communicate the business-logic structure. This weakens the main purpose of the graph view.

## Reproduction

1. Load a generated graph with a long chain, several alternate branches, and multiple edges that converge on one predicate.
2. Inspect the graph without using run data.
3. Follow one outgoing edge from a predicate to its target.
4. Observe that the edge can leave from the bottom, travel away from the target, cross unrelated regions, and enter a distant node through a crowded top port.
5. Inspect the dense convergence above a shared target.
6. Observe that overlapping and parallel routes cannot be traced back to their sources and that a crossing can look like a merge.
7. Expected: Direct relationships are local, branches have clear meaning, convergence is explicit, and each edge can be followed from source to target.
8. Actual: The diagram is collision-free in narrow terms but structurally difficult to read.

## Complete Visual Audit

### A. Structural shape

| ID | Severity | Current problem | Required correction |
| --- | --- | --- | --- |
| ST-01 | High | The node sequence drifts diagonally instead of forming a stable top-to-bottom structure. | Select a deterministic structural spine and keep its node centers within one 32-pixel corridor unless an obstacle makes this impossible. |
| ST-02 | High | Parent and child alignment changes without structural cause. | Align a single direct child with its parent. Center sibling sets around their source. |
| ST-03 | High | Direct branches can be far from their source. | Place every direct child in the next valid rank and in the nearest free branch lane. |
| ST-04 | High | Related branches do not form visible subtrees. | Keep a branch inside a bounded structural region until it converges or ends. |
| ST-05 | Medium | Large empty regions appear inside connected components. | Compact ranks and lanes after routing while preserving clearance gates. |
| ST-06 | Medium | The graph bounding box grows because remote routes reserve arbitrary columns. | Reserve named routing corridors from topology, not from incidental edge order. |
| ST-07 | Medium | The hierarchy is visually unbalanced. | Balance sibling subtrees by measured bounds, not node ID order alone. |
| ST-08 | Medium | Sequential, alternative, cyclic, and converging regions use the same arrangement. | Give each structural region a distinct placement rule without inventing business meaning. |
| ST-09 | Medium | Multiple entries and disconnected components have no clear arrangement. | Put entries in the first rank and place disconnected components in stable columns with component spacing. |
| ST-10 | Medium | Outcomes can appear inside active decision ranks. | Put terminal outcomes in the last reachable rank unless a cycle makes this impossible. |

### B. Route choice and corridors

| ID | Severity | Current problem | Required correction |
| --- | --- | --- | --- |
| RT-01 | High | Edges do not use the shortest valid orthogonal route. | Compare all valid source and target port pairs and select the lowest-cost collision-free route. |
| RT-02 | High | An edge uses a bottom port when its target is clearly left or right. | Expose north, east, south, and west ports and prefer the closest valid side. |
| RT-03 | High | An edge can start by moving away from its target. | Reject avoidable first-segment backtracking. |
| RT-04 | High | Long routes cross unrelated graph regions. | Add crossing, congestion, and unrelated-region penalties to route selection. |
| RT-05 | High | Branches leave their structural subtree. | Route normal branches inside their assigned branch region. Use an outer corridor only for a proven long or cyclic edge. |
| RT-06 | Medium | Routes contain avoidable bends and hooks. | Minimize Manhattan length first and bend count second within an 8-pixel length tolerance. |
| RT-07 | Medium | Short route segments create tight hooks. | Keep non-port route segments at least 16 CSS pixels long at layout scale. |
| RT-08 | Medium | Parallel routes are too close or overlap. | Keep distinct parallel routes at least 12 CSS pixels apart, except for an explicit shared trunk. |
| RT-09 | Medium | Edges pass too close to unrelated nodes. | Keep routes at least 16 layout pixels from unrelated node bounds. |
| RT-10 | Medium | Routing columns divide the canvas without structural meaning. | Create corridors only for branch regions, long edges, cycles, and convergence. |
| RT-11 | Medium | Long horizontal routes create artificial visual boundaries. | Prefer local downward routing and move necessary long routes to an outer corridor. |
| RT-12 | Medium | Route destinations can be outside the local reading region. | Add source and destination references to a long-edge label and make the complete route focusable. |
| RT-13 | High | A crossing and a merge look identical. | Detect crossings after routing and render a bridge or gap on the lower-priority route. Render a junction only for a real convergence group. |
| RT-14 | Medium | Route order changes can change the diagram. | Use stable graph IDs only as the final tie-breaker after geometry and topology costs. |

### C. Branch origins and meaning

| ID | Severity | Current problem | Required correction |
| --- | --- | --- | --- |
| BR-01 | High | Several outgoing edges leave one indistinct area. | Give each outgoing edge a stable port slot with at least 12 pixels between adjacent slots. |
| BR-02 | High | The user cannot associate a label with its source branch. | Put each branch label within the first 40% of its route and before the first crossing or merge. |
| BR-03 | High | Predicate branches do not consistently show their result. | Show a label for every edge when source out-degree is two or more. |
| BR-04 | High | Raw Boolean text exposes implementation language. | Present `true` as `Yes` and `false` as `No`; keep the raw value in the accessible description. |
| BR-05 | Medium | A single `next` label adds noise. | Hide `next` and a blank outcome only when the source has exactly one outgoing edge. |
| BR-06 | Medium | Two unlabeled branches are indistinguishable. | Use neutral `Branch 1`, `Branch 2`, and so on. Do not invent a business result. |
| BR-07 | Medium | Left and right branch placement is unstable. | Keep Boolean branch order stable: `Yes` uses the first branch lane and `No` uses the second, mirrored only when this reduces crossings. |
| BR-08 | Medium | A short branch and a very long branch use the same treatment. | Mark long edges as continuation routes and put them in a dedicated outer corridor. |

### D. Fan-in, merges, and crossings

| ID | Severity | Current problem | Required correction |
| --- | --- | --- | --- |
| FN-01 | High | Four or more incoming edges create a dense comb above the target. | Create a presentation-only convergence junction and shared trunk for target in-degree of four or more. |
| FN-02 | High | Incoming edges cannot be traced to their source. | Keep each feeder separate until the junction and highlight its feeder plus shared trunk on hover or keyboard focus. |
| FN-03 | High | A predicate acts visually as both a decision and a large merge. | Keep the predicate node semantic role unchanged and place convergence before the node. |
| FN-04 | High | Shared route segments hide edge multiplicity. | Render a shared trunk once and expose the incoming edge count and edge list through accessible text. |
| FN-05 | Medium | Merge order appears meaningful when it is only incidental. | Order feeders by geometric position, then stable edge ID, and do not number them unless labels are absent. |
| FN-06 | Medium | Incoming edges approach from arbitrary distant directions. | Group feeders by source branch region before they enter the convergence lane. |
| FN-07 | Medium | Unavoidable crossings are not identifiable. | Add a visible crossing bridge and preserve separate focus targets for both edges. |
| FN-08 | Medium | A crossing marker could be mistaken for a graph node. | Use no filled node shape at a crossing; only interrupt or bridge the lower route. |

### E. Topology comprehension

| ID | Severity | Current problem | Required correction |
| --- | --- | --- | --- |
| TP-01 | High | The graph has no clear structural start. | Anchor every declared entry in the first rank and use entry-node grammar. |
| TP-02 | High | Terminal results are hard to locate. | Anchor reachable outcomes in the final rank and keep their node grammar distinct. |
| TP-03 | High | The user cannot distinguish sequential flow from alternatives and rejoining paths. | Use alignment for sequence, separated lanes for alternatives, and explicit junctions for convergence. |
| TP-04 | High | Execution order is unclear in the static topology. | Make rank order follow directed reachability from entries; do not imply runtime order among sibling alternatives. |
| TP-05 | Medium | Long edges make remote nodes look locally related. | Keep direct local relationships short and mark long continuation routes at both ends. |
| TP-06 | Medium | Cycles can distort the primary downward structure. | Detect strongly connected components and route loopback edges through one outer cycle corridor. |
| TP-07 | Medium | Structural grouping can be confused with business grouping. | Label structural containers only as `Branch`, `Cycle`, or `Component`; never infer domain names from node text. |
| TP-08 | Medium | The user must follow pixels to understand the graph. | Make sequence, branch, convergence, cycle, and terminal patterns recognizable without reading edge IDs. |

### F. Duplicate labels and node context

| ID | Severity | Current problem | Required correction |
| --- | --- | --- | --- |
| CT-01 | High | Repeated labels are visually indistinguishable. | Add a compact occurrence marker such as `2 of 3` only when kind and label repeat. |
| CT-02 | High | The user cannot refer to one repeated node unambiguously. | Include the opaque node ID in the node's accessible description and technical disclosure. |
| CT-03 | Medium | The viewer can invent semantic context that is absent from the graph. | Show explicit graph attributes as context only when present. Never derive context from an ID, route, or label pattern. |
| CT-04 | Medium | A long technical ID can dominate the node. | Keep IDs out of the main label and reveal them on focus, selection, or technical disclosure. |
| CT-05 | Medium | It is unclear whether duplicates are shared nodes or separate nodes. | Keep one visual node per graph node ID and expose incoming and outgoing counts in its accessible summary. |

### G. Node and edge visual grammar

| ID | Severity | Current problem | Required correction |
| --- | --- | --- | --- |
| VG-01 | High | Most node kinds have similar weight, while predicate decoration is repetitive. | Use at most silhouette, icon, and type label; remove the predicate diamond marker. |
| VG-02 | High | A predicate does not visually expose its branch ports. | Align visible branch labels and hidden connection slots with the source side; keep edit handles hidden. |
| VG-03 | Medium | Node type chrome competes with the business label. | Keep the business label primary and the kind label secondary. |
| VG-04 | Medium | The graph lacks a focal structural hierarchy. | Give nodes higher contrast than normal edges and give entry and outcome silhouettes distinct geometry. |
| VG-05 | Medium | The dotted grid competes with dense routing. | Reduce grid contrast when edge density exceeds the defined threshold. |
| VG-06 | Medium | Arrowheads are too subtle at branches and merges. | Use a 10-to-12-pixel arrowhead with the same contrast as its edge. |
| VG-07 | Medium | Normal routes, borders, and grid points have similar tone. | Keep normal edges at 3:1 non-text contrast against the canvas and keep the grid below edge contrast. |
| VG-08 | Medium | Selection and focus can be confused with node kind. | Use a neutral selected border and an outer focus ring. Do not use a node-kind color for interaction state. |

### H. Local reasoning

| ID | Severity | Current problem | Required correction |
| --- | --- | --- | --- |
| LR-01 | High | A predicate does not show both destinations locally. | Keep direct destinations in adjacent branch lanes or provide a labelled continuation reference when distance is unavoidable. |
| LR-02 | High | Sibling branches cannot be compared. | Put sibling roots on the same rank and keep their labels close to the common source. |
| LR-03 | High | A merge requires searching for remote incoming edges. | Show the convergence count and make every feeder independently focusable. |
| LR-04 | Medium | Geometric proximity does not match logical relationship. | Optimize direct-neighbor distance before global symmetry. |
| LR-05 | Medium | Distant lines dominate unrelated local areas. | De-emphasize unselected long corridors and increase them only on hover or focus. |
| LR-06 | Medium | Keyboard users cannot inspect one edge from source to target. | Give every original edge an accessible name with branch label, source label, and target label. |

## Audit Coverage

The issue groups above cover every accepted point from the prior visual audit:

- Prior points 1–10 map to ST-01 through ST-10.
- Prior points 11–29 map to RT-01 through RT-14 and FN-07 through FN-08.
- Prior points 30–43 map to BR-01 through BR-08 and TP-03 through TP-04.
- Prior points 44–53 map to FN-01 through FN-06 and LR-03.
- Prior points 54–63 map to TP-01 through TP-08.
- Prior points 64–69 map to CT-01 through CT-05.
- Prior points 70–80 map to VG-01 through VG-08 and CT-05.
- Prior points 88–96 map to VG-03 through VG-08.
- Prior points 97–102 map to LR-01 through LR-06.
- Prior points 81–87 are excluded because they describe an active run, not a static graph.

## Regression Risk Analysis

### Blast Radius

| Area | Current correct behavior to preserve |
| --- | --- |
| Browser layout worker | The browser computes layout without blocking the SvelteKit server. |
| Node test layout | Unit tests use the same layout definition as the browser. |
| Developer graph V1 | All validated nodes and edges render exactly once. |
| Business graph V1 | The merged multi-source business format renders without conversion loss. |
| Graph preview | A selected file stays in the browser and is not stored or uploaded. |
| Run detail | The shared canvas still maps node and edge IDs to existing run highlights. |
| Search and selection | Node search and node click continue to select the exact graph node ID. |
| Large graph safety | The existing generated 250-node case still completes within its current two-second test limit. |
| Accessibility | Every node and original edge remains keyboard reachable through the canvas or semantic list. |
| Read-only graph | Nodes remain non-draggable and non-connectable; layout data never enters graph JSON. |

### Behavior Inventory and Coverage

| Behavior | Existing coverage | Gap | Risk tier |
| --- | --- | --- | --- |
| Deterministic top-to-bottom layout | `graph.test.ts` | Does not test stable structural regions | Must-Test |
| 250-node safety case | `graph.test.ts` | Does not test routing quality at scale | Must-Test |
| No edge enters unrelated nodes | Unit and Playwright geometry checks | Clearance is not measured | Must-Test |
| Parallel edges have distinct paths | Unit and Playwright checks | Shared-trunk exception is not modeled | Must-Test |
| Both V1 graph formats render | Contract and Playwright tests | New presentation metadata must not alter contracts | Must-Test |
| Run highlights map by exact IDs | Run browser journey | New virtual layout items could break mapping | Must-Test |
| Browser-only upload | Graph preview journey | Independent from layout internals | Nice-To-Test |
| Search focuses the exact node | 250-node browser journey | Independent from route computation | Nice-To-Test |

### Scope Escalation Check

**Scope:** Contained frontend bugfix. The fix adds presentation-only topology and route models, but it does not change the graph contracts, storage, or backend. The new internal models stay inside the graph layout boundary.

## Proposed Fix

Add a deterministic topology-analysis stage before node placement. It identifies entries, terminal nodes, structural spines, sibling regions, convergence groups, cycles, repeated labels, and long edges without inferring business semantics.

Let ELK place ranked node boxes. Replace fixed north/south routing with a deterministic orthogonal router that evaluates ports on all four sides, node and label obstacles, branch regions, shared convergence trunks, crossings, congestion, length, and bends. Preserve every original edge ID and add presentation-only junction and crossing data after layout.

Update the node and edge renderers to show branch meaning near its source, distinguish merges from crossings, disambiguate repeated labels, and expose complete accessible edge descriptions. Add generated topology fixtures and objective layout-quality gates before visual approval.

## Unchanged Behavior

- WHEN a valid developer graph V1 is loaded THE SYSTEM SHALL CONTINUE TO render each model node and edge exactly once in the semantic graph.
- WHEN a valid business graph V1 is loaded THE SYSTEM SHALL CONTINUE TO preserve all normalized IDs, labels, kinds, outcomes, entries, and coverage gaps.
- WHEN the graph canvas is used in a run detail THE SYSTEM SHALL CONTINUE TO apply existing run highlights by original node and edge ID.
- WHEN the browser graph preview reads a file THE SYSTEM SHALL CONTINUE TO avoid server writes, network uploads, and browser storage.
- WHEN a graph layout is recomputed with the same input THE SYSTEM SHALL CONTINUE TO return the same node positions and rendered route metadata.
- WHEN the 250-node generated safety graph is laid out THE SYSTEM SHALL CONTINUE TO finish within the existing two-second unit-test limit.

## Testing Plan

### Current Behavior

- WHEN the fixed-port detour fixture is laid out THE SYSTEM CURRENTLY routes at least one right-side destination through a south source port and a longer path.
- WHEN the twelve-to-one convergence fixture is laid out THE SYSTEM CURRENTLY creates a dense set of independent routes at the target.
- WHEN the crossing fixture is rendered THE SYSTEM CURRENTLY gives no visual distinction between a crossing and a merge.
- WHEN a multi-branch predicate is rendered THE SYSTEM CURRENTLY can hide branch labels at the current canvas state.

### Expected Behavior

- WHEN a target is outside the source horizontal span THE SYSTEM SHALL select east or west when that port produces the shortest valid route.
- WHEN another valid route is more than 8 layout pixels shorter THE SYSTEM SHALL reject the chosen route.
- WHEN two valid routes differ by no more than 8 layout pixels THE SYSTEM SHALL select the route with fewer bends.
- WHEN source out-degree is two or more THE SYSTEM SHALL show a branch label for every outgoing edge.
- WHEN target in-degree is four or more THE SYSTEM SHALL create one presentation-only convergence junction and one shared trunk without changing original edge IDs.
- WHEN two routes cross THE SYSTEM SHALL avoid the crossing when a valid lower-cost route exists; otherwise it SHALL render a bridge or gap.
- WHEN node kind and label repeat THE SYSTEM SHALL show an occurrence marker and expose the exact node ID outside the main label.
- WHEN the graph contains a cycle THE SYSTEM SHALL keep the acyclic structure top-to-bottom and route loopbacks through one outer cycle corridor.

### Unchanged Behavior

- All Must-Test behaviors in the regression table SHALL pass.
- Existing contract parser tests SHALL pass unchanged.
- Existing run-highlight unit and browser tests SHALL pass unchanged.
- Existing graph privacy and read-only assertions SHALL pass unchanged.

## Acceptance Criteria

- [ ] Every finding ST-01 through LR-06 has an implementation task and at least one objective verification method.
- [ ] Fixed north/south edge ports no longer exist as the only routing option.
- [ ] Generated chain, branch, diamond, fan-in, cycle, duplicate-label, long-edge, and unavoidable-crossing fixtures pass their defined geometry gates.
- [ ] Simple planar fixtures have zero avoidable crossings and zero unrelated-node intrusions.
- [ ] Every unavoidable crossing has a visible bridge or gap and is not rendered as a junction.
- [ ] Every fan-in of four or more uses a presentation-only convergence junction with accessible edge membership.
- [ ] Every multi-outgoing source shows distinct, source-adjacent branch labels.
- [ ] The graph JSON contracts and stored payloads do not change.
- [ ] All Must-Test unchanged behaviors pass.
- [ ] Light and dark reference images receive human design approval.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep each component to one responsibility.
- Do not add hard-coded diagrams or graph positions.
- Generate all test topology from fixtures or real project output.
