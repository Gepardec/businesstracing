# Design: Static Graph Layout Quality

## Design Outcome

The static graph view will show one stable, compact, top-to-bottom representation of the supplied topology. Direct relationships stay local, branches state their meaning near the source, convergence is explicit, crossings remain visibly separate, and repeated labels remain distinguishable. The layout will not infer business semantics that the graph document does not contain.

## Governing Rules

1. Preserve topology before optimizing appearance.
2. Optimize local reasoning before global symmetry.
3. Use graph structure for layout and graph content for labels; never derive domain meaning from IDs or geometry.
4. Keep original node and edge IDs through every presentation stage.
5. Treat junctions, shared trunks, corridors, and crossing bridges as presentation data only.
6. Make each layout decision deterministic.
7. Keep run-state behavior outside the static graph model.

## Architecture Overview

The current pipeline asks ELK to place and route the graph in one pass. The replacement separates structure, placement, routing, and rendering so each stage has one responsibility.

```text
GraphModel
  -> topology analysis
  -> ranked placement graph
  -> ELK node placement
  -> orthogonal route planning
  -> convergence and crossing projection
  -> label placement
  -> immutable GraphLayout
  -> Svelte Flow presentation
```

ELK continues to place nodes because it provides mature layered ordering. A deterministic route planner owns ports, obstacles, corridors, shared trunks, crossings, and route scoring. Svelte Flow renders the returned geometry and remains read-only.

## Internal Models

### `TopologyAnalysis`

**Responsibility:** Describe structural relationships without assigning coordinates.

```ts
interface TopologyAnalysis {
  componentByNodeId: ReadonlyMap<string, string>;
  rankByNodeId: ReadonlyMap<string, number>;
  spineNodeIds: readonly string[];
  branchRegions: readonly BranchRegion[];
  convergenceGroups: readonly ConvergenceGroup[];
  stronglyConnectedComponents: readonly StronglyConnectedComponent[];
  duplicateLabels: ReadonlyMap<string, readonly string[]>;
  longEdgeIds: ReadonlySet<string>;
}
```

The analysis uses directed reachability, strongly connected components, and rank span. It does not read node label text to decide grouping or priority.

### `LayoutPort`

**Responsibility:** Identify an exact node-side connection slot.

```ts
type PortSide = 'north' | 'east' | 'south' | 'west';

interface LayoutPort {
  id: string;
  nodeId: string;
  edgeId: string;
  side: PortSide;
  slot: number;
  point: LayoutPoint;
}
```

Port IDs exist only in layout output. They never enter either graph JSON contract.

### `RenderedRoute`

**Responsibility:** Store the complete presentation route for one original graph edge.

```ts
interface RenderedRoute {
  edgeId: string;
  sourcePort: LayoutPort;
  targetPort: LayoutPort | LayoutJunctionPort;
  points: readonly LayoutPoint[];
  label: EdgeLabelLayout | null;
  sharedSegmentIds: readonly string[];
  crossingIds: readonly string[];
  length: number;
  bends: number;
}
```

### `LayoutJunction` and `SharedRouteSegment`

**Responsibility:** Show convergence without creating a graph node or graph edge.

```ts
interface LayoutJunction {
  id: string;
  targetNodeId: string;
  incomingEdgeIds: readonly string[];
  point: LayoutPoint;
}

interface SharedRouteSegment {
  id: string;
  junctionId: string;
  incomingEdgeIds: readonly string[];
  points: readonly LayoutPoint[];
}
```

The semantic node and edge list continues to contain only original graph items. A shared trunk is rendered once, but focusing any member edge highlights its feeder and the shared trunk.

### `RouteCrossing`

**Responsibility:** Mark an unavoidable geometric crossing without implying connectivity.

```ts
interface RouteCrossing {
  id: string;
  overEdgeId: string;
  underEdgeId: string;
  point: LayoutPoint;
  radius: number;
}
```

The lower route receives a gap or bridge. A crossing never receives a filled junction shape.

### `GraphLayout`

**Responsibility:** Return immutable node and presentation geometry to both browser and unit tests.

```ts
interface GraphLayout {
  nodes: readonly PositionedNode[];
  routes: readonly RenderedRoute[];
  junctions: readonly LayoutJunction[];
  sharedSegments: readonly SharedRouteSegment[];
  crossings: readonly RouteCrossing[];
  regions: readonly PositionedRegion[];
  width: number;
  height: number;
  metrics: LayoutQualityMetrics;
}
```

