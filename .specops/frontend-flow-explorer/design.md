# Design: Interactive Flow and Run Explorer

<!-- This spec contains security-sensitive architectural details. Review access before sharing. -->

## Architecture Overview

The repository has stable developer graph V1 JSON, versioned run JSON, and indexed PostgreSQL storage, but it has no browser delivery layer or durable graph catalog. A new `fachtracing-viewer` SvelteKit application owns the local read-only dashboard. A separate import command validates graph files and stores their unchanged bytes in PostgreSQL. Server-only modules load the exact graph version and query existing run tables; browser modules receive a provenance-free graph projection and adapt it into Svelte Flow nodes, edges, and run-selection state. This boundary keeps database credentials and developer source locations out of the browser and keeps layout concerns out of the Java engine.

The application uses `@xyflow/svelte` for interaction and ELK's layered algorithm for positions. ELK runs from graph data in a Web Worker. Stable input sorting, measured node sizes, orthogonal routing, and fixed spacing options produce repeatable layouts without graph-specific coordinates. `visual-design.md` defines the application shell, node language, state precedence, themes, and scale budget.

## Technical Decisions

### Decision 1: Add a standalone SvelteKit Node application

**Decision:** Create `fachtracing-viewer/` as a TypeScript SvelteKit application built with the Node adapter.

**Rationale:** The viewer needs server-side PostgreSQL access and a browser UI. One SvelteKit application supplies both while keeping the Java library framework-neutral.

### Decision 2: Read the current contracts through narrow adapters

**Decision:** Put schema parsing in `graph-contract.ts` and `run-contract.ts`. Put graph import and retrieval in `graph-catalog-repository.server.ts`, and put run SQL in `run-repository.server.ts`. Treat `fachtracing-developer-graph/v1` as the sole current developer graph contract. Its current shape is the merged multi-source shape with `sourceOrigins`, `sourceFiles`, and per-source `originId` fields.

**Rationale:** Narrow adapters make schema handling testable and keep PostgreSQL-specific work in server-only components. The browser graph projection is a presentation model, not a stored tracing format. It removes developer provenance while preserving every graph, node, and edge ID needed for path explanation.

### Decision 3: Use ELK layered layout with Svelte Flow

**Decision:** Convert sorted contract nodes and edges into an ELK graph, compute top-to-bottom positions in a worker, and pass the result to Svelte Flow.

**Rationale:** The graphs express directed business flow. Top-to-bottom flow follows page reading and vertical scrolling, and it leaves a stable right side for the run inspector. Layered layout reduces crossings and gives stronger control over rank, ports, and spacing than a force layout. Svelte Flow supplies pan, zoom, selection, minimap, and custom node/edge rendering.

### Decision 4: Use shadcn-svelte and Tailwind CSS v4

**Decision:** Build the application shell from repository-owned `shadcn-svelte` components in the `new-york` style. Use Tailwind CSS v4 and CSS custom properties for tokens. Use small scoped CSS files only for graph silhouettes, edge states, and Svelte Flow overrides.

**Rationale:** This gives the viewer consistent accessible controls while keeping the generated component source in the repository. It also keeps graph-specific styling separate from application-shell styling. The latest shadcn-svelte line targets Svelte 5 and Tailwind CSS v4.

### Decision 5: Design for the measured graph scale

**Decision:** Render the complete graph in version one. Keep the generated 250-node and 400-edge benchmark as safety headroom. Defer partial graph projections and size-specific rendering modes until repository evidence or production measurements require them.

**Rationale:** The checked-in business graphs have 7, 8, and 15 nodes. A 250-node benchmark already gives substantial headroom. A second graph model and unproved 1,000-node behavior add complexity without serving a current graph.

### Decision 6: Derive run highlights from IDs

**Decision:** Keep ordered observations as the source of truth. Derive the current node, selected edge, observed-node set, and resolved connecting-edge set from `nodeId`, `selectedEdgeId`, and the graph topology.

**Rationale:** The Java model already uses stable IDs and sequence values. Derivation avoids duplicated mutable highlight state and keeps repeated node visits correct.

### Decision 7: Use cursor pagination and explicit filters

**Decision:** Page by `(completed_at, execution_id)` and support exact indexed correlation lookup plus bounded metadata filters.

