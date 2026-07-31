# Feature: Maven Developer Graph Export

## Overview

The Maven plugin stops at presentation diagrams, so developers cannot obtain the revision-pinned JSON artifact through the normal build workflow.

## Developer Use Cases

### Use Case 1: Generate developer JSON during a Maven build

**As a** developer using the Fachtracing Maven plugin
**I want** the plugin to write developer JSON when source-link settings are present
**So that** another tool can render the graph and open the exact committed source

**Acceptance Criteria (EARS):**

- WHEN both the repository URL and source URL template are configured THE SYSTEM SHALL write one UTF-8 `*-developer.json` artifact for each analyzed decision.
- WHEN developer JSON is written THE SYSTEM SHALL add its relative link to `index.md` beside the Mermaid and PlantUML links.
- IF only one source-link setting is configured THEN THE SYSTEM SHALL fail with a message that names both required settings.
- IF the Git worktree is dirty, an analyzed source is absent from the captured commit, or source content does not match that commit THEN THE SYSTEM SHALL fail before it writes new output.
- WHEN source-link settings are absent THE SYSTEM SHALL continue to generate the existing diagram formats without Git access.
- WHEN prior generated JSON exists but the next run does not generate developer JSON THE SYSTEM SHALL remove the stale generated JSON without deleting unrelated files.

**Progress Checklist:**

- [x] Maven writes revision-pinned JSON for configured builds.
- [x] The generated index links to the JSON artifact.
- [x] Partial or unsafe provenance settings fail explicitly.
- [x] Existing diagram-only builds remain compatible.
- [x] Stale generated JSON is removed safely.
- [x] Ignored or generated source that is absent from the commit is rejected.

### Use Case 2: Verify the interchange contract as a consumer

**As a** developer integrating a JSON graph tool
**I want** executable consumer-level contract checks
**So that** malformed JSON and missing coverage gaps do not pass through text-fragment assertions

**Acceptance Criteria (EARS):**

- WHEN the Maven contract reads a generated developer artifact THE SYSTEM SHALL parse the complete JSON with an independent test parser and verify the schema, commit, nodes, edges, and source URL.
- WHEN an incomplete graph is exported THE SYSTEM SHALL include its non-empty coverage gaps in the parsed JSON structure.

**Progress Checklist:**

- [x] A separate test parser accepts the complete generated document.
- [x] An incomplete graph proves non-empty coverage-gap export.

## API Design Principles

- Developer JSON remains opt-in because a correct source URL needs repository-specific settings.
- Diagram-only generation remains the default and does not invoke Git.
- Maven supplies build paths; the engine continues to own the JSON contract and Git validation.
- No production or test dependency is added.

## Compatibility Requirements

- Existing Maven plugin invocations continue to work without configuration changes.
- Developer JSON is always UTF-8, independent of the diagram source encoding.
- Generated file names use the same deterministic collision handling as diagram files.

## Out of Scope

- A hosted visualizer, browser application, or IDE plugin.
- Guessing repository-browser URL formats from Git remote strings.
- Relaxing the clean-commit requirement.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Do not use subagents.
- Keep repository data outside business records and diagrams.
