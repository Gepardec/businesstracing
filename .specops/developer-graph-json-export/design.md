# Design: Developer Graph JSON Export

## Architecture Overview

The analyzer already emits a business graph and a developer-only manifest keyed by the same opaque node IDs, but consumers receive them only as Java objects. A new developer projection combines those existing artifacts with an explicitly captured clean Git revision and emits deterministic JSON. External tools can render `nodes` and `edges`, then open a node's revision-pinned `source.url` without learning absolute build-machine paths.

## Technical Decisions

### Decision 1: Add a developer projection, not source fields on the business model

**Decision:** Add `DeveloperGraphExporter` under a new exported developer package. It accepts `AnalysisManifest.AnalysisResult` and `SourceRevision` and returns JSON.

**Rationale:** The analysis result is the only existing boundary containing both graph topology and source mappings. Projecting there preserves the established rule that business records contain no Java coordinates.

### Decision 2: Capture Git provenance strictly

**Decision:** `SourceRevision.captureGit` invokes Git through `ProcessBuilder`, resolves the repository root, requires a clean working tree, and records `HEAD` plus its committer timestamp.

**Rationale:** A commit identifier only proves source content when the analyzed files match that commit. Rejecting dirty trees prevents links from pointing at code different from the exported labels and line numbers.

### Decision 3: Use caller-supplied source URL templates

**Decision:** A template must contain `{commit}` and `{path}` and may contain `{line}` and `{column}`. The exporter substitutes encoded repository-relative values.

**Rationale:** GitHub, GitLab, Bitbucket, and internal browsers use different URL layouts. A template makes navigation portable without vendor branches in Fachtracing.

### Decision 4: Serialize JSON without a new dependency

**Decision:** Implement a focused deterministic JSON writer inside the exporter.

**Rationale:** The schema contains fixed records, lists, maps, strings, numbers, and booleans. A small encoder keeps the engine dependency-free and avoids approving a general JSON library for one projection.

### Decision 5: Verify analysis fingerprints before export

**Decision:** Before it writes JSON, the exporter recomputes SHA-256 for each analyzed source file and compares it with the analysis manifest.

**Rationale:** A repository can move to another clean commit after analysis. Clean Git state alone does not prove that the captured commit contains the analyzed code. The fingerprint check closes that gap.

## Module Design

### Developer Graph Exporter

**Responsibility:** Convert one analysis result and one clean source revision into versioned JSON.

**Interface:**

```java
String json = new DeveloperGraphExporter().export(
        analysis,
        DeveloperGraphExporter.SourceRevision.captureGit(
                repositoryRoot,
                "https://github.com/acme/rules",
                "https://github.com/acme/rules/blob/{commit}/{path}#L{line}"));
```

### Source Revision

**Responsibility:** Hold validated repository metadata and convert an absolute analyzer path into a safe repository-relative path and revision-pinned URL.

**Failure behavior:** Git command failure, dirty state, missing URL placeholders, and paths outside the repository root throw an explicit exception before JSON is returned.

## JSON Contract

```json
{
  "schema": "fachtracing-developer-graph/v1",
  "graph": {
    "id": "opaque-id",
    "version": 1,
    "label": "Eligibility",
    "completeness": "COMPLETE",
    "nodes": [
      {
        "id": "opaque-node",
        "kind": "PREDICATE",
        "label": "age is below 24",
        "attributes": {},
        "source": {
          "path": "src/main/java/example/Policy.java",
          "line": 42,
          "column": 9,
          "syntaxKind": "LESS_THAN",
          "sha256": "...",
          "url": "https://host/repo/blob/<commit>/src/main/java/example/Policy.java#L42"
        }
      }
    ],
    "edges": [],
    "coverageGaps": []
  },
  "sourceRevision": {
    "repository": "https://host/repo",
    "commit": "<full commit>",
    "committedAt": "2026-07-31T08:00:00+02:00"
  }
}
```

`source` is omitted for synthetic nodes. Fingerprints come from the existing analysis manifest and are normalized to repository-relative keys before lookup.

## Security and Data Handling

- **Classification:** Repository coordinates are internal developer data and remain outside confidential/restricted business records.
- Absolute filesystem paths are rejected and never serialized.
- URL templates are data only; the exporter performs placeholder substitution and does not execute their content.
- JSON encoding escapes quotes, backslashes, control characters, and U+2028/U+2029.

## Testing Strategy

- Analyze the existing eligibility fixture and verify all graph IDs and source-backed coordinates appear in valid deterministic JSON.
- Verify no workspace absolute path occurs in the output.
- Verify source URLs contain the supplied commit and repository-relative path.
- Exercise clean and dirty temporary Git repositories through `captureGit`.
- Exercise escaping and path-containment failures.

## Dependency Decisions

No new dependencies introduced. Java compiler APIs, `ProcessBuilder`, and the existing SHA-256 source fingerprints cover the complete feature.

## Risks and Mitigations

- **Risk:** A source URL template produces an unusable link. **Mitigation:** Validate required placeholders and test substitution while leaving host-specific syntax to the integrator.
- **Risk:** Source mappings do not exist for synthetic dispatch alternatives. **Mitigation:** Omit `source` rather than inventing a coordinate.
- **Risk:** Analysis and export occur at different revisions. **Mitigation:** Strict Git capture rejects any working-tree difference; source fingerprints remain in the export for independent verification.
