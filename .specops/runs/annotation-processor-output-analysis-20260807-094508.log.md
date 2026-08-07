---
specId: "annotation-processor-output-analysis"
startedAt: "2026-08-07T09:45:08Z"
completedAt: "2026-08-07T09:53:33Z"
finalStatus: "completed"
phases: [1, 2, 3, 4]
---

## Phase 1: Context

### [09:45:08] Inspect the Hogajama diagnostic

- Action: Trace the missing `javax.annotation.processing.Generated` type through the effective
  compiler model.
- Result: Maven `source` and `target` are converted into `--release`. This changes the visible JDK
  API and does not match the successful Maven compilation.

## Phase 2: Specification

### [09:45:08] Reopen version 4

- Action: Add Task 6 and preserve explicit `release` behavior as an unchanged contract.
- Result: One contained compiler-model correction is in scope. No new dependency is required.

## Phase 3: Implementation

### [09:45:08] Task 6: Preserve Maven source and target semantics

- Action: Set Task 6 to In Progress before code and test changes.
- Result: Implementation started.

- Result: The focused contracts and real Java 8 generated-source reactor pass.

## Phase 4: Completion

### [09:53:33] Verify and complete version 4

- Action: Run `./scripts/verify-pr.sh` with Java 21 and review the changed compiler paths.
- Result: All gates pass. Five Mega graphs are complete. The load gate reports 0.251% p95 overhead
  and zero errors, mismatches, drops, or contamination.
