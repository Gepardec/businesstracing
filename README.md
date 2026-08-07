# Fachtracing

Fachtracing is an embeddable Java 21 library that learns a business-decision graph from an
unknown method marked with `@FachTracing`, correlates one runtime path with opaque graph IDs,
and stores a typed explanation of what was decided, why, and how.

Every generated decision has one business `Start` and one `Stop`. All normal and exceptional
paths converge on that `Stop`; normal terminal edges say what is returned. Technical identifier
suffixes are removed, while result-relevant Java null checks are expressed as the business facts
“absent” or “exists.”

The generic extractor supports exact paths for source-proven structured exception flow,
synchronized business logic, atomic short-circuit predicates, ternaries, Java 21 switches,
proven dynamic candidates, and standard asynchronous boundaries. A controlled fingerprinted
bytecode subset can recover simple source-unavailable Boolean rules. It does not claim every Java
program. Unsupported result-relevant variants stay as source-located coverage gaps.

## Maven quick start

For static business diagrams, add `fachtracing-api`, annotate decision methods, and run:

```sh
mvn compile at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-rc.1:analyze
```

The module receives a Markdown index plus Mermaid and PlantUML diagrams under
`target/fachtracing`. Maven supplies the current module's decision-entry roots plus the active
reactor's source and compile-classpath union. Interface implementations in sibling reactor modules
are therefore available for dispatch resolution, but sibling annotations do not create duplicate
diagrams in the current module. A single-module build uses the same behavior as before. No analysis
launcher, plugin block, or path assembly is required. See the copyable
[Maven plugin setup](docs/maven-plugin.md), including automatic lifecycle and parent-POM usage for
many modules. When repository and source-link settings are present, the same goal also writes
revision-pinned developer JSON that another tool can visualize.

See the [self-tracing dogfood example](docs/self-tracing.md) for a graph that Fachtracing
generates from its own Maven plugin policy.

See [runtime integration](docs/runtime-integration.md) for the verified release-candidate agent,
delivery, JDBC, upgrade, and rollback flow.

## Integration flow

1. Add `fachtracing-api` and annotate a decision entry method with `@FachTracing`.
2. Generate static diagrams with the Maven plugin, or call `FachtracingEngine.analyze` directly for
   custom build-tool integration.
3. Start the JVM with `fachtracing-agent` on `-javaagent`, then configure it early in application
   startup with the returned developer-only manifest and matching compiled-class fingerprints.
   Already loaded selected classes are retransformed when the JVM permits it.
4. Register a `DecisionValueCodec` with an application-owned redaction policy through
   `FachtracingEngine.activate`.
5. Drain completed captures asynchronously with `saveNext`; implement
   `DecisionRecordRepository` for the application's database.
6. Retrieve the immutable record to obtain the business explanation and structural/execution
   PlantUML and Mermaid source.
7. For developer tooling, export the analysis result as versioned JSON with revision-pinned source
   links. This artifact is separate from the source-free business record.

No database or filesystem operation occurs in injected probes. Probe failures are suppressed
from application control flow and exposed separately through `TraceRuntime.pollDiagnostic()`.
For Java 21 classes from `javac`, a complete Boolean branch binding records the exact opaque
`true` or `false` graph edge. An incomplete exact binding creates a located coverage gap. If an
exception leaves an instrumented entry method, the collector creates one generic failed record
and rethrows the same exception object. The failed record contains no exception type, message,
or stack trace.

## Visualization exports and source navigation

PlantUML and Mermaid are formats for business diagrams. Developer tools can use the
`fachtracing-developer-graph/v1` JSON data format. It contains `nodes`, `edges`, stable opaque IDs,
coverage gaps, and developer source data.

Capture Git source data from a clean working tree. Supply a source-browser URL template:

```java
import at.gepardec.fachtracing.developer.DeveloperGraphExporter;

var revision = DeveloperGraphExporter.SourceRevision.captureGit(
        repositoryRoot,
        "https://github.com/acme/decision-rules",
        "https://github.com/acme/decision-rules/blob/{commit}/{path}#L{line}");

String json = new DeveloperGraphExporter().export(analysis, revision);
```

An external tool renders `graph.nodes` and `graph.edges`. When the user selects a node with a
`source` object, it opens `source.url`; that URL contains the analyzed commit, repository-relative
path, and line. `source.sha256` identifies the analyzed file content. Before export, Fachtracing
compares each source file with this fingerprint. It stops if the file content changed. Synthetic
nodes have no `source` object because Fachtracing does not make false code locations.

`captureGit` rejects tracked or untracked working-tree changes. Commit the analyzed code before
exporting so the source URL and line number refer to exactly the code represented by the graph.
You can use the template with GitHub, GitLab, Bitbucket, or an internal browser. The template must
contain `{commit}` and `{path}`. It can also contain `{line}` and `{column}`.

Repository coordinates are developer data. They never enter `BusinessDecisionGraph`, decision
explanations, persisted decision records, Mermaid, or PlantUML.

## Verification

Run all dependency-free executable contracts:

```sh
./scripts/verify.sh
```

Run the pinned realistic-brownfield conformance harness (optionally set `MEGA_BACKEND_DIR`):

```sh
./scripts/verify-mega-backend.sh
```

Its approved immutable oracles, exact graph evidence, runtime path, and anti-overfitting checks are documented in
[the Mega conformance report](conformance/mega-backend/conformance-report.md).

Run the required load comparison (60 total disabled baseline seconds paired across ten minutes
enabled at 1,000 requests/second):

```sh
JAVA21="$(/usr/libexec/java_home -v 21)/bin/java"
CP="fachtracing-api/target/classes:fachtracing-engine/target/classes:fachtracing-engine/target/test-classes"
"$JAVA21" -ea --add-modules jdk.compiler -cp "$CP" \
  at.gepardec.fachtracing.performance.FachtracingLoadTest
```

The harness uses a deterministic 10 ms representative application decision latency, interleaves
baseline/enabled windows to control machine-state drift, validates every captured result/evidence
pair, and reports throughput plus aggregate p50/p95 latency. Adjust its
arguments to characterize a different application workload; the 10% target is relative to the
measured application request, not a claim about probe-only nanoseconds.

See [supported Java constructs](docs/supported-java-constructs.md) and the checked-in
[PlantUML architecture flows](docs/plantuml/). Decision records provide both PlantUML and
Markdown-native Mermaid projections. The latest reproducible measurement is recorded in
[performance results](docs/performance-results.md).
