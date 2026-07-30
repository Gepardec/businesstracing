# Implementation Journal: Generic Fachtracing Walking Skeleton

## Summary

Version 1 completed six walking-skeleton tasks for a framework-neutral Java 21 Fachtracing library.
The implementation now learns result-relevant graphs from unknown annotated sources, captures
opaque runtime paths and polymorphic edges without application-thread I/O, projects typed
business explanations and PlantUML, and persists records through an application-owned port.
The principal implementation decisions were dependency-free contract tests, strict separation
of developer provenance from business records, and implementation-entry dispatch correlation.
The full verification suite passes; the ten-minute enabled load run completed 600,000 traces at
1,000 RPS with zero integrity failures and 0.043% measured p95 overhead on the documented
workload. Wider Java-language coverage remains explicitly represented as coverage gaps and is
assigned to the initiative's follow-on specifications.

Version 2 completes the mandatory pinned `Gepardec/mega-backend` proof. Five independently
source-reviewed business graphs across four areas match immutable exact topology oracles; the
72-node/89-edge journey-warning manager graph resolves three generic strategy implementations and
a real invocation captures the typed collection input/result plus all selected opaque edges.
Source-line probe correlation prevents unrelated JVM branches from becoming reasons, and the
execution explanation/PlantUML contain only business-facing content. The unchanged non-Mega suite
passes, and the final phase-balanced ten-minute gate completed 600,000 traces at 1,000 RPS with
0.267% p95 overhead and zero integrity failures.

## Phase 1 Context Summary

- Config: no `.specops.json`; defaults used — `specsDir: .specops`, vertical inferred as `library`, task tracking `none`, review disabled, evaluation enabled
- Context recovery: none; this is the first spec
- Steering files: loaded 6 files (`dependencies.md`, `product.md`, `reference-application.md`, `repo-map.md`, `structure.md`, `tech.md`)
- Repo map: generated; the local Fachtracing project has no source files yet
- Memory: no memory files found
- Vertical: library
- Affected files: new Maven modules under `fachtracing-api/`, `fachtracing-engine/`, and `fachtracing-agent/`; root `pom.xml`; `README.md`; `docs/`
- Project state: greenfield library with `Gepardec/mega-backend` as one brownfield validation corpus
- Reference context: `Gepardec/mega-backend` reviewed at commit `782cdec8dfe5b4062eb5c1859e6a9e53afe02770`; no source was copied and no target-specific behavior is permitted
- Scope assessment: decomposition approved through iterative interview correction; created initiative `generic-java-fachtracing` with five specs in three waves
- Coherence check: pass — the 1,000-RPS, ten-minute, and 10% p95 targets are consistent across requirements, design, and tasks
- Vocabulary check: pass — library vocabulary uses developer use cases, modules, and public API contracts
- Plan validation: pass — all implementation paths are explicitly new files in this greenfield project

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
|---|----------|-----------|------|-----------|
| 1 | Use plain-Java executable contract tests | The approved dependency set contains no test framework; assertion-based test mains exercise behavior without expanding the dependency surface. | 1 | 2026-07-10T14:12:00Z |
| 2 | Keep compiler provenance outside business records | Source locations and Java owner/member hints are required for instrumentation, but business graphs retain only opaque IDs and business labels. | 2 | 2026-07-24T07:57:43Z |
| 3 | Correlate dispatch at implementation entry | The call site cannot generically duplicate receivers beneath arbitrary JVM arguments without heavier data-flow machinery. Candidate implementation entries therefore emit the precomputed opaque edge ID while an active graph context verifies that the edge belongs to it. | 3 | 2026-07-24T08:05:00Z |
| 4 | Make Mega a mandatory opaque conformance corpus | A realistic brownfield application must prove generality, while strict test-only oracle boundaries and same-artifact non-Mega regressions prevent a Gepardec-specific implementation. | 7 | 2026-07-24T08:47:01Z |
| 5 | Correlate implementation edges with an expected dispatch call site | Real source can invoke the same interface member many times; thread-local expectation prevents one implementation entry from selecting unrelated dispatch nodes. | 7 | 2026-07-24T12:00:00Z |
| 6 | Preserve unavailable dependency logic as a coverage gap | The external holiday library source was not supplied; claiming its internal business decision as complete would fabricate an explanation. | 7 | 2026-07-24T12:10:00Z |
| 7 | Use the fully source-visible journey-warning manager as the polymorphic Mega proof | Its three strategies exercise real brownfield dispatch without treating binary-only dependency logic as understood. | 7 | 2026-07-24T09:20:00Z |
| 8 | Bind predicate probes to manifest source lines | Matching all conditional bytecode by sequence produced false reasons; line provenance remains developer-only and safely correlates only source-derived predicate sites. | 7 | 2026-07-24T09:45:00Z |
| 9 | Interleave long-load baseline and enabled windows | Adjacent paired windows preserve the same 60/600-second totals and 1,000-RPS enabled load while preventing one-way phase-order machine drift from distorting the p95 comparison. | 7 | 2026-07-24T10:15:00Z |

