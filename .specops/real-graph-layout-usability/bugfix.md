# Bug Fix: Real Graph Layout Usability

## Problem Statement

The static graph preview accepts and renders real business graph JSON, but medium graphs are not usable as explanations. The initial complete fit makes nodes too small to read. Dense cycles and convergence paths still produce route walls, excessive crossings, and long detours. Layout can take several seconds while the canvas gives no progress state. Search moves the viewport but does not clearly mark the matched node.

This is a high-severity usability defect in the graph view. A technically complete graph is not acceptable when a user cannot read its structure or follow a local decision path.

## Scope Boundary

**In scope:** Local JSON graph preview, layout progress, readable initial viewport, explicit overview mode, deterministic node placement, route quality, selected-node search state, fixed-control safe areas, objective layout metrics, and review with the three supplied real business graphs.

**Out of scope:** Active-run state, current-step state, complete-run path highlighting, run evidence, dashboard behavior, PostgreSQL, HTTP APIs, graph JSON changes, SQL changes, saved node positions, graph editing, production benchmarking, CI configuration, and a new animation library.

The complete topology must remain available. The initial view does not have to show all labels at once.

## Evidence Set

The review used these browser-only inputs. The specification identifies them by content hash so that a later review can detect changed evidence. The files are not copied into production code or converted into hard-coded diagrams.

| Evidence ID | Decision | Graph ID | Nodes | Edges | SHA-256 |
| --- | --- | --- | ---: | ---: | --- |
| RG-KEYCLOAK-SEARCH | search users | `business-4b96c0355f90f3cde5e4` | 45 | 77 | `c26ee0221ed3045a0fb8da5854b5b607f23b4d10e0ef98f49de98a964694580f` |
| RG-MEGA-WARNINGS | determine journey warnings | `business-89ccdbd83d6992a08021` | 55 | 89 | `b9de4b83451c1df09aa626fc66f28902c95765ae88459e98523eb648bab1812e` |
| RG-MEGA-DIRECTION | validate journey direction | `business-c614225d9ac8bbdd883e` | 19 | 29 | `03ae3344e07137fed2597f11bc3e5dc5ffd0bad6554023471711410a68014ce8` |

### Measured current behavior

Measurements use the current browser preview at 1,800 by 1,200 CSS pixels. They are POC review evidence, not production service-level objectives.

| Evidence ID | Browser layout time | Complete-fit zoom | Effective node width | Layout size | Crossings | Long routes |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| RG-KEYCLOAK-SEARCH | 5.4 s | 0.115 | 27 px | 888 by 7,112 | 54 | 17 |
| RG-MEGA-WARNINGS | 2.7 s | 0.180 | 42 px | 7,104 by 2,224 | 169 | 34 |
| RG-MEGA-DIRECTION | 0.6 s | 0.320 | 74 px | 888 by 2,600 | 3 | 3 |

RG-MEGA-WARNINGS also reports three avoidable crossings, six branch-region violations, one label collision, and two routes with detour ratios of 7.5 and 9.1. The two routes leave `a matching project entry exists` and go to the warning-type rules.

## Root Cause Analysis

### 1. Complete fit is also the default reading policy

`FitGraph.svelte` always fits the full graph after layout. The fit can use zoom values below normal reading size. The graph has no separate reading and overview modes, so complete topology and readable labels compete for the same viewport.

### 2. Layout state is implicit

`FlowCanvas.svelte` starts asynchronous worker layout with empty node and edge arrays. Until the worker returns, the canvas looks empty. The page exposes no `loading`, `ready`, or `failed` layout state and no accessible progress message.

### 3. Placement rebuilds flat global ranks

`compactPositions` keeps ELK horizontal order but discards its vertical placement. It reconstructs one global row for every topology rank. A wide rank becomes a 7,104-pixel row. A deep rank sequence becomes a 7,112-pixel column. The algorithm has no viewport-shape objective and does not compare placement candidates.

### 4. Route scoring accepts visually expensive results

The route planner prevents node intrusion and performs two local crossing-refinement passes. Its acceptance metrics can still call 169 crossings unavoidable, accept branch-region violations, and allow an outer route to be more than nine times its direct distance. The current `avoidableCrossings` check only detects an available single-edge improvement in the current placement. It does not prove that the placement and route set are understandable.

### 5. Dense cycle and convergence routes are not sufficiently bundled

The planner creates shared trunks only for selected fan-in groups. Cycle returns and repeated convergence can create parallel full-width corridors. The graph stays topologically correct, but the visual result becomes a wall of horizontal and vertical lines.

