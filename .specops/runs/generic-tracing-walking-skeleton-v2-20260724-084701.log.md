---
specId: "generic-tracing-walking-skeleton"
startedAt: "2026-07-24T08:47:01Z"
completedAt: "2026-07-24T10:30:55Z"
finalStatus: "completed"
phases: [1, 2, 3, 4]
---

## Scope version 2

- User requirement: `mega-backend` must prove the generic extractor on a realistic brownfield
  system and must not shape production into a Gepardec-specific solution.
- Requirements: added Use Case 5 and seven unchecked Definition-of-Done criteria.
- Design: added an opaque-conformance-corpus decision, anti-overfitting boundaries, oracle review,
  same-artifact non-Mega regression, and reference-specific risk controls.
- Tasks: added pending Task 7 with exact graph-oracle, runtime-path, forbidden-reference, generic
  construct, reproducibility, and conformance-report tests.
- State: reopened spec as version 2 with status `implementing`; prior implementation evaluation is
  marked stale for the expanded scope.
- Spec evaluation: passed iteration 2 with scores 9, 8, 9, and 10.

## Completion

- Task 7 completed with five immutable exact Mega graph oracles across four business areas.
- The journey-warning manager produced a complete 72-node/89-edge polymorphic graph and a real
  typed collection execution selecting all three strategy edges.
- `./scripts/verify.sh` and `./scripts/verify-mega-backend.sh` passed.
- The final 600-second enabled run completed 600,000 traces at 1,000 RPS with 0.267% p95
  overhead and zero errors, mismatches, dropped traces, or contamination.
- Implementation evaluation iteration 3 passed with scores 9, 9, 8, and 10.
