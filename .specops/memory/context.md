# Project Memory

## Completed Specs

### generic-tracing-walking-skeleton

Completed version 2 on 2026-07-24. The framework-neutral Java 21 library derives typed
business-decision graphs from `@FachTracing` methods, correlates source-line-bound runtime probes
and polymorphic edges, stores typed/redacted evidence without arbitrary object stringification,
and produces business explanations plus structural/execution PlantUML.

The mandatory opaque brownfield proof uses pinned
`Gepardec/mega-backend@782cdec8dfe5b4062eb5c1859e6a9e53afe02770`. Five complete graphs
across four areas exactly match immutable independently source-derived oracles. A real
journey-warning manager execution records an empty typed collection input/result and all three
selected strategy edges. Production modules/configuration contain no Mega-specific hints.

Final verification passed the generic suite, pinned brownfield suite, and 600-second enabled load:
600,000 traces at 1,000 RPS, 0.267% p95 overhead, zero errors, mismatches, drops, or contamination.

Important follow-on constraint: new Java coverage must remain construct-level and target-neutral;
unsupported relevant constructs must create visible coverage gaps rather than guessed topology.

### mermaid-diagram-rendering

Completed on 2026-07-24. Decision records now provide deterministic structural and execution
Mermaid source alongside PlantUML. Both formats share one execution-path resolver, so observed and
inferred connecting edges are highlighted consistently. Mermaid output has stable aliases,
business-only labels, escaped sensitive characters, and explicit incomplete-coverage content.
Mega conformance generates five structural `.mmd` files and one highlighted runtime `.mmd` file.

### business-graph-terminal-semantics

Completed on 2026-07-24. Every generated decision uses exactly one Start and one Stop; return
edges state the returned expression and exceptional paths, including failures inside expanded
callees, converge on Stop. Standalone id/ids vocabulary is stripped generically. Result-relevant
null comparisons remain meaningful as absent/exists while raw Java null never reaches business
artifacts. Generic and pinned Mega verification pass; the journey-warning graph is 72 nodes and
90 edges after its nested validator failure was connected to Stop.

### maven-project-analysis

Completed on 2026-07-31. Maven projects can add `fachtracing-api`, annotate decisions, and run a
fully qualified `fachtracing-maven-plugin:analyze` goal without plugin configuration. Projects can
optionally bind the goal to `process-classes`, making normal `mvn process-classes`, `package`, or
`verify` generate per-module Mermaid, PlantUML, and `index.md` output under `target/fachtracing`.
Maven source roots and resolved classpaths are discovered automatically; empty reactor modules skip,
strict incomplete coverage is configurable, and stale generated graphs are safely removed. Generic
and pinned Mega conformance remain green.

### developer-graph-json-export (feature) — 2026-07-31

Completed 2 tasks. Fachtracing now emits a deterministic schema-v1 JSON graph for developer tools.
The export contains stable topology, clean Git commit metadata, repository-relative locations,
source fingerprints, and commit-pinned links. Business graphs, records, Mermaid, and PlantUML stay
free of repository data. A final correction made source fingerprints an enforced check instead of
passive evidence. All executable contracts pass.

### maven-developer-graph-export (feature) — 2026-07-31

Completed 4 tasks. The Maven plugin can now write one opt-in UTF-8 `*-developer.json` artifact per
decision and link it from `index.md`. Diagram-only builds remain Git-free. Configured export captures
one clean revision and proves that every analyzed source matches the exact blob in that commit, so
ignored generated files cannot create false source links. A separate test parser verifies the full
artifact and non-empty coverage gaps. The missing Maven guide now documents the workflow. Full
verification passed with 0.146% p95 overhead and zero errors, mismatches, drops, or contamination.

### runtime-decision-path-capture

