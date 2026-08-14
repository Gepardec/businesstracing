# Implementation Journal: Self Runtime Tracing

## Summary

All three tasks are complete. The existing self-tracing command now generates the current static
graph and activation bundle, starts the current Java agent in a separate Java 21 process, and
captures disabled, enabled, and invalid production executions. The guide explains the Maven parts,
runtime parts, general algorithm, and current evidence boundary. The full repository verifier
passed.

## Phase 1 Context Summary

- Config: defaults; `specsDir=.specops`, vertical `library`, task tracking `none`
- Context recovery: one unrelated active spec, `release-gate-timeout-budget`; no file ownership
  conflict exists
- Steering files: loaded `dependencies.md`, `product.md`, `reference-application.md`,
  `repo-map.md`, `structure.md`, and `tech.md`
- Repo map: fresh before this spec; source hash matched the current file list
- Memory: loaded project decisions, patterns, and file overlaps
- Project state: brownfield with 140 Java source files
- Related completed specs: `self-dogfood-business-tracing` and
  `runtime-decision-path-capture`
- Affected implementation files: one new Maven-plugin executable test, the existing self-tracing
  script, and the existing self-tracing guide
- Scope assessment: one end-to-end library feature; no decomposition is required

## Phase 2 Completion Summary

- Requirements: run the project through its own agent and prove three production paths.
- Design: use a Maven static pass followed by a separate Java 21 runtime pass.
- Tasks: runtime contract, gate integration, and documentation with complete verification.
- Dependencies: no new dependency; all 14 current exact direct external versions returned empty
  OSV results.
- Evaluation: all four specification dimensions meet the threshold.

## Phase 3 Completion Summary

- Tasks completed: 3 of 3.
- Runtime records: two successful executions and one generic failed execution.
- Exact runtime paths: disabled, enabled, and invalid paths all contain selected graph edges.
- Declared boundary: two deduplicated evidence-availability diagnostics match the activation
  bundle; no unexpected runtime or agent diagnostic exists.
- Tests: focused compilation, focused self-tracing, and the full repository verifier passed.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
|---|----------|-----------|------|-----------|
| 1 | Use a separate Java 21 process for runtime capture | The completed activation bundle must exist before transformation, and this matches user integration | 1 | 2026-08-14T08:37:34Z |
| 2 | Encode `Optional` only as `present` or `empty` | The value codec denies unknown objects and the self-trace must not use arbitrary string conversion | 1 | 2026-08-14T08:37:34Z |
| 3 | Verify declared evidence gaps instead of rejecting them | The static plan has exact edges but cannot bind two predicates over derived local booleans to raw method arguments | 1 | 2026-08-14T08:40:43Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
|---------|--------|--------|------|
| Require no runtime diagnostic | Require only the evidence-availability diagnostics declared by the activation bundle | The self-trace honestly exposes the current derived-local evidence boundary while it still proves exact branch selection | 1 |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
|---------|------------|--------|------|
| GitHub CLI has an invalid stored token | Continue local implementation and verification; restore authentication before PR creation | Publication is pending | Completion |

## Session Log

### 2026-08-14 — Task 1 anchor

- Objective: execute the developer-export policy through the generated activation and current
  Java agent.
- Files: one new executable test in the Maven-plugin test package.
- Checks: two successful records, one failed record, selected edges, exact record count, declared
  evidence gaps, and empty agent diagnostics.
- Safety: no production API or dependency change.

### 2026-08-14 — Task 1 complete

- Added one package-local executable runtime contract.
- Added a safe exact-type adapter that records `Optional` as `present` or `empty`.
- The Maven-plugin reactor test compilation passed.
- Task 2 changed from Pending to In Progress.

### 2026-08-14 — Task 2 anchor

- Objective: run the executable contract after static self-analysis.
- Files: `scripts/verify-self-tracing.sh`.
- Checks: invalid static path, current Java agent JAR, current test classpath, runtime success marker,
  and fail-fast process behavior.
- Integration: the existing full verifier already calls this script after reactor installation.

### 2026-08-14 — Task 2 complete

- The gate now checks the static failure path and the activation schema.
- The gate builds the current Maven-plugin test classpath and starts Java 21 with the current agent.
- The harness captured disabled, enabled, and invalid production executions.
- The focused self-tracing gate passed.
- Task 3 changed from Pending to In Progress.

### 2026-08-14 — Task 3 anchor

- Objective: explain the verified Maven, static-analysis, activation, agent, and runtime flow.
- Files: runtime harness output and `docs/self-tracing.md`.
- Source: checked graph and execution records from the focused self-tracing gate.
- Checks: focused gate, documentation review, and full repository verification.

### 2026-08-14 — Dependency safety check

- The Maven test-scope dependency tree resolved successfully.
- The official OSV API returned no advisory for all 14 exact direct external versions.
- No dependency safety blocker exists.

### 2026-08-14 — Task 3 complete

- Updated the guide with the current three-path graph and checked runtime summaries.
- Explained Maven adapter, engine, activation, agent, bridge, collector, and harness responsibilities.
- The full verifier passed the core, self-tracing, performance, external-release, Mega, and
  PetClinic checks. PostgreSQL stayed skipped because no connection was configured.

## Documentation Review

| File | Status | Review result |
|------|--------|---------------|
| `docs/self-tracing.md` | Updated | Explains the two-pass flow, three paths, module responsibilities, and the declared evidence limit. |
| `README.md` | Up-to-date | It already links to the self-tracing guide. |
| `docs/runtime-integration.md` | Up-to-date | The public runtime integration contract did not change. |
| `docs/maven-plugin.md` | Up-to-date | Maven goal configuration did not change. |
