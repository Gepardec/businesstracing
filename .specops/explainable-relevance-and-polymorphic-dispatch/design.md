# Design: Explainable Relevance and Polymorphic Dispatch

## Architecture Overview

The backward slicer remains the source of result relevance. A small relevance policy will interpret the slice. A separate auditor will find graph-eligible constructs that the policy excludes. `DecisionGraphBuilder` will collect inclusion, exclusion, gap, and dispatch-candidate decisions in the developer-only `AnalysisManifest`.

The business graph stays unchanged. Runtime activation serialization will continue to omit this audit data.

## Technical Decisions

### Decision 1: Use expression-bounded descendant relevance

**Decision:** A tree is relevant when it is in the slice, contains a sliced tree, or is inside a sliced `ExpressionTree`. A tree is not relevant only because it is inside a sliced statement or control body.

**Rationale:** A method call inside a returned expression must remain visible. An unrelated method call in the body of a relevant `if` must not become visible only because the `if` is in the slice.

### Decision 2: Store the audit trail in the analysis manifest

**Decision:** Add immutable `AnalysisDecision` records to `AnalysisManifest`. Each record has an action, reason, source location, construct kind, related node IDs, and an optional subject.

**Rationale:** The manifest already owns source provenance and runtime bindings. The business graph must not contain source paths, Java type names, or analyzer mechanics.

### Decision 3: Audit exclusions in a separate source pass

**Decision:** Add an `AnalysisDecisionAuditor` that visits graph-eligible source constructs after slicing. It reports the first excluded construct in each excluded subtree.

**Rationale:** This keeps audit collection separate from graph extraction. It also prevents one irrelevant subtree from producing many duplicate exclusion records.

### Decision 4: Audit polymorphic candidates at the dispatch boundary

**Decision:** During dynamic dispatch expansion, classify source-indexed contract subtypes before candidate nodes are created. Concrete receiver-compatible types are included. Abstract and receiver-incompatible subtypes are excluded with exact reasons.

**Rationale:** Java polymorphism can make the runtime target unknown. The analyzer must preserve every proven possible target and must not choose one without runtime evidence.

### Decision 5: Keep every branch definition and terminal failure

**Decision:** The dependency builder will retain every assignment to a local result dependency. The slicer will also seed each source `throw`. If a local has later assignments, its seed initializer stays implicit in the graph. Final Java `Enum` queries are proven read-only.

**Rationale:** A last-write-only definition map can remove a valid assignment from another branch. A strict descendant policy can also remove a terminal failure. The slice must retain these result paths without restoring unrelated work from a whole control body.

## Data Model

```text
AnalysisDecision
  action: INCLUDED | EXCLUDED | GAP
  reason: ENTRY_POINT | RETURN_VALUE | DATA_DEPENDENCY |
          CONTROL_DEPENDENCY | DISPATCH_CANDIDATE |
          NO_RESULT_EFFECT | ABSTRACT_IMPLEMENTATION |
          INCOMPATIBLE_IMPLEMENTATION | UNRESOLVED_RELEVANCE
  source: Path
  line: long
  column: long
  constructKind: String
  nodeIds: List<String>
  subject: String
```

## Component Responsibilities

### `DecisionRelevance`

- Decide if one AST tree is relevant to one backward slice.
- Contain no graph-building or source-mapping logic.

### `AnalysisDecisionAuditor`

- Find excluded graph-eligible constructs.
- Stop at the first excluded construct in an excluded subtree.
- Return AST trees only. It does not build graph nodes.

### `DecisionGraphBuilder`

- Record one inclusion decision for each source-derived node.
- Record explicit exclusion and polymorphic candidate decisions.
- Include immutable decisions in the final manifest.

### `StaticDecisionAnalyzer`

- Use `DecisionRelevance` for all current relevance checks.
- Run `AnalysisDecisionAuditor` for every extracted source method.
- Classify polymorphic candidates with compiler subtype information.

## Compatibility

- Add a compatibility constructor that accepts the old complete manifest parameter list.
- Keep existing shorter compatibility constructors.
- Do not serialize analysis decisions in `RuntimeActivationBundle`.
- Do not change `BusinessDecisionGraph`.

## Testing Strategy

- Add a fixture with an irrelevant call in a relevant `if` body and a relevant call in the returned expression.
- Verify included decisions refer to real graph node IDs.
- Verify the irrelevant call has one `EXCLUDED/NO_RESULT_EFFECT` decision and no graph node.
- Extend the strategy fixture with an abstract subtype and a sealed interface hierarchy.
- Verify two concrete alternatives remain, the abstract subtype is excluded, and dispatch targets remain exact.
- Add a focused receiver-compatibility fixture that proves an incompatible contract subtype is excluded.
- Add a fixture that proves all branch definitions and a terminal failure remain visible.
- Add a fixture that proves excluded final `Enum` queries do not create mutation gaps.
- Run engine tests, repository verification, and the full project verification script.

## Risks and Mitigations

- **Risk:** A strict descendant rule can remove calls nested in returned expressions. **Mitigation:** Permit descendants of sliced expressions, not all sliced trees.
- **Risk:** Exclusion records become noisy. **Mitigation:** Stop traversal at the first excluded graph-eligible construct in each subtree.
- **Risk:** Audit data leaks technical names into business output. **Mitigation:** Store it only in `AnalysisManifest` and do not add it to runtime activation serialization.
- **Risk:** Polymorphic filtering removes a valid implementation. **Mitigation:** Use compiler-attributed subtype checks and preserve the current dispatch target tests.
- **Risk:** Last-write-only local definitions remove a valid branch. **Mitigation:** Slice every assignment for a result-dependent local and verify the reviewed Mega topology.

## Dependency Decisions

No new dependency is introduced.
