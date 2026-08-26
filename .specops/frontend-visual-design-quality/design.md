# Design: Graph Viewer Visual Design Quality

## Design Outcome

The viewer must look like a calm internal investigation tool. It must lead with the business decision, make the selected path easy to follow, and keep technical data available without making it the first thing a user sees. Correct topology remains mandatory, but readable scale and semantic clarity become equal gates.

## Design Principles

1. **Explanation first:** Put the business question, answer, evidence, and result before IDs and raw values.
2. **Readable before complete fit:** Never shrink the normal starting view below the reading floor to show every node.
3. **One color, one meaning:** Status, node kind, path, current step, focus, and warning use separate roles.
4. **One dominant cue:** A state uses one main border cue. Supporting icons and labels must stay quiet.
5. **Progressive detail:** Show the business summary first. Put raw records in technical disclosures.
6. **Stable composition:** The same zones, spacing, and control positions apply across compatible screen sizes.
7. **Generated proof:** All graph examples and reference screens come from generated or real runtime artifacts, never a hard-coded product diagram.

## Page Composition

### Application shell

- Use a 56-pixel top bar on desktop and a 52-pixel top bar on phone.
- Keep brand, primary navigation, and theme action in one row.
- Use repository-owned shadcn-svelte buttons, badges, sheets, tooltips, separators, scroll areas, and skeletons.
- Use Tailwind CSS v4 for page layout and spacing. Use semantic CSS variables in `src/app.css` for color, radius, shadow, and graph tokens.
- Use scoped CSS only for Svelte Flow internals, node silhouettes, and SVG edges.

### Decision dashboard

- Use one page title: `Recorded decisions`.
- Place the correlation name, exact value, and search action in one compact search bar.
- Put optional filters in a shadcn collapsible section below the primary search.
- State result count as `N results shown` and show `More available` when a next cursor exists.
- At 720 pixels and wider, use the semantic table.
- Below 720 pixels, use stacked result rows with decision label, readable result, status, completion time, and one `Explain` action. Keep the execution ID in details.

### Decision detail

- Use a compact context header of at most 132 pixels at 1,080 pixels and wider.
- Row 1: back action, decision title, status badge.
- Row 2: readable final result.
- Row 3: completion time and a `Technical details` disclosure with execution ID, graph ID, graph version, and copy actions.
- Main area: graph canvas plus inspector. The canvas keeps at least 680 CSS pixels when the inspector is fixed.
- Use a 360-pixel default inspector. Allow resize from 320 to 480 pixels.
- Below 1,080 pixels, move the inspector to a sheet.

### Loaded graph preview

- Before load, show the page title, privacy note, and file drop or choose action.
- After load, replace the large introduction with one 64-pixel graph toolbar.
- The toolbar shows readable graph name, node count, edge count, completeness, privacy state, and `Replace file`.
- The graph canvas uses the remaining viewport and has a minimum height of 620 pixels on desktop.

## Spacing and Type Scale

Use a 4-pixel base spacing system.

| Role | Size | Line height | Weight |
| --- | --- | --- | --- |
| Page title | 28 px desktop, 22 px phone | 1.15 | 650 |
| Decision title | 20 px desktop, 18 px phone | 1.25 | 650 |
| Section title | 16 px | 1.3 | 650 |
| Body | 14 px | 1.5 | 400 |
| Node business label | 13 px preferred, 12 px minimum effective | 1.35 | 600 |
| Node kind | 10 px | 1.2 | 750 |
| Edge label | 11 px minimum effective | 1.2 | 650 |
| Metadata | 12 px | 1.4 | 500 |

Use monospace only for execution ID, graph ID, canonical value, and timestamps in technical details. Do not use monospace for the primary result.

## Semantic Color Model

All values live behind CSS variables. Components must not contain raw color values.

| Role | Token | Meaning |
| --- | --- | --- |
| Primary interaction | `--interactive-primary` | Current step, selected control, active navigation |
| Recorded path | `--interactive-path` | Nodes and edges visited in the selected run |
| Keyboard focus | `--focus-ring` | Focus only |
| Success status | `--status-success` | Successful run status only |
| Failure status | `--status-failure` | Failed run status and error only |
| Incomplete status | `--status-incomplete` | Incomplete run or coverage gap only |
| Normal graph route | `--graph-edge` | Topology with no selected state |
| Node-type accents | `--node-*` | Icon and 3-pixel rail only |

The current step uses the primary blue interaction role. The complete path uses a separate cool indigo role. Success stays green, failure stays red, and incomplete stays amber. Outcome nodes use a neutral slate type accent.

