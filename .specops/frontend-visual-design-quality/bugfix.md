# Bug Fix: Graph Viewer Visual Design Quality

## Problem Statement

The viewer can load, lay out, and navigate real Fachtracing graphs, but its visual design does not yet support the main user task: explain a recorded business decision quickly and with confidence. The current screens are technically correct but often small, sparse, noisy, ambiguous, or hard to read. The saved screenshots show these defects in both themes and at desktop, tablet, and phone widths.

This is a design defect, not a request for optional polish. The user cannot judge graph structure, path state, and business evidence with enough speed or certainty.

## Root Cause Analysis

The first viewer spec defined many sound parts, but it did not define a complete page composition or an executable visual acceptance model. The implementation then optimized for graph completeness and geometry safety. It did not optimize for readable scale, clear semantic color, calm hierarchy, or business-language explanation.

The current quality gate checks route collisions, label collisions, hidden handles, node counts, and successful screenshots. It does not compare images to approved references. It does not test effective text size, visual state meaning, route simplicity, useful canvas use, page density, or operator comprehension. A screenshot file can therefore be produced even when the design is visibly poor.

**Affected Components:**

- Application shell and detail header
- Decision dashboard and filters
- Graph preview page
- Graph canvas, initial viewport, and controls
- Node and edge visual grammar
- Run-path and current-step states
- Explanation inspector and generated explanation text
- Light, dark, desktop, tablet, and phone layouts
- Visual and accessibility test gates

**Error Symptoms:**

- Small graphs occupy a narrow strip inside a large canvas.
- Deep graphs can be fit until their labels are too small to read.
- The 250-node search view removes almost all surrounding context.
- The current step uses orange-red, which looks like a warning or failure.
- The recorded path and outcome use green, which looks like success even when the decision is not successful.
- Node-kind colors, run-state colors, status colors, borders, rails, icons, badges, and glows compete.
- Predicate nodes repeat their kind through an icon, label, color rail, and diamond marker.
- Entry, choice, dispatch, outcome, and gap shapes do not form one calm visual family.
- Edge labels are small pills that can look detached from the branch they describe.
- Parallel and converging routes are collision-free but can still form tight hooks and visual tangles.
- Every node exposes only top and bottom connection handles, so a branch to a node on the right can leave through the source bottom, turn sideways, and take an avoidable detour instead of attaching to the source right side.
- The detail header gives long IDs and raw enum values too much space.
- Long values are cut without a clear reveal action.
- The success badge is far from the title it describes.
- Copy buttons look like detached icons and do not show which value they copy.
- The inspector repeats the graph but does not lead with the business question, answer, and evidence.
- The active inspector card uses a red-tinted surface that looks like an error.
- Text such as `Followed “false”` exposes a technical Boolean before the business meaning.
- Generated sentences can be contradictory or mechanical, for example, `has graph entry source was true. Therefore, does not have graph entry source was false.`
- Final results wrap as long machine tokens and dominate the explanation.
- The graph preview keeps a large marketing-style heading and file card after a graph is loaded, which reduces the main work area.
- The mobile detail header is dense and the explanation trigger competes with status and navigation.
- The mobile sheet close button and focus ring are too strong, while the content below has weak hierarchy.
- Dark-mode nodes and normal edges have weak separation, while the current orange state is too strong.
- The dashboard uses a wide table with a fixed 760-pixel minimum and forces horizontal scrolling on narrow screens.
- The dashboard count says `shown` but does not state whether more results exist.
- The search form repeats headings and uses a large card for two primary fields.
- No saved proof covers hover, keyboard focus, selected node, failed run, incomplete run, missing evidence, loading, layout failure, no-match search, or long business labels.

## Evidence Inventory

