# Evaluation Report: Exact Runtime Decision Path Capture

## Spec Evaluation

### Iteration 1

**Evaluated at:** 2026-07-31T08:49:10Z
**Threshold:** 7/10

| Dimension | Evidence | Findings | Score | Threshold | Pass/Fail |
| --- | --- | --- | --- | --- | --- |
| Criteria Testability | `requirements.md` lines 17-19 define observable edge order and fallback behavior. Lines 35-37 define one failed record, identity-preserving rethrow, data exclusion, and nested cleanup. Lines 53-55 define stack matching and non-consuming mismatch behavior. | The criteria have binary outcomes and map to unit or transformed-code tests. The performance criterion at line 65 states that probes do no file or database I/O, but it does not define a direct test method. A code inspection can verify it, but an automated regression test is less direct. | 8 | 7 | Pass |
| Criteria Completeness | `requirements.md` lines 17-19 cover true, false, and incomplete branch metadata. Lines 35-37 cover explicit and propagated failure through the entry-boundary rule. Lines 53-55 cover stack order, mismatch, and cleanup. Lines 72-73 state the supported compiler boundary and excluded capabilities. | The main success and error paths are present. The spec does not state what happens when exact edge metadata names an edge that is not in the registered graph. Task 1 says that the runtime ignores an invalid edge, but this behavior is not a feature-level acceptance criterion. | 7 | 7 | Pass |
| Design Coherence | `design.md` lines 9-19 put edge capture at the branch site and preserve the legacy fallback. Lines 21-25 use one entry-boundary handler for explicit and propagated failures. Lines 27-31 move dispatch expectations into `InvocationContext`. Lines 71-83 describe the API and privacy effects. | Each use case has a matching design decision and module. The design does not define the exact rule that maps a JVM conditional opcode to the source `true` and `false` graph edges. This rule is important because requirements lines 17-18 assign semantic outcomes to fall-through and jump paths. The Java 21 `javac` scope and fallback reduce this risk, but the implementer must still derive the rule from the current analyzer and transformer. | 9 | 7 | Pass |
| Task Coverage | `tasks.md` lines 25-28 cover the execution model, collector, dispatch stack, and runtime tests. Lines 71-75 cover branch metadata, trampolines, the catch-all handler, and documentation. Lines 79-84 and 100-103 cover both branch outcomes, explicit and propagated failure, legacy fallback, and full verification. | The two tasks follow the dependency order and cover all design modules. `ExecutionPathResolver` is listed at line 90, but no task criterion or named test states that diagrams must prefer recorded edges and must not present an inferred edge as observed. The full verification command protects existing behavior, but it does not prove this new renderer rule. | 8 | 7 | Pass |

**Verdict:** PASS — 4 of 4 dimensions passed.

**Non-blocking improvements:**

- Add a requirement for invalid exact-edge metadata, or remove the task-only behavior if it is not part of the feature contract.
- State the opcode-to-business-outcome mapping rule in `design.md` before transformer implementation.
- Add a task acceptance criterion and test for explicit-edge precedence in `ExecutionPathResolver`.

### Iteration 2: Revised short-circuit specification

**Evaluated at:** 2026-07-31T09:20:33Z
**Threshold:** 7/10
**Context:** Primary-agent validation because `AGENTS.md` and the user prohibit subagents.

| Dimension | Evidence | Findings | Score | Threshold | Pass/Fail |
| --- | --- | --- | --- | --- | --- |
| Criteria Testability | `requirements.md` defines one-sided `&&` and `||` completion, all-or-none fallback, and one exact edge for the full predicate. `tasks.md` defines a six-path compound matrix plus an unsupported-shape contract. | Each supported and fallback path has an observable record-level result. | 9 | 7 | Pass |
| Criteria Completeness | The revised criteria cover simple predicates, flat conjunctions, flat disjunctions, invalid edges, incomplete topology, mixed and nested shapes, failure completion, and nested dispatch. | Exact support is intentionally bounded. Unsafe forms have an explicit legacy behavior. | 9 | 7 | Pass |
| Design Coherence | `design.md` defines method-local predicate occurrence, three completion modes, the all-or-none invariant, one-sided trampolines, and compatibility constructors. | The design now distinguishes an atomic jump from completion of the full source predicate. | 9 | 7 | Pass |
| Task Coverage | Task 3 maps the revised branch design to analyzer, manifest, builder, transformer, fixture, documentation, and full-build verification work. | The task includes the missing analyzer-to-transformer evidence and the current Maven plugin baseline. | 9 | 7 | Pass |

**Verdict:** PASS — 4 of 4 dimensions passed.

---

## Implementation Evaluation

### Iteration 1

**Evaluated at:** 2026-07-31T09:10:19Z
**Threshold:** 7/10

#### Verification evidence

- `./scripts/verify.sh` passed.
- The load test completed 5,000 records with zero errors, mismatches, dropped records, or contamination.
- The transformer tests preserve normal results and the original exception object for explicit and propagated failures.
- The runtime tests cover invalid edge rejection, generic failed records, nested dispatch order, wrong-entry rejection, parent cleanup, concurrency, and probe isolation.
- The path resolver test proves that a valid explicit edge takes precedence over an ambiguous inferred route.
- `git diff --check` passed.

