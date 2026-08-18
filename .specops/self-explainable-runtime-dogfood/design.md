# Design: Self-Explainable Runtime Dogfood

## Architecture Overview

The existing analyzer produces an exact graph and an analysis manifest. The business projector converts the exact graph into the reader graph. This change keeps both outputs and adds immutable decision records at each boundary. A developer renderer groups those records into deterministic Mermaid. The Maven writer emits the new files beside existing artifacts.

Two small production policy methods expose real decisions that existing algorithms use. Root Maven configuration selects those methods without annotations. A test runner executes the compiled methods with the normal agent and activation bundle.

## Technical Decisions

### Decision 1: Record decisions before rendering

**Decision:** Store structured audit records in the analysis and projection models. Render Mermaid only from those records.

**Rationale:** The diagram then proves what the code decided. The renderer cannot invent a topology.

### Decision 2: Keep audit output developer-only

**Decision:** Put source locations, Java kinds, and reason codes only in audit and developer artifacts.

**Rationale:** Business outputs must remain free of technical provenance.

### Decision 3: Extract production policy methods

**Decision:** Extract node inclusion and source-set policy into single-responsibility methods that their current callers use.

**Rationale:** Fachtracing can select and execute the authoritative policy. No duplicate demonstration algorithm is necessary.

### Decision 4: Use configured roots

**Decision:** Select both self-analysis methods with `businessEntryPoints` in the root build.

**Rationale:** This applies the merged PR 27 feature to the project and does not add source annotations for the example.

## Component Design

- `BusinessGraphProjection`: owns immutable projection decision records.
- `BusinessGraphAudit`: pairs the final summarized graph with final decision relations.
- `DecisionAuditMermaidRenderer`: groups recorded analysis or projection decisions and renders Mermaid.
- `BusinessGraphProjector`: classifies each exact node once and records the result.
- `AnalysisSourceSelector`: selects the analysis source policy and builds the request used by `StaticDecisionAnalyzer`.
- Maven graph writer: writes audit files and links them from the generated index.
- Self-verification runner: executes representative compiled calls under the Java agent and checks generated paths.

## Testing Strategy

- Model tests validate immutable decisions and referential integrity.
- Projector tests cover every keep, remove, replacement, and unreachable reason.
- Renderer tests prove deterministic grouping and data dependence.
- Maven tests prove file creation, index links, and stale-file cleanup.
- The self-verification script runs reactor analysis twice, compares bytes, executes five runtime paths, and rejects fixed example text in production.
- Existing repository and external conformance scripts prove compatibility.

## Security and Dependencies

The change adds no dependency and no network call. Audit files can contain source locations and are developer artifacts. Runtime business output keeps the existing redaction and isolation rules.
