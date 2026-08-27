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

### Rejected implementation evidence

The first implementation passed its own metrics but failed direct visual review on RG-MEGA-DIRECTION. The edge from `choose by current direction` to `operation failed` uses a 665-pixel outer route although both nodes are close. The route leaves through the west side, moves above the complete layout, and then returns to a lower target. Its reported detour ratio is 1.0 because the baseline excludes shorter candidates with a different crossing score. `Branch 2` also moves 72 pixels away from its route and uses a detached leader.

The viewport adds a focus summary and reduces unrelated graph content to 10 to 12 percent opacity. This makes the complete graph hard to understand and makes navigation feel destructive. Overview also removes the node header and business label below a fixed zoom threshold, which leaves empty node boxes.

### Rejected completion evidence — explanation layout

The third implementation passes geometry checks but still fails the product goal. Explore places a small local graph in a large undifferentiated canvas. It invents `Path 1`, `Path 2`, and `Path 3` for connections that have no supplied outcome. These labels do not explain the business alternatives. Sequence cards hide most member rules, Overview is only technically rendered at an unreadable scale, and the three-sentence summary reports counts instead of explaining the first decision and possible results.

Geometry is necessary but is not visual acceptance. A completed result must let a user identify the start, understand the first alternatives, inspect every collapsed step, and continue through the graph without guessing what an invented path number means.

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

- [x] WHEN a valid graph file has been parsed and layout has not completed THE SYSTEM SHALL show `Arranging <node count> nodes and <edge count> connections` inside the canvas.
- [x] WHILE layout is pending THE SYSTEM SHALL keep the file summary and replace-file action available.
- [x] WHILE layout is pending THE SYSTEM SHALL expose an accessible busy state and status message.
- [x] IF a newer graph replaces a graph with pending layout THEN THE SYSTEM SHALL ignore the stale layout result.
- [x] IF layout fails THEN THE SYSTEM SHALL show the existing layout error without leaving a permanent busy state.

### RB-02 — Explore and Overview are separate

- [x] WHEN a graph becomes ready THE SYSTEM SHALL open in `Explore` at the first declared entry or the stable first entry when multiple entries exist.
- [x] THE SYSTEM SHALL keep business-label text at an effective size of at least 12 CSS pixels in Explore where the local context fits.
- [x] THE SYSTEM SHALL create an independent compact layout for Explore so complete-graph coordinates cannot separate local neighbors.
- [x] AT the initial entry THE SYSTEM SHALL follow a bounded straight setup path to the first material split and include its immediate alternatives.
- [x] AFTER node selection THE SYSTEM SHALL include the focused node, its direct predecessors, its direct successors, and free context where those nodes fit in the canvas.
- [x] THE SYSTEM SHALL state that a user can select a visible node to continue through the graph.
- [x] WHEN the user selects `Overview` THE SYSTEM SHALL fit the complete graph and expose `Overview` as the active mode.
- [x] WHILE the user zooms out THE SYSTEM SHALL keep each node kind and business label present; it SHALL NOT render an empty node box because of zoom.
- [x] WHILE the zoom is below normal reading size THE SYSTEM SHALL show a full-size node kind and business label when the pointer is over that node.
- [x] WHILE the user zooms out THE SYSTEM SHALL keep every non-empty route label present in the graph model and rendered canvas.
- [x] WHEN the user selects a node from Overview THE SYSTEM SHALL return to Explore and focus that node.
- [x] THE SYSTEM SHALL retain the complete current presentation for Overview while Explore renders only its derived local subgraph.
- [x] IF the user selects Overview while the complete layout is pending THEN THE SYSTEM SHALL show that complete layout when it becomes ready.

### RB-03 — Balanced top-to-bottom placement

- [x] THE SYSTEM SHALL preserve top-to-bottom direction for normal forward flow.
- [x] THE SYSTEM SHALL use ELK placement coordinates or another topology-derived candidate; it SHALL NOT rebuild all nodes into one flat row solely from a global rank number.
- [x] THE SYSTEM SHALL compare deterministic placement candidates with one shared quality score before route planning is accepted.
- [x] FOR a 16-to-100-node graph that is not a single structural chain THE SYSTEM SHALL reject a placement with width-to-height ratio below 0.25 or above 2.25 when a valid candidate inside that range exists.
- [x] THE SYSTEM SHALL keep node overlap and unrelated-node route intrusion at zero.
- [x] THE SYSTEM SHALL keep entry nodes before their forward descendants and reachable outcomes after their forward predecessors.
- [x] THE SYSTEM SHALL keep each multi-node strongly connected component in one compact `Cycle` region.

