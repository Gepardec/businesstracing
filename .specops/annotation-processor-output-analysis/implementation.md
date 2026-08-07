# Implementation Journal: Analyze Annotation-Processor Output

## Summary

Completed six tasks across four versions. The Maven adapter accepts projects that use annotation
processors, removes all Java 21 processor-only compiler controls from the analysis model, and keeps
`-proc:none` in the private compiler task. Both Maven goals now use one generated-source discovery
rule for developer provenance, including configured roots outside the Maven build directory. A real
two-module processor fixture proves generated Java extraction through both Maven goals. The fixture
processor disables processing only while it compiles itself, so clean builds behave consistently on
Java 21 and later releases. Maven `source` and `target` now keep their running-JDK API surface, while
an explicit `release` remains strict. Focused, standard, and complete PR verification pass. No new
dependency was added.

## Phase 1 Context Summary

- Config: SpecOps 1.8.0 defaults; library vertical; `.specops`; no external task tracking.
- Context recovery: No incomplete spec exists.
- Steering files: Loaded product, technology, structure, reference application, dependencies, and
  the generated repository map.
- Repo map: Fresh at Phase 1 start.
- Memory: Loaded prior build-tool-adapter, compiler-boundary, and fail-closed analysis decisions.
- Vertical: Brownfield Java library.
- Affected files: Maven compiler-model resolver, executable plugin contracts, Maven integration
  fixtures, standard verification script, and Maven plugin documentation.
- Project state: Brownfield.
- Scope assessment: One compiler-boundary defect, one verification cluster, and three tasks. No
  decomposition is required.
- Plan validation: Pass. All existing file paths resolve, and each planned fixture path is marked as
  a new file.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Consume generated Java after Maven compile and never run processors in Fachtracing. | The engine already owns a deterministic `-proc:none` attribution task. | 2 | 2026-08-07T08:43:56Z |

## Documentation Review

- `docs/maven-plugin.md`: Updated. It gives the required same-invocation command and states that
  Fachtracing consumes generated source without executing processors.
- `docs/supported-java-constructs.md`: Updated. It states the source-generating support boundary and
  the AST-only attribution limit.
- Version 2 review: Both Maven documents remain accurate after location-independent generated-root
  provenance and complete Java 21 processor-option sanitization.
- Version 3 CI review: No user documentation changed. The correction only affects bootstrap of the
  test processor module.
- Version 4 review: `docs/maven-plugin.md` now states the difference between Maven `release` and
  `source` plus `target` during attribution.
- `README.md`: Checked. It does not describe effective compiler-model details and needs no change.
- `AGENTS.md`: Checked. No project instruction changed.

## Session Log

- 2026-08-07T08:39:42Z: Started the medium-severity bug-fix workflow. Confirmed that Hogajama uses
  MapStruct through `annotationProcessorPaths` and generates normal Java source.
- 2026-08-07T08:43:56Z: Coherence, split detection, reference validation, dependency introduction,
  dependency safety, and spec evaluation passed. No new dependency is introduced.
- 2026-08-07T08:45:00Z: Task 1 anchor. Freeze configuration sanitization and a complete Maven
  generate-then-analyze flow. The task must fail against the current resolver before production
  code changes. Required proof: generated Java exists, analysis reaches the current rejection, and
  no test changes weaken unrelated compiler checks.
- 2026-08-07T08:47:20Z: Task 1 completed. The focused resolver contract fails on configured
  `proc=full`. The two-module Maven fixture compiles its processor, generates
  `GeneratedApprovalPolicy.java`, and then fails at the same effective-model rejection reported by
  Hogajama.
- 2026-08-07T08:47:20Z: Task 2 anchor. Change only the analysis projection of Maven compiler
  settings. Required proof: processing configuration and arguments are absent, `-Xlint:none`
  remains, forked compilers still fail, and the generated decision graph is extracted.
- 2026-08-07T08:50:09Z: Task 2 completed. The resolver ignores configuration-level processing
  controls and removes `-A`, processor selection/path/module-path, processing mode, and processor
  diagnostic arguments. Safe arguments remain ordered and unique. The focused executable contract
  and the generated-source Maven reactor pass. The graph contains `request age is at least 18`.
- 2026-08-07T08:50:09Z: Task 3 anchor. State the exact post-compile support boundary and run the
  complete standard verifier. Required proof: docs distinguish generated Java from AST-only
  transformations, the new fixture runs in `scripts/verify.sh`, and all existing tests pass.
- 2026-08-07T08:53:48Z: Task 3 completed. Documentation defines the post-compile, `-proc:none`
  boundary and the AST-only limit. `scripts/verify.sh` passes both per-module and aggregate generated
  decision extraction, all existing executable contracts, external release activation, and 5,000
  decisions at 1,000 RPS with 0.321% p95 overhead. PostgreSQL was skipped because no connection is
  configured.
- 2026-08-07T08:54:34Z: Implementation evaluation passed all four dimensions. All six bug-fix
  acceptance criteria and all task criteria are verified.
- 2026-08-07T08:55:30Z: Updated project memory and refreshed the repository map. Documentation
  review is complete.
- 2026-08-07T08:56:32Z: Captured completion metrics and marked the spec completed.
- 2026-08-07T09:14:50Z: Reopened version 2 for review remediation. Task 4 anchor: remove the official
  default-module processor option in both forms and preserve generated provenance for configured
  roots outside the Maven build directory. No new dependency or architecture change is required.
- 2026-08-07T09:19:35Z: Task 4 completed. The focused Maven plugin contract and `./scripts/verify.sh`
  pass. The verifier covered both Maven goals, processor-generated Java, developer export contracts,
  external release activation, and 5,000 decisions at 1,000 RPS with 0.309% p95 overhead. PostgreSQL
  was skipped because no connection is configured.
- 2026-08-07T09:20:07Z: Version 2 implementation evaluation passed. Documentation, memory, metrics,
  repository map, and the run log were refreshed.
- 2026-08-07T09:26:36Z: Reopened version 3 after the Java 21 pull-request job reproduced a clean
  processor-module bootstrap failure. Task 5 anchor: disable processing only for compilation of the
  processor implementation, then prove the application still executes it. No production code or
  dependency changes are required.
- 2026-08-07T09:29:15Z: Task 5 completed. The focused clean fixture and `./scripts/verify-pr.sh`
  pass under Java 21. The application generates `GeneratedApprovalPolicy.java`; both Maven goal
  paths extract its decision. External release activation and all five Mega graphs pass. The short
  load completed 5,000 decisions at 1,000 RPS with 0.269% p95 overhead and zero errors, mismatches,
  drops, or contamination. PostgreSQL was skipped because no connection is configured.
- 2026-08-07T09:45:08Z: Reopened version 4 after Hogajama generated sources failed to resolve
  `javax.annotation.processing.Generated`. Task 6 anchor: preserve Maven `source` and `target` as
  `-source` and `-target`, keep explicit `release` strict, and prove attribution through the real
  compiler task. No dependency is required.
- 2026-08-07T09:53:33Z: Task 6 completed. The focused analyzer and resolver contracts pass. The
  two-module fixture compiles generated Java with Java 8 `source` and `target`, including
  `javax.annotation.processing.Generated`, and aggregate extraction succeeds. The complete PR
  verifier passes all five Mega graphs and 5,000 decisions at 1,000 RPS with 0.251% p95 overhead
  and zero errors, mismatches, drops, or contamination. PostgreSQL was skipped because no
  connection is configured.
