# Requirements: Explicit opaque library boundaries

## Overview

The analyzer must not infer that all dependency JARs are technical libraries. A dependency can contain business rules. The user must select each technical dependency that the analyzer can treat as an opaque operation boundary.

## User Story

As a Fachtracing user, I want to select technical library artifacts explicitly, so that the analyzer does not report a complete graph when an unselected dependency can contain hidden business logic.

## Functional Requirements

1. THE SYSTEM SHALL treat source-unavailable dependency code as decision-bearing by default.
2. WHEN a user selects a resolved Maven dependency with an exact `groupId:artifactId` coordinate THE SYSTEM SHALL use only that artifact's exact compile-classpath JAR path as an opaque library boundary.
3. WHEN a selected library has a reference-returning operation THE SYSTEM SHALL keep the library internals outside the graph and preserve source-visible receiver effects.
4. WHEN a selected library Boolean operation is used as a source control predicate THE SYSTEM SHALL keep the source predicate without claiming that other binary Boolean decisions are understood.
5. IF a configured coordinate is invalid or is not a resolved compile-classpath JAR THEN THE SYSTEM SHALL fail before graph extraction with a clear message.
6. THE SYSTEM SHALL support the same explicit boundary configuration in `analyze` and `analyze-reactor`.

## Safety Requirements

- WHEN a dependency JAR is not selected THE SYSTEM SHALL mark result-relevant source-unavailable calls from that JAR as incomplete.
- WHEN a direct Boolean dependency decision affects a result THE SYSTEM SHALL continue to use controlled bytecode analysis or mark the graph incomplete, including when its artifact is selected.
- WHEN the first binary owner is an application class directory THE SYSTEM SHALL mark unsupported decision logic as incomplete.
- THE SYSTEM SHALL not contain application-specific artifact names or package allowlists.

## Acceptance Criteria

- [x] Default engine analysis keeps the compiled dependency-JAR fixture incomplete.
- [x] Explicit selection of that exact JAR makes both reference-operation fixture graphs complete.
- [x] The selected fixture keeps the maximum-number, owner, and date predicates visible.
- [x] Direct Boolean dependency logic and application class-directory logic stay incomplete.
- [x] Maven coordinate resolution accepts resolved compile JARs and rejects invalid, missing, or directory-only selections.
- [x] Both Maven goals expose and pass the explicit boundary.
- [x] Strict Hogarama analysis fails without selection and passes with the required technical artifacts selected.
- [x] Public documentation explains the fail-closed default, coordinate syntax, and trust risk.
- [x] All repository and pull-request gates pass.

## Scope Assessment

This work has one independent deliverable: make external binary trust explicit. The engine contract, Maven adapter, tests, and documentation are coupled parts of that boundary. Decomposition is not useful.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep one responsibility in each component.
- Do not use subagents.
- Commit, push, update the pull request, and verify CI.