### RB-04 — Route simplicity

- [x] THE SYSTEM SHALL report candidate-relative route detour, route-crossing density, parallel-corridor density, label collision, branch-region violation, and route backtracking metrics.
- [x] THE SYSTEM SHALL measure route detour against the shortest collision-free candidate before it filters by crossing or corridor class.
- [x] THE SYSTEM SHALL reject a route when another collision-free candidate is at least 48 pixels or 15 percent shorter unless the shorter route has a node intrusion or terminal reversal.
- [x] THE SYSTEM SHALL NOT force an edge into an outer corridor only because its topology rank span marks it as long.
- [x] WHEN a target is below a source THE SYSTEM SHALL reject a route that travels above the source layout boundary when a collision-free route through the south, east, or west source side exists.
- [x] THE SYSTEM SHALL keep each visible branch label on its route or within 24 layout pixels of its route anchor; it SHALL NOT draw a detached label leader.
- [x] THE SYSTEM SHALL select the shortest collision-free route before it uses port-orientation, crossing, congestion, or corridor preferences.
- [x] WHEN two collision-free candidates have the same length THE SYSTEM SHALL prefer a source and target port that agrees with the top-to-bottom flow.
- [x] THE SYSTEM SHALL show long return or reference connections as secondary quiet routes until the user inspects them.
- [x] THE SYSTEM SHALL route a cycle return through the shortest safe perimeter corridor of its cycle.
- [x] THE SYSTEM SHALL keep avoidable crossings, branch-region violations, node intrusions, and label collisions at zero for accepted layouts.
- [x] THE SYSTEM SHALL keep visible crossing bridges at or below one crossing per two graph edges for the supplied evidence set.
- [x] THE SYSTEM SHALL keep each normal route at no more than 2.0 times its shortest valid orthogonal candidate and each outer or cycle route at no more than 3.0 times its shortest valid candidate.
- [x] WHEN three or more routes share a target corridor THE SYSTEM SHALL evaluate a presentation-only shared trunk before it accepts separate parallel corridors.
- [x] WHEN a cycle edge leaves and returns to the same cycle region THE SYSTEM SHALL keep its corridor adjacent to that region unless obstacle or crossing metrics prove an outer corridor is better.
- [x] THE SYSTEM SHALL keep original edge identity, focusability, accessible labels, and endpoint semantics when routes share presentation geometry.

### RB-05 — Search and selection

- [x] WHEN search matches a node by exact ID or label THE SYSTEM SHALL persistently mark that node as selected until another node is selected or another graph is loaded.
- [x] THE SYSTEM SHALL distinguish static selection from node kind, keyboard focus, run path, current run step, success, failure, and coverage state.
- [x] WHEN a selected node is focused THE SYSTEM SHALL create and frame its local predecessor-and-successor neighborhood in Explore.
- [x] IF a label matches multiple nodes THEN THE SYSTEM SHALL select the stable first match and state the occurrence count; exact ID SHALL always select the exact node.
- [x] IF search has no match THEN THE SYSTEM SHALL preserve the current viewport and selected node.
- [x] WHILE no run highlight is active THE SYSTEM SHALL keep unrelated static nodes, edges, junctions, crossings, and regions at normal contrast, except for the topology-based secondary style of a long route.
- [x] THE SYSTEM SHALL NOT add a focus-summary badge that repeats the selected node or graph count inside the canvas toolbar.

### RB-06 — Canvas safe areas

- [x] THE SYSTEM SHALL calculate Explore and Overview viewports inside bounds that exclude the search toolbar, zoom controls, minimap, and attribution.
- [x] THE SYSTEM SHALL keep the selected node outside every fixed-control rectangle with at least 16 CSS pixels of clearance.
- [x] THE SYSTEM SHALL preserve the same selected node and viewport mode after a theme change or canvas resize.

### RB-07 — Generic evidence and compatibility

- [x] THE SYSTEM SHALL CONTINUE TO accept `fachtracing-business-graph/v1` and `fachtracing-developer-graph/v1` without contract changes.
- [x] THE SYSTEM SHALL derive all node positions, routes, groups, and viewport focus from the supplied graph.
- [x] THE SYSTEM SHALL NOT contain graph IDs, node IDs, product labels, fixed node coordinates, fixed routes, or hard-coded diagrams in production code.
- [x] THE SYSTEM SHALL provide a local graph-review command that accepts arbitrary JSON file paths and emits the same layout metrics used by acceptance tests.
- [x] THE SYSTEM SHALL verify the three supplied evidence files by content hash during local acceptance when those files are available.
- [x] IF an evidence file is absent THEN THE SYSTEM SHALL still verify topology-equivalent generated fixtures and report that the optional real-file review was not run.

