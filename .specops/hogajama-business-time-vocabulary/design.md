# Design: Hogajama business time vocabulary

## Architecture Overview

The guard must identify structural markers, but its current patterns also match ordinary business
words inside longer labels. Full-label anchors keep terminal-marker validation in the guard and let
the analyzer preserve the source-derived business meaning.

## Technical Decisions

### Decision 1: Narrow the guard instead of changing analyzer labels

**Context:** `today start time` is valid business vocabulary. The analyzer label states the source
decision correctly.

**Options Considered:**

1. Rewrite analyzer labels that contain `start` or `stop`. This changes valid source meaning to meet
   an overbroad validation rule.
2. Anchor the two structural-marker patterns. This corrects the rule at its policy boundary.

**Decision:** Anchor the `start` and `stop` patterns to the complete label.

**Rationale:** The guard owns vocabulary policy. It must distinguish an exact structural marker from
an ordinary word in a business phrase.

## Module Design

### `BusinessLogicArtifactGuard`

**Responsibility:** Reject exact structural markers and other defined technical vocabulary in a
business-only graph.

**Interface:** Keep `violations` and `requireClean` unchanged.

### `BusinessGraphProjectionTest`

**Responsibility:** Prove accepted and rejected business vocabulary at the public guard boundary.

## Testing Strategy

- Add an acceptance contract for compound phrases that contain `start` and `stop`.
- Keep the existing rejection matrix, including exact `Start`.
- Run the full pull-request gate.
- Test the fix together with the strict Hogajama analyzer commit in a temporary integration branch.

## Risks & Mitigations

- **Risk:** A technical terminal marker could pass with surrounding whitespace. **Mitigation:** Graph
  labels are normalized values; the exact existing fixtures remain rejected.
- **Risk:** The change could weaken unrelated vocabulary rules. **Mitigation:** Change only two
  patterns and run the full matrix.

## Dependency Decisions

No new dependencies are introduced.

## Documentation Decision

The public business-artifact contract already says that technical control flow is removed. This fix
only corrects a false positive, so no user documentation change is required.