| Evidence | Main finding |
| --- | --- |
| `fachtracing-viewer/test-results/visual/decision-1440-light.png` | Excess header metadata, small centered graph, status-color conflict, inspector repetition |
| `fachtracing-viewer/test-results/visual/decision-1440-dark.png` | Weak normal-edge contrast and an over-strong orange current state |
| `fachtracing-viewer/test-results/visual/decision-1024-light.png` | Graph text approaches the lower readable limit and the inspector dominates width |
| `fachtracing-viewer/test-results/visual/decision-390-closed-light.png` | Dense header, competing controls, and little room for graph navigation |
| `fachtracing-viewer/test-results/visual/decision-390-sheet-light.png` | Heavy sheet chrome, repeated data, machine-value wrapping |
| `fachtracing-viewer/test-results/visual/branch-routes-1440-light.png` | A four-node graph uses a small center column in a large work area |
| `fachtracing-viewer/test-results/visual/branch-routes-1440-dark.png` | Low contrast makes inactive routes hard to follow |
| `fachtracing-viewer/test-results/visual/business-v1-preview-light.png` | The graph is readable but page chrome and unused space still dominate |
| `fachtracing-viewer/test-results/visual/node-grammar-1440-light.png` | Too many shape and color signals compete in one graph |
| `fachtracing-viewer/test-results/visual/node-grammar-1440-dark.png` | Dark surfaces merge and type accents lose balance |
| `fachtracing-viewer/test-results/visual/graph-250-focused.png` | Search focus shows only three nodes and gives too little structural context |
| `fachtracing-viewer/test-results/dogfood/fachtracing-graph-preview.png` | A seven-node real graph is fit below a useful reading size |
| `fachtracing-viewer/e2e/decision-explorer.spec.ts` | Geometry assertions exist, but approved visual comparison and readability assertions do not |

## Impact Assessment

- **Severity:** High
- **Users Affected:** All users who inspect a graph or explain a decision
- **Frequency:** Always on the decision detail and graph preview pages; often on the dashboard
- **Business Impact:** The local tool can show the correct data but can still make the operator uncertain. This weakens the core explainability promise.

## Reproduction Steps

1. Open a generated run at 1,440 by 900 CSS pixels.
2. Compare the business title, result, status, IDs, graph, and step inspector.
3. Observe that machine metadata and state decoration compete with the explanation.
4. Open the same run at 390 by 844 CSS pixels and open the explanation sheet.
5. Observe the dense header, repeated information, and long raw values.
6. Upload the generated seven-node graph and fit it to the canvas.
7. Observe that the full graph is visible but node text is too small for normal reading.
8. Expected: The page leads with a readable business explanation, the graph uses the available area, and colors have one meaning each.
9. Actual: The page proves topology but does not give a clear, calm, readable explanation.

## Visual Audit Findings

### A. Information hierarchy

| ID | Severity | Current problem | Required correction |
| --- | --- | --- | --- |
| IA-01 | High | The detail header gives title, raw result, time, execution ID, graph ID, copy actions, and status similar weight. | Put title and status in one primary group. Put the readable result second. Put time and machine IDs in a compact details group. |
| IA-02 | High | Raw enum-like results are more visible than readable business text. | Show `displayValue` or a safe human label first. Keep canonical values in technical details. |
| IA-03 | Medium | Truncated values do not have a consistent reveal rule. | Every truncated value must expose the full value by tooltip, disclosure, or copy action with an accessible name. |
| IA-04 | Medium | The loaded graph preview keeps a large page introduction and metadata card. | Change to a compact loaded-state toolbar so the graph gets the main vertical area. |
| IA-05 | Medium | The dashboard repeats `Run explorer`, `Recorded decisions`, `Exact lookup`, and `Find recorded decisions`. | Use one page title and one concise search label. |

### B. Layout and density

