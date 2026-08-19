# Design: Interactive Flow and Run Explorer

<!-- This spec contains security-sensitive architectural details. Review access before sharing. -->

## Architecture Overview

The repository has stable graph JSON, versioned run JSON, and indexed PostgreSQL storage, but it has no browser delivery layer. A new `fachtracing-viewer` SvelteKit application owns the read-only web experience. Server-only modules load versioned graph artifacts and query existing tables; client modules adapt these contracts into Svelte Flow nodes, edges, and run-selection state. This boundary keeps database credentials out of the browser and keeps layout concerns out of the Java engine.

The application uses `@xyflow/svelte` for interaction and ELK's layered algorithm for positions. ELK runs from graph data in a Web Worker. Stable input sorting, measured node sizes, orthogonal routing, and fixed spacing options produce repeatable layouts without graph-specific coordinates. `visual-design.md` defines the application shell, node language, state precedence, themes, and large-graph behavior.

## Technical Decisions

### Decision 1: Add a standalone SvelteKit Node application

**Decision:** Create `fachtracing-viewer/` as a TypeScript SvelteKit application built with the Node adapter.

**Rationale:** The viewer needs server-side PostgreSQL access and a browser UI. One SvelteKit application supplies both while keeping the Java library framework-neutral.

### Decision 2: Read the current contracts through narrow adapters

**Decision:** Put schema parsing in `graph-contract.ts` and `run-contract.ts`, and put SQL in one `run-repository.server.ts` module.

**Rationale:** The UI model must not become a second wire format. Narrow adapters make schema-version handling testable and contain PostgreSQL-specific work in one server-only component.

### Decision 3: Use ELK layered layout with Svelte Flow

**Decision:** Convert sorted contract nodes and edges into an ELK graph, compute top-to-bottom positions in a worker, and pass the result to Svelte Flow.

**Rationale:** The graphs express directed business flow. Top-to-bottom flow follows page reading and vertical scrolling, and it leaves a stable right side for the run inspector. Layered layout reduces crossings and gives stronger control over rank, ports, and spacing than a force layout. Svelte Flow supplies pan, zoom, selection, minimap, and custom node/edge rendering.

### Decision 4: Use shadcn-svelte and Tailwind CSS v4

**Decision:** Build the application shell from repository-owned `shadcn-svelte` components in the `new-york` style. Use Tailwind CSS v4 and CSS custom properties for tokens. Use small scoped CSS files only for graph silhouettes, edge states, and Svelte Flow overrides.

**Rationale:** This gives the viewer consistent accessible controls while keeping the generated component source in the repository. It also keeps graph-specific styling separate from application-shell styling. The latest shadcn-svelte line targets Svelte 5 and Tailwind CSS v4.

### Decision 5: Use progressive large-graph modes

**Decision:** Render full detail through 250 nodes. From 251 through 1,000 nodes, use semantic zoom, viewport-only rendering, reduced edge labels, and cached worker layouts. Above 1,000 nodes, open with the selected run path plus one-hop context and require an explicit full-graph action.

**Rationale:** A single fixed level of detail becomes unreadable before it becomes technically impossible to draw. Progressive modes preserve orientation and interaction. The UI always states when it shows a partial topology and never changes the stored graph.

### Decision 6: Derive run highlights from IDs

**Decision:** Keep ordered observations as the source of truth. Derive the current node, selected edge, observed-node set, and resolved connecting-edge set from `nodeId`, `selectedEdgeId`, and the graph topology.

**Rationale:** The Java model already uses stable IDs and sequence values. Derivation avoids duplicated mutable highlight state and keeps repeated node visits correct.

### Decision 7: Use cursor pagination and explicit filters

**Decision:** Page by `(completed_at, execution_id)` and support exact indexed correlation lookup plus bounded metadata filters.

**Rationale:** Offset pagination becomes unstable as new runs arrive. The existing graph-time and correlation-time indexes support the required read paths with small, parameterized queries.

## Component Design

### Graph Catalog

**Responsibility:** Load and cache supported developer graph JSON files from one configured read-only directory.

**Interface:** `getGraph(graphId, graphVersion): Promise<GraphDocument>` and `listGraphs(): Promise<GraphSummary[]>`.

**Failure behavior:** Reject unsupported schemas, duplicate graph/version pairs, path traversal, files outside the configured directory, and graph documents that fail their published JSON Schema.

### Run Repository

**Responsibility:** Read run summaries and one V1 payload from PostgreSQL.

**Interface:** `searchRuns(query): Promise<RunPage>` and `getRun(executionId): Promise<RunDocument | null>`.

**Failure behavior:** Use parameterized queries, a 30-second statement timeout, bounded filter lengths, a maximum page size of 50, and generic public errors. Log no credentials, correlation values, payloads, or SQL parameter values.

