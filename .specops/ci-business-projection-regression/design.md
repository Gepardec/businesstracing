# Design: CI Business Projection Regression

## Architecture Overview

The exact analyzer remains the source of semantic evidence, the business projector keeps all
externally visible terminal outcomes, and release scripts verify the boundary between exact and
business artifacts. The fix changes only those boundary rules. It does not change JSON schemas,
graph IDs, or graph topology outside the removed terminal filter.

## Technical Decisions

### Use neutral punctuation for aggregate qualifiers

**Decision:** `aggregateQualifier` returns plain evidence. `AggregateBusinessLabelRenderer` joins
the optional qualifier as `subject — collection: condition — qualifier`.

**Rationale:** The renderer owns presentation punctuation. The analyzer must not encode call
syntax in a semantic value. An em dash stays language-neutral and keeps all four source roles.

### Keep every exact terminal edge

**Decision:** Remove owner-name filtering from terminal-result creation.

**Rationale:** An owner suffix does not prove that a failure is irrelevant to the business result.
An exact terminal edge is stronger evidence than the class name. Existing technical-node reduction
still removes infrastructure mechanics from the path.
The existing label policy uses the source decision name when it has semantic evidence, so a
complete graph does not fall back to the generic `operation failed` text.

### Prefer direct semantic evidence to owner suffixes

**Decision:** Reduce predicates and material actions before classifying an enclosing owner as
infrastructure. Keep a clear normalized source label unless it is a known technical placeholder.

**Rationale:** A controller can contain user-visible validation and persistence outcomes. The
predicate, action, terminal edge, and normalized source label are direct evidence. An owner suffix
is indirect evidence and must not delete or rename stronger business semantics. Logical-complement
wrappers without a called business method and duplicate negative forms remain technical details.

### Verify reduction at the correct artifact boundary

**Decision:** Self-tracing searches the exact structure for the technical choice and searches the
projection audit for its removal reason.

**Rationale:** Requiring the choice in the business graph contradicts semantic reduction. The two
assertions prove both discovery and intentional removal without a hard-coded diagram.

### Keep independent viewer work parallel and bound integration separately

**Decision:** Move standalone viewer verification from the PostgreSQL job to a sixth parallel job.
Keep storage, dogfood generation, viewer build, and all 17 browser tests together. Give this
integration job five minutes. Keep the five independent jobs at three minutes.

**Rationale:** Remote step timing shows that the independent viewer gate consumes 50 seconds before
the integrated journey starts. The next run shows that the integrated boundary itself needs more
than three minutes. A five-minute limit preserves all browser coverage without slowing independent
feedback.

## Component Design

### AggregateBusinessLabelRenderer

**Responsibility:** Join source-derived aggregate roles with neutral punctuation.

### BusinessGraphProjector

**Responsibility:** Map every exact terminal edge to a business result and preserve traceability.

### BusinessSemanticReducer

**Responsibility:** Prefer direct predicate and action semantics, remove wrapper conditions, and
use owner roles only as a fallback classification signal.

### Self-tracing verifier

**Responsibility:** Assert artifact-boundary contracts for generated self-analysis output.

## Testing Strategy

- Add focused analyzer assertions for qualified and unqualified aggregate labels.
- Extend the projector failure contract with infrastructure-owner semantic attributes.
- Run self-tracing twice to keep its determinism check.
- Run Mega, PetClinic, viewer dogfood, and the full pull-request gate.
- Push the commit and wait for all required pull-request checks.
- Require five parallel jobs to finish within three minutes and PostgreSQL within five minutes.

## Risks and Mitigations

- **Risk:** Failure paths become noisy. **Mitigation:** The projector still removes technical path
  nodes; it keeps only the externally visible terminal result.
- **Risk:** Punctuation removes a qualifier. **Mitigation:** Focused tests assert all source roles.
- **Risk:** A script change weakens proof. **Mitigation:** Assert both the exact source node and the
  audited reduction reason.

### Dependency Decisions

No new dependencies are introduced. The fix uses existing Java and shell code.
