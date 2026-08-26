# Design: Real Graph Layout Usability

## Design Outcome

The graph preview will open as a readable inspection tool. It will show progress while layout runs, focus the entry at reading scale, provide an explicit complete overview, mark search selection, and route dense real graphs with bounded visual complexity. All presentation remains derived from input topology.

## Governing Decisions

1. Explore and Overview are separate viewport modes.
2. Explore renders a separately laid-out local subgraph. Overview keeps the complete current presentation.
3. Top-to-bottom flow remains the default direction.
4. ELK supplies candidate placement coordinates; Fachtracing evaluates and selects placement and routes.
5. Route acceptance uses set-level quality, not only local collision safety.
6. Static selection is separate from run state and status colors; selection never fades unrelated static topology.
7. Fixed controls define safe rectangles that viewport calculations must exclude.
8. Real graph files are review inputs, not production configuration or hard-coded diagrams.

## Presentation Pipeline

```text
GraphModel
  -> reversible readable presentation
  -> three-sentence business summary
  -> topology analysis
  -> deterministic placement profiles
  -> placement normalization and scoring
  -> route planning and set-level refinement
  -> layout-quality report
  -> GraphPresentationState
  -> Svelte Flow
       -> compact Explore layout
       -> complete Overview layout
       -> selected-node focus
```

The pipeline keeps one responsibility per module. Svelte components render layout and interaction state. They do not repair geometry.

## State Model

```ts
type LayoutPhase =
  | { kind: 'idle' }
  | { kind: 'arranging'; requestId: number; nodeCount: number; edgeCount: number }
  | { kind: 'ready'; requestId: number; layout: LayoutResult }
  | { kind: 'failed'; requestId: number; message: string };

type ViewMode = 'explore' | 'overview';
type DetailMode = 'readable' | 'full';

interface GraphPresentationState {
  phase: LayoutPhase;
  viewMode: ViewMode;
  detailMode: DetailMode;
  selectedNodeId: string | null;
  focusRevision: number;
}
```

`requestId` is monotonic. Only the current request can replace the active layout. A new graph immediately changes the phase to `arranging` and clears the old selection.

## Layout Progress

`FlowCanvas` receives the graph and creates the request state before it calls the worker. The canvas renders one neutral shadcn-style status surface above the graph background. The message contains node and edge counts. It uses `aria-busy=true` and a polite live region.

The worker remains the only place that runs ELK and route planning. No progress animation changes graph coordinates. Reduced-motion mode uses a static progress icon.

## Viewport Modes

### Explore view

Explore is the default after layout. It does not reuse complete-graph coordinates. It derives a local subgraph and runs the same deterministic placement and routing pipeline again with compact spacing. Compact spacing is accepted only when it produces a safe route set. If it fails, Explore retries with the standard spacing. It enters Overview only when both local attempts fail.

For the initial entry, the local subgraph follows a bounded straight setup path. It stops after four linear presentation nodes or at the first node with more than one distinct successor. When it finds a split, it includes all immediate alternatives. This makes the first material decision visible without opening the complete topology.

After selection or search, the local subgraph contains the selected node, its direct predecessors, and its direct successors. The toolbar states the visible node count and tells the user to select a node to continue. Selecting a visible node creates a new local layout. It does not fade any visible node or connection.

Explore fits the local layout inside the safe canvas rectangle. When that context cannot fit at the normal context zoom, the viewport keeps the focused node at zoom `0.86`. This keeps the 14-pixel business label at approximately 12 CSS pixels. Complete-graph corridors cannot move local neighbors apart because they are not part of the local layout.

### Overview

Overview fits the complete layout inside the safe canvas rectangle. It can use any supported zoom. Node kind, business-label text, and non-empty route labels remain rendered at every supported zoom. Natural canvas scaling can make the text small, but the renderer never replaces a node with an empty box. Below zoom `0.72`, pointer hover shows a fixed-size readout with the complete node kind and business label.

Selecting a node changes to Explore view and frames that node's neighborhood.

The complete layout starts before the local layout. The component caches it for Overview. If the user requests Overview while layout is pending, the completed global result is applied directly instead of starting or discarding a local layout.

### Safe canvas rectangle

The usable rectangle is the canvas box minus measured overlay rectangles and 16-pixel gutters. It excludes:

- Search and mode toolbar
- Zoom controls
- Minimap or large-graph guide
- Svelte Flow attribution

