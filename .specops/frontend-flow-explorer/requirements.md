# Feature: Interactive Flow and Run Explorer

## Overview

Business users cannot efficiently inspect large Fachtracing graphs or compare stored runs in the current text and Mermaid outputs. This feature adds a browser application that presents a stable, readable graph, shows one run as an ordered path, and supports search across persisted runs without changing the existing graph or decision-record contracts.

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
- WHEN a graph contains more than 250 nodes THE SYSTEM SHALL enter the documented large-graph mode and keep search, selected-run focus, fit, pan, and zoom available.
- WHEN a graph contains more than 1,000 nodes and a run is selected THE SYSTEM SHALL open with the selected run path and one-hop context, state that the view is partial, show the total node count, and provide an explicit action to request the full graph.
- IF a graph is incomplete or references an unknown node or edge THEN THE SYSTEM SHALL show an explicit non-technical gap and SHALL NOT invent a connection.

**Progress Checklist:**

- [ ] Render a deterministic, data-driven graph.
- [ ] Show all supported node kinds and coverage gaps.
- [ ] Apply the documented node, edge, state, and theme tokens.
- [ ] Support semantic zoom and explicit large-graph focus mode.
- [ ] Support pan, zoom, fit, and node selection.
- [ ] Fail closed on invalid graph references.

### Story 2: Follow one run through the graph

**As a** support or business user
**I want** an ordered step list beside the graph
**So that** I can see how one result was produced

**Acceptance Criteria (EARS):**

- WHEN a run is selected THE SYSTEM SHALL show its observations in sequence order in a right-side inspector with outcome and already-redacted display evidence.
- WHEN a step is selected THE SYSTEM SHALL focus and highlight the matching node and its selected edge while preserving the user's current zoom when the item is already visible.
- WHERE full-path highlighting is enabled THE SYSTEM SHALL highlight all observed nodes and resolved connecting edges for the selected run.
- WHEN a run visits the same node more than once THE SYSTEM SHALL keep each visit as a separate ordered step and identify the active visit.
- IF the run and graph identifiers or versions do not match THEN THE SYSTEM SHALL block path highlighting and explain the mismatch.

**Progress Checklist:**

- [ ] Show the ordered run inspector on the right.
- [ ] Link each step to its graph node and selected edge.
- [ ] Add the full-path highlight control.
- [ ] Preserve repeated visits and reject version mismatch.

### Story 3: Search all runs

**As a** support user
**I want** a searchable run list
**So that** I can find the execution that explains a reported result

**Acceptance Criteria (EARS):**

- WHEN the runs view opens THE SYSTEM SHALL show cursor-paged run summaries ordered by completion time descending and execution ID descending.
- WHEN a user submits an execution ID, graph ID, status, inclusive time range, or exact redacted correlation key/value filter THE SYSTEM SHALL apply the filters on the server and reset pagination.
- WHEN a user selects a result THE SYSTEM SHALL open the matching graph version and run inspector through a linkable URL.
- IF no result matches THEN THE SYSTEM SHALL show an empty result state without changing the active filters.
- IF a database query fails or exceeds its timeout THEN THE SYSTEM SHALL show a retry action and SHALL NOT expose connection details, SQL, or credentials.

**Progress Checklist:**

- [ ] Add the paged and filterable runs view.
- [ ] Use linkable run URLs.
- [ ] Add safe empty and error states.

### Story 4: Preserve current contracts

**As a** Fachtracing integrator
**I want** the viewer to use existing JSON and SQL data
**So that** I do not maintain a second tracing format

**Acceptance Criteria (EARS):**

- THE SYSTEM SHALL accept `fachtracing-developer-graph/v1` and `/v2` graph documents and `fachtracing-decision-record/v1` payloads through explicit adapters.
- THE SYSTEM SHALL query the existing `fachtracing_decision_record` and `fachtracing_correlation` schema without changing stored payloads or correlation semantics.
- WHEN an unsupported schema ID is received THE SYSTEM SHALL reject it with a visible compatibility message.
- WHEN a supported record contains unknown fields THE SYSTEM SHALL ignore those fields in line with the V1 forward-read contract.

**Progress Checklist:**

- [ ] Add tested graph and run contract adapters.
- [ ] Keep the V1 storage payload unchanged.
- [ ] Reject unsupported schemas and accept unknown fields.

## Non-Functional Requirements

- Accessibility: All run navigation and graph selection actions shall work with a keyboard. Visible controls shall meet WCAG 2.2 AA contrast and focus requirements.
- Performance: With 250 nodes and 400 edges, initial layout shall finish within 2 seconds on the CI browser profile. With 1,000 nodes and 1,600 edges, a cached or fresh large-graph view shall become interactive within 5 seconds. A step selection shall update visible highlighting within 100 milliseconds after the data is loaded.
- Search: A page request of at most 50 run summaries shall complete within 500 milliseconds at p95 against the PostgreSQL contract fixture with one million metadata rows, excluding network latency.
- Security: The browser shall receive already-redacted values only. Database credentials shall remain in server-only environment variables. All filters shall use parameterized queries and bounded lengths.
- Responsive layout: At widths of 1,200 CSS pixels or more, the graph and inspector shall appear side by side. At smaller widths, the inspector shall become a drawer without hiding the selected step.
- Visual quality: Reference screenshots for light and dark themes at desktop and narrow widths shall pass automated visual comparison with an approved baseline and manual review of label collisions, clipped controls, focus visibility, and node-state ambiguity.

## Constraints & Assumptions

- The first deployable version targets PostgreSQL because it is the required production JDBC contract. H2 remains a test-only Java adapter.
- The SvelteKit server reads graph artifacts from a configured read-only directory and reads runs from the existing PostgreSQL tables.
- Deployment is behind a trusted reverse proxy. Authentication and role management are outside this increment; the server must not bind publicly by default in documented examples.
- The UI displays business graph labels by default. Developer source links are an optional graph capability and never appear in run payloads.
- Layout positions are always computed from graph data. No graph-specific coordinates or hardcoded diagrams are permitted.

## Dependencies & Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| developer-graph-json-schema | Defines the graph documents consumed by the viewer. | Yes | completed |
| generic-application-readiness | Defines the stored run payload, storage port, and PostgreSQL schema. | Yes | completed |
| mermaid-diagram-rendering | Provides the existing path semantics that the interactive view must preserve. | No | completed |

### Cross-Spec Blockers

| Blocker | Blocking Spec | Resolution Type | Resolution Detail | Status |
| --- | --- | --- | --- | --- |
| None | — | — | — | resolved |

## Success Metrics

- A reviewer can find a stored run, open it, and identify its final result and every observed step without reading Java or Mermaid source.
- Automated contracts prove that graph and run adapters preserve every ID used for node and edge highlighting.
- The graph layout benchmark and PostgreSQL search contract meet the stated limits.

## Out of Scope

- Editing graphs, moving nodes persistently, or writing records from the UI.
- Authentication, authorization, tenant isolation, and public internet deployment.
- Live streaming of in-progress runs.
- Cross-run overlays on one graph, aggregate analytics, and custom saved searches.
- Support for databases other than PostgreSQL in the SvelteKit server.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep every component to one responsibility.
- Do not hardcode diagrams or graph-specific layout positions.

## Review Questions

- Confirm whether PostgreSQL-only server access is acceptable for the first version.
- Confirm whether the initial deployment can rely on a trusted reverse proxy for authentication.
- Confirm whether exact redacted correlation-value search is sufficient, or whether prefix/full-text search is required later.
