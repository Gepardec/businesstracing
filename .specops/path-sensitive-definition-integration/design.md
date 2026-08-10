# Design: Path-Sensitive Definition Integration

## Architecture Overview

The integration uses one source of truth for local data flow. `ReachingDefinitionIndex` owns
path-sensitive definition state. `DependencyGraphBuilder` owns dependency collection and passes
the active definitions at each call to the callback-effect resolver. `BackwardDecisionSlicer`
consumes use-site snapshots and does not infer control flow again.

## Technical Decisions

### Decision 1: Keep initializers in the flow state

**Context:** The index already replaces definitions on assignment and merges reachable branch
states.

**Decision:** Remove the global initializer filter from snapshots.

**Rationale:** Flow state, not a method-wide assigned-name set, decides if an initializer reaches a
use. This change fixes conditional overwrites and keeps full overwrites excluded.

### Decision 2: Resolve callback definitions at the call site

**Context:** PR #15 needs local callback definitions that are active when a stream or collection
operation is called.

**Decision:** Keep callback-definition lookup as a separate call-site concern in
`DependencyGraphBuilder` and preserve the path-sensitive result-slice index.

**Rationale:** This keeps callback effect analysis and result slicing separate and gives each
consumer the correct flow snapshot.

### Dependency Decisions

No new dependencies are introduced.

## Component Design

### ReachingDefinitionIndex

**Responsibility:** Compute immutable local definition snapshots for each syntax tree.

### DependencyGraphBuilder

**Responsibility:** Collect syntax parents, returns, throws, effects, and callback-definition uses.

### BackwardDecisionSlicer

**Responsibility:** Traverse definitions and effects that can affect returns, uncaught throws, or
caller-visible state.

## Testing Strategy

- Add a fixture where a method-call initializer reaches one return path and one branch overwrites it.
- Keep the full-overwrite regression contract.
- Run the focused analyzer contract before the full PR gate.

## Risks and Mitigations

- **Risk:** A dead initializer returns to graphs. **Mitigation:** Keep and run the full-overwrite test.
- **Risk:** Conflict resolution drops callback changes. **Mitigation:** Run all PR #15 callback tests.
- **Risk:** Conflict resolution drops failure or audit changes. **Mitigation:** Run all PR #12 contracts.
