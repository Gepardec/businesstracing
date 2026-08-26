# Feature: Interactive Flow and Run Explorer

## Overview

Internal support users cannot efficiently find prior business decisions or explain one decision from the current text and Mermaid outputs. This proof of concept adds a local browser dashboard that lists stored decisions, finds decisions through an arbitrary exact correlation, opens one decision, presents its complete business graph, and explains the observed path without changing the current graph or decision-record wire contracts.

## Primary Workflow

1. A support user opens the dashboard and sees the newest recorded decisions.
2. The user enters a correlation name and its exact stored value. Examples can include a route ID, person reference, address reference, or customer ID.
3. The dashboard shows all matching decisions with completion time, business decision label, status, and final result.
4. The user opens one decision.
5. The application loads the exact graph ID and version, highlights the selected run path, and shows the ordered explanation beside the complete graph.
6. Each step combines the business node label, the recorded outcome, the selected edge, and only the already-redacted evidence captured for that observation.

## User Stories

### Story 1: Read the business graph

**As a** business analyst
**I want** a well-spaced interactive graph
**So that** I can understand the decision flow without reading Java or Mermaid source

**Acceptance Criteria (EARS):**

- WHEN the application loads a supported graph JSON document THE SYSTEM SHALL render every node and edge with a deterministic layered layout derived from the document.
- THE SYSTEM SHALL use a top-to-bottom layout by default so that the main execution direction follows normal page reading and scrolling.
- THE SYSTEM SHALL distinguish entry, predicate, choice, computation, dispatch, outcome, and coverage-gap nodes by a documented combination of silhouette, icon, text label, and color without exposing Java symbols.
- THE SYSTEM SHALL use the visual tokens and node-state precedence defined in `visual-design.md`; color SHALL NOT be the only signal for type, selection, run path, or status.
- WHEN a user pans, zooms, fits, or selects a node THE SYSTEM SHALL keep labels readable and preserve the generated layout.
- WHEN zoom makes node text unreadable THE SYSTEM SHALL replace detail with a stable type symbol and short label before hiding optional metadata.
- THE SYSTEM SHALL render the complete validated graph and SHALL NOT remove nodes or edges to meet a display-size threshold.
- IF a graph is incomplete or references an unknown node or edge THEN THE SYSTEM SHALL show an explicit non-technical gap and SHALL NOT invent a connection.

**Progress Checklist:**

- [x] Render a deterministic, data-driven graph.
- [x] Show all supported node kinds and coverage gaps.
- [x] Apply the documented node, edge, state, and theme tokens.
- [x] Support semantic zoom without changing graph topology.
- [x] Support pan, zoom, fit, and node selection.
- [x] Fail closed on invalid graph references.

### Story 2: Follow one run through the graph

**As a** support or business user
**I want** an ordered step list beside the graph
**So that** I can see how one result was produced

**Acceptance Criteria (EARS):**

- WHEN a run is selected THE SYSTEM SHALL show its observations in sequence order in a right-side inspector with the business label, recorded outcome, selected branch, and already-redacted display evidence.
- WHEN recorded evidence explains a predicate THE SYSTEM SHALL present a plain-language statement such as `age was 17; age below 18 was true` without deriving or inventing an unrecorded fact.
- IF an observation has no recorded business evidence THEN THE SYSTEM SHALL show its outcome and SHALL state that no additional evidence was recorded.
- WHEN a step is selected THE SYSTEM SHALL focus and highlight the matching node and its selected edge while preserving the user's current zoom when the item is already visible.
- WHERE full-path highlighting is enabled THE SYSTEM SHALL highlight all observed nodes and resolved connecting edges for the selected run.
- WHEN a run visits the same node more than once THE SYSTEM SHALL keep each visit as a separate ordered step and identify the active visit.
- IF the run and graph identifiers or versions do not match THEN THE SYSTEM SHALL block path highlighting and explain the mismatch.

**Progress Checklist:**

- [x] Show the ordered run inspector on the right.
- [x] Link each step to its graph node and selected edge.
- [x] Add the full-path highlight control.
- [x] Preserve repeated visits and reject version mismatch.

### Story 3: Search all runs