Completed on 2026-07-31. Runtime executions now store validated opaque branch edges, generic failed
terminal state, and invocation-local nested dispatch expectations. Java 21 `javac` predicates use
compile-time occurrence and completion metadata, so runtime probes only record which precomputed
edge completed the full predicate. Flat conjunctions and disjunctions record one exact edge;
ambiguous compound forms use legacy observations. Registered graph edges are pre-indexed for
constant-time validation. Full verification passed at 1,000 requests per second with 0.150% p95
overhead and zero errors, mismatches, drops, or contamination.

### reactor-wide-implementation-resolution (feature) — 2026-07-31

Completed 3 of 3 tasks. The analyzer separates current-module graph roots from the full attributed
source universe, and the Maven plugin supplies stable reactor-wide source and classpath inputs.
Publication review moved the source-empty skip before reactor classpath resolution and added
inherited parent-POM verification. Focused contracts, single-module integration, a two-module JPMS
reactor build, and the full repository verifier pass with zero correctness or isolation failures.

### jpms-reactor-source-resolution (bugfix) — 2026-07-31

Completed one bugfix task. Maven source discovery omits module descriptors from the analyzer input,
while Maven still compiles each descriptor and enforces JPMS boundaries. A valid two-module JPMS
fixture generates one current-module graph with both sibling candidates, and the full verifier
passes.

### generic-application-readiness (feature) — 2026-07-31

Completed version 4 on 2026-08-05 with 27 tasks. Fachtracing has project-aware source boundaries,
safe external source inputs, multi-origin developer provenance, one aggregate reactor goal,
isolated flat, JPMS, and mixed compiler contexts, bounded runtime diagnostics, explicit async
context propagation, a verified Java capability matrix, deterministic V1 records, bounded delivery,
and a vendor-neutral JDBC adapter.

Remediation version 4 adds UUID-namespaced execution IDs and strict content-aware idempotency for
both durable keys. A timed-out or interrupted active save is `unknown`, stops its delivery worker,
and cannot be falsely counted as dropped. Activation V3 carries exact JVM descriptors for normal,
branch, dispatch, and lambda bindings, while V2 remains readable. The graph-entry project now
selects flat or JPMS analysis in a mixed reactor; unavailable cross-mode source logic becomes an
explicit coverage gap.

Release commit `facd1daf052f4e3ffae42c48a876dc46e4dd9576` passed the isolated external project,
five complete pinned Mega graphs from 420 source files, and the clean-clone long gate. The gate
completed 600,000 decisions at 1,000 RPS with 0.078% p95 overhead and zero errors, mismatches,
drops, or contamination.

### generic-java-extractor-completion (feature) — 2026-08-05

Completed all 12 tasks. The generic analyzer now supports structured exception and synchronized
flow, exact atomic Boolean and switch paths, proven dynamic candidates, a fingerprinted bytecode
subset, automatic standard asynchronous context propagation, and owned mixed-JPMS sources.
Unsupported variants remain actionable gaps; no Mega-specific rule entered production code.

PostgreSQL 18.4, source-free Activation V3, and five complete Mega graphs passed. The clean-clone
gate completed 600,000 decisions at 1,000 RPS with 6.126% p95 overhead and zero errors, result
mismatches, dropped records, or trace contamination.

### release-explanation-async-correctness (bugfix) — 2026-08-05

Completed all seven tasks. Release output capture now preserves the producer status and evidence,
and all verification scripts use declared POSIX tools. Static analysis binds only proven,
result-relevant operands to predicate nodes, so explanations include typed business facts and do
not store unrelated method identifiers.

Asynchronous instrumentation uses exact JDK owner, method, descriptor, and callback-position
bindings. Atomic reservation states close rejection and cancellation paths exactly once. Canonical
indexed collection loops now use business iteration vocabulary, and a generic export guard rejects
technical counter and ordinal candidate text.

Release commit `280368d58263c045a01e6879769dac89f820a220` passed source-free activation, five
complete Mega graphs, and 600,000 decisions at 1,000 RPS with 0.054% p95 overhead and zero errors,
mismatches, dropped records, or trace contamination.