Every text and control pair must meet WCAG 2.2 AA. Normal graph edges and arrowheads must have at least 3:1 contrast against the canvas. Tests measure these values after theme and zoom transforms.

### Reference token values

These values are the first reference palette. Contrast tests can require a small adjustment, but any change must keep the semantic role and update the approved references.

| Token | Light | Dark |
| --- | --- | --- |
| `--interactive-primary` | `oklch(0.54 0.19 255)` | `oklch(0.72 0.15 250)` |
| `--interactive-path` | `oklch(0.56 0.17 290)` | `oklch(0.74 0.13 290)` |
| `--focus-ring` | `oklch(0.59 0.20 245)` | `oklch(0.78 0.14 245)` |
| `--status-success` | `oklch(0.50 0.15 150)` | `oklch(0.72 0.14 150)` |
| `--status-failure` | `oklch(0.54 0.21 28)` | `oklch(0.72 0.18 28)` |
| `--status-incomplete` | `oklch(0.64 0.15 75)` | `oklch(0.78 0.13 75)` |
| `--graph-edge` | `oklch(0.50 0.03 255)` | `oklch(0.70 0.03 255)` |
| `--node-entry` | `oklch(0.58 0.14 245)` | `oklch(0.72 0.12 245)` |
| `--node-predicate` | `oklch(0.55 0.14 275)` | `oklch(0.72 0.12 275)` |
| `--node-choice` | `oklch(0.60 0.16 315)` | `oklch(0.75 0.13 315)` |
| `--node-computation` | `oklch(0.52 0.03 255)` | `oklch(0.70 0.03 255)` |
| `--node-dispatch` | `oklch(0.56 0.12 210)` | `oklch(0.74 0.11 210)` |
| `--node-outcome` | `oklch(0.48 0.03 250)` | `oklch(0.68 0.03 250)` |
| `--node-gap` | `oklch(0.64 0.15 75)` | `oklch(0.78 0.13 75)` |

## Node Grammar

All nodes use a 232-by-96-pixel base box, a 12-pixel corner system, a 3-pixel type rail, a 16-pixel type icon, a 10-pixel type label, and a 13-pixel business label. The content box must not change size when state changes.

| Kind | Shape | Accent role | Notes |
| --- | --- | --- | --- |
| Entry | Capsule | Blue | One start point; no success meaning |
| Predicate | Common rounded card | Indigo | Branch icon; no extra diamond marker |
| Choice | Restrained six-sided outline | Violet | Use only for multi-path selection |
| Computation | Common rounded card | Slate | Calculator icon |
| Dispatch | Common card with one cut top corner | Cyan | Send icon |
| Outcome | Common terminal card with 16-pixel radius | Neutral slate | Result type, not status |
| Coverage gap | Common card with dashed border | Amber | Warning icon and incomplete meaning |

### Node state precedence

1. Default: neutral 1-pixel border.
2. Hover or pointer selection: neutral elevated surface.
3. Recorded path: 2-pixel path border and small route icon.
4. Current step: 2-pixel primary border and sequence badge. It replaces the path border.
5. Keyboard focus: 2-pixel outer focus ring with 2-pixel offset. It does not replace state.
6. Coverage gap: dashed amber type border remains visible, but focus and current use separate outer cues.

Do not use an outer glow for current or path. Keep dimmed node text at full contrast and reduce only decorative surface contrast. Keep dimmed opacity at 0.8 or more.

## Edge Grammar

- Keep ELK layered layout with direction `DOWN` and exact routed edge sections.
- Define a stable main vertical spine through the entry and the longest or selected route.
- Put alternate branches in left or right corridors selected by stable edge ID order.
- Keep parallel routes at least 12 CSS pixels apart at reading zoom.
- Keep non-port route segments at least 16 CSS pixels long at reading zoom.
- Keep labels at least 8 pixels from a node and 6 pixels from another label.
- Hide `next` when a node has only one outgoing edge.
- Map exact Boolean `true` and `false` to `Yes` and `No` on the canvas. Keep other concise business branch labels unchanged.
- Keep raw outcome values in technical details.
- Use 1.25-pixel normal routes, 2-pixel path routes, and 2.5-pixel active routes.
- Keep arrowheads proportional and at least 10 by 10 CSS pixels at reading zoom.
- Do not animate route strokes.

For generated simple acyclic fixtures, reject avoidable backtracking, segments shorter than 16 pixels, more than four bends per route, unrelated-node intrusion, label collision, and non-distinct parallel paths.

### Port selection and shortest valid routes

Nodes expose north, east, south, and west ports to the layout model. The ports stay hidden in the read-only Svelte Flow canvas. Each routed edge records its selected source port and target port. The renderer must use those exact ports and ELK sections.

