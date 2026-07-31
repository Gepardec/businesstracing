# Feature: Exact Runtime Decision Path Capture

## Overview

The current runtime can lose failed calls and can infer a branch that the application did not record. This feature records each supported branch edge and each terminated invocation so that a decision record does not claim an inferred path as observed fact.

## Developer Use Cases

### Use Case 1: Record the selected branch

**As a** Java developer who traces a business decision
**I want** the runtime to record the selected static graph edge
**So that** the stored execution shows the branch that the invocation took

**Acceptance Criteria (EARS):**

- WHEN a supported simple or final Java 21 `javac` conditional selects its fall-through path THE SYSTEM SHALL record the matching opaque `true` edge before the next business node.
- WHEN a supported simple or final Java 21 `javac` conditional selects its jump path THE SYSTEM SHALL record the matching opaque `false` edge before the next business node.
- WHEN a supported `&&` or `||` predicate uses more than one bytecode jump THE SYSTEM SHALL record exactly one edge for the result of the full source predicate.
- WHEN a non-final operand of a supported flat `&&` predicate jumps THE SYSTEM SHALL record only the full predicate's `false` edge.
- WHEN a non-final operand of a supported flat `||` predicate jumps THE SYSTEM SHALL record only the full predicate's `true` edge.
- IF a compound predicate has a mixed, nested, negated-compound, or ambiguous bytecode mapping THEN THE SYSTEM SHALL use legacy evaluated-node probes for the complete predicate group and SHALL NOT emit an exact edge.
- IF an analyzed predicate does not have one `true` edge and one `false` edge THEN THE SYSTEM SHALL use the legacy evaluated-node probe and SHALL NOT claim an exact edge.
- IF runtime branch metadata names an edge that does not leave the observed predicate THEN THE SYSTEM SHALL ignore the edge and SHALL NOT present it as observed.

**Progress Checklist:**

- [x] A true branch records its exact edge and outcome.
- [x] A false branch records its exact edge and outcome.
- [x] A predicate without an exact branch binding remains explicit and safe.
- [x] Invalid branch metadata does not create observed path evidence.
- [x] A supported short-circuit predicate records one non-contradictory result edge.

### Use Case 2: Record a failed invocation

**As a** consumer of decision records
**I want** an annotated invocation that throws to produce a failed execution
**So that** the absence of a normal result does not remove the attempted decision from the trace

**Acceptance Criteria (EARS):**

- WHEN an exception leaves an instrumented decision entry THE SYSTEM SHALL queue one failed execution and SHALL rethrow the same exception object.
- THE SYSTEM SHALL exclude the Java exception class, stack trace, and raw message from the business execution record.
- WHEN nested annotated decisions fail THE SYSTEM SHALL close each active invocation once and SHALL preserve the application exception flow.

**Progress Checklist:**

- [x] An explicit exception creates a failed execution.
- [x] An exception from a called method creates a failed execution.
- [x] Failure capture does not expose technical exception data.

### Use Case 3: Correlate nested polymorphic dispatch

**As a** Java developer who traces nested business rules
**I want** each invocation to own a stack of expected dispatch calls
**So that** a nested or re-entrant call cannot overwrite the outer dispatch selection

**Acceptance Criteria (EARS):**

- WHEN an invocation expects more than one nested dispatch THE SYSTEM SHALL match implementation entries in last-in-first-out order.
- IF an implementation entry does not match the current expectation THEN THE SYSTEM SHALL ignore it without changing the expectation.
- WHEN an invocation ends THE SYSTEM SHALL remove only that invocation's pending dispatch state.

**Progress Checklist:**

- [x] Nested dispatch expectations use invocation-local stack order.
- [x] A wrong implementation entry does not consume an expectation.
- [x] Invocation cleanup does not change its parent context.

## Library Quality Requirements

- Performance: THE SYSTEM SHALL keep probe work in memory and SHALL perform no file or database input/output on the application thread.
- Compatibility: Existing successful `DecisionExecution` constructors and projections SHALL continue to work.
- Privacy: Business records SHALL contain opaque graph identifiers, redacted values, branch outcomes, and generic failure state only.
- Reliability: Probe failures SHALL remain separate diagnostics and SHALL not change application results or exceptions.

## Constraints and Assumptions

- Exact conditional-edge binding applies to Java 21 bytecode produced by `javac` and protected by the existing class fingerprint check.
- Exact compound binding in this increment supports simple predicates and homogeneous flat `&&` or `||` groups. Other compound shapes use the legacy fallback.
- This spec does not add durable storage, cross-thread context transfer, multi-graph agent configuration, or operand-level predicate evidence.
- No new dependency is required.
- Repository text follows ASD-STE100 Simplified Technical English.

## Dependencies and Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `generic-tracing-walking-skeleton` | It defines the graph, runtime, agent, and record contracts that this feature extends. | Yes | completed |

## Success Metrics

- Contract tests record the correct edge for both outcomes of one transformed predicate.
- Analyzer-to-transformer contracts record one correct edge for all decisive paths of flat `&&` and `||` predicates.
- Contract tests record failed executions for explicit and propagated exceptions.
- The current `main` verification script, including the Maven plugin contracts, passes with no application result, exception, concurrency, or diagram regression.

## Out of Scope

- Durable repository adapters and crash-safe delivery.
- Executor, callback, and reactive context transfer.
- One agent registry for many active manifests.
- Complete runtime support for binary-only, proxy, reflection, or lambda dispatch targets.
- Exact edge binding for mixed or nested short-circuit expressions and ambiguous synthetic methods.

## Team Conventions

- Use ASD-STE100 Simplified Technical English for repository text.
- Do not use subagents.
