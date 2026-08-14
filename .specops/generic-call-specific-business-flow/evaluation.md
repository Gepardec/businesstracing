# Evaluation Report: Generic Call-Specific Business Flow

## Spec Evaluation

### Iteration 1

**Evaluated at:** 2026-08-14T09:22:46Z
**Threshold:** 7/10

| Dimension | Evidence | Findings | Score | Threshold | Pass/Fail |
| --- | --- | --- | ---: | ---: | --- |
| Criteria Testability | `requirements.md` Stories 1-4 define observable graph rewrites, branch-specific outputs, forbidden data, and named gates. Success Metrics define two-branch, gap-region, mutation, and non-Java review checks. | The final non-Java review needs a person and cannot run in normal unit tests. The automated graph checks provide a clear proxy but do not replace that review. | 9 | 7 | Pass |
| Criteria Completeness | Story 2 covers selected paths, different branches, named results, incomplete evidence, and graph mismatch. Story 3 covers threading, privacy, and unsupported return objects. | Failed endpoint calls are covered by existing named failure results but are not a separate EARS criterion. Task 2 must retain the existing failure-result contract. | 8 | 7 | Pass |
| Design Coherence | Decisions 1-5 map traceability, selection order, graph summary, shared rendering, and failure behavior to Components 1-6. Risks have matching mitigations and tests. | The safe fallback for missing terminal proof is defined by behavior, not by a new wire type. This keeps the design small but requires a focused constructor-level test. | 9 | 7 | Pass |
| Task Coverage | Tasks 1-4 cover every designed component, tests precede production changes, and dependencies form one valid order. Every existing file path resolves; new files are marked as new. | The manual review rubric is part of Task 4 documentation and needs explicit recorded answers before final completion. | 8 | 7 | Pass |

**Verdict:** PASS — 4 of 4 dimensions passed.

**Remediation:** None required before implementation. Keep failed-call compatibility and record the manual review result in Task 4.

### Iteration 2

**Evaluated at:** 2026-08-14T10:23:34Z
**Threshold:** 7/10

| Dimension | Evidence | Findings | Score | Threshold | Pass/Fail |
| --- | --- | --- | ---: | ---: | --- |
| Criteria Testability | Story 5 defines observable rules for caller predicates, lazy callbacks, caught paths, nested binary types, and unresolved counterexamples. The success metrics set a Keycloak gap limit and require two live call diagrams. | The non-contradiction and connection checks need explicit graph assertions in addition to manual inspection. Task 5 includes this work. | 9 | 7 | Pass |
| Criteria Completeness | Story 5 covers each current generic gap class and states that application facts cannot affect classification. It also keeps unresolved external behavior visible. | The first implementation can leave up to three justified boundaries, so the conformance report must name why each remaining boundary is not provable. | 8 | 7 | Pass |
| Design Coherence | Decisions 6 and 7 separate source-boundary classification from nested binary lookup. Component 7 has one responsibility and a fail-closed result. | Runtime path correction can require a separate projector fix after the static rules pass. The design keeps that proof in the existing runtime component. | 9 | 7 | Pass |
| Task Coverage | Task 5 starts with target-neutral fixtures, includes negative counterexamples, regenerates Keycloak, runs two live calls, and finishes with all repository and hosted gates. | The task is large because live external proof is expensive, but the ordered gates prevent target-specific rules from entering production. | 8 | 7 | Pass |

**Verdict:** PASS — 4 of 4 dimensions passed.

**Remediation:** None required before implementation. Record each remaining Keycloak gap and its generic unresolved cause.

---

## Implementation Evaluation

**Automated verdict:** PASS

- The focused synthetic contracts prove generic traceability, summary, branch selection, safe gaps, semantic mutation, deterministic output, and identity rejection.
- The exact local PR gate passes.
- Keycloak, Mega, and PetClinic conformance pass without external-project rules in production.
- Hosted CI passes `pr-gate`, `mega`, `petclinic`, and `postgres` for commit `759af55`.

