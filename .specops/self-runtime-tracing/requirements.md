# Feature: Self Runtime Tracing

## Overview

The project can generate a static graph from its own production code, but it does not run that
code through its own Java agent. Maintainers need one repeatable two-pass example that connects
Maven analysis, runtime instrumentation, captured paths, and the general Fachtracing algorithm.

## Developer Use Cases

### Use Case 1: Run the Project Through Its Own Runtime

**As a** Fachtracing maintainer
**I want** the project to execute its traced Maven-plugin policy through the generated activation
bundle and Java agent
**So that** I can verify the complete static-to-runtime flow on production code

**Acceptance Criteria (EARS):**

- WHEN a maintainer runs the self-tracing gate THE SYSTEM SHALL generate the activation bundle
  from the current reactor before it starts runtime capture.
- WHEN runtime capture starts THE SYSTEM SHALL use the Java agent and production classes from the
  same current build.
- IF the activation fingerprint does not match the loaded production class THEN THE SYSTEM SHALL
  fail the gate and SHALL NOT print the success marker.

### Use Case 2: Prove All Configuration Outcomes

**As a** maintainer who studies the traced policy
**I want** deterministic evidence for all three configuration outcomes
**So that** I can compare source control flow with observed runtime paths

**Acceptance Criteria (EARS):**

- WHEN both developer-output settings are absent THE SYSTEM SHALL capture one successful execution
  with the disabled result and at least one selected branch edge.
- WHEN both developer-output settings are present THE SYSTEM SHALL capture one successful execution
  with the enabled result and at least one selected branch edge.
- WHEN only one developer-output setting is present THE SYSTEM SHALL capture one failed execution,
  preserve the application exception, and include no technical exception details in the record.
- WHEN the three scenarios finish THE SYSTEM SHALL have exactly three completed executions, no
  agent diagnostic, and only the evidence-availability diagnostics declared by the activation
  bundle.

### Use Case 3: Understand the Two-Pass Algorithm

**As a** developer who evaluates Fachtracing
**I want** the self-tracing guide to explain Maven analysis and runtime capture as one flow
**So that** I can understand the project from a real example

**Acceptance Criteria (EARS):**

- WHEN a reader opens the self-tracing guide THE SYSTEM SHALL explain the static Maven pass, the
  activation bundle, the runtime agent pass, and the execution records.
- THE SYSTEM SHALL show that the static graph contains the invalid configuration path.
- THE SYSTEM SHALL give one command that generates and verifies both passes.

## Library Quality Requirements

- The change shall add no dependency.
- The runtime harness shall use the existing public activation, agent, collector, and value-adapter
  contracts.
- The runtime harness shall run outside the Maven process that creates the activation bundle.
- The repository verifier shall continue to use the existing self-tracing command.
- Repository text shall use ASD-STE100 Simplified Technical English.

## Constraints and Assumptions

- The traced production decision remains `ProjectGraphGenerator.developerOutput`.
- Runtime values of type `Optional` require an explicit safe value adapter.
- Java 21 is available through the same repository convention as the other runtime integration
  scripts.
- The active `release-gate-timeout-budget` spec can change CI scheduling, but it does not own the
  self-tracing harness or its script.

## Dependencies and Blockers

No required spec dependency exists. This feature extends the completed
`self-dogfood-business-tracing` and `runtime-decision-path-capture` contracts.

## Success Metrics

- One command generates the current activation bundle and records three production executions.
- The three executions prove disabled, enabled, and failed outcomes.
- The full repository verification passes with the runtime self-trace enabled.

## Out of Scope

- Adding annotations to more production decisions.
- Persisting the self-trace to a database.
- Running the agent inside the Maven JVM that creates the activation bundle.
- Changing the production decision or the runtime record model.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Use the single-responsibility principle.
- Do not use subagents.
