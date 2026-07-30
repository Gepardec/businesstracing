---
specId: "business-graph-terminal-semantics"
startedAt: "2026-07-24T11:18:08Z"
completedAt: "2026-07-24T11:32:48Z"
finalStatus: "completed"
phases: [1, 2, 3, 4]
---

- Added shared Start/Stop semantics and explicit business return edges.
- Preserved multiple bytecode return probes against one Stop node and typed final result.
- Removed standalone id/ids vocabulary and rendered meaningful null comparisons as absent/exists.
- Connected exceptional paths from root methods and expanded callees to the shared Stop.
- Updated generic fixtures, renderer snapshots, reviewed Mega oracles, generated diagrams, and docs.
- `./scripts/verify.sh` passed with 5,000 traces at 1,000 RPS and zero errors.
- `./scripts/verify-mega-backend.sh` passed five exact graphs and the polymorphic runtime capture.
