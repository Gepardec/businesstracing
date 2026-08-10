# Design: Result Relevance Review Findings

## Architecture Overview

The fix makes definition selection use-site aware and keeps exception and audit classification at their correct boundaries. A new definition index owns structured local-flow state. The slicer only consumes that index and selects result sinks. The auditor only classifies resolved excluded trees.

## Technical Decisions

### Decision 1: Index reaching definitions at each use tree

**Decision:** Add `ReachingDefinitionIndex`. It records the local definitions that can reach each AST tree. A sequential assignment replaces the prior definition. `if`, conditional, switch, loop, and try alternatives merge conservative definition states.

**Rationale:** A flat history cannot distinguish an overwritten assignment from mutually exclusive reachable assignments. A use-site index fixes that distinction without a full control-flow graph or SSA form.

### Decision 2: Expand identifier and use-site pairs

**Decision:** `BackwardDecisionSlicer` will expand one identifier at one use tree. When it adds a definition, it will expand the identifiers used by that definition at that definition's source point.

**Rationale:** The same local name can have different reaching definitions at different uses. Name-only expansion loses that relation.

### Decision 3: Seed only escaping source throws

**Decision:** `CaughtThrowResolver` will use compiler-attributed types to identify a compatible local catch. The slicer will not seed a throw in that set. Other throws remain sinks.

**Rationale:** A locally caught throw is not a terminal result path by itself. The return or mutation inside a relevant catch already makes that catch relevant when it changes the result.

### Decision 4: Reserve unresolved trees before exclusion audit

**Decision:** `StaticDecisionAnalyzer` will compute unknown result effects before exclusion audit. `AnalysisDecisionAuditor` will not exclude an unresolved tree or prune a containing subtree as excluded.

**Rationale:** `GAP` and `EXCLUDED` are mutually exclusive decisions for one source construct. The auditor needs the unresolved boundary before it classifies exclusions.

## Component Responsibilities

### `ReachingDefinitionIndex`

- Track local definitions in source order.
- Merge definitions across structured alternative paths.
- Return immutable definition snapshots by AST use tree.

### `DependencyGraphBuilder`

- Collect source dependencies, effects, and parent relations.
- Attach the definition index to `MethodDependencies`.

### `BackwardDecisionSlicer`

- Expand result dependencies from identifier and use-site pairs.
- Select return sinks, relevant state effects, and escaping throw sinks.

### `CaughtThrowResolver`

- Compare attributed thrown and catch types.
- Identify only throws handled inside the current method.

### `AnalysisDecisionAuditor`

- Find resolved graph-eligible trees that have no result effect.
- Keep unresolved trees out of excluded subtrees.

### `StaticDecisionAnalyzer`

- Compute unresolved result effects before it requests exclusion decisions.
- Build gap nodes from the same unresolved tree set.

## Compatibility

- Do not change `BusinessDecisionGraph`, `AnalysisManifest`, or runtime activation formats.
- Do not add a dependency.
- Keep current branch and Mega topology contracts.

## Testing Strategy

- Add one fixture entry for a sequential overwritten assignment.
- Add one fixture entry for a caught result-independent throw.
- Strengthen the existing unknown-effect manifest contract to require one gap action and no exclusion action at the gap source.
- Update the supported-construct contract for reaching definitions, escaping throws, and exclusive audit actions.
- Run the focused analyzer contract, the pinned Mega gate, and the full pull-request verifier.

## Risks and Mitigations

- **Risk:** Definition merging removes a reachable branch assignment. **Mitigation:** Keep the existing branch-definition contract and pinned Mega topology gate.
- **Risk:** Throw filtering removes an escaping failure. **Mitigation:** Use compiler type compatibility and keep the existing uncaught terminal-failure contract.
- **Risk:** The unresolved boundary hides a separate irrelevant sibling. **Mitigation:** Skip only the unresolved tree and ancestors that contain it; continue audit for other source subtrees.
- **Risk:** Structured flow becomes a second graph extractor. **Mitigation:** Keep `ReachingDefinitionIndex` limited to local-definition snapshots and leave graph extraction in the slicer and analyzer.

## Dependency Decisions

No dependency is added or changed.