**Final verdict:** PENDING — a person who does not know Java must complete the documented live Keycloak review before the specification can close.

### Iteration 2 — Generic boundary rules

**Evaluated at:** 2026-08-14T11:13:13Z
**Threshold:** 7/10

| Dimension | Evidence | Findings | Score | Threshold | Pass/Fail |
| --- | --- | --- | ---: | ---: | --- |
| Functionality Depth | Synthetic boundary fixtures cover caller predicates, caller actions, lazy callbacks, nested binary types, repeated effects, direct external decisions, and unavailable stream suppliers. Two real Keycloak calls return data and produce connected selected graphs. | Each live Keycloak graph still has two explicit gaps because the pinned source and classpath do not prove the permission-boundary state. This is correct but leaves the business explanation incomplete. | 9 | 7 | Pass |
| Design Fidelity | `SourceUnavailableCallClassifier` separates structural boundary evidence. `ObservedBusinessSegmentConnector` uses the complete generated graph and an explicit gap for equivalent call-site routes. The implementation journal records the Spring and runtime connector deviations. | Caught-path behavior stays in the try/catch scanner instead of calling the classifier directly. The result matches Decision 6, but the implementation is less centralized than Component 7 suggests. | 8 | 7 | Pass |
| Code Quality | Compiler symbols prevent shadow-name matches. New classifier and connector classes have one responsibility. Production code contains no Keycloak, Mega, reviewed label, or fixed topology. | The connector performs repeated graph scans and label-equivalence searches. This is acceptable on the daemon sink thread, but a precomputed graph index can reduce cost if business graphs become much larger. | 8 | 7 | Pass |
| Test Verification | Focused engine and Spring suites, repository integrity, Java capabilities, Keycloak, Mega, PetClinic, and `./scripts/verify-pr.sh` pass. Live checks prove one entry, full reachability, one selected outcome per rule, and one terminal result for both calls. Hosted `pr-gate`, `mega`, `petclinic`, and `postgres` jobs pass for commit `e4118c6`. | The live Keycloak check is local and not part of pull-request CI because it needs a running pinned distribution and administrator account. | 9 | 7 | Pass |

**Verdict:** PASS — 4 of 4 dimensions passed. The spec stays open only for the manual non-Java review.

### Iteration 3 — Runtime predicate correlation correction

**Evaluated at:** 2026-08-14T11:44:19Z
**Threshold:** 7/10

| Dimension | Evidence | Findings | Score | Threshold | Pass/Fail |
| --- | --- | --- | ---: | ---: | --- |
| Functionality Depth | A target-neutral multiline disjunction records all nine evaluated predicates in order. Two final Keycloak calls produce connected graphs, and the unfiltered call records `enabled exists` and `exact exists` as `no`. | The two external permission and returned-state boundaries remain explicit gaps because the available source and classpath do not prove them. | 9 | 7 | Pass |
| Design Fidelity | The transformer accepts a preceding source line only after a non-final disjunction operand. The connector removes a dangling rule when runtime evidence proves no outcome. | Runtime correlation still depends on `javac` branch order inside the declared supported subset. Unsupported shapes fail closed. | 9 | 7 | Pass |
| Code Quality | The source-correlation rule and the unproved-rule connector guard each have one responsibility. No application name, label, method, or topology enters production code. | The correlation rule is intentionally narrow. Other compiler line-table differences must first get an independent failing fixture. | 9 | 7 | Pass |
| Test Verification | The failing synthetic regression, executable agent suite, pinned Keycloak static and live checks, Mega, PetClinic, repository integrity, and `./scripts/verify-pr.sh` pass. | Hosted CI runs after the correction is pushed. | 9 | 7 | Pass |

**Verdict:** PASS — 4 of 4 dimensions passed. The spec stays open only for the manual non-Java review.
