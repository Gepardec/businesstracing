# Feature: Configured Endpoint Business Tracing

## Overview

Teams cannot inspect a third-party Java endpoint with Fachtracing unless they edit its source. This feature lets a team select an endpoint method in configuration, call it with the Java agent active, and receive a business-only text explanation and Mermaid flow for that invocation.

## User Stories

### Story 1: Select an endpoint without source changes

**As a** system analyst
**I want** to select an exact Java endpoint method and give it a business label
**So that** I can trace code that I do not own.

**Acceptance Criteria (EARS):**

- WHEN configuration names an owner, method, optional parameter types, and business label THE SYSTEM SHALL analyze that method as a graph root without a `@FachTracing` annotation.
- IF an entry-point selection is absent or matches more than one overload THEN THE SYSTEM SHALL stop analysis with a precise configuration error.
- WHEN configured roots and annotated roots occur together THE SYSTEM SHALL analyze each distinct root once and use the configured business label for a configured root.

**Progress Checklist:**

- [x] Exact configured roots work without source changes.
- [x] Missing and ambiguous selections fail clearly.
- [x] Configured and annotated roots remain compatible.

### Story 2: Explain one called endpoint

**As a** business analyst
**I want** one text file and one Mermaid flow for each completed endpoint call
**So that** I can understand the selected business path without Java terms.

**Acceptance Criteria (EARS):**

- WHEN the agent starts with an activation file and output directory THE SYSTEM SHALL activate all selected graphs without application bootstrap code.
- WHEN a selected endpoint call completes THE SYSTEM SHALL write one deterministic business explanation and one Mermaid flow that contains the evaluated steps and result.
- IF an endpoint returns `null` or an unsupported object type THEN THE SYSTEM SHALL still complete an incomplete trace with a safe generic result.
- THE SYSTEM SHALL redact captured values in automatic file output and SHALL NOT write Java owner, method, descriptor, source path, or exception data to business files.

**Progress Checklist:**

- [x] Agent arguments activate tracing and file output.
- [x] Each completed call writes text and Mermaid artifacts.
- [x] Arbitrary endpoint return types do not discard the trace.
- [x] Automatic output is redacted and business-only.

### Story 3: Prove the workflow on external applications

**As a** maintainer
**I want** Mega and Keycloak examples
**So that** I can verify the feature on realistic brownfield endpoint code.

**Acceptance Criteria (EARS):**

- WHEN the Mega conformance gate runs THE SYSTEM SHALL select its five reviewed roots without an annotation overlay and SHALL preserve the reviewed results.
- WHEN a maintainer follows the pinned Keycloak example THE SYSTEM SHALL select `UsersResource.getUsers` as `search users` without editing Keycloak Java source.
- THE SYSTEM SHALL keep Mega and Keycloak names out of production classes and generic configuration types.

**Progress Checklist:**

- [x] Mega uses configuration instead of a source overlay.
- [x] A pinned Keycloak endpoint example is documented and reproducible.
- [x] Production remains reference-application neutral.

## Non-Functional Requirements

- Existing annotation-based analysis and programmatic agent configuration must remain compatible.
- Probe failures must not change endpoint control flow.
- The automatic writer must perform file I/O only on its daemon thread.
- The feature must use the Java standard library and current project dependencies only.

## Constraints & Assumptions

- A user selects the Java method that implements the endpoint. Automatic HTTP route discovery is out of scope.
- The Keycloak example uses `GET /admin/realms/{realm}/users`, implemented by `org.keycloak.services.resources.admin.UsersResource.getUsers` at the pinned revision.
- Keycloak build and runtime integration can take longer than the repository's three-minute pull-request gate, so its full external run is an explicit conformance command.
- The existing `activation.json` contract supplies graphs, probe plans, fingerprints, and the agent JAR option.

## Dependencies & Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| — | — | — | — |

### Cross-Spec Blockers

| Blocker | Blocking Spec | Resolution Type | Resolution Detail | Status |
| --- | --- | --- | --- | --- |
| — | — | — | — | — |

## Success Metrics

- All current core, Maven, Mega, PetClinic, and PostgreSQL checks remain green.
- The Mega gate produces the same five reviewed graphs without applying a Java source patch.
- One executable contract proves automatic text and Mermaid output for two endpoint calls.

## Out of Scope

- Automatic discovery of JAX-RS, Spring MVC, or other HTTP routes.
- A hosted trace viewer.
- Storage of request bodies, credentials, tokens, or unredacted endpoint values.
- A Keycloak-specific production adapter.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep each component responsible for one function.
- Keep external-application knowledge in conformance and documentation files.