`FitGraph` and `FocusCurrent` become small viewport coordinators that consume a shared calculated safe rectangle. They do not own separate fit policies.

## Placement Strategy

### Preserve ELK placement

The current placement keeps only ELK `x` ordering and rebuilds `y` from global topology rank. The new placement normalizes the full ELK `x` and `y` result for each connected component. Topology ranks become ordering constraints and quality checks, not fixed row numbers.

### Deterministic profiles

The layout engine evaluates a bounded profile set. Each profile uses top-to-bottom layered ELK with stable input order and fixed options. Profiles can vary only:

- Layering strategy
- Node-placement strategy
- Compaction alignment
- Inter-layer spacing within documented bounds
- Component packing direction

No profile reads graph labels, graph IDs, application names, or current browser state.

### Placement score

Candidate selection is lexicographic. A later criterion cannot compensate for a failed earlier criterion.

1. Node overlap count
2. Forward-order and cycle-region violations
3. Unrelated-node intrusion count after provisional routes
4. Branch-region violations
5. Avoidable crossings
6. Route crossing density
7. Maximum candidate-relative detour
8. Total candidate-relative detour
9. Aspect-ratio penalty outside 0.25 to 2.25 for non-chain graphs
10. Total area
11. Stable profile ID

A structural chain is a connected component where at least 90 percent of nodes belong to one entry-to-terminal spine and no node has more than one non-terminal branch. The aspect-ratio band does not apply to that component.

### Components and cycles

Disconnected components are laid out independently and packed after route scoring. Multi-node strongly connected components remain one local cycle region. A cycle region uses its own internal top-to-bottom placement and reserves a nearby return corridor before global routes are planned.

## Route Planning Changes

### Candidate-relative detour

For each edge, the planner retains the shortest valid orthogonal candidate discovered for the selected placement. A route's detour ratio is:

```text
selected route length / shortest valid candidate length
```

The ratio uses every collision-free, terminal-safe, obstacle-aware candidate before crossing or corridor preferences are applied. A crossing cannot remove a shorter candidate from the detour baseline. A topology rank span or cycle loopback can allow an outer candidate, but neither can make that candidate preferred over a shorter collision-free route.

### Direction and label attachment

Route scoring uses this order: node intrusion, terminal reversal, short internal segment, wrong-way boundary excursion, route length, flow-port tie-break, backtracking, bends, corridor fallback, crossings, and congestion. The flow-port score only resolves candidates with the same safe route length. It prefers south-to-north ports for forward top-to-bottom flow, horizontal ports for return flow, and facing ports for same-rank flow. For a target below a source, a candidate that leaves above the complete layout loses to a collision-free in-bounds candidate. The same rule applies in reverse for a target above a source.

The label planner first tests positions along the route. It does not move a label more than 24 layout pixels from its route anchor and does not render a detached leader. A label can move to another clear fraction of the same route when its first position collides with a node or another label.

Normal decision routes use a solid 1.5-pixel stroke. A long return or reference route uses a lighter 1.1-pixel solid stroke. Pointer or keyboard inspection restores the primary route style. A cycle return uses the shortest collision-free route outside its cycle bounds. A cycle region uses a tinted solid boundary only when its rectangle does not contain an unrelated node.

### Route-set refinement

Refinement operates on the route set:

1. Identify edges that participate in crossings, branch violations, dense parallel corridors, or detour breaches.
2. Re-evaluate these edges in stable severity and edge-ID order.
3. Compare a candidate against the complete route-set score.
4. Accept only a strict lexicographic improvement.
5. Stop after three deterministic passes or a pass with no changes.

The pass limit prevents unbounded work. The chosen route set remains deterministic.

### Shared corridor evaluation

When three or more edges approach the same target or pass through the same narrow convergence lane, create one candidate convergence group. Compare separate routes with a presentation-only junction and trunk. Choose the shared candidate when it reduces crossings or parallel-corridor density without hiding individual feeder identity.

Repeated cycle returns can share a presentation corridor when they have the same cycle region boundary. The shared corridor does not create a semantic graph edge.

### Crossing density

`RouteCrossing` continues to show bridges for remaining crossings. The quality report adds:

```ts
interface LayoutQualityMetrics {
  // existing fields
  crossingDensity: number;
  parallelCorridorDensity: number;
  maximumNormalDetourRatio: number;
  maximumLongDetourRatio: number;
  avoidableDetours: number;
}
```