### ci-isolated-maven-repository (bugfix) — 2026-08-06

Completed all four tasks. Verification scripts now resolve one Maven repository path for local,
release, and explicit use. Manual test launchers use Maven-resolved dependency classpaths, so a
warm home cache cannot hide missing or stale transitive artifacts. The resolver also normalizes
the repeated separator produced by the trailing slash in macOS `TMPDIR`.

Hosted monitoring found that the cold macOS release job reached its old 35-minute limit. The job
now has a bounded 60-minute budget. Standard verification enforces a minimum of 50 minutes so the
clean builds, Mega analysis, and 600-second load gate have time to finish.

Release commit `defe774040f5a5604caffc15838519fd753c0db2` passed source-free activation, five
complete Mega graphs, and 600,000 decisions at 1,000 RPS with 0.059% p95 overhead and zero errors,
mismatches, dropped records, or trace contamination.

### runtime-evidence-async-identity-correctness (bugfix) — 2026-08-06

Completed all five tasks. Automatic async completion now uses the exact prepared callback handle,
and Thread constructors bind the handle to the actual Thread object. Future, CompletableFuture,
and ForkJoinTask cancellation keeps the original result object and releases a pre-start reservation
once. Direct parameter facts are read at each predicate branch. Unavailable or unsafe required facts
make the execution incomplete with a source-located gap.

One generic label normalizer and artifact guard remove Java construction, enum-type, and helper-role
terms without Mega-specific rules. Release commit `d9a1d4cc7c38a604b41ce91ade069ffe3ca50de2`
passed source-free activation, five complete Mega graphs, and 600,000 decisions at 1,000 RPS with
0.054% p95 overhead and zero errors, mismatches, dropped records, or trace contamination.

### stage-lifecycle-evidence-label-correctness (bugfix) — 2026-08-06

Completed all four tasks. Returned CompletionStage objects now release a reservation when normal
or exceptional completion skips the callback. Typed operand spilling supports every exact callback
position. One fingerprinted class pass observes cancel calls in methods without graph probes.

Direct method-parameter receivers create exact predicate or return evidence. Unsupported value
receivers create source-located gaps. Validation helper cleanup uses new-object and validate-only
call roles, so unrelated business uses of `validator` keep their meaning.

Release commit `9690240f3810a043f52af5af65d17818c4473cfb` passed source-free activation, five
complete Mega graphs, and 600,000 decisions at 1,000 RPS with 0.077% p95 overhead and zero errors,
mismatches, dropped records, or trace contamination.

### outcome-evidence-cancellation-slice-label-correctness (bugfix) — 2026-08-06

Completed all four tasks. Terminal observations merge staged receiver facts with the typed result,
and explanations show non-result facts as business reasons. Activation fingerprints exact supported
cancellation callers from compiled application output without scanning dependency artifacts.

The analyzer uses attributed platform mutation contracts and source-proven mutation summaries.
Ignored read-only calls stay outside the result slice. Unknown effects on returned references create
source-located gaps. Validation helper labels use proven roles; other receivers remain distinct.

Release commit `8e62f243850c06fbd16d57c0c807c4177d91c9df` passed source-free activation, five
complete Mega graphs from 420 source files, and 600,000 decisions at 1,000 RPS with 0.051% p95
overhead and zero errors, mismatches, drops, or contamination.

### jdk-mutation-alias-effect-correctness (bugfix) — 2026-08-07

Completed all three tasks. Platform call effects now separate explicit mutation contracts from
proved read-only operations. An unknown JDK or `javax` reference effect fails closed when it can
change returned state. Standard deque operations such as `offer` retain their mutation and control
predicates.

Direct local reference aliases are resolved in source order, invalidated on non-identity assignment,
and mapped back to source parameters. Effect-relevant source helper bodies now receive exact mutation
roots, so their business predicates and writes remain in caller graphs without including unrelated
parameter mutations.