**As a** support user
**I want** a searchable decision list
**So that** I can find the execution that explains a reported result

**Acceptance Criteria (EARS):**

- WHEN the dashboard opens THE SYSTEM SHALL show cursor-paged decision summaries ordered by completion time descending and execution ID descending.
- THE SYSTEM SHALL show completion time, business decision label, status, and already-redacted final result for each summary.
- WHEN the search form loads THE SYSTEM SHALL offer up to 200 distinct stored correlation names in an editable combobox and SHALL NOT enumerate correlation values.
- WHEN a user submits an execution ID, graph ID, status, inclusive time range, or exact correlation name and value THE SYSTEM SHALL send a JSON search document with HTTP `QUERY`, apply the filters on the server, and reset pagination.
- THE SYSTEM SHALL keep correlation names and values out of the request URI, browser history, result URLs, and application logs.
- WHEN an exact correlation matches several decisions THE SYSTEM SHALL show all matching decisions in the same result list.
- WHEN a user selects a result THE SYSTEM SHALL open the matching graph version and run inspector through a linkable URL.
- IF no result matches THEN THE SYSTEM SHALL show an empty result state without changing the active filters.
- IF a database query fails or exceeds its timeout THEN THE SYSTEM SHALL show a retry action and SHALL NOT expose connection details, SQL, or credentials.

**Progress Checklist:**

- [x] Add the paged and filterable runs view.
- [x] Offer stored correlation names without limiting the field to those suggestions.
- [x] Show the decision label and final result in each result summary.
- [x] Use HTTP `QUERY` with confidential filters in the request body.
- [x] Use linkable run URLs.
- [x] Add safe empty and error states.

### Story 4: Preserve current contracts

**As a** Fachtracing integrator
**I want** the viewer to use existing JSON and SQL data
**So that** I do not maintain a second tracing format

**Acceptance Criteria (EARS):**

- THE SYSTEM SHALL accept the current merged multi-source `fachtracing-developer-graph/v1` graph documents and `fachtracing-decision-record/v1` payloads through explicit adapters.
- THE SYSTEM SHALL query the existing `fachtracing_decision_record` and `fachtracing_correlation` schema without changing stored payloads or correlation semantics.
- WHEN an operator imports a valid developer graph V1 document THE SYSTEM SHALL store its unchanged bytes immutably by graph ID and graph version in PostgreSQL.
- WHEN the same graph ID and version are imported again with identical bytes THE SYSTEM SHALL accept the import as idempotent.
- IF the same graph ID and version are imported with different bytes THEN THE SYSTEM SHALL reject the import as a contract conflict.
- WHEN a stored run is opened THE SYSTEM SHALL load its exact graph from the PostgreSQL graph catalog.
- THE SYSTEM SHALL NOT delete an imported graph automatically. Graph deletion requires an explicit maintenance operation that first proves that no stored run references the graph ID and version.
- THE SYSTEM SHALL omit developer source paths, source URLs, source origins, and source fingerprints from browser responses by default.
- WHEN an unsupported schema ID is received THE SYSTEM SHALL reject it with a visible compatibility message.
- WHEN a supported record contains unknown fields THE SYSTEM SHALL ignore those fields in line with the V1 forward-read contract.

**Progress Checklist:**

- [x] Add tested graph and run contract adapters.
- [x] Add immutable graph import and exact-version retrieval.
- [x] Keep developer provenance server-side by default.
- [x] Keep the V1 storage payload unchanged.
- [x] Reject unsupported schemas and accept unknown fields.

### Story 5: Prove the viewer with Fachtracing itself

**As a** maintainer or reviewer
**I want** the viewer to show Fachtracing's own generated graphs and runtime paths
**So that** I can verify the complete extraction, storage, query, and explanation chain without a hand-written demonstration

**Acceptance Criteria (EARS):**

- WHEN the self-tracing gate runs THE SYSTEM SHALL generate current developer graph V1 documents for the selected Fachtracing production policies.
- WHEN each selected production policy executes through the Java agent THE SYSTEM SHALL write its actual decision-record V1 payload with a generic `application=fachtracing` correlation.
- WHEN the dogfood browser proof runs THE SYSTEM SHALL import those generated graphs and runs into PostgreSQL and SHALL find them through HTTP `QUERY`.
- WHEN a reviewer opens a dogfood result THE SYSTEM SHALL render the complete generated graph, the observed path, the final result, and the ordered explanation.
- THE SYSTEM SHALL capture browser proof from generated artifacts and SHALL NOT contain a fixed Fachtracing graph, fixed node IDs, fixed edge IDs, or graph-specific positions.

