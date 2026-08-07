# Design: JDK Mutation and Alias Effect Correctness

## Design Summary

The analyzer uses a closed proof model. It classifies a call as a proved mutation, a proved
read-only operation, or an unknown reference effect. Unknown result-relevant effects make the graph
incomplete. Source mutation summaries also keep direct local alias roots and map alias writes back
to receiver or parameter state.

## Decisions

### 1. Use explicit JDK effect contracts

Extend the JDK mutation contract with standard collection, queue, deque, iterator, map, array,
mutable-text, and atomic mutation operations. Add a separate read-only contract for common JDK
value and collection queries. Do not use a `java.*` namespace as proof of purity.

If neither contract matches and source is not available, use the existing possible-reference-effect
path. It produces a coverage gap only when that state can affect the returned decision.

### 2. Track direct reference aliases in source order

During source mutation scanning, keep `local -> root names` for variable declarations and simple
assignments whose right side is an identifier, member selection, parenthesized identity, or cast
identity. Resolve aliases transitively when a write occurs. Clear an alias when a later assignment
is not a proved identity.

This is a local, flow-ordered proof. It does not guess aliases through arbitrary method returns,
array elements, or data structures. Unproved calls still use the unknown-effect contract.

### 3. Keep completeness fail-closed

A proved mutation enters the backward slice. An unknown effect does not become a guessed graph
node. If it can change returned state, the analyzer adds the existing source-located gap and marks
the graph incomplete.

## Failure Handling

- An unresolved call or type is an unknown effect, not a read-only call.
- A cyclic alias chain is stopped by a visited-name set.
- A reassigned alias loses its old root relation.
- Compiler attribution failures keep the existing analysis diagnostics.

## Verification

Add independent fixtures for `Deque.offer`, direct local alias mutation, alias invalidation, and an
unknown JDK reference effect. Run focused analyzer tests, standard verification, external activation,
Mega Backend conformance, and the clean long release gate.

## Dependencies

No new dependency is required. The implementation uses Java compiler tree and element APIs that are
already in the engine.