| ID | Severity | Current problem | Required correction |
| --- | --- | --- | --- |
| LY-01 | High | Small graphs use too little of the canvas. | At the initial reading view, a graph of 2 to 15 nodes must use at least 45% of one usable canvas dimension without clipping the focused node. |
| LY-02 | High | Fit-to-overview can reduce text below a readable size. | Keep effective business-label text at 12 CSS pixels or more in the initial reading view. Offer a separate overview action when the full graph cannot meet this floor. |
| LY-03 | High | Deep graphs trade readability for full visibility. | Default to a reading viewport at zoom 0.78 or more. Center the entry node in preview and the current node in run detail. |
| LY-04 | Medium | The 250-node focus view loses structural context. | Search focus must show the match, its direct predecessors, its direct successors, and at least 80 CSS pixels of context around that local subgraph. |
| LY-05 | Medium | The desktop inspector can make the graph column feel secondary. | Use a 360-pixel default inspector, a 320-to-480-pixel resize range, and a clear divider. Keep at least 60% of usable width for the canvas at 1,280 pixels and wider. |
| LY-06 | Medium | Search, controls, attribution, and graph content do not form one control system. | Group graph actions in one compact toolbar with search, zoom, overview, and current-step actions. Keep attribution visually quiet. |
| LY-07 | Medium | The dashboard table requires horizontal scrolling on a phone. | At less than 720 pixels, render each result as a stacked decision row or card with no horizontal page scroll. |

### C. Graph grammar

| ID | Severity | Current problem | Required correction |
| --- | --- | --- | --- |
| GR-01 | High | A node can show type through four competing cues. | Use at most three cues: silhouette, icon, and type label. Use the type accent only on the icon and a 3-pixel rail. Remove the predicate diamond marker. |
| GR-02 | High | Outcome green implies success. | Use a neutral outcome style. Show run success or failure only through a status badge outside the node kind grammar. |
| GR-03 | High | Current orange-red implies warning or failure. | Use the primary interaction color for current step. Reserve red for failed status and destructive errors. Reserve amber for incomplete coverage. |
| GR-04 | High | Recorded-path green implies success. | Use a cool path color that has no status meaning. Keep a step or route icon as a non-color cue. |
| GR-05 | Medium | Node silhouettes do not share consistent geometry. | Use one 12-pixel corner system and a 232-by-96-pixel base. Entry uses a capsule, choice uses a restrained six-sided outline, and all other kinds use the common card frame. |
| GR-06 | Medium | Current state has border, glow, badge, and colored incoming edge at once. | Keep one 2-pixel current border, one small sequence badge, and the active edge. Do not add an outer halo except for keyboard focus. |
| GR-07 | Medium | Dimmed nodes can become hard to read. | Keep text at full contrast and reduce only surface and decorative accents. Do not reduce a node below 80% opacity. |
| GR-08 | Medium | Node labels can become too small at fit. | Keep an effective 12-pixel text floor and a 13-pixel preferred reading size. Use three-line wrap with a full accessible label. |
| GR-09 | Medium | Visual types are not proven in monochrome. | A monochrome state-gallery test must keep every node kind identifiable by silhouette, icon, and label. |

### D. Edge routing and labels

| ID | Severity | Current problem | Required correction |
| --- | --- | --- | --- |
| ED-01 | High | Routes can be collision-free but still look tangled. | For simple acyclic graphs, keep the main route on a stable vertical spine and put alternate branches in separate side corridors. |
| ED-02 | High | Short hook segments and tight fan-in reduce clarity. | Keep non-port orthogonal segments at least 16 CSS pixels long at reading zoom. Keep parallel routes at least 12 CSS pixels apart. |
| ED-03 | Medium | Edge labels look detached from their routes. | Place each label on its route with an 8-pixel node clearance and a 6-pixel label clearance. Add a short leader only when placement is offset. |
| ED-04 | Medium | `next`, `true`, and `false` expose low-value technical text. | Hide `next` on a single unambiguous edge. Show Boolean branches as `Yes` and `No`. Preserve the raw token in technical details. |
| ED-05 | Medium | Normal edges are too weak in dark mode. | Normal routes, arrowheads, and labels must meet a 3:1 non-text contrast ratio against the canvas in both themes. |
| ED-06 | Medium | Selected and path edges use status-like colors. | Apply the state color model from GR-03 and GR-04 to edges and arrowheads. |
| ED-07 | Medium | Geometry tests permit excessive bends. | For the generated four-node branching fixture, each edge must have no more than four bends and no avoidable reversal toward its source. |
| ED-08 | High | Fixed north and south ports create avoidable detours to targets on the left or right. | Evaluate valid ports on all four node sides and select the shortest collision-free orthogonal route. A route to a target outside the source horizontal span must use an east or west source port when that produces the shortest valid route. |

