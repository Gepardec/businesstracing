# Implementation Tasks: Interactive Flow and Run Explorer

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| unify-developer-graph-contract | Current multi-source V1 graph input contract | Yes | completed |
| generic-application-readiness | Run, storage, and SQL input contract | Yes | completed |

## Task Breakdown

### Task 1: Scaffold the Viewer and Contract Types

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Create the SvelteKit Node application, install only approved dependencies, initialize Tailwind CSS v4 and shadcn-svelte, add strict TypeScript settings, and implement immutable graph and run adapters from existing schema fixtures.

**Implementation Steps:**

1. Create `fachtracing-viewer/` with SvelteKit, Svelte 5, the Node adapter, and a committed lockfile.
2. Initialize Tailwind CSS v4 and shadcn-svelte with the `new-york` style, neutral base, Svelte 5 components, and explicit CSS tokens.
3. Copy the generated developer graph V1 schema and synthetic graph and run documents into test fixtures without changing their wire shape.
4. Implement graph and run parsers with explicit version dispatch and integrity checks.
5. Add unit tests for supported, forward-compatible, invalid, and mismatched documents.

**Acceptance Criteria:**

- [x] The viewer builds with strict TypeScript checks.
- [x] The application shell uses repository-owned shadcn-svelte components and Tailwind CSS v4 tokens.
- [x] Developer graph V1 and decision-record V1 adapters preserve all node, edge, observation, and selected-edge IDs.
- [x] Unsupported versions, dangling references, duplicate IDs, and graph mismatches fail visibly.

**Files to Modify:**

- `fachtracing-viewer/package.json` (new)
- `fachtracing-viewer/package-lock.json` (new)
- `fachtracing-viewer/components.json` (new)
- `fachtracing-viewer/src/app.css` (new)
- `fachtracing-viewer/src/lib/components/ui/` (new)
- `fachtracing-viewer/src/lib/contracts/graph-contract.ts` (new)
- `fachtracing-viewer/src/lib/contracts/run-contract.ts` (new)
- `fachtracing-viewer/src/lib/contracts/contracts.test.ts` (new)

**Tests Required:**

- [x] Contract unit tests
- [x] Type check

---

### Task 2: Add the Graph Catalog and Decision API

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add the immutable PostgreSQL graph catalog, its separate import command, the decision repository, and the versioned dashboard endpoints.

**Implementation Steps:**

1. Add the graph catalog table and completion/execution cursor index through the repository storage migration.
2. Implement the explicit graph import command with JSON Schema validation, unchanged payload bytes, checksums, and immutable conflict behavior.
3. Implement exact graph retrieval and the provenance-free browser projection.
4. Implement bounded, parameterized correlation-name discovery, run detail, and cursor-search queries.
5. Implement `QUERY /api/v1/runs` through the SvelteKit fallback method handler with JSON content, `Accept-Query`, and `no-store` responses.
6. Add the remaining SvelteKit server routes and generic problem responses.
7. Verify the API and migration against the PostgreSQL fixture.

**Acceptance Criteria:**

- [x] The import command stores one exact graph version immutably and rejects conflicting bytes.
- [x] The API returns graph summaries, one provenance-free exact graph version, up to 200 distinct correlation names without values, decision summaries with final results, and one unchanged V1 run payload.
- [x] Arbitrary exact correlation search uses HTTP `QUERY`; confidential search fields never enter the URI or logs.
- [x] Cursor pages are stable and correlation/time semantics match the JDBC repository.
- [x] Invalid input, timeout, missing data, and unavailable graph states expose no internal details.

**Files to Modify:**

- `fachtracing-storage-jdbc/src/main/java/at/gepardec/fachtracing/storage/jdbc/JdbcDecisionRecordRepository.java`
- `fachtracing-viewer/src/lib/server/graph-catalog-repository.server.ts` (new)
- `fachtracing-viewer/src/lib/server/graph-import.server.ts` (new)
- `fachtracing-viewer/src/cli/import-graphs.ts` (new)
- `fachtracing-viewer/src/lib/server/run-repository.server.ts` (new)
- `fachtracing-viewer/src/routes/api/v1/` (new)
- `fachtracing-viewer/tests/postgres/` (new)

**Tests Required:**

- [x] Server route tests
- [x] Immutable graph catalog and migration integration tests
- [x] HTTP `QUERY`, confidentiality, and cursor pagination tests

---

### Task 3: Build the Data-Driven Graph Canvas

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Render the complete node and edge grammar with Svelte Flow and compute deterministic top-to-bottom positions with ELK in a worker.

**Implementation Steps:**

