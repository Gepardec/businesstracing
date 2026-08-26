# Design: Real Graph Layout Usability

## Design Outcome

The graph preview will open as a readable inspection tool. It will show progress while layout runs, focus the entry at reading scale, provide an explicit complete overview, mark search selection, and route dense real graphs with bounded visual complexity. All presentation remains derived from input topology.

## Governing Decisions

1. Reading and Overview are separate viewport modes.
2. Complete topology stays loaded in both modes.
3. Top-to-bottom flow remains the default direction.
4. ELK supplies candidate placement coordinates; Fachtracing evaluates and selects placement and routes.
5. Route acceptance uses set-level quality, not only local collision safety.
6. Static selection is separate from run state and status colors.
7. Fixed controls define safe rectangles that viewport calculations must exclude.
8. Real graph files are review inputs, not production configuration or hard-coded diagrams.

## Presentation Pipeline

```text
GraphModel
  -> topology analysis
  -> deterministic placement profiles
  -> placement normalization and scoring
  -> route planning and set-level refinement
  -> layout-quality report
  -> GraphPresentationState
  -> Svelte Flow
       -> Reading viewport
       -> Overview viewport
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

type ViewMode = 'reading' | 'overview';

interface GraphPresentationState {
  phase: LayoutPhase;
  viewMode: ViewMode;
  selectedNodeId: string | null;
  focusRevision: number;
}
```

`requestId` is monotonic. Only the current request can replace the active layout. A new graph immediately changes the phase to `arranging` and clears the old selection.

## Layout Progress

`FlowCanvas` receives the graph and creates the request state before it calls the worker. The canvas renders one neutral shadcn-style status surface above the graph background. The message contains node and edge counts. It uses `aria-busy=true` and a polite live region.

The worker remains the only place that runs ELK and route planning. No progress animation changes graph coordinates. Reduced-motion mode uses a static progress icon.

## Viewport Modes

### Reading view

Reading view is the default after layout. It selects the stable first entry as the focus anchor but does not mark it as a search selection. It calculates bounds from:

- The focus node
- Direct predecessor nodes
- Direct successor nodes
- Routes between those nodes
- A 64-pixel context margin

The viewport uses the largest zoom that fits these bounds and never uses a zoom that makes the 14-pixel business label smaller than 12 CSS pixels. The current base node label therefore needs a minimum zoom of `12 / 14`, rounded up to `0.86`.

If the local neighborhood cannot fit at 0.86, the viewport centers the focus node at 0.86 and lets adjacent context extend outside the viewport. It does not reduce below the reading floor.

### Overview

Overview fits the complete layout inside the safe canvas rectangle. It can use any supported zoom. When zoom is below 0.72, nodes use topology-level detail:

- Keep silhouette, kind accent, selected state, and accessible name.
- Hide business label and occurrence text that cannot be read.
- Keep branch labels only when their rendered text meets the same readable floor.
- Show a clear `Overview` mode indicator.

Selecting a node changes to Reading view and frames that node's neighborhood.

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

The ratio uses valid obstacle-aware candidates, not straight-line distance. A candidate is valid only when it preserves endpoint port semantics and all required clearances.

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

The focus coordinator calculates the selected node's local neighborhood and changes to Reading view. A no-match result changes only the status message.

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
| `layout-client.ts` | Worker lifecycle and request cancellation |
| `graph-viewport.ts` | Reading, Overview, neighborhood bounds, and safe rectangle calculations |
| `FlowCanvas.svelte` | Presentation state and Svelte Flow composition |
| `GraphLayoutStatus.svelte` | Busy and failure presentation |
| `BusinessNode.svelte` | Node level of detail and static selected appearance |
| `scripts/review-graphs.ts` | Generic file-driven local acceptance report |

## Failure Behavior

- A layout failure displays the existing error and clears busy state.
- A stale worker result has no effect.
- If no placement meets all quality gates, select the best valid topology-preserving candidate, display the graph, and include failed metrics in a development diagnostic. Do not drop graph items.
- If a graph is too dense for readable full fit, Overview uses topology-level detail. Reading view remains readable.
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
| RB-02 | Viewport unit tests and Reading/Overview screenshots |
| RB-03 | Generated placement profiles and real-file review metrics |
| RB-04 | Pure route metrics, generated dense topologies, and real-file review |
| RB-05 | Search component and browser interaction tests |
| RB-06 | Safe-rectangle unit tests and responsive browser geometry checks |
| RB-07 | Contract tests, source scan, and generic review command |
| RB-08 | Local worker timing and main-thread responsiveness checks |

## Rollout

This is a local POC frontend change. Replace the current fit policy and placement acceptance in one branch. Keep the old complete-fit action as Overview. Do not add a feature flag, database migration, API version, or compatibility mode.

