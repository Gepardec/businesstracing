# Implementation Journal: Interactive Flow and Run Explorer

## Summary

## Phase 1 Context Summary

- Config: SpecOps defaults; full-stack vertical; `.specops` specification directory; no external task tracker; review not required; evaluation enabled with a 7/10 threshold and two iterations.
- Context recovery: Two unrelated specs are implementing: `generic-call-specific-business-flow` and `release-gate-timeout-budget`. This spec does not modify their current files.
- Steering files: Loaded `dependencies.md`, `product.md`, `repo-map.md`, `structure.md`, and `tech.md`.
- Repo map: Stale because the tracked file-list hash changed; refreshed during this specification run.
- Memory: Loaded 118 decisions, 18 recurring decision categories, and existing file-overlap patterns. No production learnings file exists.
- Vertical: Full-stack. The request adds a Svelte frontend, server-side read API, graph artifact access, and PostgreSQL search.
- Affected files: New `fachtracing-viewer/` application, root verification and CI scripts, root documentation, and existing JDBC migration fixtures for read-contract tests.
- Project state: Brownfield Java 21 Maven multi-module repository with no current frontend.
- Scope assessment: Three user-facing capabilities and three code domains are present. SpecOps recommends graph display, run detail, and run search as separable specs. This non-delegated run keeps one spec because the first useful release requires their shared contracts and one end-to-end stored-run path.
- Primary constraint: Prove compatibility with existing graph IDs, graph versions, node IDs, selected edge IDs, V1 payloads, and PostgreSQL semantics before UI polish.
- Plan validation: Pass. Existing contract and documentation paths resolve. Every frontend path is marked as new.
- Coherence check: Pass. The layout, interaction, and query limits refer to distinct measured operations and do not conflict.
- Dependency introduction gate: Eight Node.js dependencies are approved in `design.md`. The application lockfile and npm audit remain Phase 3 gates.
- Vocabulary check: Not required for the full-stack vertical.
- Session 8 context: The existing self-tracing profile generates exact and business graphs and executes five real paths through the Java agent. The viewer proof must add only generated developer JSON, decision-record JSON, PostgreSQL import, and browser evidence.

## Phase 2 Completion Summary

- Requirements: One interactive graph, a right-side ordered run inspector, current-step and full-path highlighting, a searchable cursor-paged runs view, and strict compatibility with current JSON and SQL contracts.
- Design: A standalone SvelteKit Node application uses narrow server adapters, Svelte Flow, an ELK layout worker, ID-derived highlights, and PostgreSQL cursor queries.
- Tasks: Six ordered tasks cover contracts, read APIs, graph layout, run inspection, run search, and delivery verification.
- Dependencies: SvelteKit, Svelte Flow, ELK, PostgreSQL, and test tools are explicit approved dependencies. Existing completed JSON and JDBC specs are required.
- Initial open review points were PostgreSQL-only scope, reverse-proxy authentication, and exact-only correlation search. Session 4 resolved these as PostgreSQL-only, loopback-only, and exact-only for the POC.

## Phase 2 Revision Summary (scale decisions superseded by Session 3)

- The visual design is now explicit in `visual-design.md`.
- The default ELK direction changed from left-to-right to top-to-bottom.
- The application shell now uses shadcn-svelte in the `new-york` style with Tailwind CSS v4.
- Every node kind has a stable silhouette, icon, label, and semantic color token.
- Large graphs use measured full, large, and run-focus modes. Partial views are always visible to the user.
- Visual regression, monochrome recognition, theme, and large-graph tests are now delivery gates.

## Phase 2 Scale Revision Summary

- Repository inspection found checked-in graph fixtures with 7, 8, and 15 nodes.
- Version one now renders the complete graph with one interaction model.
- The 250-node and 400-edge generated graph remains a safety benchmark, not a UI threshold.
- The unproved 1,000-node mode, partial run projection, and graph view model are deferred.

## Phase 2 Product Workflow Revision Summary