#### Findings

1. **Blocking: a short-circuit predicate can record contradictory exact edges.** `StaticDecisionAnalyzer.addPredicate` creates more than one predicate probe with the same node ID and source line for `&&` and `||` expressions. `DecisionGraphBuilder.branchTargets` creates the same complete true/false binding for each of these probes. `FachtracingTransformer.visitJumpInsn` then emits one whole-predicate edge for each matched conditional jump. For `a && b`, the path where `a` is true and `b` is false records the graph's true edge for the first jump and its false edge for the second jump. The execution therefore claims both mutually exclusive graph edges. The included eligibility fixture uses `age < 24 && location.equals("Vienna")`, but the analyzer test checks only that each binding names valid edges. The transformer test uses one simple comparison. No test runs an analyzer-produced short-circuit binding through the transformer. This behavior violates the requirement that the stored execution show the branch that the invocation took and can cause the path resolver to show an incorrect route.

#### Dimension scores

| Dimension | Evidence | Findings | Score | Threshold | Pass/Fail |
| --- | --- | --- | --- | --- | --- |
| Requirements Satisfaction | Simple true and false branches, legacy fallback, invalid-edge rejection, failed records, exception identity, privacy, and nested dispatch meet their acceptance tests. | Short-circuit predicates can store both exact outcomes for one graph predicate. The primary exact-path requirement is not satisfied for a supported Java construct. | 6 | 7 | Fail |
| Correctness and Edge Cases | The failure handler rethrows the same `Throwable`; failure data is generic; collector edge validation and invocation-local dispatch state are correct in the tested cases. | The branch binding models an atomic bytecode jump as the result of the full source predicate. This model is incorrect when `javac` emits more than one conditional jump for one `&&` or `||` predicate. | 5 | 7 | Fail |
| Test Quality | The suite covers the main simple branch, failure, fallback, dispatch, concurrency, resolver, and load paths. All tests pass. | The tests do not connect the compound analyzer fixture to transformed bytecode. They also do not assert that one predicate invocation records only one mutually exclusive exact edge. | 5 | 7 | Fail |
| Code Quality and Maintainability | The new records are defensive, compatibility constructors are present, failure capture is localized, and exact-edge validation is clear. | Branch metadata lacks the bytecode control-flow information that identifies the terminal outcome of a compound predicate. Repeating one binding per probe hides this missing distinction. | 7 | 7 | Pass |

**Verdict:** FAIL — 1 of 4 dimensions passed.

#### Required remediation

- Bind exact metadata to the overall source predicate result, not to each atomic short-circuit jump. The metadata must identify the bytecode points where the full predicate becomes true or false.
- Keep the legacy evaluated-node probes when this complete mapping is not available.
- Add an analyzer-to-transformer contract for `a && b` and `a || b`. Cover paths where the first operand continues and the second operand decides the opposite outcome. Assert that exactly one graph edge is recorded for the predicate.

### Iteration 2

**Evaluated at:** 2026-07-31T09:39:13Z
**Threshold:** 7/10
**Context:** Direct adversarial review because `AGENTS.md` prohibits subagents.

#### Verification evidence

- The flat conjunction matrix covers first-operand short circuit, second-operand false, and full true.
- The flat disjunction matrix covers first-operand short circuit, second-operand true, and full false.
- Side-effect counters prove that short-circuited second operands are not evaluated.
- Partial, mixed, ternary, switch-expression, and ambiguous lambda plans use safe legacy behavior.
- Exact edges use an immutable registration-time lookup index.
- `./scripts/verify.sh` passed, including Maven plugin integration and the load contract.
- Load result: 5,000 completed, zero errors, zero mismatches, zero drops, zero contamination, and 0.150% p95 overhead.
- `git diff --check` passed.

| Dimension | Evidence | Findings | Score | Threshold | Pass/Fail |
| --- | --- | --- | --- | --- | --- |
| Requirements Satisfaction | All requirement checklists and task criteria are verified by executable contracts. | Exact and fallback behavior now match the full-source-predicate contract. | 9 | 7 | Pass |
| Correctness and Edge Cases | Tests cover simple, compound, partial, mixed, ternary, failure, nested dispatch, invalid edges, and exception identity. | No blocking correctness finding remains. | 9 | 7 | Pass |
| Test Quality | Analyzer-to-transformer tests verify all decisive flat compound paths and observable short-circuit side effects. The full suite includes concurrency, diagrams, Maven integration, and load. | Unsupported switch-expression behavior is enforced by the same source-shape guard and documented fallback; no separate switch fixture is required for this increment. | 9 | 7 | Pass |
| Code Quality and Maintainability | Branch completion is explicit metadata, groups are validated all-or-none, compatibility constructors remain, and runtime validation is indexed. | Conservative fallback keeps compiler-specific uncertainty outside observed evidence. | 8 | 7 | Pass |

**Verdict:** PASS — 4 of 4 dimensions passed.
