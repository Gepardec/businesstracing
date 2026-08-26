---
specId: "frontend-flow-explorer"
startedAt: "2026-08-19T13:06:48Z"
completedAt: "2026-08-19T13:06:48Z"
finalStatus: "draft"
phases: [1, 2]
---

## Phase 1: Confirm Product Workflow

### [13:06:48] User decisions

- Developer graph V2 does not exist and is not supported.
- Use PostgreSQL for the POC.
- Deploy as a local internal tool.
- Use exact correlation search.
- Hide developer source metadata from the browser.
- Keep exact historical graphs available; JSON or binary storage is feasible.
- Remove production-scale benchmarking.
- Primary use case: find every decision for a customer and explain one customer-specific path during a support call.

## Phase 2: Revise Specification

### [13:06:48] HTTP search

- Verified RFC 10008, published June 2026, defines safe and idempotent HTTP `QUERY` with request content.
- Verified SvelteKit provides a fallback handler for unhandled methods.
- Selected same-origin `QUERY /api/v1/runs` with JSON content, `Accept-Query`, `no-store`, bounded bodies, and no sensitive URI fields.

### [13:06:48] Durable graph catalog

- Add an immutable PostgreSQL graph catalog keyed by graph ID and version.
- Import unchanged developer graph V1 JSON bytes with schema ID, media type, checksum, and conflict detection.
- Keep the running dashboard read-only and keep developer provenance out of browser graph responses.
- Do not invent a binary graph contract. The media-type column permits one after it is separately specified.

### [13:06:48] Dashboard and explanation

- List newest decisions with decision label, status, completion time, and final result.
- Exact customer lookup returns all matching decision pages.
- Decision selection opens the complete graph, full observed path, and ordered plain-language evidence.
- Missing evidence is stated explicitly and never inferred.

### [13:06:48] Evaluation

- Result: Pass at 9/10 for all four dimensions.
- Open blocker: define how an operator-entered raw customer ID maps to the stored redacted canonical correlation value.