## Topology Analysis

### Components and entries

1. Build directed adjacency and reverse-adjacency maps once.
2. Use the declared `entryNodeIds` as roots.
3. Put nodes unreachable from declared entries in deterministic secondary components.
4. Sort components by the smallest reachable entry ID, then smallest node ID.

### Cycles

Use Tarjan's algorithm to identify strongly connected components. Collapse each component to one acyclic placement unit for rank calculation. Keep member nodes visible. Route edges that return to the same or an earlier rank through the component's outer cycle corridor.

### Ranks

Assign ranks on the collapsed directed acyclic graph:

- Each declared entry uses rank zero.
- A normal target uses one more than the maximum predecessor rank.
- A terminal outcome uses at least the maximum reachable non-terminal rank plus one.
- Nodes in one strongly connected component use adjacent sub-ranks inside the component region.

### Structural spine

Select one layout spine per connected component. The spine is the longest entry-to-terminal path after cycle collapse. Tie-break in this order:

1. More `next` or blank single-outgoing transitions.
2. Fewer long edges.
3. Fewer convergence targets.
4. Stable node ID sequence.

The spine is a geometry tool. The UI does not label it as the primary or executed path.

### Branch regions

For each node with out-degree greater than one:

1. Find the first common reachable convergence node, if one exists.
2. Assign each outgoing branch a lane between the source and convergence.
3. Keep branch descendants in that region until convergence or termination.
4. Balance branch regions around the source by measured subtree width.
5. Use geometric mirroring only to reduce crossings. Preserve stable Boolean ordering otherwise.

### Long edges

An edge is long when its target rank is more than two ranks after its source or when it exits its branch region. Long edges use the nearest outer corridor. The layout marks both ends with the same concise continuation reference. The reference includes source and target labels in accessible text.

### Duplicate labels

Group nodes by normalized kind plus exact visible label. When a group has more than one node, assign a stable occurrence index by component, rank, horizontal position, then node ID. The node shows `n of total`. The node ID appears only in accessible or technical detail.

### Structural region presentation

Branch regions are layout constraints by default. Do not draw a background box around each branch because nested branch boxes would add clutter. Branch labels and sibling lanes show the branch structure.

A strongly connected component with two or more visible nodes can use one neutral dashed enclosure labelled `Cycle`. A disconnected component that has no declared entry can use one neutral dashed enclosure labelled `Component`. An entry-connected acyclic component has no enclosure. Region labels do not use node-kind or run-state colors, do not receive graph connectivity, and do not enter the semantic node or edge count.

## Node Placement

### ELK responsibility

ELK receives the collapsed placement graph, ranks, node dimensions, region constraints, and model order. It places node rectangles but does not provide final edge routes.

Recommended placement options:

```text
elk.algorithm=layered
elk.direction=DOWN
elk.edgeRouting=UNDEFINED
elk.layered.considerModelOrder.strategy=NODES_AND_EDGES
elk.layered.spacing.nodeNodeBetweenLayers=72
elk.spacing.nodeNode=48
elk.padding=[top=32,left=32,bottom=32,right=32]
```

The implementation can adjust values by no more than 16 pixels to satisfy measured clearances. A value change requires a visual baseline update and a Decision Log entry.

### Placement correction pass

After ELK returns node boxes:

1. Align spine centers to the component median X corridor.
2. Center a single child under its parent.
3. Center sibling-region bounds around their source.
4. Move branch regions inward until the next move would violate node or corridor clearance.
5. Put outcomes in the final reachable rank.
6. Keep component rectangles at least 96 pixels apart.

The correction pass must preserve rank order and must not create node overlap.

## Orthogonal Route Planning

### Obstacles and visibility graph

Build a sparse rectilinear visibility graph from:

- Exact north, east, south, and west port slots
- Node bounds expanded by 16 pixels
- Reserved branch-label rectangles expanded by 6 pixels
- Branch-region boundaries
- Convergence lanes
- Long-edge and cycle corridors
- Existing routes expanded by their congestion clearance

Candidate coordinates come from port points and obstacle sides. The route planner must not create a dense pixel grid.

### Port slots