**Rationale:** Offset pagination becomes unstable as new runs arrive. Exact correlation lookup uses the existing correlation index. A cursor index on completion time and execution ID supports the unfiltered newest-decision dashboard.

### Decision 8: Use HTTP QUERY for decision search

**Decision:** Send search documents to `QUERY /api/v1/runs` as `application/json`. Use the SvelteKit fallback method handler to accept `QUERY` and reject all other unhandled methods.

**Rationale:** RFC 10008 defines `QUERY` as safe and idempotent and gives request content explicit query semantics. It keeps confidential correlation values out of the request URI. The local same-origin POC does not need CORS. A future proxy must explicitly allow the method.

### Decision 9: Store immutable graph payloads in PostgreSQL

**Decision:** Add a graph catalog table keyed by graph ID and version. Store the unchanged developer graph V1 JSON as bytes with schema ID, media type, SHA-256 checksum, and import time. Use a separate command for import; keep the running dashboard read-only.

**Rationale:** A decision record is explainable only while its exact graph version exists. Database storage keeps graph retention with decision retention and permits JSON or a future documented binary media type without changing the table. Version one imports only the current JSON contract and does not invent a binary wire format.

### Decision 10: Use generated self-tracing artifacts as browser proof

**Decision:** Configure the existing self-tracing profile to emit developer graph V1 JSON. Extend the existing Java-agent runtime proof to emit the captured decision-record V1 payloads. Import these generated files into the PostgreSQL browser test and capture a screenshot from the real run detail page.

**Rationale:** This exercises the same analyzer, graph exporter, agent, runtime collector, JSON contracts, database schema, HTTP `QUERY` endpoint, ELK layout, and Svelte Flow UI that an integrating application uses. A fixed demonstration graph would not prove this chain.

### Decision 11: Preview graph JSON in browser memory

**Decision:** Add a `/graphs` page that accepts one JSON file of at most 5 MiB. Read and validate the file in the browser with the existing developer graph adapter. Pass only its provenance-free `GraphModel` to the existing `FlowCanvas`. Do not send the file to a server endpoint or store it in browser storage.

**Rationale:** A local preview lets a user inspect a generated graph before database import. Reusing the contract adapter and canvas keeps one validation rule and one visual grammar. Browser-memory handling also keeps developer source metadata out of network requests and durable storage.

### Decision 12: Make explanation layout responsive to usable CSS width

**Decision:** Keep a resizable 320-to-520-pixel explanation inspector visible at 1,024 CSS pixels or more. Below that width, use a repository-owned shadcn-style Sheet built on the installed Bits UI dialog primitive. Put its trigger in the normal header flow. Cap initial graph fit at 0.9 zoom, show only concise edge tokens, omit the minimap for graphs with eight or fewer nodes, and make current-step decoration replace the full-path ring. Use the compact minimap only through 100 nodes. Above 100 nodes, show the node count and direct users to search and fit controls because a compressed overview is not legible.

**Rationale:** The first dogfood screenshot exposed an intermediate-width gap: the inspector moved off-screen, its absolute trigger was absent, long edge outcomes crossed the graph, and additive state borders competed. CSS width, not screenshot pixel count, controls responsive behavior. A lower pinned-inspector breakpoint, an accessible dialog primitive, bounded labels, and exclusive state rings make the explanation dependable without graph-specific layout data.

### Decision 13: Preserve ELK edge routes and normalize both stable graph exports

**Decision:** Return node positions and complete edge sections from the ELK layout worker. Render those orthogonal sections directly, with separate routes for parallel edges. Hide connection handles because the canvas is read-only. Use one exclusive node border for the default, path, or current state. Let the browser-only preview normalize both `fachtracing-developer-graph/v1` and `fachtracing-business-graph/v1` documents to the shared graph model. Keep the PostgreSQL graph catalog restricted to developer graph V1.

**Rationale:** ELK already routes edges around nodes, but the original canvas discarded those sections and recomputed overlapping smooth-step paths. The repository also publishes a stable business-only graph format whose root contains `graphId`; rejecting it made the preview incompatible with a real exporter. The shared presentation model can support both browser inputs without changing durable graph storage.

## Component Design

### Graph Catalog

**Responsibility:** Retrieve and cache supported graph payloads from PostgreSQL and create provenance-free browser graph projections.