### RB-08 — POC responsiveness

- [x] THE SYSTEM SHALL show the layout busy state within one rendered frame after parsing completes.
- [x] THE SYSTEM SHALL keep layout work outside the main UI thread.
- [x] THE SYSTEM SHALL complete each supplied 19-to-55-node evidence layout within four seconds in the existing local acceptance environment.
- [x] THE SYSTEM SHALL label this timing as a POC responsiveness gate, not a production benchmark.

### RB-09 — Readable business map

- [x] WHEN a graph contains a straight sequence of two or more computation nodes with no branch or labelled transition THEN THE SYSTEM SHALL show that sequence as one presentation node by default.
- [x] WHEN predicates form a safe straight rule sequence or a guard sequence with one shared exit THEN THE SYSTEM SHALL show that sequence as one presentation node by default.
- [x] WHEN two or more semantic edges connect the same presentation nodes THEN THE SYSTEM SHALL show one connection with a combined outcome by default.
- [x] THE SYSTEM SHALL preserve every original node ID and edge ID in the presentation mapping, accessible graph list, search, and run highlight mapping.
- [x] THE SYSTEM SHALL provide a Full detail control that restores every original node and edge.
- [x] THE SYSTEM SHALL show how many original steps a sequence node contains and expose their ordered labels.
- [x] THE SYSTEM SHALL derive a generic explanation of no more than three actual sentences from the entry, first material branch, node kinds, outcomes, cycles, and terminal results.
- [x] THE SYSTEM SHALL NOT use graph-specific IDs, labels, coordinates, summaries, or diagrams.
- [x] THE SYSTEM SHALL use the readable map by default only when it removes at least one redundant node or connection.
- [x] THE SYSTEM SHALL keep current-run highlighting correct when a recorded node or edge belongs to a grouped presentation item.
- [x] THE SYSTEM SHALL calculate cycle feedback edges from directed entry traversal and SHALL NOT use lexical node-ID order to decide edge direction.

### RB-10 — Business explanation workspace

- [x] THE SYSTEM SHALL NOT invent a visible path name when the source graph supplies no outcome.
- [x] WHILE Explore is active THE SYSTEM SHALL use the right side of the canvas for a persistent explanation panel instead of leaving it as undifferentiated empty graph space.
- [x] THE explanation panel SHALL identify the current step, its type, its complete sequence members, its incoming context, and each immediate continuation that exists.
- [x] WHEN a continuation has no supplied outcome THE SYSTEM SHALL identify it by its target business label without adding a synthetic route label to the graph.
- [x] WHEN the user selects a continuation in the explanation panel THE SYSTEM SHALL move Explore to that target and show its local graph.
- [x] WHEN a readable node represents multiple source nodes THE SYSTEM SHALL expose every member label in order without requiring pointer hover.
- [x] THE graph summary SHALL explain the declared start, the first alternatives, and the declared results; it SHALL NOT use rule and action counts as a substitute for business meaning.
- [x] WHILE Overview is active THE SYSTEM SHALL state that it is a topology map and that selecting a node returns to readable inspection.
- [x] THE graph preview SHALL dedicate more vertical space to the explanation workspace by removing duplicate summary content above the canvas.

### RB-11 — Composed complete-graph Overview

- [x] WHILE Overview is active THE SYSTEM SHALL arrange the complete readable graph as one balanced top-down map instead of distant visual islands.
- [x] THE SYSTEM SHALL derive a primary forward skeleton from declared entries and graph topology. It SHALL NOT use graph-specific IDs, labels, coordinates, or diagrams.
- [x] Primary connections SHALL have higher visual priority than feedback, cross-branch, duplicate, and long-range reference connections.
- [x] Feedback connections SHALL remain present, accessible, and inspectable and MAY use a quiet style until hover, keyboard focus, or selection. Forward later-parent and convergence connections SHALL remain solid as specified by RB-13.
- [x] A primary connection SHALL use the nearest safe source and target sides. It SHALL NOT leave through a remote side only to return to a directly reachable child.
- [x] The selected complete layout SHALL include composition quality in candidate selection: occupied map balance, primary-edge span, and long empty internal corridors.
- [x] The 55-node supplied graph SHALL receive human full-resolution review at desktop Overview scale before acceptance. Passing collision and crossing metrics alone is not sufficient.
- [x] THE SYSTEM SHALL preserve every source node and connection, both V1 contracts, Readable and Full detail, search, accessibility, and run-highlight mappings.