### 6. Search focus is not selection

Search calls `setCenter` with a minimum zoom. It does not set a persistent selected-node ID or selected neighborhood. The user sees a centered area but no clear visual answer to “which node matched?”

### 7. Fixed controls have no layout safe area

The search field, controls, and minimap overlay the canvas. Focus and fit calculations use the full canvas bounds, so graph content can appear behind these controls.

## Impact Assessment

- **Severity:** High
- **Affected users:** Every user who uploads or opens a medium static graph
- **Frequency:** Always for the supplied 45-node and 55-node graphs; partially for the 19-node graph
- **Business impact:** The tool proves that a graph exists but does not reliably help a user explain its logic.
- **Data impact:** None. The defect is in presentation and interaction.

## Reproduction

1. Start the SvelteKit viewer.
2. Open `/graphs`.
3. Upload one evidence file.
4. Wait for layout.
5. Observe the empty canvas during work.
6. Observe the complete fit after layout.
7. Search for an exact node ID.
8. Expected: The UI shows progress, opens at a readable location, marks the selected node, keeps nearby structure visible, and offers a separate complete overview.
9. Actual: The canvas is blank during layout, the complete fit is unreadable, and search only moves the viewport.

## Required Behavior

### RB-01 — Explicit layout state

- [ ] WHEN a valid graph file has been parsed and layout has not completed THE SYSTEM SHALL show `Arranging <node count> nodes and <edge count> connections` inside the canvas.
- [ ] WHILE layout is pending THE SYSTEM SHALL keep the file summary and replace-file action available.
- [ ] WHILE layout is pending THE SYSTEM SHALL expose an accessible busy state and status message.
- [ ] IF a newer graph replaces a graph with pending layout THEN THE SYSTEM SHALL ignore the stale layout result.
- [ ] IF layout fails THEN THE SYSTEM SHALL show the existing layout error without leaving a permanent busy state.

### RB-02 — Reading view and overview are separate

- [ ] WHEN a graph becomes ready THE SYSTEM SHALL open in `Reading view` at the first declared entry or the stable first entry when multiple entries exist.
- [ ] THE SYSTEM SHALL keep business-label text at an effective size of at least 12 CSS pixels in Reading view.
- [ ] THE SYSTEM SHALL include the focused node, its direct predecessors, its direct successors, and at least 64 CSS pixels of free context where those nodes fit in the canvas.
- [ ] WHEN the user selects `Overview` THE SYSTEM SHALL fit the complete graph and expose `Overview` as the active mode.
- [ ] WHILE Overview zoom is below the readable label floor THE SYSTEM SHALL use topology-level detail instead of rendering illegible text as if it were readable.
- [ ] WHEN the user selects a node from Overview THE SYSTEM SHALL return to Reading view and focus that node.
- [ ] THE SYSTEM SHALL never remove nodes or edges from the underlying Svelte Flow model to create either viewport mode.

### RB-03 — Balanced top-to-bottom placement

- [ ] THE SYSTEM SHALL preserve top-to-bottom direction for normal forward flow.
- [ ] THE SYSTEM SHALL use ELK placement coordinates or another topology-derived candidate; it SHALL NOT rebuild all nodes into one flat row solely from a global rank number.
- [ ] THE SYSTEM SHALL compare deterministic placement candidates with one shared quality score before route planning is accepted.
- [ ] FOR a 16-to-100-node graph that is not a single structural chain THE SYSTEM SHALL reject a placement with width-to-height ratio below 0.25 or above 2.25 when a valid candidate inside that range exists.
- [ ] THE SYSTEM SHALL keep node overlap and unrelated-node route intrusion at zero.
- [ ] THE SYSTEM SHALL keep entry nodes before their forward descendants and reachable outcomes after their forward predecessors.
- [ ] THE SYSTEM SHALL keep each multi-node strongly connected component in one compact `Cycle` region.

### RB-04 — Route simplicity

