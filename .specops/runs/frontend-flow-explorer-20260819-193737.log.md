---
specId: "frontend-flow-explorer"
startedAt: "2026-08-19T19:37:37Z"
completedAt: null
finalStatus: "implementing"
phases: [1, 2, 3]
---

## Phase 1: Context

### [19:37:37] Recover the implemented viewer and self-tracing proof

- Action: Read the active viewer specification, viewer contracts, self-tracing profile, runtime proof, and hosted CI design.
- Result: Fachtracing already generates its own exact graphs and five real runtime paths. The viewer cannot show them because the self-trace does not emit developer JSON or decision-record JSON for import.

## Phase 2: Specification

### [19:37:37] Add generated dogfood proof

- Action: Extend the active viewer specification with a generated-artifact browser proof.
- Result: The proof must use the normal analyzer and Java agent. It must not contain a fixed graph or graph-specific positions.

## Phase 3: Implementation

### [21:51:00] Connect generated artifacts to the viewer

- Action: Emit decision-record V1 files from real Java-agent executions, add a generic run importer, and add a PostgreSQL browser journey.
- Result: The viewer checks pass and repository integrity is valid. Hosted browser proof remains pending.