**Interface:** `getGraph(graphId, graphVersion): Promise<GraphDocument>` and `listGraphs(): Promise<GraphSummary[]>`.

**Failure behavior:** Reject unsupported schemas, checksum failures, graph/version mismatches, and documents that fail their published JSON Schema. Never send source paths, URLs, origins, or fingerprints to the browser.

### Graph Import Command

**Responsibility:** Import developer graph V1 JSON files into the immutable PostgreSQL catalog before the dashboard uses them.

**Interface:** `npm run import-graphs -- --directory <path>`.

**Failure behavior:** Reject path traversal, files outside the selected directory, invalid JSON Schema documents, conflicting graph bytes, unsupported media types, and partial imports. Re-importing identical bytes is successful.

### Run Import Command

**Responsibility:** Import generated decision-record V1 files for local demonstrations and offline runtime sinks after their exact graphs exist.

**Interface:** `npm run import-runs -- --directory <path>`.

**Failure behavior:** Reject invalid records, absent graph versions, and different bytes for an existing record or execution identity. Import correlations from the unchanged payload in the same transaction.

### Run Repository

**Responsibility:** Read decision summaries and one V1 run payload from PostgreSQL.

**Interface:** `searchRuns(query): Promise<RunPage>` and `getRun(executionId): Promise<RunDocument | null>`.

**Failure behavior:** Use parameterized queries, a 30-second statement timeout, bounded filter lengths, a maximum page size of 50, and generic public errors. Log no credentials, correlation values, payloads, or SQL parameter values.

### Contract Adapters

**Responsibility:** Validate schema identity and convert one graph or run document into immutable UI types.

**Interface:** `parseGraph(unknown): GraphModel` and `parseRun(unknown): RunModel`.

**Failure behavior:** Ignore unknown fields for supported schemas. Reject missing required fields, duplicate IDs, dangling edges, non-monotonic observation sequences, and graph/version mismatch.

### Layout Worker

**Responsibility:** Compute positions and edge routes for one valid graph.

**Interface:** `layout(graph, measuredNodeSizes): Promise<LayoutResult>`.

**Failure behavior:** Cancel obsolete requests. If ELK fails, show the graph error state and keep the decision list and semantic explanation usable; do not fall back to arbitrary positions.

### Flow Canvas

**Responsibility:** Render the laid-out graph and expose selection, pan, zoom, fit, minimap, and keyboard navigation.

**Interface:** Svelte component props for `GraphModel`, `LayoutResult`, and `RunHighlight`.

**Visual behavior:** Cap automatic fit at 0.9 zoom. Show a compact minimap for graphs with 9 through 100 nodes. For larger graphs, show a compact node-count and search-navigation guide. Show short branch tokens for path edges and detailed-zoom edges. Keep the full outcome in the edge tooltip and explanation inspector. Render exact ELK edge routes, keep parallel routes distinct, hide read-only connection handles, and use one primary node border for the active visual state.

### Run Inspector

**Responsibility:** Present observations in order and own the active observation index and full-path toggle.

**Interface:** Svelte component props for `RunModel` and selection callbacks.

**Responsive behavior:** Render in a resizable pinned column at 1,024 CSS pixels or more. Render in a modal Sheet below that width. The same active visit and full-path state drive both surfaces.

### Runs Explorer

**Responsibility:** Show all newest decision summaries, submit arbitrary exact correlation and metadata searches, own cursor navigation, and keep loading, empty, and retry states.

**Interface:** `/runs` page and `/runs/[executionId]` detail route.

### Browser Graph Preview

**Responsibility:** Select or drop one local developer graph V1 or business graph V1 JSON file, validate its name and size, convert it to the provenance-free graph model, and show it on the shared canvas.

**Interface:** `/graphs` page and `parseGraphFile(file): Promise<GraphModel>`.

**Failure behavior:** Reject non-JSON names, empty files, files larger than 5 MiB, invalid JSON, and unsupported graph contracts. Keep the prior valid graph visible only until the user starts another selection. Never send, log, or persist file content.

### Self-Dogfood Proof

**Responsibility:** Generate, import, search, render, and capture Fachtracing's own production-policy graphs and runtime decisions.

