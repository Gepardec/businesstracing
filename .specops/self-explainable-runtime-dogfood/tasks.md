# Implementation Tasks: Self-Explainable Runtime Dogfood

## Task Breakdown

### Task 1: Add structured decision audits

**Status:** Completed
**Dependencies:** None

- [x] Record exact-to-business decisions with stable reasons.
- [x] Preserve decision relations through graph summarization.
- [x] Render grouped analysis and projection audit Mermaid.
- [x] Write, index, and clean audit files in Maven output.
- [x] Add focused model, projector, renderer, and Maven tests.

### Task 2: Extract and select the self-analysis policies

**Status:** Completed
**Dependencies:** Task 1

- [x] Extract the production source-selection policy without duplicating behavior.
- [x] Expose the production node-inclusion policy as one selected method.
- [x] Configure both unannotated methods in the root Maven build.
- [x] Keep business labels clear and free of low-level collection mechanics.

### Task 3: Prove static and runtime output

**Status:** Completed
**Dependencies:** Tasks 1 and 2

- [x] Generate all static and activation artifacts for both methods.
- [x] Execute removal, retention, no-entry, connected, and modular paths with the agent.
- [x] Write and verify one evaluated Mermaid path for each call.
- [x] Prove deterministic output and input-sensitive output.
- [x] Prove production has no fixed self-example diagrams or AI calls.

### Task 4: Complete verification and delivery

**Status:** Completed
**Dependencies:** Tasks 1, 2, and 3

- [x] Run focused tests and the complete repository gate.
- [x] Run Keycloak, Mega, PetClinic, and PostgreSQL conformance.
- [x] Complete implementation evaluation and dependency audit.
- [x] Commit, push, open the pull request, and confirm all CI checks pass.

## Implementation Order

1. Add generic audit data and rendering.
2. Extract and configure the two production policies.
3. Add static and runtime proof.
4. Complete all gates and delivery records.