- A side with one edge uses its center.
- Multiple side slots use at least 12 pixels of separation.
- Slots keep at least 16 pixels from rounded node corners.
- Normal top-to-bottom transitions prefer south to north only when this is not longer than a side route by more than 8 pixels.
- A target outside the source horizontal span prefers east or west when that route is shorter.
- A target above the source can use north only for a cycle or proven long return edge.

The Svelte nodes expose matching hidden handles for every selected port. Read-only handles remain invisible and non-interactive.

### Candidate scoring

Evaluate every valid source-side and target-side pair. Reject a candidate if it:

- Enters an unrelated node's expanded obstacle
- Enters a reserved branch-label rectangle
- Leaves its branch region without being a long or cycle edge
- Uses an avoidable upward first segment in an acyclic top-to-bottom transition
- Contains a non-port segment shorter than 16 pixels

Score remaining candidates lexicographically:

1. Unavoidable node intrusion count; must be zero
2. Avoidable crossing count
3. Unrelated-region traversal count
4. Congested shared-corridor distance
5. Manhattan length
6. Bend count when length differs by no more than 8 pixels
7. Backtracking distance
8. Stable source side, target side, slot, and edge ID

An implementation test enumerates the same valid candidates. It fails when another candidate has fewer violations or is more than 8 pixels shorter with equal higher-order costs.

### Routing order and refinement

Route edges in this order:

1. Local spine edges
2. Local branch edges by branch region
3. Convergence feeders
4. Long edges
5. Cycle edges

Within a group, use rank span, source rank, source position, and edge ID. After the first pass, run at most two deterministic refinement passes for avoidable crossings. Stop early when the route-quality vector does not improve.

## Branch Labels

### Display policy

| Raw outcome and source shape | Visible label |
| --- | --- |
| `true` | `Yes` |
| `false` | `No` |
| `yes` or `no`, any case | `Yes` or `No` |
| `next` with one outgoing edge | Hidden |
| Blank with one outgoing edge | Hidden |
| Blank with multiple outgoing edges | `Branch n` |
| Other nonblank value | Trimmed business outcome |

The full raw outcome remains in the edge title and accessible name.

### Placement policy

- Put the label after the source clearance and before the first crossing, junction, or 40% route point.
- Keep at least 8 pixels from node bounds and 6 pixels from another label.
- Align the label to the route segment. Add a short leader only when a collision moves it off the segment.
- Keep sibling branch labels at distinct vertical or horizontal positions.
- Do not hide multi-branch labels because of zoom or edge state.

## Convergence Design

For target in-degree one through three, use distinct target-side slots. For target in-degree four or more:

1. Place a horizontal convergence lane 40 to 64 pixels before the target.
2. Route each original edge as an independently focusable feeder to a stable slot on the lane.
3. Render one small neutral junction at the lane-to-trunk connection.
4. Render one shared trunk from the junction to the target.
5. Put the exact incoming count in the junction's accessible label.
6. On edge hover or focus, emphasize its feeder, the relevant lane part, the junction, and the shared trunk.

The threshold of four avoids adding junctions to normal two-way diamonds while fixing edge combs.

## Crossing Design

Crossing minimization runs before crossing rendering. A crossing remains only when all tested alternatives have a worse route-quality vector.

- The route with lower structural priority gets a 10-pixel gap centered on the crossing.
- The higher-priority route remains continuous.
- The gap has no fill and no node-like border.
- Edge focus increases both separated route parts together.
- A crossing contributes no graph connectivity and no semantic-list item.

The generated non-planar fixture proves that an unavoidable crossing remains understandable. Planar fixtures must have zero crossings.

## Node Grammar for Static Graphs

| Kind | Silhouette | Accent | Required content |
| --- | --- | --- | --- |
| Entry | Capsule | Entry accent | Icon, `Entry`, business label |
| Predicate | Common card | Predicate accent | Icon, `Predicate`, business label |
| Choice | Restrained six-sided outline | Choice accent | Icon, `Choice`, business label |
| Computation | Common card | Neutral action accent | Icon, `Computation`, business label |
| Dispatch | Common card with one clipped corner | Dispatch accent | Icon, `Dispatch`, business label |
| Outcome | Rounded terminal card | Neutral outcome accent | Icon, `Outcome`, business label |
| Coverage gap | Dashed common card | Coverage accent | Warning icon, `Coverage gap`, business label |