**Interface:** `./scripts/verify-viewer-dogfood.sh` for generation and artifact checks; the hosted PostgreSQL browser job for import and screenshot proof.

**Failure behavior:** Fail if the developer graph, decision record, correlation, observed path, or screenshot is absent. The proof reads generated JSON and cannot define graph topology.

## System Flow

1. Before dashboard use, an operator imports graph V1 JSON into the immutable PostgreSQL catalog.
2. The dashboard sends a bounded JSON search document with HTTP `QUERY` and returns at most 50 summaries.
3. Run selection loads the V1 run payload and its exact graph ID/version.
4. The graph catalog loads the matching immutable payload, validates it, and creates a provenance-free browser graph.
5. The layout worker computes top-to-bottom positions from the complete validated graph.
6. The canvas renders the complete graph. The inspector derives and applies current-step or full-path highlighting.
7. As a separate flow, the graph preview reads one selected JSON file into browser memory, validates it through the same adapter, and sends the resulting graph model directly to the canvas.

## State Management

The selected execution ID and non-confidential display settings can use route parameters. Correlation names, correlation values, complete search documents, and selected preview files do not enter URLs or browser persistence. Local Svelte state contains the current search, result page, parsed graph, parsed run, preview graph, layout result, active observation index, panel state, theme, and full-path flag. The graph document and layout are cached by graph ID, graph version, direction, and node-size profile. The run payload is cached by execution ID for the current navigation session. Preview state ends when the page reloads or closes.

## API Changes

### New endpoints

- `GET /api/v1/graphs` returns available graph summaries.
- `GET /api/v1/graphs/{graphId}/versions/{graphVersion}` returns one provenance-free browser graph document.
- `GET /api/v1/correlation-names` returns at most 200 distinct stored correlation names and no values.
- `QUERY /api/v1/runs` accepts a JSON search document and returns decision summaries and the next cursor.
- `GET /api/v1/runs/{executionId}` returns one unchanged `fachtracing-decision-record/v1` payload.

The search document contains optional `executionId`, `graphId`, `status`, `completedFrom`, `completedTo`, `correlation`, `cursor`, and `limit` fields. `correlation` contains one exact `name` and `value`. The endpoint rejects unknown fields and limits the decoded body and every string.

The `QUERY` endpoint requires `Content-Type: application/json`, advertises `Accept-Query: application/json`, and returns `Cache-Control: no-store`. It does not return `Location` or `Content-Location`. All endpoints reject unknown schema versions and return problem details without internal paths or database details.

## SQL Read Contract

- Summary queries select `execution_id`, `graph_id`, `graph_version`, `started_at`, `completed_at`, `status`, and `payload` from at most 50 matching records. The server reads the already-redacted final result from each V1 payload and resolves the business decision label from the graph catalog.
- Detail queries select `payload` by exact `execution_id`.
- Correlation-name discovery selects at most 200 distinct names in lexical order. The UI keeps the combobox editable so the cap does not restrict valid searches.
- Correlation filters join `fachtracing_correlation` by record ID and use an operator-supplied exact `correlation_name`, its exact already-redacted canonical `correlation_value`, and the inclusive completion range.
- Cursor predicates use `completed_at < ? OR (completed_at = ? AND execution_id < ?)` with descending order.
- The storage migration adds `idx_fachtracing_completed_execution` on `(completed_at desc, execution_id desc)` for the newest-decision page.
- The storage migration adds `fachtracing_graph` with `graph_id`, `graph_version`, `schema_id`, `media_type`, `payload`, `sha256`, and `imported_at`; `(graph_id, graph_version)` is the primary key.
- The additive migration records storage schema version 2. This number is a database migration version and is unrelated to the developer graph wire schema, which remains V1 only.
- The graph payload uses PostgreSQL `bytea`. Version one stores UTF-8 JSON bytes with a JSON media type. Payload decoding is selected by media type so that a future specified binary format can use the same catalog.

## Security Considerations

