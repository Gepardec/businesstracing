# Design: Omit Redundant Next Diagram Labels

## Architecture Overview

Mermaid and PlantUML are projections of the immutable business graph. Each renderer will map the exact outcome `next` to an empty display label when it writes an arrow. The graph model and analyzer will keep the original outcome, so stable edge IDs and runtime correlation do not change.

## Technical Decision

**Decision:** Apply the rule in both diagram renderers.

**Rationale:** The request concerns visible diagram noise. A renderer-only rule removes that noise without changing the graph contract or the immutable conformance oracles.

The comparison is exact. `next item` and `next entry` describe iteration behavior and remain visible.

## Testing Strategy

- Update the Mermaid and PlantUML snapshots to show an unlabeled ordinary sequence arrow.
- Add explicit renderer assertions that hide `next` and preserve `next item`.
- Run the engine test suite and the self-tracing verification.

### Dependency Decisions

No new dependencies are introduced.

