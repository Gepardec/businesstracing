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
- IF the selected path contains unknown evidence or incomplete analysis THEN THE SYSTEM SHALL show one business-safe coverage gap for each connected unknown region and SHALL NOT guess the missing rule.
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

### Story 5: Resolve source-visible call boundaries without duplicate gaps

**As a** business analyst
**I want** each visible gap to identify genuinely unavailable business behavior
**So that** the graph does not repeat an unknown-rule marker for behavior that the caller already states.

**Acceptance Criteria (EARS):**

- WHEN a source-unavailable call result controls an explicit source predicate THE SYSTEM SHALL use that caller predicate as the business rule and SHALL NOT add a duplicate call gap.
- WHEN a source-unavailable Boolean call is returned as the caller decision THE SYSTEM SHALL represent that call as one atomic business rule with both outcomes.
- WHEN a source-unavailable statement call can affect the caller result THE SYSTEM SHALL represent that call as one atomic business action.
- WHEN a source-unavailable value only selects a collaborator for later caller-visible rules or actions THE SYSTEM SHALL omit the collaborator lookup and preserve those later rules or actions.
- WHEN a source-unavailable callback configures a lazy transformation THE SYSTEM SHALL represent the configured transformation as one business action and SHALL NOT claim that the callback ran during the endpoint call.
- WHEN a caught failure path is explicit in source THE SYSTEM SHALL preserve the normal and caught outcomes without adding a separate unknown-trigger gap for the same call site.
- WHEN a nested binary type exists on the configured classpath THE SYSTEM SHALL resolve it by its JVM binary name.
- WHEN `javac` reports a continued short-circuit disjunction on the preceding source line THE SYSTEM SHALL keep each runtime branch aligned with its source predicate and SHALL NOT reuse this tolerance for an unrelated branch.
- IF a result-relevant call returns an opaque value and the caller gives it no rule, action, or result meaning THEN THE SYSTEM SHALL keep one coverage gap for that unresolved boundary.
- THE SYSTEM SHALL derive all boundary rules from Java type, use-site, control-flow, callback, and bytecode semantics and SHALL NOT use application package, class, method, label, or topology facts.

**Progress Checklist:**

- [x] Caller predicates remove duplicate call gaps.
- [x] Direct caller decisions become atomic business rules.
- [x] Caller-visible statement calls become atomic business actions.
- [x] Collaborator lookups do not create business nodes.
- [x] Lazy callbacks become configured actions, not claimed runtime decisions.
- [x] Explicit caught paths do not get duplicate trigger gaps.
- [x] Nested binary types use valid JVM names.
- [x] Multiline disjunction operands keep exact runtime outcomes without matching an unrelated branch.
- [x] Truly unresolved behavior stays visible once.
- [x] Production boundary rules are application-neutral.

### Story 6: Generate a clear static method overview

**As a** business analyst
**I want** the static endpoint graph to include every caller-visible business path
**So that** I can review the endpoint without a runtime call or an agent-selected path.

**Acceptance Criteria (EARS):**

- WHEN static analysis completes THE SYSTEM SHALL preserve every caller-visible rule, action, failure, and result path in the method and SHALL NOT select one runtime path.
- WHEN a callee source is unavailable THE SYSTEM SHALL keep the call as an atomic rule or action only when the caller use-site gives that meaning.
- WHEN the business graph contains collection mechanics, implementation types, or negated empty checks THE SYSTEM SHALL rewrite them as plain business language before rendering.
- IF the caller gives an unavailable value no business meaning THEN THE SYSTEM SHALL keep one explicit coverage gap and SHALL NOT invent meaning.
- THE SYSTEM SHALL derive all boundary and language rules from attributed Java use-sites and general phrase structure and SHALL NOT use a Keycloak package, class, method, label, or topology rule.

**Progress Checklist:**

- [x] Generic synthetic tests prove atomic rules, actions, collaborators, and one opaque-value counterexample.
- [x] Generic projection tests convert collection mechanics to plain business language.
- [x] The generated static graph contains all caller-visible method paths and no runtime selection.
- [x] The generated Keycloak static graph has no coverage gap and no prohibited technical term.

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
- The generated Keycloak static overview contains zero visible unknown-rule regions.
- The generated Keycloak static overview contains every caller-visible method path and does not use runtime observations.
- Two live Keycloak calls produce connected diagrams in which one rule cannot show both outcomes in the same call.
- A reviewer who does not know Java can identify the selected result, every shown rule outcome, and whether coverage is complete by reading only the generated call diagram.
- Focused tests, repository verification, Mega conformance, Keycloak conformance, and pull-request CI pass.

## Definition of Done

A person who does not know Java can open the static Keycloak user-search diagram and identify the start, every shown rule, each business action, the failure result, and the completion result without source-code help or a runtime call.

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