For each edge, evaluate valid source and target port pairs before final layout:

1. Reject a pair if its orthogonal route enters an unrelated node or crosses a protected label area.
2. Reject an avoidable upward first segment in a top-to-bottom acyclic flow.
3. Minimize total Manhattan route length.
4. When route lengths differ by 8 CSS pixels or less, prefer fewer bends.
5. Prefer a side port when the target center is outside the source horizontal span and the side route is shorter than a south-port route.
6. Spread edges that share one side across distinct port slots with at least 16 CSS pixels between adjacent slots.
7. Use stable edge ID order only as the final tie-breaker.

An edge from a source to a target on its right must normally leave the source east side. It must not leave the south side, turn left or right under the source, and then travel to the distant target unless obstacle avoidance proves that route shorter. The matching rule applies to a target on the left.

The route-quality test must compare the chosen route with all valid port-pair candidates. It fails when another collision-free candidate is shorter by more than 8 CSS pixels or has the same length with fewer bends.

## Viewport Modes

### Reading view

Reading view is the default.

- Minimum zoom: 0.78.
- Preferred maximum zoom: 1.0.
- Keep effective node label text at 12 CSS pixels or more.
- For preview, center entry and the first useful branch group.
- For a run, center the current step and include its predecessor and successor when they exist.
- For graphs with 2 to 15 nodes, use at least 45% of one usable canvas dimension.

### Overview

Overview is explicit. It fits the full graph and can use zoom below 0.78. At overview zoom, hide body details and show short labels, silhouettes, state borders, and route direction. Label it `Overview` in the toolbar.

### Search focus

Search centers the matched node at reading zoom. It must include direct predecessor and successor nodes and 80 pixels of local context when topology permits. A `Back to overview` action remains visible.

### Current-step focus

The `Current step` action returns to reading zoom and centers the selected observation without changing step selection.

## Graph Toolbar

Use one toolbar within the canvas.

- Left: node search with a result list and keyboard navigation.
- Right: `Current step` when a run exists, `Reading view`, `Overview`, zoom out, zoom in.
- Phone: use one bottom toolbar with 44-pixel targets and safe-area padding.
- Do not show a minimap for 8 nodes or fewer.
- Show a minimap for 9 to 100 nodes in overview only.
- Above 100 nodes, show node count and search guidance. Do not show a compressed minimap.
- Keep Svelte Flow attribution quiet but readable.

## Explanation Content Model

Presentation code receives a structured view model. It must not generate a causal sentence by joining arbitrary evidence text and node labels.

```text
StepExplanation:
  sequence: number
  totalVisits: number
  kind: node kind
  title: business label
  question: string or null
  answer: string or null
  evidenceItems: [{ label, displayValue, canonicalValue, type }]
  branchLabel: string or null
  rawOutcome: string
  nodeId: string
  selectedEdgeId: string or null
```

Predicate step order:

1. Business question from the node label.
2. `Answer: Yes`, `Answer: No`, or the business branch label.
3. `Because` followed by recorded evidence items.
4. Neutral missing-evidence message when the list is empty.
5. Technical disclosure with raw outcome, node ID, selected edge ID, typed canonical values, and sequence.

Outcome step order:

1. Outcome label.
2. Readable final display value when present.
3. Run status badge.
4. Technical canonical result in disclosure.

The selected step uses a neutral surface, primary border, and sequence marker. Red surface is allowed only for an actual failed step. The inspector uses a sticky toolbar and a separate scroll area for steps.

## Responsive Rules

| Width | Composition |
| --- | --- |
| 1,280 and wider | Compact header, canvas, fixed resizable inspector |
| 1,080 to 1,279 | Compact header, fixed 320-pixel inspector only if canvas stays at least 680 pixels |
| Less than 1,080 | Graph uses full width; inspector is a sheet |
| Less than 720 | Dashboard uses stacked result rows; detail metadata is disclosed |
| 390 reference | Two-row detail header, bottom graph toolbar, full-height explanation sheet |

Opening and closing the sheet preserves graph viewport, selected step, and full-path mode. Closing restores focus to the trigger.

## Motion

Use only CSS transitions for surface, border, opacity, and sheet movement. Keep transitions at 160 ms or less. Under `prefers-reduced-motion: reduce`, remove non-essential transitions. GSAP is not approved because the viewer does not need timeline animation and the extra dependency does not improve the core explanation task.

## Component Responsibilities