### Contract Adapters

**Responsibility:** Validate schema identity and convert one graph or run document into immutable UI types.

**Interface:** `parseGraph(unknown): GraphModel` and `parseRun(unknown): RunModel`.

**Failure behavior:** Ignore unknown fields for supported schemas. Reject missing required fields, duplicate IDs, dangling edges, non-monotonic observation sequences, and graph/version mismatch.

### Layout Worker

**Responsibility:** Compute positions and edge routes for one valid graph.

**Interface:** `layout(graph, measuredNodeSizes): Promise<LayoutResult>`.

**Failure behavior:** Cancel obsolete requests. If ELK fails, show the graph error state and keep the run list usable; do not fall back to arbitrary positions.

### Flow Canvas

**Responsibility:** Render the laid-out graph and expose selection, pan, zoom, fit, minimap, and keyboard navigation.

**Interface:** Svelte component props for `GraphModel`, `LayoutResult`, and `RunHighlight`.

### Graph View Model

**Responsibility:** Select the full, large, or run-focus projection without changing graph contracts.

**Interface:** `buildGraphView(graph, run, mode): GraphViewModel`.

**Failure behavior:** Mark every partial view, keep original counts, and never create synthetic business edges. A one-hop context view can include only existing nodes and edges.

### Run Inspector

**Responsibility:** Present observations in order and own the active observation index and full-path toggle.

**Interface:** Svelte component props for `RunModel` and selection callbacks.

### Runs Explorer

**Responsibility:** Own URL-backed filters, cursor navigation, loading, empty, and retry states.

**Interface:** `/runs` page and `/runs/[executionId]` detail route.

## System Flow

1. The runs page sends bounded URL filters to the SvelteKit server route.
2. The server queries run metadata with a cursor and returns at most 50 summaries.
3. Run selection loads the V1 run payload and its exact graph ID/version.
4. The graph catalog loads the matching JSON artifact and the contract adapters validate both documents.
5. The graph view model selects a full or explicit run-focus projection, and the layout worker computes top-to-bottom positions from it.
6. The canvas renders the selected graph view. The inspector derives and applies current-step or full-path highlighting.

## State Management

Route parameters and search parameters are the durable state. Local Svelte state contains the parsed graph, parsed run, graph view mode, layout result, active observation index, panel state, theme, and full-path flag. The graph document and layout are cached by graph ID, graph version, direction, node-size profile, and view mode. The run payload is cached by execution ID for the current navigation session.

## API Changes

### New endpoints

- `GET /api/v1/graphs` returns available graph summaries.
- `GET /api/v1/graphs/{graphId}/versions/{graphVersion}` returns one supported graph document.
- `GET /api/v1/runs?executionId=&graphId=&status=&from=&to=&correlationKey=&correlationValue=&cursor=&limit=` returns run summaries and the next cursor.
- `GET /api/v1/runs/{executionId}` returns one unchanged `fachtracing-decision-record/v1` payload.

All endpoints use `application/json`, reject unknown schema versions, and return problem details without internal paths or database details.

## SQL Read Contract

- Summary queries select only `execution_id`, `graph_id`, `graph_version`, `started_at`, `completed_at`, and `status` from `fachtracing_decision_record`.
- Detail queries select `payload` by exact `execution_id`.
- Correlation filters join `fachtracing_correlation` by record ID and use exact `correlation_name`, exact already-redacted `correlation_value`, and the inclusive completion range.
- Cursor predicates use `completed_at < ? OR (completed_at = ? AND execution_id < ?)` with descending order.
- No migration changes are required for the first version. The million-row performance contract decides whether a new descending metadata index is needed; any index addition requires a separate documented V2 migration.

## Security Considerations

- Data classification: graph labels and run summaries are Internal. Already-redacted evidence and correlation values are Confidential.
- Database credentials exist only in server environment variables.
- Server responses set a restrictive content security policy and do not render graph labels as HTML.
- URL, graph ID, execution ID, and correlation inputs have length and character limits before database or file access.
- The server starts on loopback by default. Public deployment requires a reverse proxy with authentication and transport security.

## Performance Considerations

- Use one ELK worker and discard stale layout replies by request token.
- Use the ELK layered `DOWN` direction and north/south ports by default.
- Render only visible Svelte Flow elements when the library option is compatible with accessibility checks.
- Apply three semantic zoom levels: overview symbols, readable labels, and full node detail.
- Hide edge labels unless the edge is selected, highlighted, or sufficiently zoomed.
- Use explicit run-focus mode above 1,000 nodes. Never silently omit topology.
- Cache validated graph documents and layout results by graph/version and file modification time.
- Keep run pages at 50 rows and return summary columns only.

## Testing Strategy