Use a 232-by-92-pixel base node. A node can grow to 280 pixels wide for a label that otherwise exceeds three lines. Nodes in the same rank use the maximum height of that rank. Remove the predicate diamond marker. Keep the kind label smaller and lower contrast than the business label.

## Visual Hierarchy

- Business labels meet WCAG 4.5:1 text contrast.
- Normal edges and arrowheads meet 3:1 non-text contrast against the canvas.
- The graph grid stays below normal edge contrast in both themes.
- Dense graphs reduce grid opacity by 40% when edge count divided by node count is greater than 2.5.
- Unfocused long corridors use 75% of normal edge opacity and return to full opacity on hover or focus.
- Selection uses a neutral two-pixel border.
- Keyboard focus uses an outer two-pixel ring with a two-pixel offset.
- Node-kind accents do not represent selection, success, failure, or run state.

## Accessibility

Each original edge has this accessible name:

```text
<visible branch label or "Continuation"> from <source label> to <target label>
```

If a source has multiple unlabeled branches, include the neutral branch number. Each duplicate node adds its occurrence and exact opaque node ID to its accessible description. Each convergence junction announces the target label and incoming edge count, but it is not presented as a business node.

The hidden semantic list preserves one item per original graph node and edge. Presentation-only regions, junctions, trunks, and crossings do not change graph counts.

## Component Responsibilities

| Component | Single responsibility |
| --- | --- |
| `topology-analysis.ts` | Derive structural regions, ranks, cycles, convergence groups, duplicates, and long edges. |
| `layout-definition.ts` | Coordinate placement and assemble immutable `GraphLayout`. |
| `route-planner.ts` | Select ports and collision-free orthogonal routes. |
| `route-quality.ts` | Calculate objective route and graph metrics. |
| `edge-label.ts` | Map and place branch labels. |
| `flow-types.ts` | Define Svelte Flow presentation data. |
| `BusinessNode.svelte` | Render one graph node and its selected hidden ports. |
| `BusinessEdge.svelte` | Render one original edge route and its label. |
| `GraphJunctions.svelte` | Render presentation-only junctions, trunks, and crossing gaps. |
| `FlowCanvas.svelte` | Compose layout output into the read-only graph canvas. |

`BusinessEdge.svelte` must not choose ports or repair routes. Layout logic stays outside Svelte components.

## Layout Quality Metrics

```ts
interface LayoutQualityMetrics {
  nodeOverlaps: number;
  unrelatedNodeIntrusions: number;
  labelCollisions: number;
  avoidableCrossings: number;
  unavoidableCrossings: number;
  totalManhattanLength: number;
  totalBends: number;
  backtrackingDistance: number;
  longEdgeCorridorViolations: number;
  branchRegionViolations: number;
}
```

Metrics are test and diagnostic data. They are not persisted and do not enter graph JSON.

## Generated Fixture Matrix

| Fixture | Purpose | Gate |
| --- | --- | --- |
| 12-node chain | Spine and rank alignment | Zero crossings; spine center drift at most 32 pixels; one bend maximum per local edge |
| Balanced binary branch | Sibling and branch regions | Zero crossings; labels near source; each child in next rank |
| Reconverging diamond | Branch plus normal merge | Zero crossings; distinct feeder routes; no junction below threshold |
| Twelve-to-one fan-in | Convergence | One junction and trunk; twelve focusable feeders; no edge comb at target |
| Fixed-port detour | Four-side routing | East or west source port; no valid route more than 8 pixels shorter |
| Long shortcut | Outer corridor | No unrelated-region traversal; matching continuation references |
| Duplicate labels | Context | Stable occurrence markers and exact accessible IDs |
| Multiple entries and outcomes | Component ordering | Entries first, reachable outcomes last, stable component order |
| Cycle | Strongly connected component | Loopback in one outer corridor; downward acyclic structure preserved |
| Non-planar crossing | Crossing grammar | Minimum known crossing count; visible bridge or gap; no false junction |
| Generated 250-node graph | Regression safety | Existing two-second unit gate; complete node and edge mapping |
| Real generated project graph | Visual proof | No hard-coded topology; human approval in both themes |

Fixtures must be generated from small topology builders. Do not commit hand-positioned nodes or hard-coded route points.

## Test Gates

### Geometry gates

