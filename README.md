# Fachtracing

Fachtracing is an embeddable Java 21 library that learns a business-decision graph from an
unknown method marked with `@FachTracing`, correlates one runtime path with opaque graph IDs,
and stores a typed explanation of what was decided, why, and how.

Every generated decision has one business `Start` and one `Stop`. All normal and exceptional
paths converge on that `Stop`; normal terminal edges say what is returned. Technical identifier
suffixes are removed, while result-relevant Java null checks are expressed as the business facts
“absent” or “exists.”

This repository is the walking skeleton for the broader generic extractor. It proves the full
flow across eligibility, pricing, and polymorphic strategy fixtures without domain-specific
analyzer rules. It does not yet claim complete Java-language coverage; unsupported
result-relevant constructs are retained as visible coverage gaps instead of being silently
omitted.

## Maven quick start

For static business diagrams, add `fachtracing-api`, annotate decision methods, and run:

```sh
mvn compile at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-SNAPSHOT:analyze
```

The module receives a Markdown index plus Mermaid and PlantUML diagrams under
`target/fachtracing`. Maven supplies the source roots and complete compilation classpath; no
analysis launcher, plugin block, or path assembly is required. See the copyable
[Maven plugin setup](docs/maven-plugin.md), including automatic lifecycle and parent-POM usage for
adding many modules.

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

No database or filesystem operation occurs in injected probes. Probe failures are suppressed
from application control flow and exposed separately through `TraceRuntime.pollDiagnostic()`.

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