**Progress Checklist:**

- [x] Emit developer graph V1 and decision-record V1 self-tracing artifacts.
- [x] Import the generated artifacts into the viewer database.
- [x] Search and open the self-traced runs in the browser.
- [x] Capture reviewable browser proof from the actual generated graph.

### Story 6: Preview a graph JSON file

**As a** developer or business analyst
**I want** to select a supported graph JSON file in the browser
**So that** I can inspect its complete graph without a database import

**Acceptance Criteria (EARS):**

- WHEN a user selects or drops one current `fachtracing-developer-graph/v1` or `fachtracing-business-graph/v1` JSON file THE SYSTEM SHALL validate it in the browser, normalize it to the shared graph model, and render its complete graph with the existing top-to-bottom ELK and Svelte Flow canvas.
- THE SYSTEM SHALL keep the selected file and its content in browser memory only and SHALL NOT send it to the server, write it to PostgreSQL, or persist it in browser storage.
- THE SYSTEM SHALL render only the provenance-free graph model and SHALL NOT display source origins, source paths, source URLs, or source fingerprints from the file.
- WHEN the selected file is empty, is larger than 5 MiB, is not named as JSON, has invalid JSON, or does not match the current graph contract THE SYSTEM SHALL show a clear validation message and SHALL NOT render a partial graph.
- WHEN a valid graph contains more than 250 nodes THE SYSTEM SHALL permit the preview and SHALL state that 250 nodes is the tested safety profile.
- WHEN the page reloads THE SYSTEM SHALL clear the selected file and graph.
- THE SYSTEM SHALL provide a control to replace the selected file without opening a new page.

**Progress Checklist:**

- [x] Add a browser-only graph file adapter.
- [x] Add an accessible file selection and drop surface.
- [x] Reuse the existing graph canvas without a run highlight.
- [x] Show file, graph, size-profile, and validation states.
- [x] Prove that the preview works with a generated graph file.
- [x] Accept and render real developer-graph V1 and business-graph V1 exports.

## Non-Functional Requirements

- Accessibility: All run navigation and graph selection actions shall work with a keyboard. Visible controls shall meet WCAG 2.2 AA contrast and focus requirements.
- Accessible alternative: The dashboard shall provide a semantic decision table, a semantic ordered explanation list, and a searchable semantic node list so that the recorded decision can be understood without operating the graph canvas.
- Performance: With 250 nodes and 400 edges, initial layout shall finish within 2 seconds on the CI browser profile. A step selection shall update visible highlighting within 100 milliseconds after the data is loaded.
- Search: A query shall return at most 50 decision summaries per page. Version one has no production-scale latency benchmark.
- Security: The browser shall receive already-redacted values only. Database credentials shall remain in server-only environment variables. All filters shall use parameterized queries and bounded lengths.
- Responsive layout: At widths of 1,200 CSS pixels or more, the graph and inspector shall appear side by side. At smaller widths, the inspector shall become a drawer without hiding the selected step.
- Visual quality: Reference screenshots for light and dark themes at desktop and narrow widths shall pass automated visual comparison with an approved baseline and manual review of label collisions, clipped controls, focus visibility, and node-state ambiguity.
- Decision layout: At 1,024 CSS pixels or more, the ordered explanation SHALL be visible beside the graph by default. Below 1,024 pixels, a visible `Explanation` action SHALL open an accessible modal Sheet that supports Escape, outside-click close, focus containment, and focus return.
- Header density: At a 1,440 by 900 CSS-pixel viewport, the decision summary header SHALL use no more than 170 CSS pixels of vertical space and SHALL provide access to complete truncated values.
- Edge labels: The canvas SHALL show concise branch tokens of at most 32 visible characters. Full outcome text SHALL remain available through a tooltip and the run inspector. Labels SHALL NOT cover node text.
- Run states: A current node SHALL use one current-step outer ring. A non-current path node SHALL use one path inner ring. The UI SHALL NOT stack both run-state rings on the same node.
- Step numbering: User-facing step badges SHALL use one-based visit order. The recorded sequence value SHALL remain unchanged in the run model.
- Canvas controls: A minimap SHALL use a compact fixed size for graphs with 9 through 100 nodes. It SHALL be omitted for graphs with eight or fewer nodes. For graphs with more than 100 nodes, the UI SHALL replace the compressed minimap with a compact node-count and search-navigation guide.
- Edge routing: The canvas SHALL render the orthogonal routes that ELK computes. An edge SHALL NOT cross an unrelated node, and parallel edges SHALL use distinct visible paths.
- Read-only canvas: Node handles SHALL NOT be visible. A node SHALL use one primary border for its default, path, or current state, and SHALL NOT stack competing state borders.
- Preview compatibility: The local preview SHALL accept the current developer-graph V1 and business-graph V1 JSON contracts. The database graph catalog remains developer-graph V1 only.

