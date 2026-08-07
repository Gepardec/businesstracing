# Feature: Explainable Relevance and Polymorphic Dispatch

## Overview

The analyzer builds a business graph from the parts of a Java method that can change its result. Developers also need to know why a construct is in the graph, why a construct is not in the graph, and which implementations are possible for a polymorphic call.

## Developer Use Cases

### Use Case 1: Inspect inclusion and exclusion decisions

**As a** developer who reviews a generated graph
**I want** a source-mapped analysis decision for included, excluded, and unresolved constructs
**So that** I can verify what the analyzer added and what it did not add

**Acceptance Criteria (EARS):**

- [x] WHEN the analyzer creates a source-derived graph node, THE SYSTEM SHALL add an `INCLUDED` decision with its node ID, source location, construct kind, and relevance reason to the developer-only analysis manifest.
- [x] WHEN a graph-eligible source construct cannot affect the sliced result, THE SYSTEM SHALL add an `EXCLUDED` decision with reason `NO_RESULT_EFFECT` and no node ID.
- [x] WHEN the analyzer creates a coverage-gap node, THE SYSTEM SHALL add a `GAP` decision with the node ID and reason `UNRESOLVED_RELEVANCE`.
- [x] THE SYSTEM SHALL keep analysis decisions out of the business graph and runtime activation payload.

### Use Case 2: Keep relevant expressions and remove unrelated branch work

**As a** developer who reads a decision graph
**I want** the relevance test to distinguish a relevant expression from unrelated work in the same branch
**So that** the graph has no audit, logging, or preparation call that does not affect the result

**Acceptance Criteria (EARS):**

- [x] WHEN a construct is in the backward slice, is an ancestor of a sliced construct, or is inside a sliced expression, THE SYSTEM SHALL treat it as relevant.
- [x] WHEN a call is only in the body of a relevant control statement and does not affect the result, THE SYSTEM SHALL exclude it from the graph.
- [x] WHEN a call is inside a relevant return expression, predicate expression, or data expression, THE SYSTEM SHALL keep it eligible for extraction.
- [x] WHEN different branches assign a value that can affect the result, THE SYSTEM SHALL consider every branch assignment relevant.
- [x] WHEN a source `throw` can terminate an analyzed path, THE SYSTEM SHALL keep the failure path relevant.
- [x] WHEN a final Java `Enum` query is outside the result slice, THE SYSTEM SHALL exclude it without reporting a possible mutation gap.

### Use Case 3: Explain Java polymorphic dispatch

**As a** developer who analyzes an interface or abstract call
**I want** all proven compatible implementations and their selection decisions
**So that** the static graph does not guess which implementation will run

**Acceptance Criteria (EARS):**

- [x] WHEN a polymorphic call has a concrete, source-visible, receiver-compatible implementation, THE SYSTEM SHALL keep it as a dispatch alternative and add an `INCLUDED` decision with reason `DISPATCH_CANDIDATE`.
- [x] WHEN a source-visible subtype is abstract, THE SYSTEM SHALL not add it as an alternative and SHALL add an `EXCLUDED` decision with reason `ABSTRACT_IMPLEMENTATION`.
- [x] WHEN a source-visible contract subtype is not compatible with the receiver type, THE SYSTEM SHALL not add it as an alternative and SHALL add an `EXCLUDED` decision with reason `INCOMPATIBLE_IMPLEMENTATION`.
- [x] IF no compatible concrete implementation is available, THEN THE SYSTEM SHALL keep the existing visible coverage gap and SHALL not select an implementation.
- [x] THE SYSTEM SHALL preserve the runtime binding from an implementation entry to its static dispatch edge.

## Library Quality Requirements

- The change SHALL add no dependency.
- Existing `AnalysisManifest` constructor calls SHALL remain source compatible.
- Analysis decisions SHALL be immutable and deterministic.
- Each new class SHALL have one responsibility.

## Constraints and Assumptions

- The Java compiler attribution result is the authority for subtype and receiver compatibility.
- Static analysis lists possible implementations. Runtime evidence identifies the implementation that ran.
- Only source-visible implementations can be expanded into source-derived decisions.

## Out of Scope

- Whole-program classpath scanning, dependency injection container discovery, or runtime class loading.
- Control-flow graph, SSA, or symbol-identity replacement of the current slicer.
- Changes to the business graph schema or developer graph JSON schema.
- Changes to runtime instrumentation or dispatch correlation.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Do not use subagents.
