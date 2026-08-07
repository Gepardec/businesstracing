# Bug Fix: JDK Mutation and Alias Effect Correctness

## Problem Statement

The analyzer can publish a `COMPLETE` graph after it removes logic that changes the returned
decision. An unlisted JDK mutation, such as `Deque.offer`, is treated as read-only. A mutation made
through a direct local alias is not connected to the source parameter. Both errors can remove the
mutation call and its controlling predicate.

## Root Cause Analysis

1. `isSupportedLibraryOperation` treats all `java.*` and `javax.*` calls as supported read-only
   operations after a small mutation-name list fails to match.
2. `mutationSummary` marks only direct parameter and field names. It does not keep an alias relation
   for a local reference assignment such as `alias = target`.

## Impact and Blast Radius

- **Severity:** High.
- **Affected users:** Any application with result-relevant mutable JDK values or helper methods that
  mutate a parameter through a local alias.
- **Frequency:** Deterministic for the affected source forms.
- **Code surface:** attributed call effects, source mutation summaries, backward slicing, graph
  completeness, analyzer fixtures, capability documentation, and Mega Backend conformance.
- **Unaffected behavior:** runtime tracing, activation bundles, storage, and application results.

## Behavior and Test Inventory

- A known JDK mutation on result-dependent state must remain in the graph.
- Its controlling predicate must remain in the graph.
- A source helper mutation through a direct local alias must map back to the caller argument.
- A JDK or `javax` reference call is read-only only when the analyzer has an explicit purity
  contract. Otherwise, a result-relevant call must create a source-located coverage gap.
- Known immutable value operations, ignored application reads, and five Mega graphs must retain
  their current behavior.

Existing tests cover `List.add`, callback mutation, unknown application effects, value operations,
and Mega graphs. They do not cover `Deque.offer`, an unknown JDK reference call, or a direct local
alias inside a source helper.

## Requirements

1. WHEN an annotated method mutates a result-dependent `Deque` with `offer`, THE GRAPH SHALL include
   that mutation and each predicate that controls it, and it SHALL be `COMPLETE` when all source is
   available.
2. WHEN a source helper assigns a reference parameter to a direct local alias and mutates the alias,
   THE MUTATION SUMMARY SHALL map the write to that parameter.
3. WHEN a local alias is reassigned to a value that is not a proved direct reference identity, THE
   ANALYZER SHALL remove the previous alias relation.
4. WHEN a result-relevant JDK or `javax` reference call has no proved mutation or purity contract,
   THE GRAPH SHALL be `INCOMPLETE` with a source-located, actionable side-effect gap.
5. THE ANALYZER SHALL NOT infer effects from application names, packages, or Mega-specific rules.
6. Mega Backend SHALL still produce the five expected complete graphs.
7. Standard and external activation verification SHALL pass. The 600-second, 1,000-RPS gate SHALL
   keep zero result changes, contamination, and silently lost records.

## Acceptance Criteria

- [x] False-before-fix fixtures reproduce both missing-business-logic defects.
- [x] `Deque.offer` and its controlling predicate occur in a complete graph.
- [x] A direct local alias helper and its controlling predicate occur in a complete caller graph.
- [x] Unknown JDK reference effects do not produce a false complete graph.
- [x] Existing read-only and mutation contracts pass.
- [x] Mega, external, standard, and long release gates pass.
- [x] No Mega-specific rule or new dependency is added.

## Scope Check

The scope is contained. This change corrects one shared static effect model. It does not add a new
public API or runtime protocol.
