# Implementation Journal: Exact Runtime Decision Path Capture

## Summary

Completed all 3 tasks. Runtime records now contain validated exact branch edges, successful or generic failed terminal state, and invocation-local dispatch correlation. The analyzer and agent use occurrence-aware completion modes for supported Java 21 `javac` predicates and use legacy observations for unsafe forms. Review found and corrected compound-edge contradictions, partial manifests, ambiguous predicate forms, variable line tables, and linear runtime edge validation. The full verification suite passes with 0.150% p95 overhead at 1,000 requests per second and no capture errors.

## Phase 1 Context Summary

- Config: no `.specops.json`; defaults used with `.specops`, library vertical, task tracking `none`, review disabled, and evaluation enabled
- Context recovery: no incomplete spec; adapted the planned `runtime-decision-path-capture` slot in initiative `generic-java-fachtracing`
- Steering files: loaded 6 files (`dependencies.md`, `product.md`, `reference-application.md`, `repo-map.md`, `structure.md`, `tech.md`) and reloaded them after the `main` sync
- Repo map: stale because `AGENTS.md` was added; refreshed at Phase 1
- Memory: loaded 11 decisions from 4 completed specs and 5 patterns after the `main` sync
- Vertical: library
- Affected files: runtime execution model, invocation context, collector, runtime bridge, analysis manifest, graph builder, path resolver, transformer, tests, README, and runtime documentation
- Project state: brownfield Java 21 Maven library
- Scope assessment: existing initiative decomposition applies; this spec contains one coupled runtime increment and needs no further split
- Coherence check: pass; exact-edge, failure, compatibility, and privacy requirements match the design and tasks
- Vocabulary check: pass; library terms use developer use cases, modules, and public API surface
- Plan validation: pass; all listed files exist and no new file reference is unresolved

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Create exact branch metadata only when one `true` and one `false` edge leave the predicate. | Incomplete or ambiguous graph data must use the safe legacy probe. | 2 | 2026-07-31 |
| 2 | Replace a bound conditional jump with false-target and fall-through probe trampolines. | The Java 21 `javac` jump target is the false continuation, and the fall-through path is the true continuation. | 2 | 2026-07-31 |
| 3 | Put one catch-all handler around each entry method body and remove direct `ATHROW` probes. | The handler records both explicit and propagated failures once and rethrows the same object. | 2 | 2026-07-31 |
| 4 | Accept an explicit diagram edge only when it leaves the observed node. | Invalid evidence must not select or infer a business route. | 2 | 2026-07-31 |
| 5 | Bind supported short-circuit jumps by method occurrence and completion mode. | A full-predicate edge can be emitted only when one atomic jump determines the whole compound result. | 2 | 2026-07-31 |
| 6 | Require an all-or-none exact plan for each compound predicate group. | Partial metadata must not mix exact edges with inferred or legacy observations. | 3 | 2026-07-31 |
| 7 | Pre-index graph edges when the runtime registers a graph. | Application-thread edge validation must not scan the static graph. | 3 | 2026-07-31 |
| 8 | Use source lines for the first group jump and method occurrence for continuation jumps. | Java 21 `javac` can assign the group line or the operand line to later jumps in the same predicate. | 3 | 2026-07-31 |
| 9 | Keep mixed, nested, negated-compound, ternary, switch-expression, and ambiguous lambda forms on legacy probes. | Exact evidence is safe only when the analyzer can map every jump that completes the full predicate. | 3 | 2026-07-31 |

## Deviations from Design

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |
| Treat each predicate probe as a complete true/false branch. | Use occurrence-aware modes for supported flat `&&` and `||` predicates; use the legacy probe for mixed or nested forms. | Adversarial evaluation proved that one full binding per atomic jump can record contradictory edges. | 2 |

## Documentation Review

