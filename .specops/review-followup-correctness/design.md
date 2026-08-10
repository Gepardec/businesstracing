# Design: Review Follow-up Correctness

## Architecture

The analyzer fix adds one flow-ordered definition resolver beside the existing alias resolver.
Callback classification gets one syntax-unwrapping boundary. Predicate callbacks retain mutation
evidence but fail closed when the analyzer cannot reconstruct their Boolean result. The runtime fix
changes only cancellation-reserve calculation.

## Decisions

### Decision 1: Merge active definitions, not definition history

Each assignment replaces the active definition for its path. An `if` merge unions definitions from
the continuing branch states. This keeps both reachable values without restoring stale values from
an earlier unconditional assignment.

### Decision 2: Normalize callback syntax once

One helper removes parentheses and casts before lambda or member-reference classification. A second
helper climbs the same wrappers when it finds the callback invocation parent.

### Decision 3: Fail closed for mutating Boolean predicate callbacks

The graph keeps the proved receiver mutation. It also adds a coverage gap because the runtime and
static graph cannot reconstruct the callback Boolean outcomes from a platform implementation.

### Decision 4: Cap cancellation reserve

The reserve is half of short bounds and at most 500 ms for long bounds. This gives loaded hosts time
to stop the worker without reducing a normal 10-second graceful drain to 5 seconds.

## Components

- `LocalDefinitionResolver`: Own active local definitions and branch joins.
- `DependencyGraphBuilder`: Record dependencies with merged definitions.
- `BackwardDecisionSlicer`: Traverse every active definition.
- `StaticDecisionAnalyzer`: Normalize callbacks and reject unsupported Boolean callback topology.
- `DecisionRecordDelivery`: Calculate the bounded cancellation reserve.

## Testing

- Freeze every reported review scenario before production edits.
- Run the full analyzer and protocol executable suites after their tasks.
- Run the repository pull-request gate and hosted checks before completion.

## Dependencies

No new dependency is required.
