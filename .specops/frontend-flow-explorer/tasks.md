# Implementation Tasks: Interactive Flow and Run Explorer

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| developer-graph-json-schema | Graph input contract | Yes | completed |
| generic-application-readiness | Run, storage, and SQL input contract | Yes | completed |

## Task Breakdown

### Task 1: Scaffold the Viewer and Contract Types

**Status:** Pending
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

- [ ] The viewer builds with strict TypeScript checks.
- [ ] The application shell uses repository-owned shadcn-svelte components and Tailwind CSS v4 tokens.
- [ ] Developer graph V1 and decision-record V1 adapters preserve all node, edge, observation, and selected-edge IDs.
- [ ] Unsupported versions, dangling references, duplicate IDs, and graph mismatches fail visibly.

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

- [ ] Contract unit tests
- [ ] Type check

---

### Task 2: Add the Graph Catalog and Decision API

**Status:** Pending
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
4. Implement bounded, parameterized run detail and cursor-search queries.
5. Implement `QUERY /api/v1/runs` through the SvelteKit fallback method handler with JSON content, `Accept-Query`, and `no-store` responses.
6. Add the remaining SvelteKit server routes and generic problem responses.
7. Verify the API and migration against the PostgreSQL fixture.

**Acceptance Criteria:**

- [ ] The import command stores one exact graph version immutably and rejects conflicting bytes.
- [ ] The API returns graph summaries, one provenance-free exact graph version, decision summaries with final results, and one unchanged V1 run payload.
- [ ] Customer correlation search uses HTTP `QUERY`; confidential search fields never enter the URI or logs.
- [ ] Cursor pages are stable and correlation/time semantics match the JDBC repository.
- [ ] Invalid input, timeout, missing data, and unavailable graph states expose no internal details.

**Files to Modify:**

- `fachtracing-storage-jdbc/src/main/java/at/gepardec/fachtracing/storage/jdbc/JdbcDecisionRecordRepository.java`
- `fachtracing-viewer/src/lib/server/graph-catalog-repository.server.ts` (new)
- `fachtracing-viewer/src/lib/server/graph-import.server.ts` (new)
- `fachtracing-viewer/src/cli/import-graphs.ts` (new)
- `fachtracing-viewer/src/lib/server/run-repository.server.ts` (new)
- `fachtracing-viewer/src/routes/api/v1/` (new)
- `fachtracing-viewer/tests/postgres/` (new)

**Tests Required:**

- [ ] Server route tests
- [ ] Immutable graph catalog and migration integration tests
- [ ] HTTP `QUERY`, confidentiality, and cursor pagination tests

---

### Task 3: Build the Data-Driven Graph Canvas

**Status:** Pending
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

