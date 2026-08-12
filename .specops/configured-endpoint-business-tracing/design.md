<!-- This spec contains security-sensitive architectural details. Review access before sharing. -->

# Design: Configured Endpoint Business Tracing

## Architecture Overview

Fachtracing currently discovers graph roots only through `@FachTracing`, and the embedding application must configure runtime capture. The change adds an exact, framework-neutral root selection contract to analysis and a separate opt-in agent file-output mode. The analyzer still owns graph creation, the Maven plugin only maps configuration, the agent only starts capture, and a dedicated sink writes business artifacts after application-thread work ends.

## Technical Decisions

### Decision 1: Select Java methods, not HTTP routes

**Decision:** Use an exact owner, method, optional erased parameter-type list, and business label.

**Rationale:** Java method identity is stable at the analyzer boundary. HTTP route composition differs by framework and can include resource locators. A separate route-discovery adapter can map routes to this contract later.

### Decision 2: Keep configured roots additive

**Decision:** Combine configured roots with annotation roots, remove duplicates by method identity, and let a configured label take precedence for that root.

**Rationale:** Existing users retain annotation behavior. External projects can select roots without modifying source.

### Decision 3: Write a path diagram from the business explanation

**Decision:** Render the ordered explanation steps as a Mermaid flow from `Start` to the redacted result.

**Rationale:** The explanation already contains the proven runtime order and business statements. This avoids exposing exact graph mechanics or adding a second mapping model to the static business projection.

### Decision 4: Redact automatic output by default

**Decision:** The automatic agent mode replaces every adapted value with a fixed redacted value before it enters a completed execution.

**Rationale:** Keycloak endpoints can process credentials and personal data. Automatic file output must be safe without application-specific policy code.

### Decision 5: Preserve traces for arbitrary endpoint results

**Decision:** `RuntimeCollector` records `No result` for `null` and `Result not recorded` plus a coverage gap for an unsupported return type.

**Rationale:** HTTP endpoints commonly return framework response or stream types. A missing safe adapter must reduce completeness, not discard the complete business path.

## Component Design

### Component 1: `BusinessEntryPoint`

**Responsibility:** Validate and identify one configured graph root.

**Interface:** Owner name, method name, optional erased parameter type names, and non-empty business label.

**Dependencies:** Java collections only.

### Component 2: Analyzer root resolver

**Responsibility:** Resolve configured selections against attributed root-source methods and report missing or ambiguous selections.

**Interface:** `AnalysisRequest.businessEntryPoints()` and the existing `analyze`/`analyzeAll` methods.

**Failure mode:** A missing or ambiguous selection stops analysis before any graph is emitted.

### Component 3: Maven entry-point configuration

**Responsibility:** Convert Maven XML values into validated engine selections for `analyze` and `analyze-reactor`.

**Interface:** `<businessEntryPoints><businessEntryPoint>...</businessEntryPoint></businessEntryPoints>`.

**Failure mode:** Invalid configuration becomes a normal Maven failure with the selection identity.

### Component 4: `BusinessExecutionMermaidRenderer`

**Responsibility:** Convert one `DecisionExplanation` into a business-only Mermaid path.

**Interface:** `String render(DecisionExplanation explanation)`.

**Failure mode:** Mermaid control characters are escaped. Empty reasons still produce Start and result nodes.

### Component 5: Agent option parser

**Responsibility:** Parse and validate opt-in `activation` and `output` agent arguments.

**Interface:** `activation=<path>,output=<path>`.

**Failure mode:** Partial or unknown options stop agent startup with no silent fallback.

### Component 6: Business trace file sink

**Responsibility:** Drain completed executions on one daemon thread and atomically write text and Mermaid files.

**Interface:** Runtime collector, activation definitions, output directory.

**Failure mode:** A file failure is reported through agent diagnostics and never changes endpoint control flow.

## Sequence Diagrams

### Build-time endpoint selection

```text
Maven configuration -> Maven adapter: owner, method, types, label
Maven adapter -> Analyzer: BusinessEntryPoint
Analyzer -> Attributed source index: resolve exact method
Analyzer -> Output: activation.json and static business diagrams
```

### Runtime endpoint call

```text
JVM -> Agent: activation and output arguments
Agent -> Runtime collector: register redacted graph definitions
Endpoint -> Injected probes: execute normal application code
Injected probes -> Runtime collector: record selected path
Runtime collector -> File sink: completed execution
File sink -> Business explanation: project evaluated steps
File sink -> Output directory: write text and Mermaid files
```

## Security Considerations

- **Data classification:** Endpoint values are Restricted until redaction. Automatic output contains fixed redacted values and business labels only.
- **Authentication and authorization:** Fachtracing does not bypass endpoint checks. It observes the path that the application executes.
- **Data protection:** The automatic mode never stores source paths, Java identifiers, exception details, tokens, or raw values in business files.
- **Input validation:** Agent options reject unknown keys and require both paths. Entry-point configuration rejects blank names and invalid parameter types.

## Performance Considerations

- Application threads only enqueue the existing immutable execution.
- One daemon thread polls the collector and performs projection and file I/O.
- No new dependency or network call is used at runtime.

## Testing Strategy

- Analyzer contracts cover configured, overloaded, missing, combined, and duplicate roots.
- Maven contracts cover configuration mapping and generated output.
- Runtime contracts cover null and unsupported endpoint results.
- Agent contracts cover option parsing, redaction, two completed calls, file naming, text, and Mermaid output.
- Mega conformance proves annotation-free external source selection.
- The Keycloak conformance command uses a pinned clean checkout and the selected user-search endpoint.

## Risks & Mitigations

- **Risk:** A method name has overloads. **Mitigation:** Require parameter types when owner and method do not identify one root.
- **Risk:** Business output stores sensitive values. **Mitigation:** Use fixed redaction in automatic mode and keep custom policies in programmatic mode.
- **Risk:** File I/O affects endpoint latency. **Mitigation:** Write only on a daemon consumer thread.
- **Risk:** Keycloak source changes. **Mitigation:** Pin the checkout and keep the Java identity in conformance configuration only.

## Dependencies & Blockers

### Dependency Decisions

No new dependencies are introduced. The implementation uses the Java standard library and current project modules.

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| — | — | — | — |

### Cross-Spec Blockers

| Blocker | Blocking Spec | Resolution Type | Resolution Detail | Status |
| --- | --- | --- | --- | --- |
| — | — | — | — | — |
