---
specId: "generic-call-specific-business-flow"
startedAt: "2026-08-14T09:16:53Z"
completedAt: null
finalStatus: "running"
phases: [1, 2]
---

## Phase 1: Context and scope

**Started:** 2026-08-14T09:16:53Z

### [09:16:53] Step 1: Load configuration

- Action: Check project configuration and working tree state.
- Result: No `.specops.json` file exists. SpecOps defaults apply with the backend vertical and `.specops` as the specification directory.
- Read: `AGENTS.md`

### [09:19:44] Step 3: Load project context

- Action: Load six steering files, project memory, the current repository map, and related completed specs.
- Result: The project requires target-neutral production rules. One unrelated spec remains in progress and does not share this feature's code path.
- Read: `.specops/steering/dependencies.md`
- Read: `.specops/steering/product.md`
- Read: `.specops/steering/reference-application.md`
- Read: `.specops/steering/repo-map.md`
- Read: `.specops/steering/structure.md`
- Read: `.specops/steering/tech.md`
- Read: `.specops/memory/context.md`
- Read: `.specops/memory/decisions.json`
- Read: `.specops/memory/patterns.json`

### [09:19:44] Decision: Keep one feature spec

- Choice: Keep build-time summary, runtime selection, and external proof in one feature spec.
- Rationale: The runtime view cannot be correct without the same traceability contract as the generated business graph. The external corpora validate that one contract and do not form independent product features.

## Phase 2: Specification

**Started:** 2026-08-14T09:19:44Z

### [09:19:44] Step 2: Create specification artifacts

- Action: Define a generic, call-specific business-flow feature with measurable conformance gates.
- Result: Requirements, design, tasks, dependency audit, and implementation journal created.
- Write: `.specops/generic-call-specific-business-flow/requirements.md`
- Write: `.specops/generic-call-specific-business-flow/design.md`
- Write: `.specops/generic-call-specific-business-flow/tasks.md`
- Write: `.specops/generic-call-specific-business-flow/spec.json`
- Write: `.specops/generic-call-specific-business-flow/dependency-audit.md`
- Write: `.specops/generic-call-specific-business-flow/implementation.md`

### [09:22:46] Step 5.5: Verify coherence

- Action: Check requirement-to-design mapping, shared terms, dependency status, and contradictory constraints.
- Result: Pass. The three required specs are completed, and no cycle or blocker exists.

### [09:22:46] Step 5.7: Validate the plan

- Action: Resolve every existing file path and classify four new production files.
- Result: Pass. Ten existing paths resolve and all new paths are explicit.

### [09:22:46] Step 5.8: Run the dependency gate

- Action: Compare the design against current project dependencies.
- Result: No new dependency is introduced.

### [09:22:46] Step 6: Evaluate the specification

- Action: Run the adversarial four-dimension specification review.
- Result: Pass in one iteration with scores 9, 8, 9, and 8.
- Write: `.specops/generic-call-specific-business-flow/evaluation.md`

## Phase 3: Implementation

**Started:** 2026-08-14T09:23:10Z

### [09:23:10] Step 1: Run implementation gates

- Action: Check review, spec dependencies, dependency introduction, task tracking, and cycle gates.
- Result: Pass. Review and task tracking are disabled, all three required specs are completed, no new package is approved or needed, and the dependency graph is acyclic.

### [09:23:10] Task start: Preserve generic projection traceability

- Action: Set Task 1 to In Progress after anchoring its scope.
- Result: The traceability contract is the only active task.

### [09:25:28] Task end: Preserve generic projection traceability

- Action: Add immutable traceability and refactor the projector around exact edge sequences.
- Result: Completed. The focused contract passes and all Task 1 criteria are checked.
- Write: `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphProjection.java`
- Edit: `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphProjector.java`
- Edit: `fachtracing-engine/src/test/java/at/gepardec/fachtracing/business/BusinessGraphProjectionTest.java`

### [09:29:33] Task start: Use one selected model for automatic files

- Action: Set Task 3 to In Progress after anchoring shared-model and privacy behavior.
- Result: Task 3 is the only active task.

### [09:32:43] Task end: Use one selected model for automatic files

- Action: Render automatic text and Mermaid from one selected business graph on the daemon sink.
- Result: Completed. Agent and programmatic explanation contracts pass.
- Write: `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessExecutionTextRenderer.java`
- Edit: `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessMermaidRenderer.java`
- Edit: `fachtracing-agent/src/main/java/at/gepardec/fachtracing/agent/BusinessTraceFileSink.java`
- Edit: `fachtracing-agent/src/test/java/at/gepardec/fachtracing/agent/FachtracingTransformerTest.java`
- Edit: `docs/runtime-integration.md`

### [09:32:43] Task start: Prove generic and brownfield conformance

- Action: Set Task 4 to In Progress after anchoring isolation, brownfield, documentation, and gate evidence.
- Result: Task 4 is the only active task.

### [09:25:28] Task start: Summarize graphs and select one execution

- Action: Set Task 2 to In Progress after anchoring summary and selection behavior.
- Result: Task 2 is the only active task.

### [09:29:33] Task end: Summarize graphs and select one execution

- Action: Add gap-region and behavioral-equivalence summary, then select a business graph from exact visited paths.
- Result: Completed. Synthetic unknown-project contracts pass for two branches, off-path gaps, semantic mutation, and version mismatch.
- Write: `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphSummarizer.java`
- Write: `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessExecutionGraphProjector.java`
- Edit: `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessGraphProjector.java`
- Edit: `fachtracing-engine/src/test/java/at/gepardec/fachtracing/business/BusinessGraphProjectionTest.java`

### [09:46:34] Task 4 verification update

- Action: Run repository integrity, the full repository gate, Keycloak conformance, Mega static and runtime conformance, and PetClinic conformance.
- Result: All individual gates pass. Keycloak reduces 169 exact nodes to 41 overview nodes and 13 selected nodes. Mega uses the generic business projector for five graphs and one real execution.
- Finding: PetClinic had two equivalent `correction required` result nodes. The generic summary correctly merges them while it preserves both incoming paths.
- Edit: `conformance/mega-backend/src/test/java/at/gepardec/fachtracing/conformance/MegaBackendConformanceTest.java`
- Edit: `conformance/spring-petclinic/src/test/resources/oracles/pet-registration-business.json`
- Edit: `conformance/spring-petclinic/src/test/resources/oracles/README.md`
- Edit: `conformance/spring-petclinic/conformance-report.md`
- Result: Task 4 remains in progress until the exact PR gate and hosted CI pass.

### [09:49:20] Focused safety review

- Action: Review the selected-flow fallback for a result-only decision with incomplete runtime evidence.
- Finding: The fallback could create a result-to-gap-to-result cycle when no rule or action preceded the result.
- Result: Exclude result nodes when the fallback finds predecessor leaves. A focused synthetic contract proves that the gap is the only entry and the named result stays terminal.
- Edit: `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessExecutionGraphProjector.java`
- Edit: `fachtracing-engine/src/test/java/at/gepardec/fachtracing/business/BusinessGraphProjectionTest.java`

### [09:52:44] Local verification complete

- Action: Run the exact pull-request gate and rerun the pinned Keycloak gate on the final product code.
- Result: `FAST_PR_GATE_OK`. Repository, unit, integration, agent, performance, release, Mega, and PetClinic checks pass. Keycloak again reports 169 exact nodes, 41 overview nodes, and 13 selected nodes.
- Result: Hosted CI and the manual non-Java Keycloak review remain before final specification completion.
