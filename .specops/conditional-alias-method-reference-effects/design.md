# Design: Conditional Alias and Method-Reference Effects

## Architecture Overview

The analyzer already separates proved writes from possible writes. This change carries that same
distinction through local alias resolution and bound method-reference callbacks. The backward
slicer then includes proved effects and reports existing coverage gaps for possible result effects.

## Technical Decisions

### Decision 1: Merge alias roots by certainty

**Decision:** `LocalAliasResolver` returns proved and possible roots. An `if` merge keeps a root as
proved only when it is proved on every branch. All other reachable roots are possible.

**Rationale:** One merged state prevents a false complete result without claiming path-sensitive
precision that the analyzer does not have.

### Decision 2: Reuse normal mutation contracts for bound member references

**Decision:** Resolve each callback member reference with compiler attribution. For a bound value
receiver, apply the existing platform or source mutation summary to the qualifier expression.

**Rationale:** `accepted::add` and `accepted.add(value)` have the same receiver effect. One mutation
contract must classify both forms.

### Decision 3: Fail closed for unresolved callback targets

**Decision:** If the analyzer cannot prove a member-reference target or its effect, record possible
receiver state instead of treating the callback as read-only.

**Rationale:** Unsupported result-relevant Java logic must create a gap, not a false explanation.

## Module Design

### LocalAliasResolver

**Responsibility:** Track local reference roots and their certainty across direct assignments and
branch merges.

### DependencyGraphBuilder

**Responsibility:** Index proved and possible writes against the correct external state roots.

### StaticDecisionAnalyzer callback-effect classifier

**Responsibility:** Convert normal calls, lambdas, and bound member references into one
`CallEffects` contract.

## Failure Handling

- An unresolved member-reference target produces a possible receiver effect when the qualifier is
  a reference value.
- A root that is not present on every `if` path is possible, not proved.
- A cyclic local alias resolves through a guarded visited-name set.
- Unsupported callback parameter effects stay outside this narrow fix and fail closed where they
  can affect returned state.

## Testing Strategy

- Add one fixture and contract for conditional alias reassignment.
- Add one fixture and contract for a bound collection method-reference callback.
- Run the full static analyzer contract after each production task.
- Run the standard pull-request gate and check hosted CI before publication is complete.

## Documentation

Update `docs/supported-java-constructs.md` only if its alias or callback support statement needs the
new boundary.

## Dependency Decisions

No new dependencies are introduced. The implementation uses existing Java compiler-tree APIs and
Java collections.
