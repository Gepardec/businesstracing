# Implementation Journal: Developer Graph JSON Export

## Summary

Completed 2 of 2 tasks. Fachtracing now exports a deterministic developer JSON graph with stable nodes and edges, clean Git revision metadata, verified source fingerprints, and commit-pinned source links. The implementation keeps all repository data outside business records and diagrams. All executable contracts pass.

## Phase 1 Context Summary

- Config: defaults; no `.specops.json`; `specsDir=.specops`, vertical `library`, task tracking `none`
- Context recovery: no incomplete specs; three prior specs are completed
- Steering files: loaded 6 (`dependencies.md`, `product.md`, `reference-application.md`, `repo-map.md`, `structure.md`, `tech.md`)
- Repo map: loaded; generated 2026-07-24 and structurally accurate for the affected engine package
- Memory: loaded 9 decisions from 3 specs and 4 recorded patterns
- Vertical: library
- Affected files: new developer exporter, engine module descriptor, analyzer contract test, README
- Project state: brownfield
- Scope assessment: single spec; visualization interchange and revision-pinned navigation form one coupled export contract
- Coherence check: pass; strict clean-revision capture and source fingerprints support the exact-code requirement
- Vocabulary check: pass; library vocabulary uses developer use cases and public module contracts
- Plan validation: pass; three existing files resolved and one new exporter path is explicitly marked new
- Dependency introduction: no new dependencies

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
|---|----------|-----------|------|-----------|
| 1 | Verify every analysis fingerprint before export | A clean repository can move to another commit after analysis. The check proves that the exported commit still contains the analyzed source. | Task 1 | 2026-07-31T08:51:54Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
|---------|--------|--------|------|
| Include analysis fingerprints in JSON | Recompute and compare fingerprints before JSON output, then include them | The final review found a clean-commit race between analysis and export. | Task 1 |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
|---------|------------|--------|------|

## Documentation Review

| File | Status | Review |
| --- | --- | --- |
| `README.md` | Updated | Added JSON interchange, Git capture, source-link usage, and the developer/business data boundary. |
| `docs/supported-java-constructs.md` | Up-to-date | Its source-provenance and business-output rules remain correct; the new API does not change supported Java constructs. |
| `AGENTS.md` | Followed | Remaining documentation and handoff text use ASD-STE100 Simplified Technical English. |

## Session Log

### Task 1 scope — 2026-07-31

Implement the versioned JSON graph projection, strict clean-Git revision capture, safe repository-relative source links, and contract assertions. Completion requires deterministic output, no absolute path leakage, explicit dirty/outside-root failures, and valid source omission for synthetic nodes.

### Task 1 completed — 2026-07-31

Added the exported `at.gepardec.fachtracing.developer` package with deterministic schema-v1 JSON, strict Git `HEAD`/committer-time capture, clean-tree enforcement, canonical path containment, content fingerprints, and template-driven revision links. Extended the analyzer executable contract and passed `./scripts/verify.sh`; the performance regression result was 0.227% p95 overhead with zero errors, mismatches, drops, or contamination.

### Task 2 scope — 2026-07-31

Document the verified Java API, distinguish JSON interchange from Mermaid/PlantUML presentation exports, and explain how developer tools render nodes/edges and open the commit-pinned `source.url` while business records remain source-free.

### Task 2 completed — 2026-07-31

Updated the README integration flow. Added the public API example. Explained how a tool uses nodes, edges, and `source.url`. Stated the clean Git rule and the separate developer-data boundary. Maven test compilation and the documentation reference check passed.

### Post-evaluation correction — 2026-07-31

The final review found that the repository could move to a different clean commit between analysis and export. The exporter now recomputes each source SHA-256 and compares it with the analysis manifest. It stops if content differs. The source revision constructor is now private, so public callers must use strict `captureGit`. The focused contract and full verification suite pass.

## Phase 3 Completion Summary

- Tasks completed: 2 of 2
- Files modified: developer JSON exporter, engine module descriptor, analyzer contract test, README
- Deviations: fingerprint data changed from passive evidence to an enforced pre-export check
- Tests: focused exporter contract passed; the final full `./scripts/verify.sh` run passed with 0.159% p95 overhead and zero integrity failures

### Phase 2 completion — 2026-07-31

- Requirements: deterministic JSON topology export; revision-pinned source coordinates; strict dirty-tree and path-containment safety.
- Design: one dependency-free developer projection with template-driven source links and explicit Git capture.
- Tasks: two ordered tasks covering implementation/contracts and README integration.
- Dependencies: no new packages; required predecessor `generic-tracing-walking-skeleton` is completed.
