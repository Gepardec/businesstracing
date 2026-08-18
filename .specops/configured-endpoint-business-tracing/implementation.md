# Implementation Journal: Configured Endpoint Business Tracing

## Summary

Implemented configured endpoint selection, automatic per-call business output, and reproducible Mega and Keycloak examples. Users can select an exact Java method without a source annotation. The agent writes redacted text and Mermaid files for each completed call.

## Phase 1 Context Summary

- Config: defaults; specs directory `.specops`; vertical `backend`; task tracking `none`; review disabled.
- Context recovery: unrelated spec `release-gate-timeout-budget` is implementing; this run starts a separate spec with no overlapping target files.
- Steering files: loaded 6 files (`dependencies.md`, `product.md`, `reference-application.md`, `repo-map.md`, `structure.md`, `tech.md`).
- Repo map: fresh; source hash matched `dad66adff01a31913236bb84b73d0d877faf776980ecb86e57ff49e93b55e491`.
- Memory: loaded 109 decisions from 38 specs and 17 recurring patterns; the relevant pattern keeps external-project knowledge in conformance artifacts.
- Production learnings: no learning file.
- Vertical: backend.
- Affected files: analyzer root discovery, Maven configuration, runtime collector, business execution rendering, Java agent, Mega conformance, Keycloak conformance guidance, and integration documentation.
- Project state: brownfield; 196 tracked source, test, script, configuration, and documentation files outside SpecOps.
- Scope assessment: three independent deliverables and more than two code domains detected; non-interactive execution keeps one spec with explicit component boundaries.
- Coherence check: pass; runtime output, root selection, and conformance criteria use compatible contracts.
- Vocabulary check: not required for the backend vertical.
- Plan validation: pass; existing file references resolve and files marked as new are listed as implementation outputs.

## Decision Log

| # | Decision | Rationale | Task | Timestamp |
|---|----------|-----------|------|-----------|
| 1 | Resolve configured roots after Java attribution. | Erased parameter types and owner identity are exact only after compiler attribution. | 1 | 2026-08-12T15:54:46Z |
| 2 | Use one Maven configuration bean for both goals. | It prevents different endpoint-selection behavior in module and aggregate analysis. | 2 | 2026-08-12T15:56:41Z |
| 3 | Write automatic output from a daemon sink. | Endpoint threads keep the existing non-blocking queue contract and do no file I/O. | 3 | 2026-08-12T16:01:20Z |
| 4 | Replace all automatically adapted values with one fixed value. | Endpoint inputs and results can contain restricted personal or credential data. | 3 | 2026-08-12T16:01:20Z |

## Deviations from Design

| Planned | Actual | Reason | Task |
|---------|--------|--------|------|
| Render the exact Keycloak business graph as the primary reader artifact. | Keep the exact 169-node activation graph and add a reviewed concise overview for readers. | The exact graph is correct but too detailed for a non-technical first view. | 4 |
| Use generic business projection without changes. | Hide general Java calculation syntax and technical return expressions. | The Keycloak example exposed technical expressions that the general business projection must remove. | 4 |

## Blockers Encountered

| Blocker | Resolution | Impact | Task |
|---------|------------|--------|------|
| The Keycloak wrapper selected Java 11. | The conformance script selects Java 21 explicitly. | No product-code impact. | 4 |
| The Keycloak runtime class path did not include all compile-time modules. | The conformance script uses the Maven test-scope dependency class path. | No product-code impact. | 4 |

## Documentation Review

| File | Status | Result |
| --- | --- | --- |
| `README.md` | Updated | Added configured endpoint tracing and external examples. |
| `docs/maven-plugin.md` | Updated | Added the exact endpoint-selection XML contract. |
| `docs/runtime-integration.md` | Updated | Added automatic agent arguments, output, and redaction behavior. |
| `conformance/mega-backend/README.md` | Updated | Replaced the annotation overlay workflow with configured roots. |
| `conformance/mega-backend/selection.md` | Updated | Recorded the five exact configured methods. |
| `conformance/mega-backend/conformance-report.md` | Updated | Recorded the annotation-free conformance result. |
| `conformance/keycloak/README.md` | Added | Added the pinned build, analysis, runtime call, and result guide. |
| `conformance/keycloak/selection.md` | Added | Recorded the selected Keycloak method and reviewed flow. |

## Phase 2 Completion Summary

- Requirements: select exact endpoint methods without source edits, capture each call as redacted business text and Mermaid, and prove the flow with Mega and Keycloak.
- Design: additive exact method selection, explanation-based path rendering, opt-in agent file output, safe arbitrary-result completion, and external-project isolation.
- Tasks: four ordered tasks cover analyzer, Maven, runtime/agent, and external conformance.
- Dependencies: no new dependencies; Java standard library and current modules only.

## Session Log

- 2026-08-12: Loaded SpecOps defaults, steering, memory, repository structure, Mega conformance, Keycloak user-search source, and current runtime activation flow.
- 2026-08-12: Started Task 1. Scope: add an exact configured root contract, preserve annotation roots, and add focused analyzer contracts.
- 2026-08-12: Completed Task 1. The engine selects unannotated methods, rejects missing or ambiguous selections, and deduplicates configured and annotated roots.
- 2026-08-12: Started Task 2. Scope: map Maven XML to engine selections and pass the same list through both plugin goals.
- 2026-08-12: Completed Task 2. Both goals accept the same validated method selections and keep existing generator overloads compatible.
- 2026-08-12: Started Task 3. Scope: preserve arbitrary endpoint results and write redacted business text and Mermaid paths from a daemon sink.
- 2026-08-12: Completed Task 3. Agent arguments activate all graphs, automatic values are redacted, arbitrary results complete safely, and a daemon sink writes one text and Mermaid pair per call.
- 2026-08-12: Started Task 4. Scope: remove the Mega source overlay and add a pinned, reproducible Keycloak user-search example.
- 2026-08-12: Completed Task 4. Mega produced its five unchanged reviewed graphs without a source overlay. The pinned Keycloak source produced an exact activation graph and a concise user-search flow. Mega, Keycloak, PetClinic, and the full repository verification passed.

## Phase 4 Completion Summary

- All requirement and task checklist items passed.
- The complete repository gate passed, including 5,000 runtime decisions with no errors, mismatches, drops, or cross-request contamination.
- Mega produced five complete reviewed graphs and its runtime strategy flow.
- Spring PetClinic produced three complete reviewed graphs.
- Keycloak revision `eba869ee597b933efc8fa2c84713db9e6c0983cf` produced the selected `search users` graph without a source edit. Its lazy stream boundary remains an explicit incomplete gap.
- No new dependency was added.