## Documentation Review

| Document | Status | Review result |
| --- | --- | --- |
| `README.md` | Updated | Describes integration, verification, performance command, and walking-skeleton boundary. |
| `docs/supported-java-constructs.md` | Updated | Matches implemented constructs, explicit gaps, failure behavior, and redaction requirements. |
| `docs/performance-results.md` | Updated | Records exact environment, duration, throughput, latency, and integrity evidence. |
| `docs/plantuml/*.puml` | Updated | Covers extraction, runtime correlation, explanation construction, and the decision-record model. |
| `.specops/generic-tracing-walking-skeleton/dependency-audit.md` | Updated | Reflects the completed Maven reactor and approved ASM dependency. |
| `conformance/mega-backend/README.md` | Updated | Documents immutable oracle verification and reproduction. |
| `conformance/mega-backend/selection.md` | Up-to-date | Records the pinned revision, business areas, entry points, and source-visible strategy rationale. |
| `conformance/mega-backend/conformance-report.md` | Updated | Links exact oracles, generated graphs, runtime evidence, generic regressions, and approval outcome. |
| `conformance/mega-backend/src/test/resources/oracles/README.md` | Updated | Records the independent derivation procedure, checks, immutable hashes, and approvals. |

## Session Log

- 2026-07-10: Interview completed, reference application corrected to `Gepardec/mega-backend`, and product boundary refined to generic extraction of previously unknown Java decisions.
- 2026-07-10: Phase 3 gates passed: no required spec dependencies, review is disabled, task tracking is `none`, and all dependency introduction decisions are present in `design.md`.
- Task 1 scope: establish a framework-neutral Java 21 Maven build, annotation and value extension API, immutable opaque-ID graph/execution/explanation models, and an in-memory storage port; verify built-in/custom values, rejection, redaction, and API boundaries.
- Task 1 completed: created the Java 21 Maven parent, framework-neutral API, immutable record model, value codec/redaction boundary, and thread-safe in-memory repository. Maven compilation and executable contract tests passed on JDK 21; `jdeps` confirmed the API module depends only on `java.base`.
- Task 2 scope: analyze arbitrary annotated Java 21 source through attributed compiler trees, derive result-relevant control/data dependencies and call relationships, build opaque graph nodes/edges, and make unsupported relevant constructs explicit in developer diagnostics and graph completeness.
- Task 2 completed: attributed compiler trees discover annotated methods, backward slicing excludes result-independent work, supported direct calls are followed, all known interface implementations contribute candidate subgraphs, and unsupported result-relevant loops produce explicit incomplete graphs and source diagnostics. The executable contracts pass unchanged across eligibility, pricing, strategy, and aggregation fixtures.
- Task 3 scope: add the approved ASM-only Java agent boundary, verify analyzed source fingerprints before transformation, inject failure-safe opaque-ID probes, and prove ordered thread-isolated runtime capture including the concrete polymorphic edge selected by an invocation.
- Task 3 completed: added the ASM 9.10.1 agent, fingerprint-gated transformation, failure-safe entry/predicate/dispatch/outcome/exception probes, implementation-entry dispatch correlation, and a thread-local in-memory collector. Executable tests prove transformed return/exception transparency, mismatch rejection, actual local/regional edge selection, 32-thread isolation, and zero synchronous persistence I/O.
- Task 4 scope: deterministically project graph plus ordered observations into a typed what/why/how explanation, enforce redaction at the record boundary, and render structural and execution PlantUML with explicit coverage status and no Java provenance.
- Task 4 completed: added deterministic business-statement and explanation projection, typed text output, full-graph and execution-highlighted PlantUML, explicit incomplete-coverage notes, and snapshots covering observed age/location values. Built-in value kinds, redaction, missing adapters, unknown observations, and prohibited technical-language checks pass.
- Task 5 scope: compose analysis, capture, explanation, storage, and PlantUML behind one public facade and prove the unchanged pipeline across eligibility, pricing, and strategy decisions, including retrieval and explicit degraded outcomes.
- Task 5 completed: the public facade now analyzes, activates capture, projects, renders, saves, and retrieves immutable records. Freshly compiled eligibility, pricing, and strategy fixtures pass the same orchestration flow; retrieved records reproduce explanation and PlantUML, polymorphic output contains only the selected opaque business branch, and incomplete/capture-failure scenarios remain explicit without changing application results.
- Task 6 scope: add a controlled enabled/disabled load harness, execute the specified ten-minute 1,000-RPS comparison, record latency/integrity evidence, and document integration, supported/gap constructs, redaction, failure behavior, plus project-wide PlantUML flows and record model.
- Task 6 completed: the rate-controlled harness sustained 1,000 enabled invocations/second for 600 seconds (600,000 records) after a 60-second disabled baseline. Measured p95 overhead was 0.043% with zero errors, mismatches, dropped traces, or cross-trace contamination. Added integration and supported-construct documentation, exact performance evidence, and PlantUML for extraction, runtime correlation, explanation, and the record model.
- Post-task audit: corrected fall-through return topology, removed sequential outcome-to-return edges, joined every direct-callee result path to the caller outcome, normalized Java call notation in business labels, and made source-unavailable decision logic explicitly incomplete. Regression contracts cover each correction.
- Completion audit: corrected Java-agent startup so `premain` retains instrumentation and application startup can install the configured transformer and retransform already loaded selected classes. A proxy-instrumentation contract verifies the lifecycle.
- Scope version 2: reopened completion and added Task 7. Its scope is to pin Mega, select and independently review representative decisions, compare exact semantic topology, capture an actual polymorphic path, publish business-facing artifacts, prohibit Mega hints in production/configuration, and rerun non-Mega domains with the identical artifact.
- Task 7 implementation start: obtain the pinned Mega source as read-only conformance input; select non-trivial decisions by structural/domain coverage; establish reviewed semantic oracles before analyzer remediation; keep all repository-specific paths, names, invocations, and expectations inside `conformance/mega-backend`; add only construct-level generic capabilities to production modules.
- Task 7 completed: the pinned/overlay run analyzes 420 Mega sources and five annotated decisions into complete graphs of 12/11, 5/4, 72/89, 7/6, and 17/20 nodes/edges. Immutable independently source-derived oracles match exactly. A real journey-manager invocation records the typed empty collection input/result, three selected strategy edges, and only the predicate actually evaluated; its explanation and highlighted PlantUML pass artifact-level technical-language guards. Generic fixtures cover every construct added from corpus findings, and production/configuration scans contain no Mega hint. Full same-artifact regression and the phase-balanced 600-second enabled load pass with 0.267% p95 overhead and no integrity failures.

