# Bug Fix: Analyze Annotation-Processor Output

## Problem Statement

`analyze` and `analyze-reactor` reject every Maven project whose effective
`maven-compiler-plugin` model contains processor paths, processor names, or an enabled `proc`
setting. This prevents analysis of projects that already compiled generated Java source.

## Root Cause Analysis

`MavenCompilerModelResolver.rejectUnsupportedConfiguration()` treats Maven annotation-processor
configuration as an instruction for Fachtracing's private compiler task. The engine already forces
`-proc:none`, so the processor configuration is build-time provenance and must not be applied to
analysis. Processor-only `compilerArgs` can also conflict with the forced analysis mode.

**Affected Components:**

- `MavenCompilerModelResolver`
- Per-module `analyze` compiler-boundary construction
- Aggregate `analyze-reactor` compiler-boundary construction
- Maven plugin integration verification and documentation

**Error Symptoms:**

- A project with `annotationProcessorPaths` fails before graph extraction.
- Running `compile` before `analyze-reactor` does not help because the effective configuration is
  still rejected.

## Impact Assessment

- **Severity:** Medium
- **Users Affected:** Maven projects that configure annotation processors
- **Frequency:** Always for a selected project with the rejected effective settings

## Dependencies & Blockers

### Spec Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `generic-application-readiness` | It introduced the effective compiler-model resolver corrected here. | Yes | Completed |

### Cross-Spec Blockers

| Blocker | Blocking Spec | Resolution Type | Resolution Detail | Status |
| --- | --- | --- | --- | --- |
| — | — | — | — | — |

## Reproduction Steps

1. Configure `maven-compiler-plugin` with `annotationProcessorPaths`.
2. Run `mvn compile at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-rc.1:analyze-reactor`.
3. Expected: Fachtracing analyzes registered generated Java without executing processors.
4. Actual: Compiler-model validation rejects the selected project before graph extraction.

## Regression Risk Analysis

### Blast Radius

- Both Maven goals call `MavenCompilerModelResolver.resolve()` for each selected project.
- `StaticDecisionAnalyzer` builds flat and JPMS compiler tasks from the sanitized model.
- Compiler arguments affect compatibility checks for connected named modules.

### Behavior Inventory

- Encoding, Java release, preview, parameter, safe compiler arguments, source roots, and module path
  are preserved today.
- Forked compiler executables, legacy compiler argument maps, and analysis-controlled compiler
  options fail before extraction.
- Static analysis always disables annotation processing with `-proc:none`.
- Compiler attribution errors fail graph extraction instead of producing a false complete graph.

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| Safe compiler settings remain in the effective model | Must-Test | The resolver logic changes directly. |
| Analysis never executes annotation processors | Must-Test | Running external processor code would change the security and determinism boundary. |
| Unsupported non-processor compiler controls still fail | Must-Test | Over-broad filtering could hide an invalid compiler context. |
| Both Maven goals consume the same sanitized model | Must-Test | They are the two public entry points. |

## Proposed Fix

Treat annotation processing as a completed Maven build step. Ignore processor paths, explicit
processor names, and the Maven `proc` mode when the analysis model is built. Remove processor-only
arguments from `compilerArgs`. Continue to pass `-proc:none` explicitly to every Fachtracing
compiler task. Keep generated Java source roots and compiled reactor outputs as normal analysis
inputs. Do not claim source support for AST-only transformations that produce no equivalent Java.

## Unchanged Behavior

- WHEN a project supplies safe compiler settings THE SYSTEM SHALL CONTINUE TO preserve them.
- WHEN analysis starts THE SYSTEM SHALL CONTINUE TO disable annotation processing.
- WHEN source attribution fails THE SYSTEM SHALL CONTINUE TO fail without claiming complete coverage.
- WHEN a non-processor analysis-controlled compiler option is configured THE SYSTEM SHALL CONTINUE
  TO reject it before graph extraction.

## Testing Plan

### Current Behavior

- WHEN the effective compiler model contains a processor path THE SYSTEM CURRENTLY rejects it.

### Expected Behavior

- WHEN Maven compile registers generated Java from a configured processor THE SYSTEM SHALL include
  that Java in aggregate graph extraction without executing the processor again.
- WHEN processor-only compiler arguments are present THE SYSTEM SHALL omit them from the analysis
  compiler model.
- WHEN `--default-module-for-created-files` is present THE SYSTEM SHALL omit the option and its
  module-name value from the analysis compiler model.
- WHEN the compiler uses a generated source directory outside the Maven build directory THE SYSTEM
  SHALL preserve generated provenance for developer graph V2.
- WHEN the fixture processor module compiles on Java 21 THE SYSTEM SHALL disable annotation
  processing until the processor class exists.

### Unchanged Behavior

- WHEN safe compiler settings are present THE SYSTEM SHALL CONTINUE TO preserve them.
- WHEN a forked compiler, legacy argument map, or classpath override is present THE SYSTEM SHALL
  CONTINUE TO reject the unsupported setting.
- WHEN analysis runs THE SYSTEM SHALL CONTINUE TO give `javac` exactly one effective `-proc:none`
  processing mode.

## Acceptance Criteria

- [x] Regression Risk Analysis is complete for medium severity.
- [x] A resolver contract proves processor configuration is accepted and processor-only arguments
  are removed.
- [x] A Maven integration fixture generates Java during `compile` and `analyze-reactor` extracts its
  annotated decision.
- [x] Existing effective compiler-model and unsupported-setting contracts pass.
- [x] Documentation states the supported source-generating and unsupported AST-only boundaries.
- [x] Standard repository verification passes.
- [x] The complete set of supported `javac` processor-only arguments is removed.
- [x] Configured generated source roots keep generated provenance outside the build directory.
- [x] The annotation-processor fixture compiles from a clean state on Java 21.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep the Maven plugin generic. Do not add Hogajama-specific behavior.
- Do not use subagents.
