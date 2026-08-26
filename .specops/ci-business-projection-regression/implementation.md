# Implementation Journal: CI Business Projection Regression

## Summary


## Phase 1 Context Summary

- Config: defaults apply because `.specops.json` is absent; library vertical; task tracking is none.
- Context recovery: unrelated incomplete specs exist; this release regression uses a fresh contained
  bugfix spec.
- Steering files: loaded product, tech, structure, reference application, dependencies, and fresh
  repo map.
- Repo map: fresh at workflow start.
- Memory: loaded completed-spec context, decisions, and recurring patterns; no production learning
  journal exists.
- Vertical: library.
- Affected files: aggregate analyzer and renderer, business projector and tests, self-tracing gate.
- Project state: brownfield pull-request regression.
- Scope assessment: two decomposition signals exist, but all failures enforce one business
  projection release contract. Non-interactive mode proceeds with one spec.
- Vocabulary check: pass.
- Plan validation: pass; all listed implementation paths exist.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Refresh three source-reviewed Mega inventories | The branch adds exact aggregate and return evidence that is present in the pinned source; keeping the older inventory rejects correct topology. | Task 1 | 2026-08-26T19:26:15Z |
| 2 | Prefer direct node semantics to owner suffixes | A controller suffix is weak architecture evidence; explicit predicates, material actions, and terminal edges are stronger business evidence. | Task 2 | 2026-08-26T19:45:30Z |
| 3 | Keep clear normalized labels before call metadata | Source labels such as `validation has errors` are clearer than reconstructed labels such as `result has errors`; metadata is a fallback for technical placeholders. | Task 2 | 2026-08-26T19:45:30Z |
| 4 | Preserve the three-minute budget with a parallel viewer job | Remote step evidence shows that viewer verification is independent and consumes the time needed by the database browser journey. | Task 3 | 2026-08-26T20:00:00Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
| --- | --- | --- | --- |
| Aggregate label correction only | Also refresh three Mega semantic inventories and hashes | The first corrected Mega run exposed stale exact-topology expectations hidden behind the label assertion. | Task 1 |
| Preserve the old PetClinic failure text | Expect the source-derived `pet registration could not be completed` result | The completed semantic explanation spec forbids generic `operation failed` when the source decision identifies the failed business operation. | Task 2 |
| Remove only the terminal owner filter | Also correct semantic-evidence precedence and duplicate complement wrappers | The first PetClinic rerun exposed rules and actions that the old owner-first reducer had hidden or renamed. | Task 2 |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
| --- | --- | --- | --- |

## Documentation Review


## Session Log

- Task 1 scope: retain all aggregate semantic roles, replace call-like qualifier punctuation, and
  prove qualified and unqualified output with the focused analyzer contract.
- Task 1 completed: analyzer and Mega contracts pass; three source-reviewed Mega inventories now
  include the branch's exact aggregate and return evidence.
- Task 2 scope: prove that an exact failure remains a business result when its source has a
  controller owner, remove the invalid owner-suffix filter, and pass PetClinic conformance.
- Task 2 completed: the focused projector contract and all three PetClinic workflows pass. Direct
  semantics now outrank owner suffixes, clear normalized labels remain stable, duplicate complement
  wrappers are removed, and the explicit registration failure is a result.
- Task 3 scope: align self-tracing proof with the artifact boundary, run all release gates, push,
  and wait for remote CI.
- Task 3 local evidence: self-tracing, viewer dogfood, complete PR verification, Mega, PetClinic,
  and Jakarta EE pass. Remote run `33007667194` passed five product gates but cancelled PostgreSQL
  during its final browser journey.
- Task 3 follow-up: move standalone viewer verification to a sixth parallel job, keep the
  three-minute hard limit, and rerun all hosted checks.
- Task 3 remote follow-up: run `33008335544` proved the job split but exposed that PostgreSQL had
  relied on the moved viewer step for `node_modules`. Add an explicit locked dependency install
  before Playwright and rerun.
