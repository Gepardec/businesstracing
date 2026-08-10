# Implementation Journal: Unified Developer Graph Contract

## Phase 1 Context Summary

- Config: no `.specops.json`; SpecOps 1.8.0 defaults apply with `specsDir=.specops`
- Context recovery: no incomplete specification exists
- Steering files: loaded 6 always-included files
- Repo map: fresh; file-list hash matches `948b751950a1c9d6d9c35c6c929824874119575c5016a737feef12be5e2a7867`
- Memory: loaded completed wire-contract, provenance, and Maven-adapter decisions
- Vertical: library
- Project state: brownfield
- Working tree: clean merged `origin/main`; branch `codex/unify-developer-graph-contract`
- Affected files: developer exporter and schema generator, Maven graph generator, consumer contracts,
  README, Maven guide, and SpecOps records
- Scope assessment: one coupled wire-contract refactor; no decomposition required
- Related specs: `developer-graph-json-schema`, `developer-graph-json-export`,
  `maven-developer-graph-export`, `generic-application-readiness`
- Coherence check: pass; one V1 multi-origin shape satisfies single- and multi-origin requirements
- Vocabulary check: pass for the library vertical
- Plan validation: all referenced production, test, and documentation paths exist
- Dependency introduction: no new dependency

## Phase 2 Completion Summary

- Requirements: one multi-origin V1 document and one formal schema for all developer output
- Design: SourceCatalog is canonical; SourceRevision delegates through one Git origin
- Tasks: test-first contract update followed by production and documentation simplification
- Dependencies: no new package and no incomplete spec dependency

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
| --- | --- | --- | --- | --- |
| 1 | Make the multi-origin shape the only V1 contract | There is no released consumer, and one origin is a valid multi-origin list. | Task 2 | 2026-08-07T13:30:55Z |
| 2 | Keep the exact old V2 filename only in stale cleanup | Existing build directories must not keep a false schema artifact, but V2 is not a supported contract. | Task 2 | 2026-08-07T13:30:55Z |
| 3 | Preserve the SourceRevision root precondition before delegation | The convenience API must still reject analyzed sources outside its captured Git root with the established error boundary. | Task 2 | 2026-08-07T13:30:55Z |

## Session Log

### Specification created — 2026-08-07T13:16:28Z

Defined one V1 multi-origin wire contract. Spec evaluation and dependency gates must pass before
test or production code changes.

### Task 1 started — 2026-08-07T13:19:11Z

The dependency, review, and task-tracking gates pass. Task 1 changes the consumer contracts before
production code so the current dual-contract implementation must fail.

### Task 1 completed; Task 2 started — 2026-08-07T13:21:04Z

The updated contract failed at test compilation because `DeveloperGraphJsonSchema` had no
no-argument `generate()` method. This is the expected test-first failure. Task 1 is complete, and
Task 2 now removes the version selector and duplicate serializer.

### Task 2 completed — 2026-08-07T13:30:55Z

All developer JSON now uses the multi-origin V1 shape. The schema generator has one no-argument API,
and Maven writes one V1 schema for single- and multi-origin projects. The old V2 filename remains
only as an exact stale-output cleanup target.

## Phase 3 Completion Summary

- Tasks completed: 2 of 2
- Production result: removed 148 lines from the duplicate exporter path and 62 lines from schema
  version branching
- Tests: independent schema parsing, both exporter overloads, both Maven origin modes, and exact
  legacy cleanup pass
- Deviations: preserved the SourceRevision root precondition explicitly after the first verifier run

## Summary

Fachtracing now has one developer graph contract. V1 uses `sourceOrigins` and `originId` for one or
many origins, `DeveloperGraphJsonSchema.generate()` creates its one formal schema, and Maven always
writes `fachtracing-developer-graph-v1.schema.json`. The exact local pull-request gate passes.

## Documentation Review

| File | Status | Review |
| --- | --- | --- |
| `README.md` | Updated | Describes one V1 schema and the no-argument Java API. |
| `docs/maven-plugin.md` | Updated | Describes one contract for Git, local, generated, and Maven origins. |
| `docs/supported-java-constructs.md` | Updated | Names generated provenance as a V1 source origin. |
| `AGENTS.md` | Followed | Uses STE, no subagents, one canonical serializer, and the publish workflow. |
