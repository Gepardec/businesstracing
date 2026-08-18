# Feature: Self-Explainable Runtime Dogfood

## Overview

Fachtracing must explain two of its own production decisions with the same static and runtime pipeline that it gives to users. The outputs must show how the analyzer selects sources and how the business projector keeps or removes graph nodes. The program must derive all diagrams from source analysis and recorded execution. It must not use fixed diagrams or AI.

## User Stories

### Story 1: Explain projection decisions

**As a** maintainer
**I want** an audit diagram for each exact-to-business projection
**So that** I can see why each exact node is kept, removed, or replaced.

**Acceptance Criteria (EARS):**

- WHEN the projector handles an exact node THE SYSTEM SHALL record its source kind, label, action, stable reason, and final business-node relation.
- WHEN Maven writes a decision graph THE SYSTEM SHALL write a deterministic projection-audit Mermaid file from those recorded decisions.
- WHEN a projection removes structural, loop, redundant, technical, or unreachable input THE SYSTEM SHALL name the applicable generic reason.
- WHEN a projection keeps a business rule, business action, or coverage gap THE SYSTEM SHALL name the applicable generic reason.
- WHEN a terminal edge becomes a result THE SYSTEM SHALL record the replacement relation.

### Story 2: Explain source-relevance decisions

**As a** maintainer
**I want** an audit diagram for source analysis
**So that** I can see which source constructs enter the exact graph and why.

**Acceptance Criteria (EARS):**

- WHEN the analyzer includes, excludes, or cannot resolve a graph-eligible construct THE SYSTEM SHALL record its location, action, reason, and exact-node relation.
- WHEN Maven writes a decision graph THE SYSTEM SHALL write a deterministic analysis-audit Mermaid file from the analysis manifest.
- THE SYSTEM SHALL group repeated audit decisions and SHALL keep representative source evidence in the diagram.

### Story 3: Apply Fachtracing to itself

**As a** maintainer
**I want** generated static and runtime graphs for two production algorithms
**So that** I can understand and verify the tool through its own outputs.

**Acceptance Criteria (EARS):**

- WHEN the reactor analysis runs THE SYSTEM SHALL select two unannotated production methods through `businessEntryPoints`: node inclusion policy and analysis source selection policy.
- WHEN self-analysis finishes THE SYSTEM SHALL write exact structure, business, business JSON, analysis audit, projection audit, and activation artifacts for both methods.
- WHEN the node policy runs with representative inputs THE AGENT SHALL record both removal and retention paths.
- WHEN the source policy runs with representative inputs THE AGENT SHALL record no-entry, connected-source, and modular-source paths.
- WHEN an evaluated call completes THE SYSTEM SHALL write a Mermaid path whose result agrees with the actual method result.
- THE SYSTEM SHALL identify the selected owner and method in developer artifacts so that the reader can place each subgraph in its code context.

## Non-Functional Requirements

- Production code shall contain no Fachtracing-specific example graph, application name, or fixed topology.
- Diagram generation shall use no AI service and no network call.
- The same input shall produce byte-identical output.
- A changed synthetic input shall change the generated graph or audit relation.
- Business diagrams shall reject low-level Java and collection-processing vocabulary through the existing artifact guard.
- Existing annotation roots, configured roots, Keycloak, Mega, PetClinic, and PostgreSQL behavior shall remain compatible.

## Success Metrics

- All focused tests and repository gates pass.
- The two exact graphs are complete and have no coverage gap.
- Runtime proof covers five calls with no dropped or mixed call data.
- The pull request targets `main` and all required CI checks pass.

## Out of Scope

- A hand-authored overview diagram.
- AI-assisted labeling or graph generation.
- A new diagram syntax or hosted viewer.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep each component responsible for one function.
