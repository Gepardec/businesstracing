# Visual Design: Interactive Flow and Run Explorer

## Design Goal

The viewer must make a run easy to follow before it makes the complete graph easy to inspect. It uses a quiet application shell, a strong graph hierarchy, and a fixed visual grammar. It does not copy Mermaid styling and it does not store graph-specific positions.

## Application Shell

- Use `shadcn-svelte` with the `new-york` style and a neutral base.
- Use Tailwind CSS v4 for layout, spacing, responsive rules, and theme tokens.
- Use CSS custom properties in `src/app.css` as the only color source.
- Use scoped CSS for node silhouettes, connection paths, and required Svelte Flow overrides.
- Use the system UI font stack for labels and controls. Use the system monospace stack only for IDs and timestamps.
- At 1,200 CSS pixels or more, use a 56-pixel application rail, a flexible graph canvas, and a 360-pixel right inspector. The inspector can resize from 320 to 520 pixels.
- Below 1,200 pixels, put the inspector in a shadcn `Sheet`. Keep the selected step and graph viewport when it opens or closes.
- Put graph search, fit, zoom, view mode, theme, and minimap controls in one compact canvas toolbar. Keep run filters on the runs page.

## Component Use

| UI purpose | shadcn-svelte component |
| --- | --- |
| Application actions | `Button`, `DropdownMenu`, `Tooltip` |
| Run and graph search | `Input`, `Command`, `Select` |
| Run status and node kind | `Badge` |
| Path mode | `Toggle` or `Switch` with a text label |
| Desktop sections | `Card`, `Separator`, `ScrollArea` |
| Narrow inspector | `Sheet` |
| Run results | semantic table with shadcn table styles |
| Loading and failures | `Skeleton`, `Alert` |

Generated shadcn source stays in `src/lib/components/ui/`. Product components can compose it but must not mix data access with presentation.

## Theme Tokens

Use OKLCH values behind semantic CSS variables. Do not put raw palette values in Svelte components.

| Token group | Purpose |
| --- | --- |
| `--background`, `--foreground`, `--card`, `--muted`, `--border` | Application surfaces and text |
| `--graph-canvas`, `--graph-grid`, `--graph-edge` | Graph background and normal edges |
| `--node-entry`, `--node-predicate`, `--node-choice` | Entry and decision categories |
| `--node-computation`, `--node-dispatch`, `--node-outcome`, `--node-gap` | Action, terminal, and incomplete categories |
| `--run-path`, `--run-current`, `--run-dimmed` | Run highlight states |
| `--status-success`, `--status-failure`, `--status-running` | Run status only |

Each theme must meet WCAG 2.2 AA for text and controls. Node type colors can have lower decorative contrast only when the icon, type label, and silhouette remain visible.

## Node Grammar

All nodes use a 12-pixel corner system, a minimum 44-pixel interaction target, a type icon, a visible type label at detail zoom, a business label, and stable north/south ports. Long labels wrap to three lines and then use an ellipsis plus a tooltip.

| Node kind | Silhouette | Type icon | Semantic color | Meaning |
| --- | --- | --- | --- | --- |
| Entry | Capsule | `LogIn` | Blue | Start of a business flow |
| Predicate | Rectangle with a diamond left marker | `GitBranch` | Indigo | Boolean condition |
| Choice | Hexagonal card | `Split` | Violet | Multi-path selection |
| Computation | Rounded rectangle | `FunctionSquare` or closest current Lucide icon | Neutral slate | Calculation or transformation |
| Dispatch | Cut-corner card | `Send` | Cyan | Call or dispatch to another operation |
| Outcome | Double-border terminal card | `CircleCheck` | Green-neutral | Declared business result; success or failure uses a separate status badge |
| Coverage gap | Dashed octagonal card | `TriangleAlert` | Amber | Known missing or unresolved flow |

The silhouette can use a CSS mask or `clip-path`, but its text area must stay rectangular. The component test must prove that a user can identify every kind in monochrome.

## Edge Grammar

- Use orthogonal edges with rounded corners and arrowheads in the top-to-bottom direction.
- Use one-pixel neutral edges by default.
- Use a two-pixel `--run-path` edge for the selected run path.
- Use a three-pixel `--run-current` edge for the active selected edge.
- Use a dashed amber edge only for a documented coverage gap.
- Show edge labels at detail zoom or when the edge is hovered, focused, selected, or part of the current step.
- Do not use animation as the only state signal. Respect `prefers-reduced-motion`.

## State Precedence

State decoration does not replace the node-kind decoration. Apply states in this order, from lowest to highest:

1. Default: type silhouette, icon, label, and token.
2. Hover or keyboard focus: neutral two-pixel focus outline.
3. Full run path: `--run-path` inner border and path badge.
4. Selected node: elevated border and selection handle.
5. Current run step: three-pixel `--run-current` outer ring, step-number badge, and matching inspector marker.
6. Compatibility or coverage error: warning icon and patterned or dashed border.

When full-path mode is active, non-path nodes use reduced contrast but stay readable and selectable. The current step remains stronger than the full path. Repeated visits use the active observation sequence number in the badge.

## Top-to-Bottom Layout

- Use ELK layered layout with direction `DOWN`.
- Place entry nodes at the top and terminal nodes at the bottom when topology permits it.
- Use fixed node and layer gaps from tokens, not graph-specific values.
- Sort graph inputs by stable IDs before layout.
- Route outgoing branches to south ports and incoming branches to north ports.
- Place the inspector on the right so it does not compete with the main flow direction.
- Do not persist dragged positions in version one. A fit or reload returns to the generated layout.

Top-to-bottom is the only supported direction in version one. An orientation switch can be added later only if user tests show a recurring need.

## Large Graph Behavior

| Graph size | Initial view | Detail behavior |
| --- | --- | --- |
| Up to 250 nodes | Full graph | All normal interactions and three semantic zoom levels |
| 251 to 1,000 nodes | Full graph in large mode | Viewport-only elements, overview symbols at low zoom, conditional edge labels, minimap, node search, cached worker layout |
| More than 1,000 nodes | Selected run path plus one-hop context | Persistent partial-view notice, visible shown/total counts, node search, and explicit `Load full graph` action |

If no run is selected for a graph above 1,000 nodes, show an overview based on the full topology with simplified symbols and no edge labels. Do not remove nodes without a visible partial-view notice. A run-focus projection contains only existing nodes and edges and must not add a synthetic connection.

The canvas must offer these recovery actions: find node, fit current view, return to current step, switch between full graph and run focus, and reset zoom.

## Semantic Zoom

- Overview: show silhouette, icon, short label, and state rings. Hide metadata and normal edge labels.
- Reading: show business label, type icon, type name, and relevant edge labels.
- Detail: add source location or business metadata only when the graph contract supplies it.

The same zoom thresholds apply to all graphs. Selection must not change a node's measured layout size.

## Visual Quality Gate

Use generated graph fixtures, never checked-in product diagrams, for visual tests. Approve reference screenshots for:

- Light and dark theme at 1,440 by 900 pixels.
- Narrow layout at 390 by 844 pixels with the inspector open and closed.
- Every node kind in default, focused, selected, path, current-step, and warning states.
- A 250-node full graph, a 1,000-node large graph, and a more-than-1,000-node run-focus view.
- Long labels, repeated run visits, empty results, loading, compatibility failure, and layout failure.

Automated image comparison must use a small documented tolerance for font rendering. A reviewer must also check label collisions, clipped controls, state ambiguity, keyboard focus, theme contrast, and reduced motion.
