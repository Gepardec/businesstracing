# Design: Generated Keycloak Diagram Correctness

## Architecture Overview

The conformance harness already receives one exact analysis result and projects it into a generic
business graph. The current output path bypasses that result and renders a second manual graph. The
fix removes the second model so one analysis result supplies both the exact activation graph and the
generated reader diagram.

## Technical Decisions

### Decision 1: Render the existing generic projection

**Decision:** Pass `fullBusinessGraph` to `BusinessMermaidRenderer`.

**Rationale:** This graph is the product output under test. Rendering it proves the complete path
from configured endpoint selection through generic projection to Mermaid.

### Decision 2: Keep reviewed anchors as assertions only

**Decision:** Keep a small set of reviewed Keycloak labels as output assertions, but never use them
to construct nodes or edges.

**Rationale:** Assertions can detect semantic loss without supplying the answer to the analyzer.

### Decision 3: Enforce the conformance boundary in repository integrity

**Decision:** Reject manual `BusinessLogicGraph` construction and embedded Mermaid flowcharts in
the Keycloak conformance source and guide.

**Rationale:** The repository rule is explicit. An executable guard prevents the same shortcut from
returning while generated build output remains untracked.

### Decision 4: Reject technical generated calculations generically

**Decision:** Make the generic artifact guard reject Java method-reference syntax. Make the generic
projector remove map population and intermediate collection setup while it keeps business actions.

**Rationale:** Pinned artifact inspection found technical data-building text that passed the old
guard. The correction must apply to all projects and must not contain Keycloak-specific labels.

## Component Design

### Keycloak conformance harness

**Responsibility:** Analyze the selected endpoint and write artifacts derived from that analysis.

**Interface:** Existing four command arguments and generated `search-users-business.mmd` plus
`activation.json` files.

**Failure mode:** Missing reviewed business anchors, technical business output, or invalid class
fingerprints stop the command.

### Repository integrity check

**Responsibility:** Reject tracked manual Keycloak diagrams.

**Interface:** Existing `scripts/verify-repository-integrity.sh` gate.

**Failure mode:** A fixed Keycloak graph or embedded flowchart stops repository verification with a
specific message.

### Generic business projection

**Responsibility:** Remove implementation calculations and reject Java syntax before rendering.

**Interface:** Existing `BusinessGraphProjector` and `BusinessLogicArtifactGuard` APIs.

**Failure mode:** Technical output stops projection or artifact validation.

## Sequence

```text
Pinned Keycloak source -> Static analyzer: exact graph and manifest
Exact analysis -> Business projector: generated business graph
Generated business graph -> Mermaid renderer: reader diagram
Exact analysis -> Activation bundle: runtime correlation data
```

## Testing Strategy

- Verify the current manual construction exists before the change.
- Run repository integrity after adding the manual-diagram guard.
- Run the focused Maven tests to preserve generic projection behavior.
- Run the full repository gate.
- Run the pinned Keycloak conformance command and inspect the generated artifact.

## Risks and Mitigations

- **Risk:** The generated graph is larger than the reviewed overview. **Mitigation:** Keep the
  artifact honest and remove only calculations that generic tests identify as implementation work.
- **Risk:** Removing the manual graph changes runtime activation. **Mitigation:** Do not change the
  activation construction; verify it still uses `analysis.graph()` and `analysis.manifest()`.

## Dependency Decisions

No new dependency is introduced.
