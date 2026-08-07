# Design: Context label symbol correctness

## Architecture Overview

The compiler already attributes each declaration, identifier, and invocation before graph
extraction. The fix uses that shared semantic model for subject identity, type, and static mutation
target selection. It does not add a name dictionary or a post-processing pass.

## Technical Decisions

### Decision 1: Use compiler elements as subject keys

**Decision:** Store local subjects by their attributed `Element`. Resolve an identifier to its
element before a subject lookup. If an identifier is a field or parameter without a stored local
subject, derive its subject from that element and type.

**Rationale:** An element represents one declaration and remains distinct when another declaration
uses the same spelling in a different lexical scope.

### Decision 2: Use attributed declaration types

**Decision:** Derive primitive, declared, array, and generic subject data from the variable
element's `TypeMirror`. Use syntax only as a safe fallback when attribution is unavailable.

**Rationale:** The attributed mirror contains the inferred type for `var` and the same generic type
information used for explicit declarations.

### Decision 3: Share static mutation classification

**Decision:** Use one static utility classifier for supported `Collections` and `Arrays` writes.
For these calls, render the first argument as the receiver and render only later arguments as
operands.

**Rationale:** Effect slicing already defines the first argument as the changed state. Label text
must use the same semantic target.

## Component Design

### Subject lookup

**Responsibility:** Bind declarations and receiver identifiers to one attributed compiler element.

### Type subject resolution

**Responsibility:** Convert one attributed type mirror into a business-facing subject.

### Static mutation target selection

**Responsibility:** Select the changed target and remaining operands for one supported platform
call.

## Testing Strategy

1. Extend the existing generic label fixture with `var`, static utility, and shadowed-field cases.
2. Add exact executable assertions for the required labels and forbidden stale labels.
3. Run the complete static analyzer contract.
4. Run the exact pull-request verification script.

## Risks and Mitigations

- **Risk:** Compiler attribution is unavailable for a malformed source. **Mitigation:** Keep the
  existing syntax-based type fallback.
- **Risk:** Static utility label rules diverge from effect rules. **Mitigation:** Use one classifier
  in both paths.
- **Risk:** A field has no stored local subject. **Mitigation:** Derive its subject directly from the
  field element, then use the existing expression label only when attribution is unavailable.

### Dependency Decisions

No new dependencies are introduced.