| Component | Single responsibility |
| --- | --- |
| `AppShell.svelte` | Global navigation and theme |
| Decision header component | Business summary and technical metadata disclosure |
| `RunFilters.svelte` | Search input and optional filters |
| `RunList.svelte` | Responsive result presentation |
| `FlowCanvas.svelte` | Graph interaction and viewport modes |
| `BusinessNode.svelte` | Node grammar and state rendering |
| `BusinessEdge.svelte` | Edge route, label, and state rendering |
| Step explanation adapter | Convert record data to structured presentation fields |
| `RunInspector.svelte` | Step navigation and explanation presentation |
| `GraphUpload.svelte` | Browser-only file selection and validation state |

Data access remains in page server and server-only repository modules. No visual component reads PostgreSQL or parses raw graph JSON.

## Visual Reference Matrix

All images use generated fixtures or generated runtime artifacts.

| Page or state | 1440 light | 1440 dark | 1024 light | 390 light |
| --- | --- | --- | --- | --- |
| Dashboard with results | Required | Required | Required | Required |
| Dashboard no match | Required | — | — | Required |
| Dashboard error | Required | Required | — | Required |
| Decision current step | Required | Required | Required | Required closed and sheet |
| Decision complete path off | Required | Required | — | — |
| Failed decision | Required | Required | — | Required sheet |
| Incomplete or missing evidence | Required | Required | — | Required sheet |
| Graph preview empty | Required | Required | — | Required |
| Graph preview 4-node branch | Required | Required | — | — |
| Graph preview 7-node grammar | Required | Required | Required | — |
| Long labels | Required | Required | — | Required |
| State gallery | Required | Required | — | — |
| 250-node search focus | Required | Required | — | — |

## Automated Quality Gates

1. **Visual regression:** Compare approved images with Playwright `threshold: 0.2` and `maxDiffPixelRatio: 0.005`. Mask only unstable timestamps or browser-native controls. A reviewer must approve any threshold or mask change.
2. **Rendered scale:** Read Svelte Flow transforms and computed font sizes. Enforce the 12-pixel effective node-label floor in reading view.
3. **Composition:** For 2-to-15-node fixtures, enforce 45% use of one usable canvas dimension.
4. **Route quality:** Enforce port choice, shortest valid route, intrusion, parallel distinction, minimum segment, bend count, reversal, and label clearance rules.
5. **Color roles:** Assert that status token values differ from interaction and node-type tokens. Measure rendered contrast in both themes.
6. **Content:** Assert question, answer, evidence, missing evidence, raw technical values, and readable final result placement.
7. **Responsive:** Assert no horizontal page scroll at 390 pixels and focus restoration from the sheet.
8. **Accessibility:** Run keyboard journeys and automated accessibility checks. Manually review zoom, color independence, and screen-reader names.
9. **Human design gate:** A reviewer signs off the full visual reference matrix. This is required even when CI passes.
10. **Zoom and forced colors:** At 200% browser zoom, the dashboard and explanation stay usable without two-axis page scroll. In forced-colors mode, node kind, path, current, focus, and status keep text or shape cues.

## Dependency Decisions

| Package | Version | Ecosystem | Decision | Rationale |
| --- | --- | --- | --- | --- |
| New runtime package | — | Node.js | Rejected | Existing Svelte 5, SvelteKit, Svelte Flow, ELK, Tailwind CSS v4, shadcn-svelte, Bits UI, and Lucide cover the design. |
| GSAP | — | Node.js | Rejected | CSS transitions cover the small state changes. Timeline animation adds weight and does not improve explanation. |

No new dependency is introduced.

## Risks and Mitigations

- **Risk:** Readable initial zoom can hide part of a deep graph. **Mitigation:** Keep explicit Overview and Reading view actions and preserve viewport state.
- **Risk:** Route-quality rules can over-constrain ELK on complex graphs. **Mitigation:** Apply strict bend and segment rules to generated simple fixtures, while keeping universal collision and clearance rules for all graphs.
- **Risk:** New content labels can claim facts not present in the record. **Mitigation:** Use only node labels, branch outcomes, final display values, and recorded typed evidence. Use a neutral missing-evidence message.
- **Risk:** Visual baselines can be brittle across platforms. **Mitigation:** Use one pinned browser, checked fonts, a documented threshold, and narrow masks.
- **Risk:** A redesign can break ID-based highlight behavior. **Mitigation:** Keep the existing model adapters and Must-Test regression suite unchanged.

## Out of Scope

- Graph editing or drag-position persistence
- A left-to-right layout option
- New graph JSON or run JSON versions
- SQL schema changes
- Binary upload implementation
- Server upload from the preview page
- GSAP or timeline animation
- Partial graph data loading
- Product-specific or customer-specific diagrams