### E. Explanation inspector and content

| ID | Severity | Current problem | Required correction |
| --- | --- | --- | --- |
| EX-01 | High | The inspector does not lead with question, answer, and evidence. | Each predicate step must show `Question`, `Answer`, and `Because` in that order. |
| EX-02 | High | Generated prose can be contradictory or ungrammatical. | Build explanation content from structured fields. Never join an evidence phrase and node label into an inferred causal sentence. |
| EX-03 | High | `Followed “false”` is technical and unclear. | Show `Answer: No` or a descriptive branch label. Put the raw outcome in technical details. |
| EX-04 | High | The active step surface looks like an error. | Use a neutral selected surface plus primary border and sequence marker. Use red only when the step records failure. |
| EX-05 | Medium | The inspector repeats full node and result data. | Show a concise step summary by default. Put IDs, canonical values, selected edge ID, and raw evidence in one technical disclosure per selected step. |
| EX-06 | Medium | Long final results wrap as machine tokens. | Show a readable display value with a two-line limit. Add a reveal and copy action for the full canonical value. |
| EX-07 | Medium | Previous, next, count, and full-path controls are separated. | Put path mode and step navigation in one sticky inspector toolbar. Label path mode `Highlight complete path`. |
| EX-08 | Medium | Missing evidence can look like an implementation failure. | Show `No additional evidence was recorded` as neutral helper text. Do not invent a cause. |
| EX-09 | Medium | Repeated visits are not easy to compare. | Show visit number and total visits, and keep the active observation distinct from the node ID. |

### F. Theme, accessibility, and interaction

| ID | Severity | Current problem | Required correction |
| --- | --- | --- | --- |
| AC-01 | High | Color roles overlap. | Status, node kind, path, current step, selection, focus, and coverage gap must each have one separate token role. |
| AC-02 | High | Focus can compete with path and current borders. | Render keyboard focus as an outer 2-pixel ring with 2-pixel offset. Keep state on the card border. |
| AC-03 | High | Contrast is not checked at the rendered zoom. | Test contrast and effective font size after Svelte Flow transforms at reading and overview zooms. |
| AC-04 | Medium | Hover, focus, selected, current, path, and dimmed states lack complete proof. | Add a generated state gallery in both themes and test every state and precedence pair. |
| AC-05 | Medium | Motion policy is not visible in the implementation proof. | Use CSS transitions of 160 ms or less. Disable non-essential movement for `prefers-reduced-motion`. Do not add GSAP for graph or shell motion. |
| AC-06 | Medium | Icon-only controls need stronger visible support. | Use tooltips on desktop and text labels where space permits. Keep accessible names for all icon actions. |

### G. Responsive behavior

| ID | Severity | Current problem | Required correction |
| --- | --- | --- | --- |
| RS-01 | High | The phone header has too many competing items. | Keep back action, title, status, and explanation action in a two-row header. Move result and IDs into a summary disclosure. |
| RS-02 | High | The sheet uses strong close chrome and weak content hierarchy. | Use a 44-pixel close target with a normal focus ring. Make the toolbar sticky and the step list the main scroll region. |
| RS-03 | Medium | Opening the sheet hides graph context without a clear return. | Preserve selected step and viewport. Closing the sheet must return focus to the explanation trigger. |
| RS-04 | Medium | Tablet width can leave neither graph nor inspector comfortable. | Use the sheet below 1,080 CSS pixels. At 1,080 and above, keep the fixed inspector only when the canvas remains at least 680 pixels wide. |
| RS-05 | Medium | Phone graph controls occupy separate corners. | Use one bottom toolbar with 44-pixel targets and safe-area padding. |

### H. Quality system

