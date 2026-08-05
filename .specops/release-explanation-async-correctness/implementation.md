# Implementation: Release, Explanation, and Async Correctness

## Current Status

Tasks 1 through 6 are complete. Task 7 is in progress. Standard verification and pinned Mega
conformance pass. The clean-clone 600-second release gate still needs the committed checkpoint.

## Requirement Trace

| Requirement | Implementation | Executable contract |
| --- | --- | --- |
| Producer failure controls release status | `capture-gate-output.sh` and split release producer | `test-capture-gate-output.sh` |
| No undeclared `rg` tool | POSIX `grep`, `sed`, and `awk` verification scripts | repository integrity with restricted `PATH` |
| Result-relevant typed facts | `EvidenceTarget`, activation serialization, predicate evidence staging | `capturesOnlyResultRelevantPredicateOperands` |
| No blanket argument capture | transformer evidence plan only | irrelevant employee identifier assertion |
| Rejection and cancellation release | atomic `AsyncReservation` and tracked `Future` | `rejectedAndCancelledSubmissionsReleaseReservations` |
| Exact callback positions | `AsyncInvocationCatalog` | `exactAsyncCallbackPositionsPropagate` |
| Unsupported async gap | exact catalog miss classification | `unsupportedAsyncBoundaryCreatesExecutionGap` |
| Business-safe indexed iteration | canonical loop lowering and expression normalization | `lowersIndexedLoopsToBusinessIteration` |
| No ordinal rule output | neutral selected-rule edges | Maven, generic analyzer, and Mega contracts |
| Export guard | `BusinessArtifactGuard` | generic loop and Mega conformance assertions |

## Compatibility Evidence

- Activation V2 read compatibility passes in `ApiModelTest`.
- The external activation bundle passes in standard verification without runtime source analysis.
- Existing direct, overload, complex Boolean, switch, exception, binary fallback, dynamic dispatch,
  platform thread, and virtual thread contracts pass.
- Pinned Mega Backend produces all five complete graphs. No Mega vocabulary exists in production
  code.
- The journey oracle changed only for generic indexed-loop lowering and neutral selected-rule edges.

## Verification Recorded So Far

- `./scripts/verify.sh`: PASS.
- Short load gate: 0.191% p95 overhead, 5,000 enabled decisions, zero errors, mismatches, drops, or
  contamination.
- `MEGA_BACKEND_DIR=/tmp/fachtracing-mega-backend ./scripts/verify-mega-backend.sh`: PASS.
- Mega source corpus: 420 Java files, five complete graphs.
- PostgreSQL: not rerun locally because no connection was configured; the unchanged CI job remains
  required.