`crossingDensity` is `crossing count / edge count`. Shared route segments are counted once for geometry and retain member edge IDs for semantics.

## Search Selection

Search returns a `SearchMatch` rather than directly moving the viewport.

```ts
interface SearchMatch {
  nodeId: string;
  occurrenceIndex: number;
  occurrenceTotal: number;
  matchedBy: 'id' | 'label';
}
```

`FlowCanvas` stores `selectedNodeId`. The corresponding Svelte Flow node receives `selected: true`. Search selection uses a neutral primary selection border. Keyboard focus remains an outer ring. No run-current badge, run-path color, success color, failure color, or coverage color is used.

The focus coordinator calculates the selected node's local neighborhood and changes to Explore. A no-match result changes only the status message.

## Generic Review Harness

Create a local command that accepts one or more JSON paths:

```text
npm run review:graphs -- <graph-a.json> <graph-b.json>
```

The command:

1. Parses each file through the current graph adapter.
2. Computes the layout with the production layout engine.
3. Emits JSON and a concise table with graph count, duration, dimensions, crossings, density, detours, collisions, branch violations, and backtracking.
4. Returns non-zero when an objective acceptance gate fails.
5. Never writes node coordinates or routes into source files.

Generated topology fixtures cover the same generic shapes in normal automated tests. The three supplied files remain optional local evidence identified by hash.

## Component Responsibilities

| Component | Responsibility |
| --- | --- |
| `topology-analysis.ts` | Components, spine, branch regions, cycle regions, and ordering constraints |
| `layout-definition.ts` | Profile coordination, normalized placement, immutable result, and metrics |
| `placement-profiles.ts` | Bounded ELK profile definitions and candidate scoring |
| `route-planner.ts` | Ports, valid orthogonal candidates, shared corridors, and set-level refinement |
| `route-quality.ts` | Pure metric calculations and gate diagnostics |
| `layout-client.ts` | Worker lifecycle and complete or compact spacing selection |
| `graph-viewport.ts` | Opening context, direct context, viewport bounds, and safe rectangle calculations |
| `FlowCanvas.svelte` | Presentation state and Svelte Flow composition |
| `GraphLayoutStatus.svelte` | Busy and failure presentation |
| `BusinessNode.svelte` | Node level of detail and static selected appearance |
| `scripts/review-graphs.ts` | Generic file-driven local acceptance report |
| `graph-presentation.ts` | Reversible action-sequence grouping, parallel-connection grouping, original-ID mappings, and generic business summary |
| `graph-guide.ts` | Pure focused-step, incoming-context, and continuation model |
| `GraphGuide.svelte` | Persistent decision summary, sequence details, and source-derived navigation |

## Readable Business Map

The source graph remains immutable. A pure presentation transform runs before layout. It can group a maximal computation chain only when each internal connection is unlabelled, each internal node has one presentation predecessor and one presentation successor, and no internal node is an entry. It can also group a safe predicate sequence when Boolean results converge on the same next rule, or when consecutive guards have one shared exit and one continuation. It can group parallel edges only when their presentation endpoints are equal. Internal sequence edges remain in the mapping but do not need separate geometry.

A presentation node and edge contain the ordered original IDs that they represent. Search resolves original IDs and labels to their presentation node. Run state marks a presentation item when any mapped original ID is active or belongs to the recorded path. Full detail bypasses the transform and restores the original graph without reparsing the file.

The business summary uses only graph topology and supplied labels. It states the declared entry, the first material alternatives, and the distinct terminal results in no more than three sentences. It can mention a return path, but it does not replace business meaning with rule or action counts and does not infer domain facts that are absent from the JSON.

## Business Explanation Workspace

Static graph preview places a persistent explanation panel on the right side of the canvas. The safe viewport rectangle ends before this panel, so graph nodes and routes cannot appear below it. The panel uses the current presentation node, but it retains the ordered original labels for every grouped sequence.

The panel always shows the decision summary. In Explore it also shows the focused step, its type, all sequence members, immediate continuations, and optional incoming context. A continuation uses a supplied edge outcome when one exists. If the outcome is empty or `next`, the graph draws no invented route label and the panel identifies the continuation by its target business label. Selecting a continuation reuses the existing node-selection path and creates a new local Explore layout.

In Overview the panel labels the canvas as a topology map. Pointer hover shows the full node kind and label in the panel, and node selection returns to Explore. The graph-upload page does not repeat the decision explanation above the canvas. Run pages do not enable this panel because they already provide recorded-step evidence beside the graph.

