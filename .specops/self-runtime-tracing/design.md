# Design: Self Runtime Tracing

## Architecture Overview

The self-tracing command has two process boundaries. Maven first analyzes the reactor and writes
the activation bundle. A separate Java 21 process starts with the current Java agent, loads the
bundle, registers the current business graphs, and calls the traced production policy three times.
The process validates each execution before it prints a success marker.

```text
Current source -> Maven analyze-reactor -> activation.json
activation.json + current classes -> Java agent -> RuntimeCollector -> three checked executions
```

## Technical Decisions

### Decision 1: Keep Static Analysis and Runtime Capture in Separate Processes

**Decision:** The shell gate shall finish Maven analysis before it starts a Java process with
`-javaagent`.

**Rationale:** The activation bundle contains class fingerprints and probe plans. The agent needs
this completed static data before it can transform the production class. The process boundary also
matches the normal integration model.

### Decision 2: Use an Executable Contract Harness

**Decision:** Add one package-local executable test class in the Maven-plugin module.

**Rationale:** The harness must call the package-local production policy. It can use the existing
executable-test convention, and it does not change the production API.

### Decision 3: Adapt `Optional` Explicitly

**Decision:** Register one exact-type `DecisionValueAdapter` for `Optional` results. It shall emit
only `present` or `empty`.

**Rationale:** The runtime denies unknown objects and arbitrary `toString()` calls. The adapter
keeps the test safe and makes enabled and disabled outcomes deterministic.

### Decision 4: Validate Outcomes and Paths at the Record Boundary

**Decision:** For each call, validate terminal status, safe result or generic failure, observations,
and selected edges. After all calls, reject extra records, agent diagnostics, and runtime
diagnostics that are not declared evidence-availability gaps in the activation bundle.

**Rationale:** A return-value check alone does not prove instrumentation. The completed
`DecisionExecution` record is the business evidence that users consume. Declared evidence gaps are
useful technical evidence because they show where exact branch capture exists without an exact
mapping from a derived local predicate back to an input argument.

## Module Design

### `SelfTracingRuntimeTest`

**Responsibility:** Configure the runtime from one activation bundle, invoke the policy scenarios,
and validate the three execution records.

**Inputs:** One activation-bundle path.

**Output:** `FACHTRACING_SELF_RUNTIME_TRACE_OK` only after all checks pass.

**Failure response:** Throw with a focused message. Do not print the success marker.

### `verify-self-tracing.sh`

**Responsibility:** Orchestrate the static and runtime self-tracing passes.

**Inputs:** The current Maven reactor and optional existing-build flag.

**Output:** Static artifacts under `target/fachtracing` and one final self-trace success marker.

**Failure response:** Exit non-zero on build, graph, activation, agent, classpath, or harness failure.

### `docs/self-tracing.md`

**Responsibility:** Explain the verified two-pass flow and all three policy outcomes.

## Runtime Flow

1. Maven compiles the current project and runs `analyze-reactor`.
2. The gate checks the static graph and activation bundle.
3. Maven writes the Maven-plugin test classpath.
4. Java 21 starts with the current agent JAR and current reactor classes.
5. The harness loads the activation bundle and registers all graphs.
6. The agent transforms `ProjectGraphGenerator` after fingerprint validation.
7. The harness calls disabled, enabled, and invalid configuration scenarios.
8. The collector returns three checked executions.

## Testing Strategy

- Run the self-tracing command from a current reactor build.
- Check all static graph files and all three graph outcomes.
- Check two successful runtime records and one failed runtime record.
- Check selected branch evidence, exact record count, declared runtime evidence gaps, and empty
  agent diagnostics.
- Run the full repository verifier.

## Risks and Mitigations

- **Risk:** The production class loads before agent configuration. **Mitigation:** Agent
  configuration retransforms matching loaded classes and also handles later loads.
- **Risk:** The agent JAR and class files come from different builds. **Mitigation:** The gate uses
  only current reactor `target/` outputs and relies on fingerprint validation.
- **Risk:** An `Optional` result cannot enter the record. **Mitigation:** Register an exact safe
  adapter before the first invocation.
- **Risk:** A test only checks method results. **Mitigation:** Validate completed records and
  selected branch edges for every scenario.

## Dependency Decisions

No new dependencies are introduced. The feature uses Java 21, Maven, the existing Fachtracing
modules, and the current test classpath.
