# Design: Keycloak Live Business Output Correctness

## Decision 1: Preserve Boolean Outcomes at Gaps

When a predicate frontier enters a coverage-gap node, keep a leading `true` or `false` outcome and
append the unresolved state. The runtime branch builder already accepts outcomes that start with
`true;` or `false;`. Non-Boolean frontiers continue to use `unresolved`.

This change records only the observed predicate result. It does not claim that the unavailable
logic is complete.

## Decision 2: Add an Automatic Business Presentation Boundary

Keep `DecisionExplanationProjector` as the exact developer explanation. Add a small business
projector for automatic files. It maps a successful unsupported result to `Completed` and maps all
technical coverage details to one safe statement. It does not read, enumerate, or wrap the result.

Add a separate text renderer for this projected business model. The renderer omits developer type
tags. The existing Mermaid path renderer consumes the same safe model.

## Compatibility and Failure Behavior

- Existing activation bundles remain readable.
- Existing exact developer explanations and runtime records do not change.
- Endpoint return identity, stream behavior, values, and exceptions do not change.
- Incomplete analysis stays visible as incomplete coverage.
- Automatic values remain redacted.

## Verification

1. Prove branch binding with a focused analyzer test.
2. Prove the presentation boundary with an agent file-sink test.
3. Prove the two Keycloak input predicates in pinned conformance.
4. Run the same real Keycloak HTTP request and inspect the generated text and Mermaid files.
5. Run the full local and hosted gates.

## Dependency Safety

No dependency is added, removed, or updated. The change uses current engine and agent classes.