- [ ] THE SYSTEM SHALL report candidate-relative route detour, route-crossing density, parallel-corridor density, label collision, branch-region violation, and route backtracking metrics.
- [ ] THE SYSTEM SHALL reject a route when another valid candidate is at least 48 pixels or 15 percent shorter without increasing node intrusion, label collision, branch violation, or crossing count.
- [ ] THE SYSTEM SHALL keep avoidable crossings, branch-region violations, node intrusions, and label collisions at zero for accepted layouts.
- [ ] THE SYSTEM SHALL keep visible crossing bridges at or below one crossing per two graph edges for the supplied evidence set.
- [ ] THE SYSTEM SHALL keep each normal route at no more than 2.0 times its shortest valid orthogonal candidate and each outer or cycle route at no more than 3.0 times its shortest valid candidate.
- [ ] WHEN three or more routes share a target corridor THE SYSTEM SHALL evaluate a presentation-only shared trunk before it accepts separate parallel corridors.
- [ ] WHEN a cycle edge leaves and returns to the same cycle region THE SYSTEM SHALL keep its corridor adjacent to that region unless obstacle or crossing metrics prove an outer corridor is better.
- [ ] THE SYSTEM SHALL keep original edge identity, focusability, accessible labels, and endpoint semantics when routes share presentation geometry.

### RB-05 — Search and selection

- [ ] WHEN search matches a node by exact ID or label THE SYSTEM SHALL persistently mark that node as selected until another node is selected or another graph is loaded.
- [ ] THE SYSTEM SHALL distinguish static selection from node kind, keyboard focus, run path, current run step, success, failure, and coverage state.
- [ ] WHEN a selected node is focused THE SYSTEM SHALL frame its local predecessor-and-successor neighborhood in Reading view.
- [ ] IF a label matches multiple nodes THEN THE SYSTEM SHALL select the stable first match and state the occurrence count; exact ID SHALL always select the exact node.
- [ ] IF search has no match THEN THE SYSTEM SHALL preserve the current viewport and selected node.

### RB-06 — Canvas safe areas

- [ ] THE SYSTEM SHALL calculate Reading and Overview viewports inside bounds that exclude the search toolbar, zoom controls, minimap, and attribution.
- [ ] THE SYSTEM SHALL keep the selected node outside every fixed-control rectangle with at least 16 CSS pixels of clearance.
- [ ] THE SYSTEM SHALL preserve the same selected node and viewport mode after a theme change or canvas resize.

### RB-07 — Generic evidence and compatibility

- [ ] THE SYSTEM SHALL CONTINUE TO accept `fachtracing-business-graph/v1` and `fachtracing-developer-graph/v1` without contract changes.
- [ ] THE SYSTEM SHALL derive all node positions, routes, groups, and viewport focus from the supplied graph.
- [ ] THE SYSTEM SHALL NOT contain graph IDs, node IDs, product labels, fixed node coordinates, fixed routes, or hard-coded diagrams in production code.
- [ ] THE SYSTEM SHALL provide a local graph-review command that accepts arbitrary JSON file paths and emits the same layout metrics used by acceptance tests.
- [ ] THE SYSTEM SHALL verify the three supplied evidence files by content hash during local acceptance when those files are available.
- [ ] IF an evidence file is absent THEN THE SYSTEM SHALL still verify topology-equivalent generated fixtures and report that the optional real-file review was not run.

### RB-08 — POC responsiveness

- [ ] THE SYSTEM SHALL show the layout busy state within one rendered frame after parsing completes.
- [ ] THE SYSTEM SHALL keep layout work outside the main UI thread.
- [ ] THE SYSTEM SHALL complete each supplied 19-to-55-node evidence layout within four seconds in the existing local acceptance environment.
- [ ] THE SYSTEM SHALL label this timing as a POC responsiveness gate, not a production benchmark.

## Regression Risk Analysis

### Blast radius

- `layout-definition.ts` and `layout-client.ts`: placement ownership, worker result, and metrics
- `topology-analysis.ts`: ranks, components, branch regions, convergence, and cycles
- `route-planner.ts` and `route-quality.ts`: route selection and quality gates
- `FlowCanvas.svelte`, `FitGraph.svelte`, and `FocusCurrent.svelte`: layout state and viewport modes
- `BusinessNode.svelte`, `BusinessEdge.svelte`, and `GraphJunctions.svelte`: selection and level-of-detail rendering
- `/graphs`: loaded-state composition
- Graph unit tests, generated fixtures, Playwright graph-preview tests, and viewer documentation

### Behavior inventory

- Both current V1 JSON formats load in browser memory only.
- The full original node and edge set reaches Svelte Flow.
- Node and edge IDs remain unchanged.
- Layout runs in a worker and ignores stale results.
- Four-side ports, branch labels, fan-in junctions, crossing bridges, duplicate occurrences, cycle regions, and accessible graph lists remain available.
- The shared canvas still supports run pages, but this specification adds no run-state behavior.
- Search by exact ID or label remains available.
- Theme, zoom, pan, minimap, and keyboard controls remain available.