| ID | Severity | Current problem | Required correction |
| --- | --- | --- | --- |
| QA-01 | High | Screenshots are created but not compared. | Add approved visual baselines and fail on a documented pixel-difference threshold. |
| QA-02 | High | Geometry success is treated as design success. | Keep geometry tests and add composition, scale, route-complexity, contrast, and content assertions. |
| QA-03 | High | No operator task proves the core use case. | Add an end-to-end test in which a user finds a run, opens it, selects a step, and can read the question, answer, evidence, and final result without opening technical details. |
| QA-04 | Medium | State coverage is incomplete. | Cover light, dark, 1,440, 1,024, 900, 390, loading, empty, no match, error, failed, incomplete, missing evidence, long label, repeated visit, and 250-node states. |
| QA-05 | Medium | There is no explicit design sign-off gate. | Require manual review of the approved reference set before the bugfix spec can complete. CI success alone is not design approval. |

## Regression Risk Analysis

### Blast Radius

- Graph contract normalization for both supported V1 JSON formats
- ELK layout and exact routed edge sections
- Run-to-graph ID mapping and repeated visits
- Node search, fit, current-step focus, zoom, and pan
- Desktop inspector resize and mobile sheet focus management
- Decision search through HTTP `QUERY`
- PostgreSQL graph and run loading
- Theme persistence

### Behavior Inventory

- The viewer loads `fachtracing-developer-graph/v1` and `fachtracing-business-graph/v1` in browser preview.
- The run page loads the exact stored graph version for a decision.
- The complete graph remains available and no graph-specific position is stored.
- The selected observation highlights the matching node and selected edge.
- Complete-path mode highlights all observed and resolved connecting nodes and edges.
- Search focuses a node by ID or label.
- ELK routes do not enter unrelated node rectangles.
- Parallel edges keep distinct paths.
- Correlation values stay out of the URL.
- The phone inspector remains an accessible dialog and returns focus on close.

### Test Coverage Assessment

- **Covered:** contract parsing and both V1 graph formats → `src/lib/contracts/contracts.test.ts`, `src/lib/graph/graph-file.test.ts`
- **Covered:** run highlight derivation → `src/lib/runs/run-highlight.test.ts`
- **Covered:** ELK scale and route safety → `src/lib/graph/graph.test.ts`, `e2e/decision-explorer.spec.ts`
- **Covered:** browser load, theme switch, phone sheet, and 250-node navigation → `e2e/decision-explorer.spec.ts`
- **Gap:** approved visual baseline comparison → no current test
- **Gap:** effective text size and useful initial zoom → no current test
- **Gap:** semantic color separation and rendered contrast → no current test
- **Gap:** route bend, segment-length, and visual-corridor quality → no current test
- **Gap:** source and target port choice and shortest valid route → nodes expose only fixed north and south handles
- **Gap:** structured business explanation language → current test checks only one evidence phrase
- **Gap:** complete interaction-state gallery → no current test
- **Gap:** dashboard phone layout and no horizontal scroll → no current test
- **Gap:** failed, incomplete, missing-evidence, repeated-visit, loading, and error screenshots → no current test

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| Exact graph and run contract support | Must-Test | Visual adapters consume these models directly. |
| Selected node and edge accuracy | Must-Test | State style changes can hide or misstate the active step. |
| Complete-path accuracy | Must-Test | The new color system must not change path membership. |
| Route collision safety | Must-Test | Route quality changes touch layout and edge render code. |
| Correlation privacy and HTTP `QUERY` | Must-Test | Dashboard layout changes must not change request behavior. |
| Mobile dialog focus | Must-Test | The responsive breakpoint and sheet design will change. |
| Theme persistence | Nice-To-Test | Token changes touch theme output but not stored state. |
| PostgreSQL import behavior | Low-Risk | No database or import format change is planned. |

### Scope Escalation Check

**Scope:** Contained. The fix changes the existing viewer design and its quality gates. It does not change graph JSON, run JSON, SQL schema, import formats, or tracing semantics.

## Proposed Fix

Replace the current collection of local style choices with one page composition, one semantic color model, one node grammar, one edge grammar, and one explanation content model. Keep Svelte 5, SvelteKit, Svelte Flow, ELK, Tailwind CSS v4, and repository-owned shadcn-svelte components. Do not add GSAP or a new design dependency.

