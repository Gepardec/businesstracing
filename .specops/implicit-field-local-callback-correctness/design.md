# Design: Implicit Field and Local Callback Correctness

## Field State Roots

`StaticDecisionAnalyzer` owns attributed source information. It supplies enclosing field names to
`DependencyGraphBuilder`. The builder adds these names to the state roots that survive a conditional
definition join. This keeps attribution outside the syntax-only state tracker.

## Local Callback Resolution

`DependencyGraphBuilder` supplies an immutable active-definition snapshot with each invocation.
`StaticDecisionAnalyzer` resolves an identifier argument to its reachable callback definitions and
classifies each definition with the existing lambda and member-reference logic.

The dependency result also links callback definition trees to their invocation use sites. Flow
analysis uses this link to keep the mutation label and the source-located Boolean-result gap.

## Conservative Merge

An effect is proven when all reachable callback definitions prove it. An effect that only some
definitions prove is possible. This rule prevents a false complete result after a conditional
callback assignment.

## Dependencies

No dependency or build descriptor changes are required.