### Coverage assessment

- **Covered:** V1 parsing, privacy, graph counts, topology fixtures, deterministic layout, node intrusion, labels, four-side ports, fan-in, crossings, duplicates, cycles, and 250-node chain safety.
- **Gap:** No current test uses the three supplied real graph structures as acceptance evidence.
- **Gap:** No current test separates Reading view from Overview.
- **Gap:** No current test observes a busy canvas while worker layout runs.
- **Gap:** `avoidableCrossings` does not reject the 169-crossing warnings layout.
- **Gap:** No current test checks selected-node visual state after search.
- **Gap:** No current test checks fixed-control exclusion from viewport framing.

### Risk tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| V1 graph parsing and complete topology | Must-Test | All changed presentation stages consume normalized graph data. |
| Original node and edge identity | Must-Test | Route sharing and level-of-detail must not alter semantics. |
| Worker cancellation and stale-result handling | Must-Test | New explicit state changes asynchronous coordination. |
| Route intrusion, labels, ports, crossings, and junctions | Must-Test | Placement and routing change directly. |
| Search exact-ID behavior and keyboard access | Must-Test | Search gains persistent selection and neighborhood framing. |
| Shared run-canvas rendering | Must-Test | `FlowCanvas` is shared even though run behavior is out of scope. |
| PostgreSQL and HTTP behavior | Low-Risk | No server, query, or persistence file changes. |

### Scope escalation check

No scope escalation is required. The defect can be corrected in the existing frontend layout and presentation boundary without a new wire contract, dependency, service, or database change.

## Proposed Fix

Introduce an explicit graph presentation state machine, separate Reading and Overview viewports, preserve ELK-derived placement coordinates, compare deterministic placement candidates, strengthen route-quality scoring, bundle dense convergence corridors, persist static selection, and reserve canvas safe areas. Keep Svelte 5, SvelteKit, Svelte Flow, ELK, Tailwind CSS v4, and repository-owned shadcn-svelte components. Do not add GSAP or another graph library.

## Unchanged Behavior

- WHEN a valid developer graph V1 file is selected THE SYSTEM SHALL CONTINUE TO render every normalized node and edge.
- WHEN a valid business graph V1 file is selected THE SYSTEM SHALL CONTINUE TO render every normalized node and edge.
- THE SYSTEM SHALL CONTINUE TO process uploaded files only in browser memory.
- THE SYSTEM SHALL CONTINUE TO preserve original node IDs, edge IDs, labels, outcomes, accessibility names, and graph counts.
- WHEN the shared canvas receives run highlights THE SYSTEM SHALL CONTINUE TO map them to the same original node and edge IDs.
- THE SYSTEM SHALL CONTINUE TO render valid topologies even when optional real-file evidence is unavailable.

## Testing Plan

### Current behavior

- Record current metrics for all three hashed evidence graphs.
- Prove that RG-KEYCLOAK-SEARCH opens below the readable scale and exceeds four seconds.
- Prove that RG-MEGA-WARNINGS exceeds the crossing, branch-region, collision, and detour gates.
- Prove that search centers a node without assigning a persistent selected state.
- Prove that the canvas has no visible pending-layout state.

### Expected behavior

- Verify layout state transitions: idle, arranging, ready, replaced, and failed.
- Verify Reading view, Overview, node selection, local-neighborhood framing, and safe-area exclusion.
- Verify generic placement profiles for deep, wide, cyclic, fan-in, duplicate-label, and multi-entry graphs.
- Verify all route-quality limits from RB-04.
- Run the local graph-review command on all available hashed evidence files.
- Inspect Reading and Overview screenshots for all three graphs in light and dark themes.

### Unchanged behavior

- Run both V1 graph-file tests and browser upload tests.
- Verify exact graph counts and IDs before and after layout.
- Run existing topology, routing, label, fan-in, cycle, crossing, duplicate, and 250-node tests.
- Verify shared run-highlight mapping at the unit boundary without adding run-view requirements.
- Verify keyboard search, selection, zoom, pan, theme, and accessible graph list behavior.

## Acceptance Decision

The bugfix is acceptable only when all objective criteria pass and a human can use each supplied graph to do these tasks without inspecting source JSON:

1. Identify the entry and the immediate choices in Reading view.
2. Open Overview and recognize the main shape of the complete topology.
3. Search an exact node ID and identify the selected node without guessing.
4. Follow the selected node's incoming and outgoing routes without crossing an unrelated node or losing the route in a corridor wall.

Passing unit tests alone is not design approval.

