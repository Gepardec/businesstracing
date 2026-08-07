# Design: Analyze Annotation-Processor Output

## Architecture Overview

Maven owns annotation processing. Fachtracing owns a separate read-only attribution task. The
boundary converts the effective Maven compiler model into analysis-safe settings:

```text
Maven compile -> generated Java + target/classes + registered source roots
             -> MavenCompilerModelResolver removes processor execution settings
             -> StaticDecisionAnalyzer runs javac with -proc:none
             -> graph extraction reads original and generated Java
```

## Technical Decisions

### Decision 1: Sanitize processor controls instead of rejecting the project

**Context:** Processor configuration is required for Maven compilation but is not required for the
later Fachtracing attribution task.

**Options Considered:**

1. Keep rejection and require an analysis-only Maven profile. This makes normal processor projects
   change their build configuration for Fachtracing.
2. Run annotation processors again. This can execute arbitrary build code twice and can create
   different output.
3. Consume compiled output and generated Java with processing disabled.

**Decision:** Use option 3.

**Rationale:** It matches the existing `compile ...:analyze-reactor` lifecycle contract and keeps
analysis deterministic.

### Decision 2: Remove all processor-only compiler arguments

The resolver will remove `-A` options and processor selection, path, module-path, and processing-mode
arguments. It will also remove a separate value token for options that require one. It will retain
the order and uniqueness of all other safe arguments, then apply the existing validation rules.

### Decision 3: Keep fail-closed attribution

If a processor did not produce Java source or compiled output that the compiler task can resolve,
the existing compiler diagnostic failure remains. Fachtracing does not infer generated members and
does not claim support for AST-only transformations such as Lombok-generated members.

### Decision 4: Preserve Maven language-selection semantics

`source` plus `target` and `release` select different JDK API surfaces. The compiler model will
carry the selection mode with the normalized Java version. The analyzer will emit `-source` and
`-target` for the first mode and `--release` for the second mode. Existing engine callers keep the
strict release mode by default.

## Component Design

### `MavenCompilerModelResolver`

**Responsibility:** Convert effective Maven compiler configuration to deterministic analysis input.

**Changes:**

- Stop rejecting `proc`, `annotationProcessorPaths`, and `annotationProcessors` configuration.
- Sanitize processor-only entries from `compilerArgs`.
- Include `--default-module-for-created-files` and its value in processor-only sanitization.
- Keep validation for forked compilers, legacy argument maps, language level, classpath, output,
  encoding, and other analysis-owned settings.
- Expose the configured and registered generated source roots to Maven developer-provenance
  collection, including roots outside `project.build.directory`.

### Maven integration fixture

**Responsibility:** Prove the complete general Maven flow.

The fixture contains a small processor module and an application module. Maven compiles the
processor, generates an annotated Java decision in the application, registers the generated source
root, and then runs aggregate Fachtracing analysis in the same reactor session.

The processor module disables annotation processing while it compiles its own implementation. The
application module enables processing and loads the completed processor artifact. This keeps the
fixture deterministic on Java 21 and later Java releases.

## Security Considerations

- Data classification: Public build metadata and test source.
- Fachtracing must not load or execute configured annotation processors.
- `-proc:none` remains explicit for flat and modular compiler tasks.

## Testing Strategy

- Unit contract: processor configuration and processor-only arguments are sanitized.
- Unit regression: safe compiler settings remain unchanged and unrelated unsupported settings fail.
- Integration contract: a real Maven annotation processor creates Java that appears in the
  aggregate Fachtracing graph.
- Repository regression: run the standard verification script.

## Risks & Mitigations

- **Risk:** A processor option consumes a following argument. **Mitigation:** Sanitize arguments with
  an indexed pass and skip the value token for known two-token processor options.
- **Risk:** A generated directory is absent. **Mitigation:** Require `compile` in the documented
  invocation and keep compiler diagnostics fail-closed.
- **Risk:** Users assume Lombok is fully modeled. **Mitigation:** State that only source-visible or
  compiled-output-visible results are supported.

## Dependencies & Blockers

### Dependency Decisions

| Package | Version | Ecosystem | Decision | Rationale |
| --- | --- | --- | --- | --- |
| None | — | Java | Approved | The fix uses existing Maven and JDK compiler APIs. |

## Future Enhancements

- Add explicit diagnostics that distinguish a missing compile step from another attribution error.
- Add separate conformance coverage for AST-only transformation tools if a safe source model becomes
  available.
