# Implementation Journal: Hogajama strict aggregate regression

## Summary

Completed one task. The analyzer now accepts supported platform operations as known exception
triggers inside source-visible catches. Method-local dependency, definition, mutation, validation,
and audit scans stop at nested class bodies. The strict Hogajama reactor produces both reported
graphs as complete.

## Phase 1 Context Summary

- Config: SpecOps defaults; vertical `library`; specsDir `.specops`; task tracking `none`.
- Context recovery: no incomplete spec; the completed `fix-hogarama-aggregate-completeness` spec
  documents the earlier archive-origin fix.
- SpecOps fallback: the installed skill has no referenced `core/` modules or templates, so this spec
  follows the repository's established SpecOps artifact format.
- Steering files: loaded dependencies, product, reference application, repo map, structure, and
  technology.
- Repo map: loaded; current for the checked-out `main` source structure.
- Memory: loaded analyzer decisions and recurring source-effect, dispatch, and library-boundary
  patterns. No production learnings file exists.
- Vertical: library.
- Project state: brownfield.
- Affected files: analyzer dependency collection, analyzer catch and dispatch logic, analyzer
  contracts, capability documentation, and derived SpecOps files.
- Scope assessment: one tightly coupled aggregate-completeness defect; decomposition is not
  recommended.
- Vocabulary check: pass.
- Plan validation: pass; all planned source and documentation paths exist.

## Phase 2 Completion Summary

- Requirement: both reported strict aggregate graphs must complete without weakening unsupported
  binary exception, effect, or dispatch behavior.
- Design: reuse supported-operation evidence, isolate executable scans, and require attributed source
  dispatch evidence.
- Tasks: one focused implementation and verification task.
- Dependencies: no new package; two required completed specs.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
|---|----------|-----------|------|-----------|
| 1 | Reuse supported-operation evidence for caught calls. | A call that is supported in normal flow must not become unavailable only because source catches its exception. | Task 1 | 2026-08-11T09:40:22Z |
| 2 | Keep method-local scans inside one executable body. | A nested class has its own receiver, parameters, and method effects. | Task 1 | 2026-08-11T09:40:22Z |
| 3 | Keep the existing generated-source dispatch contract. | The documented same-invocation compile and analysis flow already supplies generated Java and selects the mapper implementation. | Task 1 | 2026-08-11T09:40:22Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
|---------|--------|--------|------|
| Correct generated implementation selection. | No production dispatch change was required. | The documented same Maven invocation registered the generated mapper source and existing subtype selection worked. | Task 1 |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
|---------|------------|--------|------|
| The shared Maven cache briefly contained a different same-version engine JAR from another worktree. | Reinstalled this branch and verified that the target and installed analyzer class checksums matched before the real test. | No product change. | Task 1 |
| The first full gate used a Maven JSON fixture whose platform catch was expected to stay incomplete. | Kept the JSON gap test focused on an unavailable dynamic implementation and added a separate untrusted binary catch regression. | Test data changed to match the corrected public contract. | Task 1 |

## Documentation Review

- `docs/supported-java-constructs.md`: updated for supported caught platform calls and nested-class
  effect scope.
- `docs/java-capabilities.json`: updated with executable contracts for both behaviors.
- `docs/maven-plugin.md`: checked; it already requires compile and analysis in one Maven invocation
  for generated Java and documents exact opaque-library coordinates.
- `README.md` and `AGENTS.md`: checked; no project overview or policy change is required.
- Conformance reports and oracles: checked; the full gate found no topology change.

## Session Log

- 2026-08-11T09:14:44Z: Reproduced the exact strict aggregate failure in Hogajama.
- 2026-08-11T09:14:44Z: Explicit Morphia and Apache Commons boundaries removed most gaps but left
  source catch and nested-class effect gaps. A separate direct goal did not register generated
  source, so final validation used the documented same Maven invocation.
- 2026-08-11T09:40:22Z: The focused analyzer contract passed with supported and fail-closed cases.
- 2026-08-11T09:40:22Z: Strict Hogajama analysis passed for both graphs with the three explicit
  technical-library coordinates.
- 2026-08-11T09:40:22Z: `./scripts/verify-pr.sh` passed, including five complete Mega graphs, all
  Spring PetClinic expectations, external release, and 5,000 load decisions.

## Phase 3 Completion Summary

- Supported platform calls inside source-visible catches use the existing operation contract.
- Method-local scanner roles stop at nested class bodies; lambdas remain in the enclosing
  executable and nested methods remain separately indexed.
- One compiled integration fixture covers generated dispatch, an anonymous comparator, caught
  platform calls, explicit opaque libraries, and an untrusted binary counterexample.
- The real strict Hogajama reactor passed with Morphia, Commons Lang, and Commons Collections as
  exact opaque technical boundaries.

## Phase 4 Completion Summary

- All five acceptance criteria passed.
- The implementation evaluation passed.
- Documentation, memory, index, and run records were updated.
- The repository structure did not change, so the repo map content remains current.
