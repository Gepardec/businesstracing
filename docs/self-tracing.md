# Self-Tracing Fachtracing

Fachtracing applies its own tracing flow to three production decisions. These examples connect the
Maven plugin, static analysis, business projection, activation data, the Java agent, and runtime
execution records.

`ProjectGraphGenerator.developerOutput` is the runtime example. It decides if the Maven plugin can
export a developer graph. Two static examples explain the exact-node keep or remove decision and
the source inputs for graph analysis.

## Run the Complete Example

Run:

```sh
./scripts/verify-self-tracing.sh
```

The command runs two passes:

1. Maven analyzes the current reactor and writes static files to `target/fachtracing`.
2. A separate Java 21 process starts with the current agent and executes the traced method three
   times.

The process boundary is important. The static pass must create probe positions and class
fingerprints before the agent can transform a loaded class.

## The Traced Business Decision

The source method has `@FachTracing("enable developer graph export")`. The static pass checks its
current graph.

The repository does not store a copy of this graph. The tool creates it from the current method.
After the self-tracing command finishes, inspect the generated Mermaid:

```sh
cat target/fachtracing/enable-developer-graph-export-structure.mmd
```

The graph has three paths:

- If both settings are absent, export is disabled and the method returns an empty `Optional`.
- If both settings are present, export is enabled and the method returns the developer settings.
- If only one setting is present, the decision cannot continue and the method fails.

## The Exact-Node Keep or Remove Decision

`BusinessGraphProjector.classifyNode` has
`@FachTracing("include exact node in business graph")`. This is the production decision that
returns the final reason for each exact node. `BusinessGraphProjector.projectNode` calls it and then
creates a business rule, action, or gap, or removes the exact node.

This traced method is one focused part of the larger projection flow:

1. `projectWithAudit` collects loop and redundancy context.
2. `projectNode` derives the technical-label flag and calls `classifyNode`.
3. `classifyNode` selects structural, loop, technical, business, or gap reasons.
4. `projectNode` creates or removes the business node from that reason.
5. The projector connects kept nodes and removes nodes that are unreachable from a business entry.

Inspect the generated algorithm and its two audits:

```sh
cat target/fachtracing/include-exact-node-in-business-graph-structure.mmd
cat target/fachtracing/include-exact-node-in-business-graph-analysis-audit.mmd
cat target/fachtracing/include-exact-node-in-business-graph-projection-audit.mmd
```

The exact structure graph shows the actual reason switch. It covers redundant rules, loop
mechanics, loop rules, every exact node kind, the technical flag, and the business alternative. The
projection audit has a different purpose: it shows how the tool projects this classifier's own
exact graph into a business graph.

## The Graph Analysis Source Decision

`AnalysisSourceSelector.select` has
`@FachTracing("select source inputs for graph analysis")`. `StaticDecisionAnalyzer.analyzeAll`
calls it once for each project in the validated application boundary. It returns no request for a
project without an entry source. Otherwise, it builds the request that the flat or modular analyzer
uses.

The complete call placement is:

1. `AnalyzeReactorMojo` reads the effective Maven reactor and creates
   `ApplicationSourceBoundary`.
2. `ProjectGraphGenerator` gives the boundary to `StaticDecisionAnalyzer`.
3. `StaticDecisionAnalyzer.analyzeAll` calls `AnalysisSourceSelector.select` for each project.
4. The selector computes the connected project closure and the source roles.
5. The analyzer sends the selected request to the flat or modular Java compiler task.
6. Graph builders analyze only annotated methods in the selected entry sources. Resolution sources
   can resolve reachable calls but cannot create a graph entry.

The selector uses these inputs:

| Input | Use |
| --- | --- |
| Current project entry sources | Find `@FachTracing` graph entries. |
| Connected project resolution sources | Resolve reachable source methods and implementations. |
| External resolution sources | Resolve configured code that is outside the reactor. |
| Connected project classpaths | Attribute source against compiled dependencies. |
| Current project compiler character set | Read source with the effective Maven compiler setting. |
| Module descriptors | For a modular entry, select only connected modular project sources. |

Inspect the generated source-selection algorithm and its audits:

```sh
cat target/fachtracing/select-source-inputs-for-graph-analysis-structure.mmd
cat target/fachtracing/select-source-inputs-for-graph-analysis-analysis-audit.mmd
cat target/fachtracing/select-source-inputs-for-graph-analysis-projection-audit.mmd
```

The exact graph names project resolution sources, external resolution sources, combined analysis
sources, connected classpaths, entry sources, modular filtering, and the empty-entry path. These
names come from current production variables and control flow.

## Runtime Traces

The runtime harness uses the same generated activation bundle and calls these three cases. It
prints one checked summary for each execution:

```text
scenario=disabled status=SUCCEEDED result=empty outcomes=[false, true; returns optional empty]
scenario=enabled status=SUCCEEDED result=present outcomes=[false, false; returns optional of new developer output ...]
scenario=invalid status=FAILED result=FAILED outcomes=[true]
```

The actual lines also show the count of declared evidence gaps. The harness checks these facts:

- every call creates one `DecisionExecution`;
- every record belongs to the generated graph;
- every path has selected edge evidence;
- success records contain only the safe `present` or `empty` value;
- the failure record contains only generic failure data;
- the application exception still leaves the method; and
- fingerprint validation permits instrumentation and the agent reports no installation diagnostic.

`Optional` is not a built-in decision value. The harness registers an exact value adapter that
converts it only to `present` or `empty`. The adapter does not call an application object's
`toString()` method.

## How the General Algorithm Works

The tool creates two audit graphs for every analyzed method. These graphs explain its algorithm for
that method:

```sh
cat target/fachtracing/enable-developer-graph-export-analysis-audit.mmd
cat target/fachtracing/enable-developer-graph-export-projection-audit.mmd
```

The analysis audit relates each source construct to an `INCLUDED`, `EXCLUDED`, or `GAP` decision,
its stable reason, and its exact graph nodes. The projection audit relates each exact node to a
`KEPT`, `REMOVED`, or `REPLACED` decision, its stable reason, and its business graph nodes. The
analyzer and projector record these facts. A generic Java formatter writes Mermaid. It does not
use AI, network access, a method-specific label list, or a stored diagram body.

1. The Maven adapter collects reactor source roots, output directories, dependencies, and external
   source boundaries.
2. The engine finds `@FachTracing` entries, resolves result-relevant control and data flow, and
   projects a business graph.
3. The developer manifest keeps technical probe positions. The business graph keeps business
   labels and opaque node and edge IDs.
4. The activation bundle joins the graph, manifest, and SHA-256 class fingerprints.
5. The agent checks the loaded class fingerprint. It then uses ASM to add entry, predicate, result,
   and failure probes.
6. The probes send in-memory events to `RuntimeCollector`. The collector validates edge IDs and
   creates one ordered `DecisionExecution`.
7. Other product parts can project an explanation or store the record. The self-trace stops after
   it validates the collector record.

## Maven Parts and Runtime Parts

| Part | Responsibility in this example |
| --- | --- |
| `AnalyzeReactorMojo` | Reads Maven reactor structure and starts aggregate analysis. |
| `ProjectGraphGenerator` | Connects Maven inputs to the engine and writes graph artifacts. It also contains the traced policy. |
| `StaticDecisionAnalyzer` and graph builders | Find the annotated decision and create its result-relevant graph and probe plan. |
| `activation.json` | Transfers static graph, manifest, and fingerprint data to runtime. |
| `FachtracingAgent` and transformer | Check class identity and inject non-throwing probe calls. |
| `TraceRuntime` | Gives injected bytecode a stable bridge to the configured collector. |
| `RuntimeCollector` | Builds selected-edge observations and completes success or failure records. |
| `SelfTracingRuntimeTest` | Runs the three production cases and verifies the resulting records. |

This separation keeps Maven-specific work outside the analysis engine. The engine owns graph
semantics. The agent owns bytecode instrumentation. The collector owns runtime record assembly.

## Exact Evidence and the Current Boundary

The activation bundle has exact branch-edge bindings for both predicates. Therefore, the runtime
can prove which graph edge each call selected.

The two predicates use derived local booleans named `hasRepository` and `hasTemplate`. The current
analyzer cannot also bind those derived values to raw method arguments as exact input evidence. It
declares this limit in the activation bundle. At runtime, the collector reports two deduplicated
`EXACT_PATH_UNAVAILABLE` diagnostics, one for each declared evidence target. The harness requires
these declared diagnostics and rejects other diagnostics.

This boundary does not make the selected path uncertain. It means that the trace has exact branch
selection but does not include exact raw input evidence for those derived local predicates.

## Generated Files

The static pass writes these main files under `target/fachtracing`:

- `enable-developer-graph-export-structure.mmd`;
- `enable-developer-graph-export-structure.puml`;
- `enable-developer-graph-export-analysis-audit.mmd`;
- `enable-developer-graph-export-projection-audit.mmd`;
- `include-exact-node-in-business-graph-{structure,analysis-audit,projection-audit}.mmd`;
- `select-source-inputs-for-graph-analysis-{structure,analysis-audit,projection-audit}.mmd`;
- `index.md`; and
- `activation.json`.

The runtime classpath file is `target/self-tracing-classpath.txt`. All files are build output. Git
does not track them.
