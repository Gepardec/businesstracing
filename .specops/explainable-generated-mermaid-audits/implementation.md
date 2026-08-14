# Implementation Journal: Explainable Generated Mermaid Audits

## Summary

Implementation is in progress.

## Phase 1 Context Summary

- Config: SpecOps defaults; backend Java library; `.specops` directory; no task tracker; review is
  not required; evaluation threshold is 7/10 with two iterations.
- Context recovery: The Keycloak branch is the base. A different Keycloak task has active changes
  in another worktree, so this feature uses an isolated branch and worktree.
- Steering: Product, dependency, reference-application, repository-map, structure, and technology
  guidance were loaded.
- Memory: Existing decisions require application-neutral production rules, explicit gaps, source
  provenance, deterministic output, and conformance-first proof.
- Vertical: Backend Java library with Maven plugin and external Keycloak conformance.
- Current state: Keycloak generates a 169-node exact graph, a 41-node overview, and a 13-node
  evaluated path. It does not generate analysis or projection audit Mermaid.
- Scope: One feature. Projection decisions, summary mapping, rendering, output lifecycle, and
  Keycloak proof depend on one shared audit contract.
- Dependency gate: Pass. No package, Maven coordinate, install command, or service is added.

## Phase 2 Summary

- Requirements define compact source-analysis and exact-to-business audit diagrams.
- Design keeps runtime traceability and business output compatible.
- Three ordered tasks cover decision data, rendering and export, and Keycloak proof.
- Spec evaluation passed in one iteration.

## Decision Log

| # | Decision | Reason | Task | Time |
| --- | --- | --- | --- | --- |
| 1 | Use an isolated branch from the Keycloak branch. | Another worktree has overlapping active changes. | Planning | 2026-08-14T11:00:24Z |
| 2 | Group audit decisions and show bounded current examples. | Per-node Keycloak audit output would be too large to read. | Task 2 | 2026-08-14T11:00:24Z |
| 3 | Keep raw runtime projection separate from final summarized audit. | Runtime edge paths and final display have different traceability needs. | Task 1 | 2026-08-14T11:00:24Z |

## Deviations

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |

## Blockers

| Blocker | Resolution | Impact | Task |
| --- | --- | --- | --- |

## Documentation Review

Pending.

## Session Log

### Session 1 — Specification created (2026-08-14)

Created one focused feature spec for generated, application-neutral analysis and projection audit
Mermaid. No implementation task has started.

### Session 2 — Task 1 started (2026-08-14)

Task 1 scope: Add stable projection decisions and trace business-node merges through summary. Keep
the current runtime mappings, business graph API, and exact graph unchanged.

Task 1 completed. Every exact node and terminal result has a stable projection decision. Summary
now maps each input node to its final representative. The existing runtime maps remain unchanged.
The focused projection executable contract passes.

### Session 3 — Task 2 started (2026-08-14)

Task 2 scope: Format recorded analysis and final projection decisions as compact, deterministic
Mermaid. Add the two files to Maven output, index, and stale-file cleanup. Do not add classification
logic to the renderer.

Task 2 completed. The renderer groups actual source or exact kinds, stable actions and reasons, and
final target kinds. It includes counts and up to three current examples. Maven writes, links, and
removes both audit files. Focused engine and Maven executable contracts pass.

### Session 4 — Task 3 started (2026-08-14)

Task 3 scope: Generate both audit files from the pinned Keycloak input, assert actual decision
categories, enforce application-neutral production code, compare repeat output, and run all local
and hosted gates.

The pinned Keycloak run passes twice. The analysis audit has 63 lines. The projection audit has 43
lines. Repeat SHA-256 values are `f7b58e09186682e6f5bab8db6d39ceaf60e797c2493d3ed7d178f0aa16f5cad4`
and `7deae8c9f288f8d611ed92fe031263313d78107bcdea38810345a4b9728f401b`.
The full local pull-request gate passes, including Mega and PetClinic. Hosted CI is pending.
