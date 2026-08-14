# Design: Self-Traced Core Algorithms

## Architecture Overview

The business projector already has one authoritative node classifier. Mark that method as a traced
decision. Extract the source-selection block from `StaticDecisionAnalyzer` into one package-private
component, mark its selection method as a traced decision, and call it for every project.

```text
exact node -> traced projectNode -> kept or removed business node

application boundary -> traced source selector -> no request
                                            \-> analysis request -> static analyzer -> graph
```

The current analyzer and generic output pipeline create all Mermaid files. No diagram code or
algorithm-specific renderer is added.

## Technical Decisions

### Decision 1: Trace the Existing Projection Classifier

**Decision:** Extract the final reason switch from `projectNode` into
`BusinessGraphProjector.classifyNode`, add
`@FachTracing("include exact node in business graph")` to it, and call it from `projectNode`.

**Rationale:** The classifier becomes the production authority for every keep or remove reason.
`projectNode` still owns node creation. The narrow method does not pull label-cleaning mechanics
into the traced business projection.

### Decision 2: Extract One Source Selector

**Decision:** Add package-private `AnalysisSourceSelector` with a traced `select` method. Move the
project-closure algorithm into this component. Return an empty selection for projects without entry
sources. Return an immutable request, selected project closure, and modular flag for other projects.

**Rationale:** Source selection is a separate responsibility from Java parsing and graph creation.
The extracted method becomes both the traced algorithm and the production call path.

### Decision 3: Keep Source Roles Explicit

**Decision:** Build the request from these inputs:

- root files: current project entry sources;
- source files: selected project resolution sources plus external resolution sources;
- classpath: every connected project classpath;
- compiler character set: current project compiler model;
- modular context: connected projects that have module descriptors.

**Rationale:** The graph must explain the different roles. Entry sources create decision graphs.
Resolution sources only resolve reachable code.

### Decision 4: Extend the Existing Self-Proof

**Decision:** Make `verify-self-tracing.sh` require both new file sets, current classifier/source
labels, production call-site evidence, and equal audit checksums from two runs.

**Rationale:** The existing generated-output gate already proves deterministic, input-driven
rendering. Extending it prevents manual diagrams and missing annotations from passing.

## Module Design

### `BusinessGraphProjector`

**Responsibility:** Project exact graph nodes and record final projection reasons.

**Change:** Trace the extracted final-reason classifier and call it from `projectNode`. Do not
change projection rules.

### `AnalysisSourceSelector`

**Responsibility:** Select one project analysis request from an application source boundary.

**Failure response:** Rely on validated boundary project dependencies and immutable request
validation. Reject null inputs.

### `StaticDecisionAnalyzer`

**Responsibility:** Run flat or modular graph analysis for a selected request.

**Change:** Consume `AnalysisSourceSelector.Selection`; do not select inputs locally.

### Self-Tracing Gate and Guide

**Responsibility:** Prove generated algorithm files and tell maintainers where each traced method is
called.

## Compatibility

- `StaticDecisionAnalyzer` public methods keep their signatures.
- `BusinessGraphProjector` public methods keep their signatures and output.
- `AnalysisSourceSelector` is package-private.
- No schema, runtime record, activation version, or Maven parameter changes.

## Testing Strategy

- Verify that source selection omits projects without entry sources.
- Verify that flat selection includes the full connected project source closure, external sources,
  all connected classpaths, and only current-project entries as roots.
- Verify that modular selection removes non-modular project sources and retains connected
  classpaths.
- Reuse projection classification tests and require the traced production annotation.
- Generate all project self-graphs twice and compare the new audit files.
- Run the full repository verifier.

## Risks and Mitigations

- **Risk:** Extraction changes project closure behavior. **Mitigation:** Move the same dependency
  and reverse-dependency traversal and add direct selection tests.
- **Risk:** The self-analyzer cannot model one Java construct in the new methods. **Mitigation:**
  keep explicit coverage gaps and assert current generated labels, not a hand-written topology.
- **Risk:** More annotations add runtime activation entries. **Mitigation:** the runtime test selects
  its decision by label and registers all generated graphs.

## Dependency Decisions

No new dependency is introduced. Current Java records, collections, streams, and the Fachtracing
annotation provide the required behavior.