1. Implement the top-to-bottom layout worker with stable sorting, measured sizes, north/south ports, and spacing.
2. Implement one custom node component and one custom edge component with the documented kind and state variants.
3. Implement semantic zoom without changing graph topology.
4. Add pan, zoom, fit, node search, minimap, keyboard selection, loading, and layout-error states.
5. Add generated-topology layout, monochrome recognition, component, and visual tests.

**Acceptance Criteria:**

- [x] All contract nodes and edges render from data with no graph-specific positions.
- [x] Layout is deterministic and completes within 2 seconds for 250 nodes and 400 edges.
- [x] Every node kind is distinguishable by silhouette, icon, text, and token in light, dark, and monochrome checks.
- [x] Coverage gaps and invalid references never appear as invented normal flow.
- [x] Keyboard and focus behavior meet the accessibility criteria.
- [x] A semantic node list exposes the graph without requiring canvas operation.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/layout-definition.ts` (new)
- `fachtracing-viewer/src/lib/graph/FlowCanvas.svelte` (new)
- `fachtracing-viewer/src/lib/graph/BusinessNode.svelte` (new)
- `fachtracing-viewer/src/lib/graph/BusinessEdge.svelte` (new)
- `fachtracing-viewer/src/lib/graph/graph.test.ts` (new)

**Tests Required:**

- [x] Layout benchmark and determinism tests
- [x] Canvas component and keyboard tests
- [x] Semantic node-list and screen-reader structure tests
- [x] Node-state and 250-node safety-profile visual tests

---

### Task 4: Add the Run Inspector and Highlighting

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Tasks 1 and 3
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add the right-side ordered explanation inspector, current-step navigation, graph focus, and optional full-path highlighting.

**Implementation Steps:**

1. Derive highlight state from ordered observations and graph topology.
2. Build plain-language step explanations from the node label, recorded outcome, selected edge, and already-redacted display evidence.
3. Implement the inspector, active step, next/previous actions, missing-evidence state, and full-path toggle.
4. Connect list selection to canvas focus without unwanted zoom resets.
5. Test repeated visits, selected edges, inferred connecting edges, absent evidence, failures, and mismatches.

**Acceptance Criteria:**

- [x] Every observation appears once and in sequence order.
- [x] Every step explains only recorded facts and states when no additional evidence exists.
- [x] Step selection focuses and highlights the correct node and selected edge within 100 milliseconds.
- [x] Full-path mode highlights all proved nodes and connecting edges.
- [x] Repeated nodes remain distinct steps and graph/version mismatch disables highlights.

**Files to Modify:**

- `fachtracing-viewer/src/lib/runs/run-highlight.ts` (new)
- `fachtracing-viewer/src/lib/runs/RunInspector.svelte` (new)
- `fachtracing-viewer/src/routes/runs/[executionId]/+page.svelte` (new)
- `fachtracing-viewer/src/lib/runs/run-highlight.test.ts` (new)

**Tests Required:**

- [x] Highlight derivation tests
- [x] Inspector component tests

---

### Task 5: Add the Decisions Dashboard

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Tasks 2 and 4
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Build the shadcn-svelte decisions dashboard, confidential generic correlation lookup, paged result list, deep-link detail navigation, themes, and responsive explanation layout.

**Implementation Steps:**

1. Compose the navigation, toolbar, cards, forms, table, badges, tooltips, and alerts from repository-owned shadcn-svelte components.
2. Show newest decision summaries with label, status, completion time, and already-redacted final result.
3. Add an editable stored-correlation-name combobox and bounded execution, graph, status, time, and arbitrary exact correlation-name and correlation-value filters.
4. Submit searches with HTTP `QUERY` and retain confidential inputs only in component memory.
5. Add cursor navigation, loading, empty, error, and retry states.
6. Link each row to the exact run detail URL without copying correlation filters into it.
7. Add token-based light and dark themes.
8. Move the inspector into an accessible sheet below 1,200 pixels.

**Acceptance Criteria:**

- [x] Filters execute on the server and reset the cursor.
- [x] The correlation-name control offers stored names, accepts another valid name, and never enumerates values.
- [x] One arbitrary exact correlation lookup shows every matching decision page without exposing its name or value in a URL or log.
- [x] Result order and cursor navigation stay stable as new runs arrive.
- [x] Run detail URLs are linkable and restore the selected run.
- [x] Desktop and narrow layouts preserve the selected step and keyboard access.
- [x] Light and dark themes preserve contrast, node-state meaning, and layout.

**Files to Modify:**

- `fachtracing-viewer/src/routes/runs/+page.server.ts` (new)
- `fachtracing-viewer/src/routes/runs/+page.svelte` (new)
- `fachtracing-viewer/src/lib/runs/RunFilters.svelte` (new)
- `fachtracing-viewer/src/lib/runs/RunList.svelte` (new)
- `fachtracing-viewer/src/lib/layout/AppShell.svelte` (new)

**Tests Required:**

- [x] Filter and pagination component tests
- [x] Responsive layout tests

---

### Task 6: Integrate Verification, Documentation, and CI

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Tasks 2, 3, 4, and 5
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:**
Add browser workflows, dependency audit, repository verification, and deployment documentation.

**Implementation Steps:**

1. Add Playwright flows for the generic correlation-to-explanation journey: newest decisions, arbitrary correlation lookup, multiple results, decision selection, evidence explanation, step navigation, full-path mode, semantic zoom, themes, deep links, and narrow layout.
2. Add `npm audit --audit-level=high`, checks, unit tests, build, and browser tests to the repository verifier and CI.
3. Document graph import, PostgreSQL variables, loopback-only binding, correlation lookup policy, backup and retention, and compatibility limits.
4. Run the full Java, PostgreSQL, frontend, and repository-integrity gates.
5. Generate Fachtracing's own developer graphs and Java-agent decision records, import them into the hosted PostgreSQL test, open a real run, and capture browser proof.

**Acceptance Criteria:**

- [x] All EARS criteria have automated evidence or an explicit manual accessibility review item.
- [x] High and critical npm audit findings block CI.
- [x] The viewer cannot introduce hardcoded graph positions or committed generated product diagrams.
- [x] Approved visual baselines cover all node kinds, states, themes, responsive sizes, and the 250-node safety profile.
- [x] The full repository gate and hosted CI pass.
- [x] Fachtracing's generated developer graph and actual Java-agent path render in the viewer without a fixed test topology.
- [x] Hosted browser proof is available as a downloadable CI artifact.

**Files to Modify:**

- `fachtracing-viewer/e2e/` (new)
- `fachtracing-viewer/README.md` (new)
- `scripts/verify-viewer.sh` (new)
- `scripts/verify-pr.sh`
- `.github/workflows/` (existing workflow files as required)
- `README.md`
- `scripts/verify-self-tracing.sh`
- `scripts/verify-viewer-dogfood.sh` (new)
- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/SelfTracingRuntimeTest.java`
- `fachtracing-viewer/src/cli/import-runs.ts` (new)
- `fachtracing-viewer/src/lib/server/run-import-repository.server.ts` (new)
- `fachtracing-viewer/src/lib/server/run-import.server.ts` (new)

