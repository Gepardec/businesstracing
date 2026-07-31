# Design: Maven Developer Graph Export

## Architecture Overview

The engine already creates a validated developer JSON string, while the Maven generator already owns per-decision file names and the output index. The change passes an optional developer-output configuration into the generator. The generator analyzes first, captures one clean Git revision before it changes the output directory, and uses the engine exporter for each decision.

## Technical Decisions

### Decision 1: Enable JSON through a complete configuration pair

**Decision:** Add `fachtracing.repositoryUrl` and `fachtracing.sourceUrlTemplate`. Both absent means diagram-only output; both present enables JSON; a partial pair fails.

**Rationale:** Repository browsers use different URL formats. Explicit values avoid unreliable remote-URL guessing and preserve existing builds.

### Decision 2: Capture Git after analysis and before output writes

**Decision:** The generator captures one `SourceRevision` only after it finds decisions and before it creates or cleans the output directory.

**Rationale:** Modules without decisions do not need Git. Capturing before writes prevents generated files from changing the cleanliness result in repositories that do not ignore the output directory.

### Decision 3: Validate JSON with a test-only parser

**Decision:** Add a small independent recursive JSON parser to the executable contract test.

**Rationale:** Raw substring assertions cannot prove that the complete document is valid JSON. A test-only parser closes this gap without adding a library dependency.

### Decision 4: Verify the captured Git blob

**Decision:** Compare each analysis fingerprint with both the current file and the file blob at the captured commit.

**Rationale:** Git status ignores ignored generated files. A clean worktree alone cannot prove that an analyzed file exists in `HEAD`.

## Module Design

### Analyze Mojo

**Responsibility:** Validate the Maven parameter pair and pass the repository root plus source-link settings to the generator.

### Project Graph Generator

**Responsibility:** Capture provenance once, write UTF-8 JSON beside each graph, link it from the index, and remove stale generated JSON.

### Developer Graph Exporter

**Responsibility:** Keep the existing schema and Git/source integrity rules. Its production API does not change; the Maven consumer contract parses its output.

## Public Plugin Configuration

```xml
<configuration>
  <repositoryUrl>https://github.com/acme/decision-rules</repositoryUrl>
  <sourceUrlTemplate>https://github.com/acme/decision-rules/blob/{commit}/{path}#L{line}</sourceUrlTemplate>
</configuration>
```

The same values are available as `-Dfachtracing.repositoryUrl=...` and `-Dfachtracing.sourceUrlTemplate=...`.

## Failure Behavior

- A partial parameter pair fails before analysis output changes.
- Dirty Git state, missing Git blobs, missing Git metadata, out-of-root source paths, and fingerprint mismatches fail the goal.
- Diagram-only mode never invokes Git and preserves current behavior.

## Testing Strategy

- Generate from a temporary clean Git repository and parse the complete JSON artifact.
- Verify commit-pinned URLs, UTF-8 content, index links, and stale cleanup.
- Verify partial settings and dirty Git repositories fail.
- Verify ignored source that is absent from the commit fails.
- Parse an incomplete Maven-generated graph and assert its coverage-gap object.
- Run the complete repository verification script.

## Dependency Decisions

No new dependencies introduced. Production uses the existing engine exporter. Tests use a local test-only parser.

## Risks and Mitigations

- **Risk:** Strict Git validation surprises users. **Mitigation:** JSON is opt-in and the guide explains the clean-tree rule.
- **Risk:** Output encoding changes JSON bytes. **Mitigation:** JSON always uses `StandardCharsets.UTF_8`.
- **Risk:** Old JSON survives after configuration removal. **Mitigation:** Cleanup recognizes only the generated `*-developer.json` suffix.