The graph opens in a readable view, not an unconditional complete fit. An explicit overview action shows the complete topology. The current step and recorded path use interaction colors, not status colors. The inspector uses structured business fields and keeps raw values behind technical details. Approved visual baselines and measurable layout checks become completion gates.

## Unchanged Behavior

- WHEN a valid current developer graph JSON is selected THE SYSTEM SHALL CONTINUE TO render its complete normalized graph.
- WHEN a valid business graph V1 JSON is selected THE SYSTEM SHALL CONTINUE TO render its complete normalized graph.
- WHEN a stored run is opened THE SYSTEM SHALL CONTINUE TO load the exact graph ID and version referenced by that run.
- WHEN an observation is selected THE SYSTEM SHALL CONTINUE TO highlight the node and selected edge identified by the run record.
- WHEN complete-path mode is enabled THE SYSTEM SHALL CONTINUE TO derive the path from recorded IDs and graph topology.
- WHEN graph layout completes THE SYSTEM SHALL CONTINUE TO render ELK edge sections without entering unrelated nodes.
- WHEN a user searches by a confidential correlation THE SYSTEM SHALL CONTINUE TO send the value in an HTTP `QUERY` body and not in the URL.
- WHEN the mobile explanation closes THE SYSTEM SHALL CONTINUE TO restore focus to its trigger.

## Testing Plan

### Current Behavior

- WHEN the current 1,440-pixel run proof is reviewed THE SYSTEM CURRENTLY shows a small graph, status-like path colors, dense machine metadata, and mechanical explanation text.
- WHEN the current seven-node graph proof is reviewed THE SYSTEM CURRENTLY fits node labels below the required reading size.
- WHEN the current screenshot test runs THE SYSTEM CURRENTLY writes images without comparing them to approved baselines.

### Expected Behavior

- WHEN a 2-to-15-node graph opens THE SYSTEM SHALL show a reading view with effective node text of at least 12 CSS pixels and at least 45% use of one canvas dimension.
- WHEN complete fit requires zoom below 0.78 THE SYSTEM SHALL keep reading zoom and offer a separate overview action.
- WHEN a current step is shown THE SYSTEM SHALL use the primary interaction color and a sequence badge, with no warning or failure color unless the run failed.
- WHEN a recorded path is shown THE SYSTEM SHALL use a non-status path color and a non-color path cue.
- WHEN a predicate observation is selected THE SYSTEM SHALL show its question, answer, and evidence before technical data.
- WHEN an edge is the only outgoing continuation THE SYSTEM SHALL hide the `next` label.
- WHEN a Boolean branch label is shown THE SYSTEM SHALL show `Yes` or `No` and keep the raw token in technical details.
- WHEN an edge target is to the right or left of its source THE SYSTEM SHALL use the shortest collision-free port pair and SHALL NOT force an avoidable route through the source bottom.
- WHEN the dashboard is 390 CSS pixels wide THE SYSTEM SHALL show decision results without horizontal page scroll.
- WHEN visual regression tests run THE SYSTEM SHALL compare every approved reference image and fail beyond the documented threshold.

### Unchanged Behavior

- WHEN the contract, highlight, routing, search, privacy, mobile-focus, and theme regression suites run THE SYSTEM SHALL CONTINUE TO pass all Must-Test behavior checks.

## Acceptance Criteria

- [ ] Every finding IA-01 through QA-05, including ED-08, maps to a design rule and an implementation task.
- [ ] The initial reading view meets the 12-pixel effective text floor and 0.78 minimum zoom rule.
- [ ] Status colors are not used for node kind, path, current step, selection, or focus.
- [ ] The inspector shows structured question, answer, and evidence without inferred causal prose.
- [ ] The desktop, tablet, phone, light, dark, failure, incomplete, and error states have approved visual references.
- [ ] Automated visual comparison, geometry, scale, route-quality, contrast, and content tests pass.
- [ ] All Must-Test unchanged behaviors pass.
- [ ] A human reviewer approves the complete reference image set.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep each component focused on one responsibility.
- Do not hard-code a product graph or diagram.
- Use generated fixtures for graph and visual tests.