- Node overlap count equals zero.
- Unrelated-node intrusion count equals zero.
- Label-node and label-label collision counts equal zero on planar fixtures.
- Adjacent route clearance is at least 12 pixels unless routes share an explicit trunk.
- Node clearance is at least 16 pixels.
- Non-port segments are at least 16 pixels.
- Simple local edges have no more than four bends and no avoidable backtracking.
- Every selected route passes candidate optimality comparison.

### Semantic gates

- Every original node and edge appears exactly once in the semantic graph.
- Every source with two or more outgoing edges has one visible label per edge.
- Boolean labels map to `Yes` and `No` without changing raw outcomes.
- Junctions and crossings do not change graph topology or item counts.
- Duplicate occurrence markers remain stable across five repeated layouts.

### Visual gates

- Capture graph-only baselines at 1,440 by 1,000 in light and dark themes.
- Include the balanced branch, fan-in, cycle, crossing, duplicate-label, and real generated graph states.
- Use Playwright image comparison with `threshold: 0.2` and `maxDiffPixelRatio: 0.005`.
- Do not mask nodes, edges, labels, junctions, crossings, or graph bounds.
- Require human approval of the complete reference set.

### Regression gates

- Run `npm run check`, `npm test`, `npm run build`, and graph-only Playwright tests.
- Run the existing browser-only upload journey.
- Run the existing run-detail graph journey to prove ID-based highlighting still works.
- Run generated Fachtracing dogfood and confirm every graph node and edge renders.

## Failure Behavior

- If topology analysis fails, show the existing generic layout error without graph details that expose internal paths.
- If route planning cannot find a collision-free route, retry once with the nearest outer corridor.
- If the retry fails, return a layout error. Do not draw an edge through a node.
- If branch-label placement cannot meet clearance, use a source-adjacent leader label. Do not hide a multi-branch label.
- If layout exceeds the existing safety time limit in unit tests, fail the test. Do not silently fall back to fixed ports.

## Technical Decisions

### Keep ELK for node placement

**Decision:** Keep ELK for layered node placement and remove final route ownership from it.

**Rationale:** ELK already provides stable ranks and crossing-aware node order. The defect comes from the one-pass fixed-port contract and missing presentation semantics. A separate router adds the needed control without replacing mature placement logic.

### Use presentation-only junctions

**Decision:** Add junction and shared-trunk geometry only after graph normalization.

**Rationale:** Large fan-in cannot remain traceable when every edge enters one node independently. A presentation junction solves the visual defect while the semantic graph remains unchanged.

### Do not infer business groups

**Decision:** Use only structural group labels and explicit node attributes.

**Rationale:** Generic graphs can describe any application. Inferring groups such as identity or search from label text would add hidden product rules and can misstate the graph.

### Keep one shared canvas

**Decision:** Extend the current `FlowCanvas` presentation model instead of adding a second graph renderer.

**Rationale:** Preview and run views must show identical topology. The static graph mode does not add run behavior, but the shared renderer prevents layout divergence.

### Dependency Decisions

No new dependency is introduced. The design uses the existing Svelte 5, SvelteKit, Svelte Flow, ELK, Tailwind CSS, shadcn-svelte, Vitest, and Playwright stack.

## Risks and Mitigations

- **Risk:** A custom orthogonal router adds algorithmic complexity. **Mitigation:** Keep ELK placement, use a sparse visibility graph, separate route scoring, and cover each topology class with generated fixtures.
- **Risk:** Shared trunks can hide edge identity. **Mitigation:** Keep feeders focusable, retain original edge IDs, and highlight the complete feeder-plus-trunk route.
- **Risk:** Virtual layout items can enter the graph contract by accident. **Mitigation:** Define them in layout-only types and assert that normalized graph objects remain byte-for-byte unchanged.
- **Risk:** Route refinement can become slow on dense graphs. **Mitigation:** Use at most two deterministic refinement passes and preserve the existing 250-node safety gate.
- **Risk:** Visual baselines can hide a semantic regression. **Mitigation:** Keep geometry and semantic assertions as primary gates; images add composition proof.

## Rollout

1. Add topology fixtures and record the current failures.
2. Add topology analysis and placement correction.
3. Add four-side route planning and branch labels.
4. Add convergence and crossing projection.
5. Update node and edge grammar.
6. Run the complete quality matrix and approve reference images.

## No New Contracts

This design changes no SQL schema, HTTP method, graph JSON schema, run JSON schema, persisted payload, or public Java API. All new types are private frontend presentation types.