## Constraints & Assumptions

- The proof of concept targets PostgreSQL only. H2 remains a test-only Java adapter.
- The proof of concept accepts any bounded correlation name and exact canonical value that already exists in `fachtracing_correlation`. It does not hardcode a domain field or apply an application-specific value transformation.
- The operator must search with the canonical value that the traced application stored after its redaction policy. A deployment that needs lookup from a different raw value must store an additional lookup-safe correlation or add a separately specified adapter.
- A separate import command reads developer graph V1 JSON files and stores unchanged payload bytes in the PostgreSQL graph catalog. The running dashboard reads graphs and runs but does not import or delete data.
- The application is a local internal tool. It binds to loopback by default. Authentication, authorization, tenant isolation, and public deployment are outside this proof of concept.
- The UI displays business graph labels. Developer source metadata stays server-side and is disabled in browser responses by default.
- The graph preview reads one local JSON file into browser memory. It does not upload or persist that file. Binary graph preview is a future compatible extension and is not part of this increment.
- Layout positions are always computed from graph data. No graph-specific coordinates or hardcoded diagrams are permitted.

## Dependencies & Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| unify-developer-graph-contract | Defines the sole current multi-source V1 graph document and schema consumed by the viewer. | Yes | completed |
| generic-application-readiness | Defines the stored run payload, storage port, and PostgreSQL schema. | Yes | completed |
| mermaid-diagram-rendering | Provides the existing path semantics that the interactive view must preserve. | No | completed |

### Cross-Spec Blockers

| Blocker | Blocking Spec | Resolution Type | Resolution Detail | Status |
| --- | --- | --- | --- | --- |
| None | — | — | The POC searches an arbitrary exact correlation name and its already-stored canonical value. | resolved |

## Success Metrics

- A reviewer can find a stored run, open it, and identify its final result and every observed step without reading Java or Mermaid source.
- Automated contracts prove that graph and run adapters preserve every ID used for node and edge highlighting.
- The graph layout benchmark and PostgreSQL search contract meet the stated limits.
- Hosted dogfood proof shows a real Fachtracing production policy and its Java-agent runtime path in the viewer.
- Generated dogfood screenshots at 1,440, 1,024, and 390 CSS pixels show no missing inspector action, clipped summary control, label collision, ambiguous run-state ring, or oversized minimap.
- Generated branch-topology proof shows distinct parallel routes, no edge crossing through an unrelated node, hidden connection handles, and readable labels in light and dark themes.
- A checked-in business-graph V1 conformance fixture opens in the local graph preview without a contract error.

## Out of Scope

- Editing graphs, moving nodes persistently, or writing records from the UI.
- Authentication, authorization, tenant isolation, shared-service deployment, and public internet deployment.
- Live streaming of in-progress runs.
- Cross-run overlays on one graph, aggregate analytics, and custom saved searches.
- Support for databases other than PostgreSQL in the SvelteKit server.
- Uploading, storing, sharing, or editing a graph selected on the preview page.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep every component to one responsibility.
- Do not hardcode diagrams or graph-specific layout positions.

## Review Questions

None. The specification is ready for implementation.
