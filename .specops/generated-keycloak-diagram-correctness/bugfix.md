# Bug Fix: Generated Keycloak Diagram Correctness

## Problem Statement

The Keycloak conformance command writes a manually constructed business diagram instead of the
business graph projected from the selected endpoint analysis. The artifact therefore cannot prove
that Fachtracing generates the shown non-technical flow.

## Root Cause Analysis

The original implementation judged the 169-node exact graph too detailed for readers. It created a
separate `reviewedOverview()` graph and rendered that graph as the conformance output. The harness
still computes the generic business projection, but it only runs the vocabulary guard against it
and then discards it.

**Affected Components:**

- The pinned Keycloak conformance harness and its generated Mermaid artifact.
- The Keycloak guide that embeds and describes the manually constructed overview.
- Repository integrity checks that currently permit a manual conformance diagram.

**Error Symptoms:**

- `search-users-business.mmd` is identical even when the generic business projection changes.
- The conformance command can pass without proving that the shown topology came from Keycloak
  analysis.

## Impact Assessment

- **Severity:** High
- **Users Affected:** Maintainers and users who rely on the Keycloak example as product proof.
- **Frequency:** Always when the pinned Keycloak conformance command runs.

## Dependencies & Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `configured-endpoint-business-tracing` | It introduced the Keycloak conformance artifact corrected here. | Yes | Completed |

### Cross-Spec Blockers

| Blocker | Blocking Spec | Resolution Type | Resolution Detail | Status |
| --- | --- | --- | --- | --- |
| — | — | — | — | — |

## Reproduction Steps

1. Inspect `KeycloakConformanceTest.main` after `fullBusinessGraph` is projected.
2. Observe that `BusinessMermaidRenderer` receives `reviewedOverview()` instead of
   `fullBusinessGraph`.
3. Expected: the written Mermaid artifact is rendered from the generic projection of the selected
   endpoint analysis.
4. Actual: the written Mermaid artifact is rendered from a fixed 11-node graph.

## Regression Risk Analysis

### Blast Radius

- `KeycloakConformanceTest` selects the endpoint, verifies the exact graph, writes the business
  diagram, and writes the runtime activation bundle.
- `scripts/verify-keycloak.sh` compiles and runs the harness against a pinned clean Keycloak tree.
- `scripts/verify-repository-integrity.sh` protects tracked conformance inputs.
- `conformance/keycloak/README.md` describes the generated artifact and live-call workflow.
- `BusinessGraphProjector` and `BusinessMermaidRenderer` supply the generic output path that the
  harness must use.

### Behavior Inventory

- Exact endpoint selection, branch bindings, graph completeness, and class fingerprints work and
  must remain unchanged.
- Generic business projection removes technical Java vocabulary and keeps explicit gaps.
- The activation bundle retains the exact graph for runtime path correlation.
- The generated Mermaid artifact remains disposable under `conformance/keycloak/target/generated`.
- The live endpoint path continues to use the exact activation graph, not the static reader graph.

### Test Coverage Assessment

- **Covered:** exact Keycloak selection and activation creation are exercised by
  `scripts/verify-keycloak.sh`.
- **Covered:** business projection and Mermaid rendering have focused engine contracts.
- **Gap:** no check requires the Keycloak Mermaid artifact to use the projected graph.
- **Gap:** no repository check rejects a manually constructed Keycloak diagram.
- **Gap:** the generic business guard accepts Java method references and technical data-building
  actions, so generated Keycloak output can pass while it is not non-technical.

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| The Keycloak artifact equals the rendered generic projection. | Must-Test | This is the broken behavior. |
| Exact graph and activation data stay unchanged. | Must-Test | Runtime correlation depends on them. |
| Business output contains no Java identifiers or source paths. | Must-Test | This is the existing reader contract. |
| Other business projection fixtures stay stable. | Nice-To-Test | They share the projector but not the conformance harness. |

### Scope Escalation Check

**Scope:** Contained. The generic projection already exists. This fix connects the conformance
artifact to it and does not add a new projection model.

## Proposed Fix

Render `fullBusinessGraph` directly, remove all manual graph construction from the Keycloak harness
and guide, and add a repository integrity check that rejects a manually embedded Keycloak flow.
Keep reviewed rule anchors as assertions against analysis and generated projection data. Do not use
those anchors as graph input. Reject Java method references in all business graphs, and remove
technical data-building calculations through the generic projector.

## Unchanged Behavior

- WHEN the pinned Keycloak endpoint is analyzed THE SYSTEM SHALL CONTINUE TO select
  `UsersResource.getUsers` without source changes.
- WHEN the activation bundle is written THE SYSTEM SHALL CONTINUE TO contain the exact analysis
  graph, manifest, and class fingerprints.
- WHEN the generated reader diagram is checked THE SYSTEM SHALL CONTINUE TO reject Java owners,
  source paths, and technical vocabulary.

## Testing Plan

### Current Behavior (verify the bug exists)

- WHEN the current harness renders its artifact THE SYSTEM CURRENTLY renders
  `reviewedOverview()` and discards `fullBusinessGraph` after validation.

### Expected Behavior (verify the fix works)

- WHEN the Keycloak conformance command runs THE SYSTEM SHALL write the Mermaid rendering of
  `fullBusinessGraph`.
- WHEN repository integrity runs THE SYSTEM SHALL reject manual Keycloak graph construction or an
  embedded Keycloak flowchart.

### Unchanged Behavior (verify no regressions)

- WHEN focused and full tests run THE SYSTEM SHALL CONTINUE TO pass business projection, exact
  activation, repository integrity, and existing conformance contracts.
- WHEN the pinned Keycloak command runs THE SYSTEM SHALL CONTINUE TO produce an activation file
  with the selected exact graph and valid class fingerprints.

## Acceptance Criteria

- [x] Regression Risk Analysis completed to the required depth for High severity.
- [x] Bug reproduction confirmed.
- [x] The Keycloak Mermaid artifact is generated from the generic projected graph.
- [x] No manually constructed or embedded Keycloak diagram remains.
- [x] Exact activation behavior and business-only output remain valid.
- [x] Focused, repository, full, and pinned Keycloak checks pass.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep each component responsible for one function.
- Do not hard-code diagrams.