- The POC is a generic local internal decision-support dashboard.
- The default page lists newest decisions. An arbitrary exact correlation-name and canonical-value lookup returns all matching decisions.
- Decision detail shows the final result, complete business graph, highlighted path, and ordered evidence-based explanation.
- Developer graph V1 is the only graph wire contract. It is the merged multi-source shape. V2 is only a stale filename cleanup target.
- HTTP `QUERY` carries confidential search content outside the URI.
- An immutable PostgreSQL graph catalog retains exact graph versions. The POC imports JSON bytes and leaves a media-type seam for a future specified binary format.
- Production-scale benchmarking is removed. Bounded functional and integration fixtures remain.
- Developer provenance stays server-side, and the POC binds to loopback.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Use top-to-bottom layout in version one. | It follows normal page scrolling and keeps the right inspector outside the main flow direction. | 3 | 2026-08-19T11:58:25Z |
| 2 | Use shadcn-svelte with Tailwind CSS v4. | It supplies consistent accessible controls while keeping component source and theme tokens in the repository. | 1, 5 | 2026-08-19T11:58:25Z |
| 3 | Use progressive modes above 250 and 1,000 nodes. | A fixed detail level becomes unreadable on large graphs. Explicit focus mode prevents silent data loss. | 3 | 2026-08-19T11:58:25Z |
| 4 | Supersede decision 3 and render the complete graph in version one. | Current checked-in graphs have at most 15 nodes. The 250-node benchmark gives enough headroom without a second topology model. | 3 | 2026-08-19T12:15:33Z |
| 5 | Use HTTP `QUERY` for decision search. | RFC 10008 gives safe, idempotent request-content semantics and keeps confidential correlation values out of URIs. | 2, 5 | 2026-08-19T13:06:48Z |
| 6 | Store immutable graph V1 bytes in PostgreSQL. | Historical decisions require their exact graph version. Co-located retention avoids an unsynchronized deployment directory. | 2 | 2026-08-19T13:06:48Z |
| 7 | Make correlation-to-decision explanation the primary workflow. | Support users need to find all decisions for a known business reference and explain the recorded branch evidence, not browse graphs in isolation. | 4, 5 | 2026-08-19T13:06:48Z |
| 8 | Keep the POC local, PostgreSQL-only, exact-search-only, and source-metadata-free. | This is the smallest safe scope for confidential records and the existing indexed correlation contract. | 2, 5, 6 | 2026-08-19T13:06:48Z |
| 9 | Prove the viewer with the existing self-tracing analyzer and Java-agent path. | Generated self-observation proves the complete product chain and avoids a hand-written demonstration diagram. | 6 | 2026-08-19T19:37:37Z |
| 10 | Preview one graph JSON file in browser memory. | It gives a fast inspection path without a database import, network upload, persistent storage, or a second graph renderer. | 7 | 2026-08-19T20:40:30Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
| --- | --- | --- | --- |
| PostgreSQL rejected the caught duplicate schema-version insert because the transaction stayed aborted. | Replaced exception-based idempotency with PostgreSQL `ON CONFLICT` and H2 `MERGE`. | One focused fix; the repeatable PostgreSQL migration then passed. | 2, 6 |
| Downloading Chromium exceeded the fixed three-minute PostgreSQL job budget. | Configured Playwright to use Chrome already installed on the GitHub Ubuntu runner. | Removed the download without reducing browser coverage; the job completed in 1 minute 45 seconds. | 6 |

## Session Log

### Session 1 — Specification created (2026-08-19)

Created a full-stack draft from the current graph JSON, decision-record V1, and JDBC schema. No implementation task started. The user review can change the three open deployment and search assumptions before Phase 3.

### Session 2 — Visual design revised (2026-08-19)

Added the missing visual language and large-graph contract after user review. The revision specifies top-to-bottom layout, shadcn-svelte and Tailwind CSS v4, node shapes and colors, state precedence, semantic zoom, explicit run-focus mode, and visual quality gates. No implementation task started.

### Session 3 — Scale scope corrected (2026-08-19)

Checked current repository fixtures after user feedback. The largest checked-in business graph has 15 nodes and 20 edges. Removed speculative 1,000-node behavior and partial graph projections. Kept a generated 250-node and 400-edge test as safety headroom. No implementation task started.

### Session 4 — Customer decision workflow completed (2026-08-19)

Reframed the POC around the support workflow: find a customer, see all matching decisions, open one result, and explain every recorded branch on the complete graph. Removed graph V2, selected HTTP `QUERY`, added immutable PostgreSQL graph retention, removed the million-row performance fixture, hid developer provenance, and fixed the deployment scope to local loopback. One input remains open: how a raw operator-entered customer ID becomes the stored redacted canonical lookup value. No implementation task started.