- `README.md`: updated with exact branch and generic failure behavior.
- `docs/supported-java-constructs.md`: updated with supported flat compound forms and safe fallback forms.
- `docs/plantuml/runtime-correlation.puml`: updated with exact edge and failed-invocation flow.
- `docs/maven-plugin.md`: checked; the runtime change does not affect Maven plugin usage.
- `AGENTS.md`: checked and followed; repository text uses STE and no subagents were used after the rule was loaded.

## Session Log

- 2026-07-31: Phase 1 loaded defaults, steering, repo map, memory, current runtime code, and the active initiative.
- 2026-07-31: Spec evaluation passed with scores 8, 7, 9, and 8. The spec now defines invalid-edge behavior, the Java 21 `javac` branch mapping rule, and explicit-edge precedence.
- Task 1 scope: add compatible success and failure execution state, exact edge validation, invocation-local nested dispatch state, and unit contracts while preserving current successful and concurrent capture behavior.
- 2026-07-31: Task 1 completed. Added compatible success and failure terminal state, graph-validated exact edge capture, generic failed records, and invocation-local LIFO dispatch expectations. Modified the runtime model, collector, bridge, explanation projector, and their unit contracts. Maven packaging, all engine contracts, concurrency checks, and the load contract passed. The full verification then stopped at the agent contract that still expects failed calls to be dropped; Task 2 owns that integration-contract update.
- Task 2 scope: add compatibility-safe branch bindings, emit exact true and false edge probes, install one entry-boundary failure handler, preserve explicit edges during path resolution, update integration contracts, and document the runtime behavior. Keep the safe legacy probe when a complete binding is absent.
- 2026-07-31: Task 2 completed. Added compatible branch bindings, exact Java 21 `javac` branch trampolines, one entry-boundary failure handler, explicit-edge precedence, analyzer and agent contracts, and runtime documentation. The full verification passed with 5,000 completed load-test records, zero errors, zero mismatches, and zero dropped records.
- Task 2 remediation scope: identify each predicate jump by its method occurrence, emit an edge only when that jump completes the full predicate, retain the legacy fallback for unsupported compound shapes, and add analyzer-to-transformer `&&` and `||` contracts.
- 2026-07-31: Fast-forwarded to `main` commit `2e0e768`, re-read `AGENTS.md`, and reloaded steering and memory. The repository now includes the Maven plugin baseline and prohibits subagents.
- 2026-07-31: Adapted the spec with Task 3. The proposed solution uses method-local predicate occurrences, all-or-none completion modes, one-sided short-circuit trampolines, and a legacy fallback for unsafe compound shapes. Implementation remains in progress and is not accepted.
- 2026-07-31: Task 3 completed. Added occurrence-aware short-circuit plans, partial-plan rejection, conservative ternary and switch-expression fallback, lambda fallback, constant-time edge validation, full conjunction and disjunction matrices, side-effect checks for short-circuit behavior, and current-main verification.
- 2026-07-31: Direct implementation review passed. No subagent was used. Full verification reported 0.150% p95 overhead, 5,000 completed records, and zero errors, mismatches, drops, or contamination.

## Phase 2 Completion Summary

- Key requirements: record exact true and false edges, retain failed invocations, preserve original exceptions, and isolate nested dispatch state.
- Design decisions: branch-target metadata, branch-site edge events, one entry-boundary failure handler, and invocation-local dispatch stacks.
- Task breakdown: Task 1 adds runtime and model contracts. Task 2 adds analysis metadata, bytecode probes, integration tests, and documentation. Task 3 makes compound branch completion exact and constant-time at runtime.
- Dependencies: the completed walking skeleton, Java 21, and the existing ASM 9.10.1 dependency; no new package.

## Phase 3 Completion Summary

- Tasks completed: 3 of 3.
- Files modified: execution model, runtime context and collector, analysis manifest and builder, static analyzer, transformer, resolver, tests, scripts, and documentation.
- Deviation: replaced per-jump full bindings with occurrence-aware all-or-none completion plans after the first implementation evaluation found contradictory compound edges.
- Verification: `./scripts/verify.sh` passed on current `main`, including Maven plugin integration, concurrency, diagrams, and the 5,000-invocation load contract.
