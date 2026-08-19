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

## Phase 2 Completion Summary

- Requirements: One interactive graph, a right-side ordered run inspector, current-step and full-path highlighting, a searchable cursor-paged runs view, and strict compatibility with current JSON and SQL contracts.
- Design: A standalone SvelteKit Node application uses narrow server adapters, Svelte Flow, an ELK layout worker, ID-derived highlights, and PostgreSQL cursor queries.
- Tasks: Six ordered tasks cover contracts, read APIs, graph layout, run inspection, run search, and delivery verification.
- Dependencies: SvelteKit, Svelte Flow, ELK, PostgreSQL, and test tools are explicit approved dependencies. Existing completed JSON and JDBC specs are required.
- Open review points: PostgreSQL-only scope, reverse-proxy authentication, and exact-only correlation search.

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

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Use top-to-bottom layout in version one. | It follows normal page scrolling and keeps the right inspector outside the main flow direction. | 3 | 2026-08-19T11:58:25Z |
| 2 | Use shadcn-svelte with Tailwind CSS v4. | It supplies consistent accessible controls while keeping component source and theme tokens in the repository. | 1, 5 | 2026-08-19T11:58:25Z |
| 3 | Use progressive modes above 250 and 1,000 nodes. | A fixed detail level becomes unreadable on large graphs. Explicit focus mode prevents silent data loss. | 3 | 2026-08-19T11:58:25Z |
| 4 | Supersede decision 3 and render the complete graph in version one. | Current checked-in graphs have at most 15 nodes. The 250-node benchmark gives enough headroom without a second topology model. | 3 | 2026-08-19T12:15:33Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
| --- | --- | --- | --- |

## Session Log

### Session 1 — Specification created (2026-08-19)

Created a full-stack draft from the current graph JSON, decision-record V1, and JDBC schema. No implementation task started. The user review can change the three open deployment and search assumptions before Phase 3.

### Session 2 — Visual design revised (2026-08-19)

Added the missing visual language and large-graph contract after user review. The revision specifies top-to-bottom layout, shadcn-svelte and Tailwind CSS v4, node shapes and colors, state precedence, semantic zoom, explicit run-focus mode, and visual quality gates. No implementation task started.

### Session 3 — Scale scope corrected (2026-08-19)

Checked current repository fixtures after user feedback. The largest checked-in business graph has 15 nodes and 20 edges. Removed speculative 1,000-node behavior and partial graph projections. Kept a generated 250-node and 400-edge test as safety headroom. No implementation task started.
