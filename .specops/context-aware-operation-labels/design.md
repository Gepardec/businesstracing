# Design: Context-aware operation labels

## Architecture Overview

The scanner already owns attributed source context for each local and call. The change uses that
context at label-generation time. It does not add a later text-rewrite pass and it does not add a
domain dictionary.

## Technical Decisions

### Decision 1: Resolve the local subject in `FlowScanner`

**Decision:** Store one subject for each visited local. Use the source name when it is meaningful.
For a one-letter reference local or a local that is a prefix of its type, use the simple declared
type. When a generic collection has the same name as its container, add its element type. Use
`item` when the type is a primitive or does not give a useful subject.

**Rationale:** The scanner has the declaration and the later receiver call in one method context.
This gives a stable relation between `c` and `Calendar` without global state or application rules.

### Decision 2: Render generic mutations from their source operands

**Decision:** For a member call named exactly `set` with two arguments, emit
`set <receiver subject> <first argument> to <second argument>`. For `add(value)`, emit
`add <value> to <receiver subject>`. Keep the existing rule for named setters such as `setValue`.

**Rationale:** The first argument identifies the changed property or position. The second argument
identifies the assigned value. `evaluate set` loses both facts.

### Decision 3: Reject the known meaningless output forms

**Decision:** Make `BusinessArtifactGuard` reject raw one-letter computation labels and the exact
fallback labels `list`, `evaluate set`, and `evaluate add`.

**Rationale:** A regression must fail the graph contract instead of returning unclear output.

## Component Design

### `StaticDecisionAnalyzer.FlowScanner`

**Responsibility:** Derive source-context-aware business labels while it builds the graph.

### `BusinessArtifactGuard`

**Responsibility:** Detect technical labels in a completed business graph.

## Testing Strategy

1. Add source fixtures for short locals, generic collections, and generic mutations.
2. Add assertions for the exact useful labels and the absence of the context-free forms.
3. Regenerate the complete Hogajama graph and audit its unique labels.
4. Run the engine analyzer contracts and the repository verification script.

## Risks and Mitigations

- **Risk:** A one-letter primitive becomes a Java type label. **Mitigation:** Use the declared type
  only for reference types; use `item` for primitives.
- **Risk:** A different `set` API has non-property semantics. **Mitigation:** Limit the new form to
  the common two-argument member call and preserve the source operands without guessing a domain.

### Dependency Decisions

No new dependencies are introduced.
