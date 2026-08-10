# Mega Backend conformance report

Status: **passed**
Pinned source: `Gepardec/mega-backend@782cdec8dfe5b4062eb5c1859e6a9e53afe02770`
Run date: 2026-08-10, Java 21

## Reproduction and isolation

`./scripts/verify-mega-backend.sh` verifies the clean pinned checkout, creates a disposable
worktree, compiles the application, applies only `annotation-overlay.patch`, scans production
source and generic configuration for forbidden reference hints, analyzes all 420 main Java source
files, compares immutable semantic oracles exactly, executes a real polymorphic decision, and
writes reproducible artifacts under `target/generated/`.

Mega-specific revision, paths, annotations, invocation data, and expectations exist only in this
conformance harness. The analyzer, agent, typed-value protocol, explanation projector, and renderer
are the same artifacts used by non-Mega applications.

## Reviewed static evidence

| Decision | Business area | Nodes | Edges | Status |
| --- | --- | ---: | ---: | --- |
| Authorize clarification resolution | Month end | 18 | 30 | Complete, exact atomic oracle match |
| Detect overlapping time entries | Time warnings | 6 | 9 | Complete, exact atomic oracle match |
| Determine journey warnings | Journey orchestration | 98 | 132 | Complete, exact atomic oracle match; reachable initializers, indexed iteration, and result-changing helper mutations are business-safe |
| Determine project activity in month | Projects | 8 | 9 | Complete, exact atomic oracle match |
| Validate journey direction | Journey validation | 23 | 35 | Complete, exact atomic oracle match; the reachable absent warning initializer is explicit |

The independent source-walk method, reviewed semantic inventories, approval outcomes, and SHA-256
hashes are recorded in `src/test/resources/oracles/README.md`. Verification cannot rewrite those
oracles. Missing or extra nodes, edges, outcomes, dispatches, or completeness state fail the run.

## Runtime evidence

The manager invocation calls `determineJourneyWarnings` with an empty business collection. The
record contains the typed input collection, all three concrete strategy selections as opaque
selected-rule edges, the one atomic early-return predicate actually evaluated, and final typed result
`[] [collection]`. The explanation is complete. The execution PlantUML highlights the path from
entry through dispatch, the three selected strategy edges, evaluated early-return route, and final
collection outcome; alternatives remain dashed.

Artifacts produced by each local or CI run:

- `target/generated/determine-journey-warnings-explanation.txt`
- `target/generated/determine-journey-warnings-execution.puml` and `.mmd`
- five `target/generated/*-structure.puml` and `*-structure.mmd` graphs
- five `target/generated/*-semantic.txt` normalized comparison outputs

These files are reproducible build output and are not version-controlled. The independently
reviewed semantic inputs remain under `src/test/resources/oracles/` and are protected by hashes.

Every graph has exactly one `Start` and one `Stop`, and every path converges on that shared Stop.
Return edges state the returned business expression. Every graph label and generated business
artifact is checked for prohibited package, source, bytecode, stack-frame, identifier suffix,
raw null, and representative Java implementation terms. Meaningful optionality is retained as
“absent” or “exists.”

## Generic capabilities proven by corpus findings

- correct branch topology with fall-through, early returns, terminal throws, ternaries, switches,
  indexed/enhanced loops, and loop-back/completion edges;
- exact atomic short-circuit predicate extraction and source-line-bound runtime probes;
- source-visible direct calls, generic interface target resolution, lambda/stream predicates, and
  per-call-site runtime dispatch correlation;
- collection-building decisions and recursively typed collection results without arbitrary
  object stringification;
- standard JDK deque and collection mutations plus source helper mutations through direct local
  aliases, with every controlling predicate retained;
- business-facing strategy labels and technical projection removal based on source structure;
- execution diagrams that infer unprobed computation segments between observed business nodes.

Each capability has target-neutral regression coverage. The unchanged full non-Mega suite passes
across eligibility, pricing, strategy, aggregation, authorization, calendar, stream, and explicit-gap
fixtures. The final load gate completed 600,000 enabled traces at 1,000 RPS for 600 seconds with
0.059% p95 overhead and zero errors, result mismatches, dropped traces, or contamination.

## Reviewer outcome

Approved. The five graphs are complete at the supported source boundary, exactly match the
independently source-derived semantic oracles, and the runtime artifacts explain the observed
polymorphic execution without exposing Java provenance. Mega proves the generic extractor on a
realistic brownfield corpus and has not shaped production behavior into a Gepardec-specific path.
