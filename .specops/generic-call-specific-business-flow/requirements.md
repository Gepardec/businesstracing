# Feature: Generic Call-Specific Business Flow

## Overview

The generated endpoint graph is too large to explain one real call, so a business reader cannot tell which rules affected that call. This feature generates a concise overview and an evaluated flow from target-neutral graph semantics and observed runtime evidence.

## User Stories

### Story 1: Generate a concise business overview

**As a** business analyst
**I want** a generated overview of an endpoint decision
**So that** I can see its main rules, actions, results, and known limits without Java details.

**Acceptance Criteria (EARS):**

- WHEN an exact decision graph contains adjacent coverage gaps THE SYSTEM SHALL replace each connected gap region with one business gap and preserve all paths into and out of that region.
- WHEN business nodes have the same kind, label, and outgoing business behavior THE SYSTEM SHALL represent them as one equivalent business state without removing a distinct result or branch outcome.
- WHEN a source or graph branch changes THE SYSTEM SHALL change the generated overview when the changed branch has different business behavior.
- THE SYSTEM SHALL derive every summary rule from node kind, label class, edge outcome, and graph topology, and SHALL NOT use an application name, package, method list, reviewed topology, or label dictionary in production code.

**Progress Checklist:**

- [x] Connected gap regions have one visible business gap.
- [x] Equivalent business states merge without semantic loss.
- [x] A semantic branch change changes generated output.
- [x] Production summary rules are application-neutral.

### Story 2: Show only what one call did

**As a** business analyst
**I want** one evaluated business flow for each endpoint call
**So that** I can explain the decisions that produced that call's result.

**Acceptance Criteria (EARS):**

- WHEN a completed execution selects an exact path THE SYSTEM SHALL map that path to the generated business graph and include only business nodes and edges supported by the observed or inferred exact path.
- WHEN two calls select different branches THE SYSTEM SHALL generate different business flows that show the selected outcome of each rule.
- WHEN the selected path reaches a result THE SYSTEM SHALL show the generated named business result instead of an arbitrary Java return value.
- IF the selected path contains unknown evidence or incomplete analysis THEN THE SYSTEM SHALL show one business-safe coverage gap and SHALL NOT guess the missing rule.
- IF runtime evidence does not match the activated graph version THEN THE SYSTEM SHALL reject the projection and SHALL NOT write a misleading flow.

**Progress Checklist:**

- [x] The evaluated flow contains only the selected path.
- [x] Different calls produce different flows.
- [x] The selected named result is visible.
- [x] Unknown behavior stays explicit and concise.
- [x] Graph-version mismatch fails closed.

### Story 3: Keep automatic output consistent

**As a** Java application owner
**I want** the automatic text and Mermaid files to use the same evaluated business flow
**So that** both files tell one consistent, non-technical story.

**Acceptance Criteria (EARS):**

- WHEN the agent writes automatic output THE SYSTEM SHALL derive both files from one call-specific business-flow model.
- THE SYSTEM SHALL perform projection and file input or output only on the daemon sink thread.
- THE SYSTEM SHALL NOT write Java owners, method names, descriptors, source paths, exception details, request values, tokens, or unredacted result values to automatic business files.
- WHEN an endpoint result has no safe value adapter THE SYSTEM SHALL retain the selected business result and mark coverage incomplete without consuming or changing the result object.

**Progress Checklist:**

- [x] Text and Mermaid use one evaluated model.
- [x] Endpoint threads keep the current non-blocking capture contract.
- [x] Automatic files contain no technical or private data.
- [x] Unsupported result objects do not change application behavior.

### Story 4: Prove general behavior before brownfield behavior

**As a** Fachtracing maintainer
**I want** unknown-project tests plus Mega and Keycloak checks
**So that** the examples validate the product instead of defining it.

**Acceptance Criteria (EARS):**

- WHEN the focused engine tests run THE SYSTEM SHALL pass synthetic applications and graphs whose names and topology do not occur in Keycloak or Mega.
- WHEN repository integrity runs THE SYSTEM SHALL reject production references to Keycloak, Mega, their selected owners, reviewed labels, or fixed diagram topology.
- WHEN the Mega conformance gate runs THE SYSTEM SHALL keep its reviewed generated graphs and runtime path valid through the generic mechanism.
- WHEN the Keycloak conformance gate runs THE SYSTEM SHALL generate its overview and an evaluated-path proof from the analyzed graph, and the test SHALL use reviewed Keycloak facts only as assertions.

**Progress Checklist:**

- [x] Synthetic unknown-project tests pass first.
- [x] Production contains no reference-application rule.
- [x] Mega remains valid.
- [x] Keycloak proves generated overview and evaluated selection.

## Non-Functional Requirements

- The output must be deterministic for equal graph and execution inputs.
- Projection work must stay off application request threads.
- The implementation must use the Java standard library and current project dependencies only.
- Existing exact graphs, activation files, developer explanations, and programmatic renderers must stay compatible.
- Business artifacts must pass `BusinessLogicArtifactGuard`.

## Constraints & Assumptions

- Exact runtime edge evidence and the existing shortest-path resolver remain the source of path truth.
- A business overview can share one equivalent state between several incoming paths. It must not invent a sequence between those paths.
- A call-specific flow can contain a loop rule once even when the call evaluates several loop items.
- Keycloak and Mega are black-box conformance inputs. Their names, labels, methods, and topology can occur only in conformance or documentation files.

## Dependencies & Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `generic-business-graph-projection` | It defines the business-only graph and guard. | Yes | Completed |
| `runtime-decision-path-capture` | It defines the exact observed-edge contract. | Yes | Completed |
| `configured-endpoint-business-tracing` | It defines automatic endpoint activation and file output. | Yes | Completed |

### Cross-Spec Blockers

| Blocker | Blocking Spec | Resolution Type | Resolution Detail | Status |
| --- | --- | --- | --- | --- |
| — | — | — | — | — |

## Success Metrics

- Two synthetic executions of one unknown-project graph produce different node and edge sets, and each output contains only its selected result.
- A synthetic repeated-gap graph contains one gap per connected gap region after summary and preserves every external incoming and outgoing path.
- A semantic mutation of a synthetic branch changes the generated Mermaid output.
- The generated Keycloak call flow contains only runtime-selected business states and no Java term; the full static graph remains available as developer evidence.
- A reviewer who does not know Java can identify the selected result, every shown rule outcome, and whether coverage is complete by reading only the generated call diagram.
- Focused tests, repository verification, Mega conformance, Keycloak conformance, and pull-request CI pass.

## Definition of Done

A person who does not know Java can call the Keycloak user-search endpoint, open the resulting diagram, and accurately explain the important decisions taken during that request.

## Out of Scope

- HTTP route discovery.
- A hosted trace viewer or interactive diagram user interface.
- Guessing behavior inside an unresolved library or lazy result consumer.
- Application-specific label dictionaries, topology files, or summary configuration.
- Removal of the exact developer graph.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Give each component one responsibility.
- Do not hard-code diagrams.
- Keep external-project knowledge in conformance files only.