- [ ] All contract nodes and edges render from data with no graph-specific positions.
- [ ] Layout is deterministic and completes within 2 seconds for 250 nodes and 400 edges.
- [ ] Every node kind is distinguishable by silhouette, icon, text, and token in light, dark, and monochrome checks.
- [ ] Coverage gaps and invalid references never appear as invented normal flow.
- [ ] Keyboard and focus behavior meet the accessibility criteria.
- [ ] A semantic node list exposes the graph without requiring canvas operation.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/layout-worker.ts` (new)
- `fachtracing-viewer/src/lib/graph/FlowCanvas.svelte` (new)
- `fachtracing-viewer/src/lib/graph/BusinessNode.svelte` (new)
- `fachtracing-viewer/src/lib/graph/BusinessEdge.svelte` (new)
- `fachtracing-viewer/src/lib/graph/graph.test.ts` (new)

**Tests Required:**

- [ ] Layout benchmark and determinism tests
- [ ] Canvas component and keyboard tests
- [ ] Semantic node-list and screen-reader structure tests
- [ ] Node-state and 250-node safety-profile visual tests

---

### Task 4: Add the Run Inspector and Highlighting

**Status:** Pending
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

- [ ] Every observation appears once and in sequence order.
- [ ] Every step explains only recorded facts and states when no additional evidence exists.
- [ ] Step selection focuses and highlights the correct node and selected edge within 100 milliseconds.
- [ ] Full-path mode highlights all proved nodes and connecting edges.
- [ ] Repeated nodes remain distinct steps and graph/version mismatch disables highlights.

**Files to Modify:**

- `fachtracing-viewer/src/lib/runs/run-highlight.ts` (new)
- `fachtracing-viewer/src/lib/runs/RunInspector.svelte` (new)
- `fachtracing-viewer/src/routes/runs/[executionId]/+page.svelte` (new)
- `fachtracing-viewer/src/lib/runs/run-highlight.test.ts` (new)

**Tests Required:**

- [ ] Highlight derivation tests
- [ ] Inspector component tests

---

### Task 5: Add the Decisions Dashboard

**Status:** Pending
**Estimated Effort:** M
**Dependencies:** Tasks 2 and 4
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Build the shadcn-svelte decisions dashboard, confidential customer lookup, paged result list, deep-link detail navigation, themes, and responsive explanation layout.

**Implementation Steps:**

1. Compose the navigation, toolbar, cards, forms, table, badges, tooltips, and alerts from repository-owned shadcn-svelte components.
2. Show newest decision summaries with label, status, completion time, and already-redacted final result.
3. Add bounded execution, graph, status, time, and exact customer correlation filters.
4. Submit searches with HTTP `QUERY` and retain confidential inputs only in component memory.
5. Add cursor navigation, loading, empty, error, and retry states.
6. Link each row to the exact run detail URL without copying the customer lookup into it.
7. Add token-based light and dark themes.
8. Move the inspector into an accessible sheet below 1,200 pixels.

**Acceptance Criteria:**

- [ ] Filters execute on the server and reset the cursor.
- [ ] One customer lookup shows every matching decision page without exposing the lookup value in a URL or log.
- [ ] Result order and cursor navigation stay stable as new runs arrive.
- [ ] Run detail URLs are linkable and restore the selected run.
- [ ] Desktop and narrow layouts preserve the selected step and keyboard access.
- [ ] Light and dark themes preserve contrast, node-state meaning, and layout.

**Files to Modify:**

- `fachtracing-viewer/src/routes/runs/+page.server.ts` (new)
- `fachtracing-viewer/src/routes/runs/+page.svelte` (new)
- `fachtracing-viewer/src/lib/runs/RunFilters.svelte` (new)
- `fachtracing-viewer/src/lib/runs/RunList.svelte` (new)
- `fachtracing-viewer/src/lib/layout/AppShell.svelte` (new)

**Tests Required:**

- [ ] Filter and pagination component tests
- [ ] Responsive layout tests

---

### Task 6: Integrate Verification, Documentation, and CI

**Status:** Pending
**Estimated Effort:** M
**Dependencies:** Tasks 2, 3, 4, and 5
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:**
Add browser workflows, dependency audit, repository verification, and deployment documentation.

**Implementation Steps:**

1. Add Playwright flows for the customer-support journey: newest decisions, customer lookup, multiple results, decision selection, evidence explanation, step navigation, full-path mode, semantic zoom, themes, deep links, and narrow layout.
2. Add `npm audit --audit-level=high`, checks, unit tests, build, and browser tests to the repository verifier and CI.
3. Document graph import, PostgreSQL variables, loopback-only binding, correlation lookup policy, backup and retention, and compatibility limits.
4. Run the full Java, PostgreSQL, frontend, and repository-integrity gates.

**Acceptance Criteria:**

- [ ] All EARS criteria have automated evidence or an explicit manual accessibility review item.
- [ ] High and critical npm audit findings block CI.
- [ ] The viewer cannot introduce hardcoded graph positions or committed generated product diagrams.
- [ ] Approved visual baselines cover all node kinds, states, themes, responsive sizes, and the 250-node safety profile.
- [ ] The full repository gate and hosted CI pass.

**Files to Modify:**

- `fachtracing-viewer/e2e/` (new)
- `fachtracing-viewer/README.md` (new)
- `scripts/verify-viewer.sh` (new)
- `scripts/verify-pr.sh`
- `.github/workflows/` (existing workflow files as required)
- `README.md`

**Tests Required:**

- [ ] Playwright end-to-end suite
- [ ] Dependency audit
- [ ] Full repository verification

## Implementation Order

1. Task 1 establishes the contracts and build.
2. Tasks 2 and 3 add the server data path and graph canvas.
3. Task 4 adds run navigation and highlighting.
4. Task 5 adds the complete runs workflow.
5. Task 6 adds final proof and delivery documentation.

## Progress Tracking

- Total Tasks: 6
- Completed: 0
- In Progress: 0
- Blocked: 0
- Pending: 6
