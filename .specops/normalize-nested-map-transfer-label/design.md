# Design: Normalize nested map transfer labels

## Architecture Overview

Keep graph discovery and exact technical labels unchanged. Extend the business-language projection
at its existing normalization boundary, before the artifact guard and all business renderers run.

## Technical Decisions

### Decision 1: Normalize the complete nested action

**Decision:** Match `add map <subject> <subject> <detail> to <target>` and produce
`add converted <subject> <detail> to <target>`.

**Rationale:** The repeated subject comes from the mapper operation and its input type. A back
reference proves the structural shape without an application-specific vocabulary list.

### Decision 2: Keep the guard strict

**Decision:** Do not weaken the prohibited `map` rule.

**Rationale:** The guard is the final business-output safety boundary. The normalizer must remove
known implementation syntax before the guard runs.

## Module Design

### `BusinessLanguageNormalizer`

**Responsibility:** Convert one additional nested implementation phrase to business language.

### `BusinessGraphProjectionTest`

**Responsibility:** Prove the generic rewrite and all existing projection behavior.

### `MegaBackendConformanceTest`

**Responsibility:** Write the existing business JSON contract and schema for all five generated
Mega graphs. The harness selects source entry points; it does not hardcode graph topology.

## Testing Strategy

- Add a generic order-domain fixture label, not a Hogarama-specific label.
- Run the focused executable projection test.
- Run the full pull-request verification.
- Run strict analysis on the pinned Hogarama checkout and validate both JSON documents.
- Generate and validate the pinned Mega JSON documents.

## Risks & Mitigations

- **Risk:** The rewrite drops a meaningful word. **Mitigation:** Require an exact repeated subject by
  regular-expression back reference.
- **Risk:** The guard becomes weaker. **Mitigation:** Do not change the guard.

## Dependency Decisions

No new dependencies are introduced.

## Documentation Decision

The public output contract does not change. No user documentation change is required.