## Failure Behavior

- A layout failure displays the existing error and clears busy state.
- A stale worker result has no effect.
- If no placement meets all quality gates, select the best valid topology-preserving candidate, display the graph, and include failed metrics in a development diagnostic. Do not drop graph items.
- If a graph is too dense for readable full fit, Overview uses topology-level detail. Explore remains readable.
- A missing optional evidence file does not fail normal graph tests.

## Accessibility

- Busy state uses `aria-busy` and a polite live message.
- View mode is exposed by text and `aria-pressed`, not color alone.
- Selected nodes use a visual border and Svelte Flow selected state.
- Exact-ID search remains keyboard operable.
- Overview level of detail does not remove semantic node or edge lists.
- Fixed-control safe areas apply at 1,440, 1,024, and 390 CSS-pixel widths.

## Dependency Decisions

### Existing dependencies

- **Svelte 5 and SvelteKit:** Approved and unchanged.
- **Svelte Flow:** Approved and unchanged for graph interaction and rendering.
- **ELK:** Approved and unchanged for deterministic layered placement candidates.
- **Tailwind CSS v4 and repository shadcn-svelte components:** Approved and unchanged for UI composition.
- **Vitest and Playwright:** Approved and unchanged for local verification.

### New dependencies

None approved. Do not add GSAP, another graph engine, a routing package, or a screenshot-comparison service for this bugfix.

## Design Verification Matrix

| Requirement | Primary verification |
| --- | --- |
| RB-01 | Component tests and delayed-worker browser test |
| RB-02 | Viewport unit tests and Explore/Overview screenshots |
| RB-03 | Generated placement profiles and real-file review metrics |
| RB-04 | Pure route metrics, generated dense topologies, and real-file review |
| RB-05 | Search component and browser interaction tests |
| RB-06 | Safe-rectangle unit tests and responsive browser geometry checks |
| RB-07 | Contract tests, source scan, and generic review command |
| RB-08 | Local worker timing and main-thread responsiveness checks |
| RB-09 | Presentation-transform tests, opening-context tests, exact Full detail counts, summary tests, and real-file screenshots |
| RB-10 | Guide-model tests, panel navigation tests, absence of invented labels, responsive safe-area tests, and real-file screenshots |
| RB-11 | Primary-skeleton tests, composition metrics, route-priority tests, and full-resolution Overview screenshots for all three supplied graphs |
| RB-12 | Branch-semantics tests, grouped-sequence tests, DOM outcome audits, and normal-reading-zoom screenshots for all three supplied graphs |

## Complete-map composition

Overview uses a topology-derived primary skeleton to establish reading order. Starting at each declared entry, the first forward incoming connection that reaches a node becomes its primary parent. Cycle returns, later incoming links, and long cross-branch links are secondary. This classification changes presentation priority only; it never removes source topology.

ELK remains the placement engine. Candidate selection also measures the distribution of node centers, primary connection span, and empty internal bands. The selected map must keep related branches near their parent and avoid a large unused center between occupied regions. The route planner prefers direct south-to-north flow for primary forward links. Secondary links can use outer corridors and render with lower contrast until inspection.

## Business branch presentation

Topology priority and business meaning are separate properties. A route can be secondary to the spanning skeleton and still be a first-class alternative from a business decision. A presentation source with more than one distinct immediate destination marks all its outgoing routes as branches. Parallel source edges to the same destination do not create a false branch. A branch uses a solid route, normal contrast, and a visible supplied outcome. Only a non-branch cross-link can use the quiet dashed reference style.

The label stays on the first route segment near the source. If the source JSON does not supply an outcome, the route remains unlabelled. The explanation panel names the destination so that the interface stays useful without inventing an outcome.

A grouped sequence card uses two levels of information. The graph shows the first complete source label and a short `then N more actions` count. The explanation panel shows every member in order for the pointer and selected states. This keeps the map concise and gives a complete explanation without a clipped arrow sentence.

The final comprehension review has five pass-or-fail dimensions: orientation, decision alternatives, route ownership, node meaning, and explanation recovery. Each supplied graph must pass all five dimensions at normal reading zoom. A complete-fit image is supporting topology evidence only.

## Rollout

This is a local POC frontend change. Replace the current fit policy and placement acceptance in one branch. Keep the old complete-fit action as Overview. Do not add a feature flag, database migration, API version, or compatibility mode.
