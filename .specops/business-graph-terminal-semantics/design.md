# Design: Business Graph Terminal Semantics

Create the `Start` node before extraction and lazily create one shared `Stop` node for every root
return or root failure. Each return adds a distinct outcome probe for the same Stop ID while its
incoming edge combines the control outcome with `returns <business expression>`. Failure paths use
a result-relevant `decision cannot continue` node and a `fails` edge to Stop, including failures
inside statically expanded callees and polymorphic implementations. A callee failure is terminal
and therefore is not propagated back as a normal caller continuation.

Normalize business words centrally: after camel/snake conversion, remove tokens equal to `id` or
`ids`. This applies uniformly to fields, parameters, constants, method-derived labels, and
collections, without application vocabulary.

Render comparisons against the Java null literal structurally rather than through the generic
operator renderer: equality becomes “is absent,” inequality becomes “exists,” and logical
complement swaps those statements. This preserves business-significant optionality without Java
terminology. Defensive `Objects.requireNonNull` calls remain outside the decision slice.

## Dependency Decisions

No dependency is introduced.
