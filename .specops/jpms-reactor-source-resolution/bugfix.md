# Bug Fix: JPMS Reactor Source Resolution

## Problem Statement

Reactor-wide analysis sends all Java sources, including multiple `module-info.java` files, to one javac task. Javac rejects that input with `too many module declarations found`, so the new feature cannot analyze JPMS reactors.

## Root Cause Analysis

`AnalyzeMojo.sourceFiles` treats Maven module descriptors as decision sources. The analyzer needs classes and methods for graph extraction, but it does not use JPMS declarations. Combining descriptors from separate modules changes several valid module compilations into one invalid javac compilation unit set.

**Affected Components:**

- Maven compile-source discovery in `AnalyzeMojo`
- Multi-module Maven integration fixture

**Error Symptoms:**

- Source attribution stops before graph extraction.
- Maven reports `too many module declarations found`.

## Impact Assessment

- **Severity:** High
- **Users Affected:** Users who run reactor-wide analysis on two or more JPMS modules
- **Frequency:** Always when the analyzed source union contains multiple module descriptors

## Reproduction Steps

1. Add `module-info.java` to both modules in the reactor fixture.
2. Run the Maven plugin for the decision-entry module.
3. Expected: the plugin resolves sibling implementations and writes the graph.
4. Actual before the fix: javac rejects the combined source set because it contains two module declarations.

## Regression Risk Analysis

### Blast Radius

- `AnalyzeMojo.sourceFiles` supplies current-module roots and reactor-wide resolution sources.
- `ProjectGraphGenerator` must still receive Java decision sources and skip source-empty modules.
- Single-module and non-JPMS Maven integrations use the same discovery path.

### Behavior Inventory

- Current-module annotations remain the only graph roots.
- Sibling implementation classes remain available as dispatch candidates.
- Duplicate and missing source roots remain deterministic and safe.
- Source-empty modules remove stale output and skip.
- Maven, not Fachtracing, compiles and validates module descriptors.

### Test Coverage Assessment

- **Covered:** current-module root isolation and sibling dispatch candidates → `AnalyzeMojoTest`
- **Covered:** source-empty skip → `AnalyzeMojoTest`
- **Covered:** single-module execution → `scripts/verify.sh` basic fixture
- **Gap:** multiple JPMS descriptors in a real reactor → reactor fixture lacks `module-info.java`

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| Sibling dispatch resolution | Must-Test | It directly uses the filtered source union. |
| Current-module root isolation | Must-Test | Root and resolution sources share the same discovery helper. |
| JPMS Maven compilation | Must-Test | It is the reported failure. |
| Source-empty skip | Nice-To-Test | The filter can reduce a root set to empty. |
| Runtime tracing | Low-Risk | No runtime module changes. |

### Scope Escalation Check

**Scope:** Contained. The analyzer does not consume module declarations, so a focused discovery filter fixes the root cause without a new abstraction or API.

## Proposed Fix

Exclude files named `module-info.java` from the Maven plugin's analyzer source lists. Keep the files in the project so Maven compiler executions still validate JPMS boundaries. Add module descriptors to both reactor fixture modules and require the generated graph to retain both sibling candidates.

## Unchanged Behavior

- WHEN Maven analyzes a reactor THE SYSTEM SHALL CONTINUE TO use only current-module annotations as graph roots.
- WHEN sibling implementation sources exist THE SYSTEM SHALL CONTINUE TO include compatible implementations as dispatch candidates.
- WHEN a current module has no decision sources THE SYSTEM SHALL CONTINUE TO skip and remove stale generated output.

## Testing Plan

### Current Behavior

- WHEN one javac task receives two `module-info.java` files THE SYSTEM CURRENTLY fails with `too many module declarations found`.

### Expected Behavior

- WHEN the two-module JPMS fixture runs THE SYSTEM SHALL generate one decision graph with two sibling candidates.

### Unchanged Behavior

- WHEN the basic single-module fixture runs THE SYSTEM SHALL CONTINUE TO generate its existing graph.
- WHEN engine and Maven executable contracts run THE SYSTEM SHALL CONTINUE TO pass all existing assertions.

## Acceptance Criteria

- [x] Regression Risk Analysis is complete for High severity.
- [x] The multi-descriptor failure is reproduced before the fix.
- [x] The JPMS reactor fixture generates the expected graph after the fix.
- [x] All Must-Test unchanged behaviors pass.
- [x] The full repository verifier passes.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Do not use subagents.