### RB-12 — Five-out-of-five graph comprehension

- [x] WHEN a source node has more than one continuation and the JSON supplies an outcome THE SYSTEM SHALL keep each supplied outcome visible at the source in Overview and Explore.
- [x] A supplied branch alternative SHALL use normal branch contrast and a solid route. The primary-skeleton classification SHALL NOT turn a business alternative into a quiet dashed reference.
- [x] WHEN a multi-route source has no supplied outcome THE SYSTEM SHALL keep the route unlabelled and SHALL identify its destination in the explanation panel. THE SYSTEM SHALL NOT invent business meaning.
- [x] A branch label SHALL remain attached to its route and close to the source. It SHALL NOT appear on an unrelated corridor or as a detached badge.
- [x] A grouped action sequence SHALL show one complete first action and an explicit remaining-action count on the graph. It SHALL NOT show a clipped repeated sentence as its summary.
- [x] THE explanation panel SHALL show all grouped members in source order without requiring a click when the node is under the pointer or selected.
- [x] THE three supplied graphs SHALL each score 5/5 for orientation, decision alternatives, route ownership, node meaning, and explanation recovery at normal reading zoom.
- [x] Acceptance SHALL include browser evidence at normal reading zoom. Complete-fit screenshots and geometry metrics alone SHALL NOT prove comprehension.

### RB-13 — Visible edge integrity

- [x] A non-feedback business continuation SHALL use a solid line and normal contrast even when it is not part of the primary spanning tree.
- [x] A feedback connection MAY use a dashed line, but it SHALL remain visibly attached to its source and target and SHALL retain an arrow at the target.
- [x] WHEN two to five connections use the same side of a node THE SYSTEM SHALL separate their attachment points sufficiently that adjacent rounded corners do not look like loops.
- [x] EVERY rendered edge SHALL visibly start on its source boundary and SHALL visibly terminate on its target boundary or on a declared convergence junction that terminates on the target.
- [x] EVERY declared outcome with incoming connections SHALL show at least one visible terminating arrow. THE SYSTEM SHALL NOT draw a floating route that appears to end in empty canvas.
- [x] Parallel connections SHALL remain individually traceable in Full detail without identical geometry, adjacent hairpins, or overlapping labels.
- [x] THE three supplied screenshot patterns SHALL receive zoomed human review after browser geometry checks pass.
- [x] THE graph JSON contracts SHALL remain unchanged.

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

Keep the explicit presentation state, deterministic ELK placement, static selection, and safe areas. Select normal routes from the full collision-free candidate set and use route length before port orientation, crossings, congestion, or corridor preference. Treat outer corridors as fallbacks, not as the default for every long edge. Route a cycle return through the shortest safe cycle perimeter. Keep branch labels attached to their routes. Show long return or reference routes with a secondary quiet solid style. Remove static focus dimming and the focus-summary badge. Keep node and route text present at every supported zoom, and show a full-size node readout on hover at small zoom. Do not add a dependency.

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
- Verify Explore view, Overview, node selection, local-neighborhood framing, low-zoom node readout, and safe-area exclusion.
- Verify generic placement profiles for deep, wide, cyclic, fan-in, duplicate-label, and multi-entry graphs.
- Verify all route-quality limits from RB-04.
- Run the local graph-review command on all available hashed evidence files.
- Inspect Explore, selected Explore, and Overview screenshots for all three graphs in light and dark themes.

### Unchanged behavior

- Run both V1 graph-file tests and browser upload tests.
- Verify exact graph counts and IDs before and after layout.
- Run existing topology, routing, label, fan-in, cycle, crossing, duplicate, and 250-node tests.
- Verify shared run-highlight mapping at the unit boundary without adding run-view requirements.
- Verify keyboard search, selection, zoom, pan, theme, and accessible graph list behavior.

## Acceptance Decision

The bugfix is acceptable only when all objective criteria pass and a human can use each supplied graph to do these tasks without inspecting source JSON:

1. Identify the entry and the immediate choices in Explore view.
2. Open Overview and recognize the main shape of the complete topology.
3. Search an exact node ID and identify the selected node without guessing.
4. Follow the selected node's incoming and outgoing routes without crossing an unrelated node or losing the route in a corridor wall.

Passing unit tests alone is not design approval.
