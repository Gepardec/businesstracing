# Bug Fix: Context-aware operation labels

## Problem Statement

The static analyzer emits labels such as `c`, `comp`, `list`, `evaluate set`, and `evaluate add`
for result-relevant Java code. These labels do not tell a business reader what object changes or
which value changes.

## Root Cause Analysis

`StaticDecisionAnalyzer.FlowScanner` uses the local variable spelling for construction nodes. It
does not use the declared type when the spelling is a short temporary name. It also does not use a
generic collection element type when the local has the same name as its container type. The
invocation label code handles named setters such as `setValue`, but it does not handle generic
`set(property, value)` or `add(value)` operations. It therefore uses context-free fallbacks.

**Affected Components:**

- Static graph label generation in `StaticDecisionAnalyzer`.
- Technical-label detection in `BusinessArtifactGuard`.
- Static analyzer contract fixtures and tests.

## Impact Assessment

- **Severity:** Low
- **Users Affected:** Readers of graphs that contain short local names, generated collection names,
  or generic setter and collection methods.
- **Frequency:** Deterministic for each affected source form.

## Reproduction Steps

1. Analyze a traced method that builds a `Calendar c` and calls `c.set(field, value)`.
2. Expected: each node states the object, property, and value.
3. Analyze generated mapper and comparator code with `list`, `comp`, and `list.add(value)`.
4. Actual: the graph contains context-free local and operation nodes.

## Regression Risk Analysis

### Blast Radius

- `FlowScanner.visitVariable` creates result-relevant local derivation nodes.
- `FlowScanner.invocationLabel` creates source call and mutation labels.
- `BusinessArtifactGuard` validates all generated graph nodes.
- `StaticDecisionAnalyzerTest` covers existing label and slice behavior.

### Behavior Inventory

- Meaningful local names remain the preferred business subject.
- Named setters such as `setValue` keep their current receiver and property labels.
- Dynamic calls, coverage gaps, topology, probes, and source mappings do not change.

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| Existing meaningful labels remain stable | Must-Test | The new subject resolver runs in the common scanner. |
| Generic setter and collection-add labels include their operands | Must-Test | These are the changed call-label paths. |
| Graph topology and completeness remain stable | Nice-To-Test | The change does not change slice or control-flow code. |

Minimal regression risk applies because the change is limited to labels and a label guard.

## Proposed Fix

Keep one local subject map in `FlowScanner`. Use the declared type for a one-letter local or a local
that is a prefix of its type. For a generic collection named after its container, include the element
type. Render `set(property, value)` and `add(value)` with receiver and argument context. Extend the
artifact guard so the known context-free forms fail validation.

## Unchanged Behavior

- WHEN a local has a meaningful name that is not a type abbreviation THE SYSTEM SHALL CONTINUE TO
  use that name.
- WHEN a method uses a named setter such as `setValue` THE SYSTEM SHALL CONTINUE TO show its receiver
  and property.

## Testing Plan

### Current Behavior

- WHEN the analyzer reads `Calendar c` and `c.set(Calendar.HOUR_OF_DAY, hour)` THE SYSTEM CURRENTLY
  emits `c` and `evaluate set`.

### Expected Behavior

- WHEN the analyzer reads the same code THE SYSTEM SHALL emit `calendar` and
  `set calendar hour of day to hour`.
- WHEN the analyzer reads `Comparator<SensorData> comp` THE SYSTEM SHALL emit `comparator`.
- WHEN the analyzer reads `List<SensorData> list` THE SYSTEM SHALL emit `sensor data list`.
- WHEN the analyzer reads `list.add(sensorData)` THE SYSTEM SHALL include the value and receiver.
- WHEN the analyzer emits a known context-free fallback THE ARTIFACT GUARD SHALL report a violation.

### Unchanged Behavior

- WHEN existing analyzer fixtures run THE SYSTEM SHALL CONTINUE TO pass all label, slice, and graph
  contracts.

## Acceptance Criteria

- [x] The new fixture reproduces `c` and `evaluate set` before the production fix.
- [x] One-letter reference locals use a useful declared-type subject.
- [x] Type abbreviations and generic collection names use useful declared-type context.
- [x] Generic two-argument setter labels state the receiver, property, and value.
- [x] Generic collection-add labels state the value and receiver.
- [x] The complete Hogajama graph has none of the known context-free fallback forms.
- [x] Existing focused and repository verification checks pass.