## Phase 3 Completion Summary

- Tasks completed: 7 of 7
- Primary outputs: three Java 21 modules, generic attributed-AST analysis, ASM probes, isolated runtime collection, typed explanations, PlantUML, orchestration/storage facade, performance harness, and integration documentation
- Files modified: new sources under `fachtracing-api/`, `fachtracing-engine/`, and `fachtracing-agent/`; root Maven build; verification script; README and docs; SpecOps artifacts
- Deviations: no dependency or module-boundary deviation; predicate operand expansion and broader unsupported Java constructs remain in the explicitly decomposed follow-on specs
- Verification: `./scripts/verify.sh` and `./scripts/verify-mega-backend.sh` pass; five exact Mega oracles and runtime artifacts pass; API `jdeps` boundary passes; the final ten-minute 1,000-RPS enabled run passes at 0.267% p95 overhead with zero integrity failures

## Phase 2 Completion Summary

- Key requirements: one annotation, domain structure learned through static analysis, backward result slicing, runtime polymorphic-path capture, typed final results, deterministic what/why/how explanations, PlantUML, explicit coverage gaps, and 1,000-RPS verification.
- Design decisions: Java 21 attributed Tree API for source analysis; relevance-based technical filtering; opaque node IDs; ASM probes for runtime correlation; separate developer source maps and business records; deterministic explanation templates.
- Task breakdown: six ordered tasks covering contracts, static analysis, runtime instrumentation, explanation/PlantUML, end-to-end proof, and performance/documentation.
- Dependencies: Java 21 `jdk.compiler` API and `org.ow2.asm:asm:9.10.1`; no application framework, parser, database, serializer, or PlantUML runtime dependency.