- Data classification: graph labels and decision summaries are Internal. Already-redacted evidence, final results, and correlation values are Confidential.
- Database credentials exist only in server environment variables.
- Server responses set a restrictive content security policy and do not render graph labels as HTML.
- Search request bodies have a small byte limit. Graph ID, execution ID, correlation, cursor, and filter fields have individual length and character limits before database access. Correlation-name discovery never returns correlation values.
- Request and application logs exclude `QUERY` content, run payloads, graph developer provenance, and response bodies.
- The preview has no upload endpoint. Its file input and parser run in the browser, and the rendered graph model excludes developer provenance.
- The server binds to loopback. Shared and public deployment is unsupported in the proof of concept.

## Performance Considerations

- Use one ELK worker and discard stale layout replies by request token.
- Use the ELK layered `DOWN` direction and north/south ports by default.
- Apply three semantic zoom levels: overview symbols, readable labels, and full node detail.
- Hide edge labels unless the edge is selected, highlighted, or sufficiently zoomed.
- Render the complete topology. Enable viewport-only rendering only after a benchmark proves a net benefit and accessibility tests pass.
- Cache validated graph documents and layout results by graph/version and file modification time.
- Keep run pages at 50 rows and return summary columns only.

## Testing Strategy

- Unit tests map every EARS criterion to contract parsing, highlight derivation, cursor encoding, and filter validation.
- Component tests cover repeated visits, current-step selection, full-path mode, mismatch errors, empty states, and keyboard navigation.
- Playwright tests cover the list-to-run workflow, responsive inspector, deep links, themes, semantic zoom, and accessibility checks.
- Layout assertions cover inspector and trigger visibility, header height, bounded edge labels, exclusive run-state rings, one-based display order, compact minimap behavior, Sheet focus and Escape close, and screenshots at 1,440, 1,024, and 390 CSS pixels.
- Visual tests compare approved desktop and narrow screenshots for both themes and all node states.
- PostgreSQL integration tests verify the additive migration, immutable graph conflicts, exact graph retrieval, parameterized search, stable pagination, correlation semantics, and timeouts with bounded generated fixtures.
- A layout benchmark uses generated topology, not a hardcoded product graph, at 250 nodes and 400 edges.
- Browser tests select a generated graph JSON file and prove that the shared canvas renders it without PostgreSQL or a fixed topology.
- Browser tests load a checked-in business graph V1 conformance fixture and prove that it renders without a contract error.
- Browser geometry tests sample rendered SVG paths and prove that routes do not enter unrelated node rectangles, parallel edges remain distinct, labels do not cover nodes or each other, and connection handles are not visible.

## Rollout Plan

1. Ship the viewer as an opt-in local POC with documented environment variables, graph import, and loopback binding.
2. Import generated developer graph V1 documents and validate them with synthetic V1 run payloads.
3. Run the PostgreSQL contract and browser suite in CI.
4. Review one Mega or Keycloak graph only as black-box conformance after generic tests pass.

## Risks & Mitigations

- **Risk:** Svelte Flow API changes affect the viewer. **Mitigation:** Isolate it behind `FlowCanvas.svelte`, pin the lockfile, and exercise browser behavior.
- **Risk:** Layout work blocks the page on an unexpectedly large graph. **Mitigation:** Run ELK in a worker, cancel stale results, keep node search available, and measure the 250-node safety profile before adding more architecture.
- **Risk:** Direct SQL drifts from the Java migration. **Mitigation:** Test against `JdbcDecisionRecordRepository.migrate()` and keep all SQL in one server-only repository.
- **Risk:** A run references a graph that was never imported. **Mitigation:** Keep the run visible, show a compatibility error, disable highlighting, and provide the exact graph ID and version to the operator.
- **Risk:** An operator knows only a raw value that differs from the stored canonical correlation. **Mitigation:** The POC does not guess or reproduce application-specific transformations. The deployment must expose the stored canonical value, record a separate lookup-safe correlation, or specify an adapter later.
- **Risk:** Historical specifications describe separate developer graph V1 and V2 shapes. **Mitigation:** Use the current exporter, generated JSON Schema, tests, and commit `b26b198` as authoritative. The current V1 identifier contains the merged multi-source fields, and the build removes stale V2 schema files.

## Dependencies & Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| unify-developer-graph-contract | Supplies the sole current multi-source V1 graph contract. | Yes | completed |
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
| `@lucide/svelte` | 1.33.0 | Node.js | Approved | Maintained Lucide Svelte package for consistent node-type and application-action SVG icons. It replaces the deprecated `lucide-svelte` package. |
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