**Tests Required:**

- [x] Playwright end-to-end suite
- [x] Dependency audit
- [x] Full repository verification
- [x] Generated self-dogfood PostgreSQL browser journey

---

### Task 7: Add Browser-Only Graph JSON Preview

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Tasks 1 and 3
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:**
Add a local file preview that validates one current developer graph V1 JSON document in browser memory and renders it with the shared graph canvas.

**Implementation Steps:**

1. Add a small file adapter for JSON name, empty-file, and 5 MiB size checks.
2. Add an accessible file selection and drag-and-drop component.
3. Add `/graphs` with transient state, clear validation errors, graph summary data, and the shared canvas.
4. Add the graph preview to the application navigation and user documentation.
5. Add unit and browser proof with a generated graph artifact and no hardcoded topology.

**Acceptance Criteria:**

- [x] A valid merged developer graph V1 JSON file renders without PostgreSQL.
- [x] File content remains in browser memory and is not sent or persisted.
- [x] Invalid names, sizes, JSON, and contracts produce clear messages and no partial render.
- [x] The preview uses the existing top-to-bottom layout, node grammar, controls, and semantic node list.
- [x] The browser proof selects generated graph JSON and makes no assumption about node IDs or positions.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/graph-file.ts` (new)
- `fachtracing-viewer/src/lib/graph/GraphUpload.svelte` (new)
- `fachtracing-viewer/src/routes/graphs/+page.svelte` (new)
- `fachtracing-viewer/src/lib/layout/AppShell.svelte`
- `fachtracing-viewer/e2e/decision-explorer.spec.ts`
- `fachtracing-viewer/README.md`

**Tests Required:**

- [x] File adapter unit tests
- [x] Generated-file browser preview test

---

### Task 8: Correct and Approve the Decision Visual Layout

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Tasks 3, 4, 5, and 6
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Correct the dogfood screenshot defects and replace weak visual assertions with viewport-specific layout evidence.

**Implementation Steps:**

1. Refactor the decision header so its summary and explanation action do not overlap or leave the normal layout flow.
2. Keep a resizable inspector visible from 1,024 CSS pixels and add a Bits UI shadcn-style Sheet below that width.
3. Bound edge labels, make run-state rings exclusive, use one-based display order, cap fit zoom, and reduce minimap size and scope.
4. Add layout assertions and dogfood screenshots for desktop, intermediate, narrow, and dark views.
5. Inspect every generated image and repeat the correction cycle until the visual gate has no open defect.

**Acceptance Criteria:**

- [x] The inspector or its visible Sheet trigger is available at every supported width.
- [x] Desktop and intermediate headers are compact and preserve access to full values.
- [x] No edge label covers a node or shows an unbounded compound outcome.
- [x] Current-step, path, node-kind, and dimmed states remain clear without stacked run rings.
- [x] Step numbers are one-based in the UI without changing stored sequences.
- [x] The Sheet has modal focus, Escape close, outside-click close, and focus return.
- [x] Generated screenshots at 1,440, 1,024, and 390 CSS pixels pass manual inspection in light and dark themes.

**Files to Modify:**

- `fachtracing-viewer/src/routes/runs/[executionId]/+page.svelte`
- `fachtracing-viewer/src/lib/components/ui/Sheet.svelte` (new)
- `fachtracing-viewer/src/lib/runs/InspectorResizer.svelte` (new)
- `fachtracing-viewer/src/lib/runs/RunInspector.svelte`
- `fachtracing-viewer/src/lib/graph/FlowCanvas.svelte`
- `fachtracing-viewer/src/lib/graph/BusinessNode.svelte`
- `fachtracing-viewer/src/lib/graph/BusinessEdge.svelte`
- `fachtracing-viewer/e2e/decision-explorer.spec.ts`

**Tests Required:**

- [x] Visual-state unit tests
- [x] Viewport layout and Sheet accessibility tests
- [x] Generated dogfood screenshot review

---

### Task 9: Repair Graph Compatibility, Routing, and Visual Quality

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Tasks 3, 7, and 8
**Priority:** Critical
**IssueID:** None
**Blocker:** None

**Description:**
Repair the contract and rendering defects found during user acceptance. Prove the result with real exported graph documents and geometry-based browser checks.

**Implementation Steps:**

1. Normalize developer graph V1 and business graph V1 JSON files in the browser-only preview.
2. Return ELK edge sections from the layout worker and render them without recomputing paths.
3. Keep parallel routes distinct, prevent edge crossings through unrelated nodes, hide read-only handles, and simplify node state borders.
4. Add unit and browser tests for real business fixtures, branch geometry, labels, themes, responsive widths, and the 250-node profile.
5. Inspect all generated proof images and repeat the correction cycle until no open visual defect remains.

**Acceptance Criteria:**

- [x] A real business graph V1 fixture and a real developer graph V1 artifact render in the local preview.
- [x] Rendered edge routes use ELK geometry and do not enter unrelated node rectangles.
- [x] Parallel edges have distinct visible paths and readable labels.
- [x] Read-only connection handles are not visible and node states do not stack borders.
- [x] Generated light, dark, focused, responsive, and 250-node evidence passes manual inspection.
- [x] Unit, browser, repository, and hosted CI gates pass.

**Files to Modify:**

- `fachtracing-viewer/src/lib/contracts/graph-contract.ts`
- `fachtracing-viewer/src/lib/graph/layout-definition.ts`
- `fachtracing-viewer/src/lib/graph/edge-route.ts` (new)
- `fachtracing-viewer/src/lib/graph/BusinessEdge.svelte`
- `fachtracing-viewer/src/lib/graph/BusinessNode.svelte`
- `fachtracing-viewer/src/lib/graph/FlowCanvas.svelte`
- `fachtracing-viewer/src/lib/graph/graph-file.ts`
- `fachtracing-viewer/e2e/decision-explorer.spec.ts`

**Tests Required:**

- [x] Contract and route geometry unit tests
- [x] Real fixture and generated branching browser tests
- [x] Generated visual proof review

## Implementation Order

1. Task 1 establishes the contracts and build.
2. Tasks 2 and 3 add the server data path and graph canvas.
3. Task 4 adds run navigation and highlighting.
4. Task 5 adds the complete runs workflow.
5. Task 6 adds final proof and delivery documentation.
6. Task 7 adds the optional browser-only graph preview.
7. Task 8 corrects and approves the complete decision visual baseline.
8. Task 9 repairs user-acceptance defects and completes the renewed visual approval.

## Progress Tracking

- Total Tasks: 9
- Completed: 9
- In Progress: 0
- Blocked: 0
- Pending: 0
