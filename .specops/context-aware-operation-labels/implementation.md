# Implementation Journal: Context-aware operation labels

## Summary

Completed four tasks. The analyzer now derives operation subjects from attributed Java declarations,
expands short type abbreviations, includes generic collection element types, and renders generic
`set` and `add` calls with receiver and operand context. The artifact guard rejects the known
context-free forms. Four independent source applications, Hogajama, the external-release fixture,
and the pinned 420-file Mega backend pass. The exact pull-request gate passes.

## Phase 1 Context Summary

- Config: defaults; specs directory `.specops`; library vertical; task tracking disabled.
- Context recovery: no incomplete spec exists.
- Steering files: loaded dependency, product, reference-application, repo-map, structure, and tech context.
- Repo map: fresh; its source hash matches the current file list.
- Memory: loaded prior label and analyzer decisions; recurring business-vocabulary and static-effect patterns apply.
- Vertical: library.
- Affected files: `StaticDecisionAnalyzer.java`, `BusinessArtifactGuard.java`, the static analyzer test,
  and generic label fixtures from five independent source domains.
- Project state: brownfield.
- Scope assessment: one label-generation defect in one code domain; no decomposition is required.
- Vocabulary check: pass.
- Plan validation: pass; three existing paths resolve and one fixture path is marked as new.

## Phase 2 Completion Summary

- Requirements: context-aware local and generic setter labels; existing label behavior stays stable.
- Design: resolve local subjects in `FlowScanner`, render two-argument setters from source operands, and guard the fallback forms.
- Tasks: add a failing contract, implement the label rule, audit a complete application, prove the
  rules across independent source applications, then run repository checks.
- Dependencies: no new dependency.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Derive labels from attributed type and call structure, without an application dictionary. | The rule must work for unknown applications and generated Java. | Task 2 | 2026-08-07 |
| 2 | Expand only type abbreviations of four characters or fewer. | The Mega audit showed that a broad subsequence rule replaced the already meaningful name `warning`. | Task 3 | 2026-08-07 |
| 3 | Update the reviewed Mega oracle only after node counts, edge counts, and completeness stayed unchanged. | The expected semantic change affects labels, not decision topology. | Task 4 | 2026-08-07 |

## Deviations from Design

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |
| Cover `c` and `evaluate set`. | Also cover `comp`, `list`, and `evaluate add`, plus independent application contracts. | The complete application audit found the same root cause in generated mapper and comparator code, and the user required cross-application proof. | Task 3 |

## Documentation Review

| File | Status | Result |
| --- | --- | --- |
| `docs/java-capabilities.json` | Updated | Registers the cross-application executable contract. |
| `docs/supported-java-constructs.md` | Updated | Describes structural context-aware labels and the four independent domains. |
| `conformance/mega-backend/src/test/resources/oracles/README.md` | Updated | Records the approved hash for the label-only Mega oracle change. |
| `README.md` | Up to date | The public analyzer and verification commands did not change. |

## Session Log

- 2026-08-07: Created the bug-fix spec after tracing `c` and `evaluate set` to local-subject and invocation-label fallbacks.
- Task 1 scope: add one calendar construction fixture and exact assertions that reproduce the raw
  local and generic setter fallbacks before production code changes.
- 2026-08-07: Task 1 completed. The new contract fails on `c` and three `evaluate set` labels.
- Task 2 scope: derive one useful local subject from the declaration, use it for a generic two-argument
  setter label, and reject the two known fallback forms without changing graph topology.
- 2026-08-07: Task 2 completed. One-letter reference locals use the declared type, generic setters
  include receiver, property, and value, and the focused analyzer contracts pass.
- Task 3 scope: audit the complete Hogajama graph for other context-free output from the same root
  cause, add generic contracts for those forms, and prove that the rules work across unrelated
  application domains.
- 2026-08-07: Task 3 completed. The regenerated graph has no `c`, `comp`, `list`, `evaluate set`, or
  `evaluate add` node. Four independent source applications prove the same structural rules without
  Hogajama packages or classes.
- Task 4 scope: run focused capability and complete repository verification, review documentation,
  and complete the spec only when all checks pass.
- 2026-08-07: Task 4 completed. `./scripts/verify-pr.sh` reports `FAST_PR_GATE_OK`; the external
  release check passes, the short load processes 5,000 decisions with 0.226% p95 overhead and zero
  errors, and Mega analyzes 420 source files with five complete decisions.