### Session 5 — Generic correlation contract completed (2026-08-19)

Removed the customer-specific product model. The dashboard accepts an arbitrary bounded correlation name and its exact stored canonical value. Customer ID, route ID, person reference, and address reference are examples only. An editable combobox offers up to 200 stored correlation names but never enumerates values. The viewer does not guess an application-specific transformation. Confirmed from commit `b26b198`, the current exporter, schema generator, and tests that the sole current `fachtracing-developer-graph/v1` contract is the merged multi-source shape. The exact V2 filename remains only for stale-file cleanup. No product or contract question remains open. No implementation task started.

### Session 6 — Viewer implementation started (2026-08-19)

Implemented Tasks 1 through 5 and started Task 6. Added the Svelte 5 and SvelteKit Node application, repository-owned shadcn-style components, Tailwind CSS v4 tokens, strict V1 adapters, immutable PostgreSQL graph catalog, graph import command, generic `QUERY` search API, Svelte Flow canvas, top-to-bottom ELK worker, run inspector, evidence explanations, exact-correlation dashboard, responsive inspector, themes, CSP, documentation, and CI wiring. The Java storage migration now adds the graph catalog and cursor index as storage schema version 2.

The first dependency audit found high and critical advisories in the proposed SvelteKit, Vite, Vitest, and Playwright point versions. Patched same-major versions replaced them. The deprecated `lucide-svelte` package was replaced with maintained `@lucide/svelte`. The final high-severity audit gate passes with six low findings in the current SvelteKit `cookie` dependency.

Local evidence: zero Svelte diagnostics, 11 passing unit tests, a passing production build, a passing HTTP `QUERY` Chromium test, the 250-node layout below two seconds, focused JDBC tests, and the complete repository gate. The full PostgreSQL browser journey is assigned to the hosted PostgreSQL job because no local PostgreSQL daemon is available. A manual Docker attempt did not start because the local Docker daemon is not running.

### Session 7 — Hosted verification completed (2026-08-19)

The hosted verification run `32285683620` passed all four jobs at commit `c281382`. The PostgreSQL job passed the repeatable storage migration, viewer unit and build gates, dependency audit, seeded graph import, HTTP `QUERY` workflow, desktop run explanation, and narrow inspector journey in 1 minute 45 seconds. Mega Backend, Spring PetClinic, and the core repository gate also passed. Task 6 stays in progress only because visual baseline approval is a manual product review item.

### Session 8 — Generated self-dogfood proof completed (2026-08-19)

The normal analyzer now emits three current developer graph V1 files. Five real Java-agent executions emit decision-record V1 files with the generic `application=fachtracing` correlation. The production import adapters load these unchanged artifacts into PostgreSQL. The Playwright journey finds them through HTTP `QUERY`, opens one result, verifies Svelte Flow nodes and full-path mode, and captures the page.

Dogfooding found and fixed two compatibility faults: the TypeScript reader rejected the Java contract's zero-based observation sequence, and an ELK bundled library cannot run inside another worker. The final client uses ELK's supported API plus dedicated worker split and initializes it only in the browser. Hosted run `32297906019` passed all four jobs at commit `0a96961`. The PostgreSQL job passed all four browser journeys in 4.5 seconds and published `fachtracing-viewer-dogfood-32297906019`. Task 6 stays in progress only for manual visual baseline approval.

### Session 9 — Browser-only graph preview completed (2026-08-19)

Added `/graphs`, an accessible file selection and drop surface, a 5 MiB browser file boundary, local contract validation, provenance removal through the existing adapter, and the shared top-to-bottom Svelte Flow canvas. The preview does not define a server endpoint and does not use browser storage. Invalid files fail before layout, and graphs above 250 nodes remain complete with a tested-profile notice.

Local evidence: zero Svelte diagnostics, 14 passing unit tests, a passing production build, repository integrity, and five passing Chromium journeys. The generated-file journey selects a current Fachtracing self-dogfood graph, reads its expected counts from that JSON, verifies every Svelte Flow node, observes no non-read network request, verifies unchanged browser storage, and captures `fachtracing-graph-preview.png`.