- Unit tests map every EARS criterion to contract parsing, highlight derivation, cursor encoding, and filter validation.
- Component tests cover repeated visits, current-step selection, full-path mode, mismatch errors, empty states, and keyboard navigation.
- Playwright tests cover the list-to-run workflow, responsive inspector, deep links, themes, semantic zoom, large-graph modes, and accessibility checks.
- Visual tests compare approved desktop and narrow screenshots for both themes and all node states.
- PostgreSQL integration tests use the existing V1 migration and verify parameterized search, stable pagination, correlation semantics, timeouts, and the performance fixture.
- Layout benchmarks use generated topology, not a hardcoded product graph, at 250 nodes and 400 edges and at 1,000 nodes and 1,600 edges.

## Rollout Plan

1. Ship the viewer as an opt-in module with documented environment variables and loopback binding.
2. Validate it with generated developer graphs and synthetic V1 run payloads.
3. Run the PostgreSQL contract and browser suite in CI.
4. Review one Mega or Keycloak graph only as black-box conformance after generic tests pass.

## Risks & Mitigations

- **Risk:** Svelte Flow API changes affect the viewer. **Mitigation:** Isolate it behind `FlowCanvas.svelte`, pin the lockfile, and exercise browser behavior.
- **Risk:** Layout work blocks the page or creates an unreadable result on large graphs. **Mitigation:** Run ELK in a worker, cancel stale results, use progressive graph modes, and test both size profiles.
- **Risk:** Direct SQL drifts from the Java migration. **Mitigation:** Test against `JdbcDecisionRecordRepository.migrate()` and keep all SQL in one server-only repository.
- **Risk:** A run references a graph artifact that is not deployed. **Mitigation:** Keep the run visible, show a compatibility error, and disable highlighting.

## Dependencies & Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| developer-graph-json-schema | Supplies validated graph contracts. | Yes | completed |
| generic-application-readiness | Supplies the V1 database, payload, and storage contracts. | Yes | completed |

### Dependency Decisions

| Package | Version | Ecosystem | Decision | Rationale |
| --- | --- | --- | --- | --- |
| `svelte` | compatible stable 5.x | Node.js | Approved | Required UI runtime; official, MIT, active, and paired with SvelteKit. |
| `@sveltejs/kit` | 2.70.2 | Node.js | Approved | Required full-stack framework; official, MIT, active, and provides typed routes and server boundaries. |
| `@sveltejs/adapter-node` | 5.5.7 | Node.js | Approved | Required to deploy the same SvelteKit application with server-only PostgreSQL access. |
| `tailwindcss` | compatible stable 4.x | Node.js | Approved | Zero-runtime utility and token compiler for the SvelteKit application shell. |
| `@tailwindcss/vite` | compatible stable 4.x | Node.js | Approved | Official Tailwind CSS v4 integration for the existing Vite build. |
| `shadcn-svelte` | compatible latest stable | Node.js | Approved for development | Generates repository-owned Svelte 5 component source. It is not an application runtime abstraction. |
| `bits-ui` | compatible stable release selected by shadcn-svelte | Node.js | Approved | Accessible headless behavior used by selected shadcn-svelte components. |
| `lucide-svelte` | compatible stable release selected by shadcn-svelte | Node.js | Approved | Consistent SVG icons for node type and application actions. |
| `tailwind-variants` | compatible stable release selected by shadcn-svelte | Node.js | Approved | Typed component variants used by generated components. |
| `clsx` | compatible stable release selected by shadcn-svelte | Node.js | Approved | Conditional class composition used by generated components. |
| `tailwind-merge` | compatible stable release selected by shadcn-svelte | Node.js | Approved | Resolves Tailwind class conflicts in generated component utilities. |
| `@xyflow/svelte` | 1.6.2 | Node.js | Approved | Exact scope match for interactive node-link UI; MIT, active, two direct dependencies. Its alpha warning is contained behind one component and browser contracts. |
| `elkjs` | 0.12.0 | Node.js | Approved | Exact scope match for layered graph layout; zero runtime dependencies. EPL-2.0 use is compatible, and the worker keeps layout off the UI thread. |
| `pg` | 8.22.0 | Node.js | Approved | Focused PostgreSQL driver with pooling and parameterized queries; MIT and widely maintained. |
| `vitest` | 4.1.10 | Node.js | Approved | Unit and component test runner aligned with Vite. |
| `@playwright/test` | 1.62.1 | Node.js | Approved | Browser behavior, keyboard, responsive layout, and end-to-end verification require a real browser. |

Registry metadata was checked on 2026-08-19. The implementation must generate and commit a lockfile, then pass `npm audit --audit-level=high` before these versions become final.

## Future Enhancements

- Cross-run comparison and aggregate path frequency.
- Live run updates.
- Saved searches and user-specific views.
- Authenticated public deployment and tenant isolation.
- Additional database adapters behind the same run repository interface.