Release commit `e5365f26bdbb52a12f9bb571dcaf8e0e128fc7d4` passed source-free activation, five
complete Mega graphs from 420 source files, and 600,000 decisions at 1,000 RPS with 0.059% p95
overhead and zero errors, mismatches, drops, or contamination.

### fast-pr-ci-pipeline (refactor) — 2026-08-07

Completed all three tasks. Pull requests now use cached Maven dependencies and an immutable pinned
Mega checkout for standard, external activation, short-load, five-graph Mega, and PostgreSQL checks.
The warm hosted PR job passed in 2 minutes 5 seconds; the first cold run took 3 minutes 12 seconds.
The unchanged clean 600-second release gate runs on `main`, version tags, nightly schedules, and
manual dispatches.

### untrack-mega-generated-artifacts (refactor) — 2026-08-07

Completed all three tasks. The five reviewed Mega semantic oracles remain tracked and protected by
immutable hashes. The 18 reproducible diagrams, normalized comparison files, and execution files
now use `conformance/mega-backend/target/generated` and stay outside Git. Repository integrity
prevents a later commit of the former generated-output path. Standalone Mega conformance and the
complete pull-request gate passed with all five graphs complete.

### annotation-processor-output-analysis (bugfix) — 2026-08-07

Completed all six tasks across four versions. Maven projects can now compile with source-generating annotation
processors and then run either Fachtracing Maven goal in the same reactor session. The adapter
removes all Java 21 processor execution controls from its private compiler model, keeps generated
Java source roots, and always analyzes with `-proc:none`. Both Maven goals classify configured
generated roots as generated provenance even when the roots are outside the Maven build directory.
The two-module fixture disables processing while it compiles its own processor, which prevents
Java 21 from loading the service provider before the implementation class exists.
The analyzer also keeps Maven language-selection semantics: explicit `release` uses `--release`,
while equal `source` and `target` values use `-source` and `-target`. This lets Java 8 projects
attribute generated QueryDSL and MapStruct source that imports `javax.annotation.processing.Generated`
when Maven runs on Java 21.

A two-module annotation processor fixture proves per-module and aggregate generated-decision graph
extraction. Standard verification and external release activation pass. The short load completed
5,000 decisions at 1,000 RPS with 0.251% p95 overhead and zero errors, mismatches, drops, or
contamination. AST-only transformations without equivalent Java source remain outside the declared
support boundary.

### self-dogfood-business-tracing (feature) — 2026-08-07

Completed all three tasks. The project now marks its Maven-plugin developer-export policy with
`@FachTracing`, generates its own aggregate Mermaid, PlantUML, index, and activation artifacts,
and verifies them in the normal repository gate. The guide explains the enabled and disabled
results and states that the current result slice omits the direct thrown validation path.

Plexus Utils moved to the advisory-free compatible release 3.6.1, and Maven JAR Plugin moved to
3.5.1. Exact dependency checks and the full repository verifier passed.

### omit-next-diagram-labels (feature) — 2026-08-07

Completed one task. Mermaid and PlantUML now omit the exact `next` arrow label while preserving meaningful outcomes such as `next item`. The graph contract, opaque edge IDs, serialized outcomes, and execution-path logic remain unchanged. Focused renderer contracts, self-tracing, and repository integrity passed.

### context-aware-operation-labels (bugfix) — 2026-08-07

Completed all four tasks. Static labels now use attributed declaration types for short abbreviations,
generic element types for container-named locals, and receiver plus operand context for attributed
platform mutations. The production rules contain no Hogajama package or class knowledge.

Four independent scheduling, pricing, access-control, and inventory applications pass. The complete
Hogajama audit contains none of the known context-free forms. The exact pull-request gate passes with
the external-release fixture and five complete Mega decisions from 420 source files. The short load
processed 5,000 decisions with 0.216% p95 overhead and zero errors, mismatches, drops, or contamination.
