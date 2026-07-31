# Implementation Tasks: Exact Runtime Decision Path Capture

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `generic-tracing-walking-skeleton` | Supplies the contracts that this spec extends. | Yes | completed |

## Task Breakdown

### Task 1: Add exact runtime terminal and dispatch state

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Extend the execution and invocation models so the collector can store exact edges, failed terminal state, and nested dispatch expectations.

**Implementation Steps:**

1. Add success and failure terminal state to `DecisionExecution` with a compatibility constructor.
2. Add exact edge append and failed completion operations to the invocation context and collector.
3. Move dispatch expectations into an invocation-local stack.
4. Add runtime contracts for valid edges, failed calls, nested calls, and cleanup.

**Acceptance Criteria:**

- [x] A valid branch edge is stored with its business outcome.
- [x] An invalid edge is ignored and is not presented as observed.
- [x] A failed call queues one generic failed execution.
- [x] Nested dispatch expectations match in stack order.
- [x] Existing successful and concurrent capture contracts pass.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/model/DecisionExecution.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/InvocationContext.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeCollector.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/TraceRuntime.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/explain/DecisionExplanationProjector.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/runtime/RuntimeCollectorTest.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/explain/DecisionExplanationProjectorTest.java`

**Tests Required:**

- [x] Exact edge validation test.
- [x] Failed execution test.
- [x] Nested dispatch test.
- [x] Existing engine test suite.

---

### Task 2: Inject exact branches and failure completion

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Extend static metadata and bytecode transformation so supported branches and escaping exceptions produce exact runtime records.

**Implementation Steps:**

1. Derive branch-target bindings from predicate probes and graph edges.
2. Add compatibility-safe branch metadata to `AnalysisManifest`.
3. Replace exact predicate probes with true and false branch trampolines.
4. Wrap instrumented entry methods with one failure handler.
5. Update integration tests and runtime documentation.

**Acceptance Criteria:**

- [x] A transformed true path records the static true edge.
- [x] A transformed false path records the static false edge.
- [x] An explicit throw records one failed execution and preserves the exception.
- [x] A propagated throw records one failed execution and preserves the exception.
- [x] A manifest without branch metadata keeps legacy evaluated-node behavior.
- [x] The path resolver preserves an explicit observed edge and does not replace it with an inferred edge.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisManifest.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DecisionGraphBuilder.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/diagram/ExecutionPathResolver.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingTransformer.java`
- `fachtracing-agent/src/test/java/at/gepardec/fachtracing/agent/FachtracingTransformerTest.java`
- `README.md`
- `docs/supported-java-constructs.md`
- `docs/plantuml/runtime-correlation.puml`

**Tests Required:**

- [x] Analyzer branch-binding test.
- [x] Transformed true and false branch tests.
- [x] Explicit and propagated failure tests.
- [x] Explicit-edge path-resolution test.
- [x] Full project verification before compound-condition remediation.

---

### Task 3: Make short-circuit completion occurrence-aware

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 2
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Replace repeated full-predicate bindings with an all-or-none jump plan for supported compound predicates.

**Implementation Steps:**

1. Add a method-local predicate index and completion mode to branch metadata.
2. Derive `BOTH_OUTCOMES`, `JUMP_FALSE`, and `JUMP_TRUE` plans for simple, flat `&&`, and flat `||` predicates.
3. Reject partial, mixed, nested, negated-compound, or ambiguous plans and keep legacy probes for the complete group.
4. Emit one-sided trampolines for non-final short-circuit operands.
5. Pre-index registered graph edges for constant-time runtime validation.
6. Add analyzer-to-transformer contracts for all decisive conjunction and disjunction paths.
7. Run the current `main` verification script, including Maven plugin contracts.

**Acceptance Criteria:**

- [x] `true && false` records only the false edge.
- [x] `false && value` records only the false edge without evaluating the second operand.
- [x] `true && true` records only the true edge.
- [x] `false || true` records only the true edge.
- [x] `true || value` records only the true edge without evaluating the second operand.
- [x] `false || false` records only the false edge.
- [x] An unsupported compound shape records legacy observations and no exact edge.
- [x] The current full verification script passes.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/AnalysisManifest.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/DecisionGraphBuilder.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/runtime/RuntimeCollector.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzerTest.java`
- `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/FachtracingTransformer.java`
- `fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java`
- `fachtracing-agent/src/test/java/at/gepardec/fachtracing/agent/FachtracingTransformerTest.java`
- `docs/supported-java-constructs.md`

**Tests Required:**

- [x] Analyzer plan-shape contract.
- [x] Analyzer-to-transformer flat `&&` path matrix.
- [x] Analyzer-to-transformer flat `||` path matrix.
- [x] Unsupported compound fallback contract.
- [x] Full project verification after remediation.

## Implementation Order

1. Task 1 establishes the runtime contract.
2. Task 2 emits events that use the runtime contract.
3. Task 3 makes compound branch completion exact and non-contradictory.

## Progress Tracking

- Total Tasks: 3
- Completed: 3
- In Progress: 0
- Blocked: 0
- Pending: 0
