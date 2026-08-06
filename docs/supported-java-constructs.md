# Supported Java constructs

The versioned machine-readable contract is in `docs/java-capabilities.json`. The release verifier
checks that every entry names an executable contract and appears in this document.

## Machine-readable capability IDs

- `annotated-entry`, `conditional-branch`, `null-optionality`, `short-circuit-boolean`
- `complex-boolean-exact-path`, `predicate-operand-evidence`, `predicate-site-evidence`, `incomplete-exact-path-gap`
- `assignment-data-flow`, `direct-source-call`, `generic-polymorphic-dispatch`, `typed-result`
- `switch-forms`, `pattern-switch-exact-path`, `ternary-expression`, `loops-and-collection-mutation`, `indexed-loop-business-lowering`, `records-and-equality`
- `lambdas-and-streams`, `result-relevant-exception-flow`, `result-relevant-finally-flow`
- `synchronized-business-logic`
- `source-unavailable-call`
- `controlled-bytecode-fallback`, `controlled-bytecode-fallback-boundary`
- `reflection-service-loader-proxy`, `unresolved-dynamic-candidate-gap`, `async-boundary`
- `exact-async-callback-position`, `async-submission-lifecycle`, `nested-async-reservation-identity`, `transparent-future-cancellation`
- `unsupported-async-boundary-gap`, `java17-java21-projects`
- `owned-external-jpms-source`, `owned-automatic-module-source`
- `try-with-resources`, `resource-close-result-gap`, `pattern-matching`, `sealed-types`, `nested-classes`, `method-references`
- `business-java-vocabulary`

Static relevance is determined by backwards data/control dependence from returned values. The
analyzer does not classify logging, metrics, packages, frameworks, or method names as
“technical”; work disappears only when it cannot affect the decision result.

| Construct | Walking-skeleton behavior |
| --- | --- |
| `@FachTracing` method | Discovers every decision entry in deterministic source order and its optional business label |
| `if / else` and comparison | Produces result-relevant predicate nodes; a complete Java 21 `javac` Boolean binding records the exact `true` or `false` edge |
| `value == null`, `value != null` | Preserves result-relevant optionality as “is absent” or “exists”; Java `null` is never business output |
| Mixed and nested `&&`, `||`, `!` | Creates one node for each atomic business predicate and records each evaluated edge with typed result-relevant operand evidence when an exact binding is available |
| Predicate operand evidence | Reads a direct parameter from its current local slot at each predicate branch; reassignment and repeated evaluation cannot reuse the method-entry value. Property, local, or calculated operands outside the exact subset produce a source-located execution gap |
| Local initialization and assignment | Retained only when it can influence a return |
| Returned mutable collection | Retains result-affecting mutations through calls and lambda bodies |
| Direct method call | Follows a source-available callee and includes its relevant slice |
| Generic interface or abstract dispatch | Uses erased subtype identity to include all source-visible candidates, including implementations in sibling modules of the active Maven reactor; runtime evidence selects the expected call site's opaque edge |
| Proxy or `ServiceLoader` dispatch | Uses the instrumented implementation entry to select one proven source candidate; proxy mechanics and provider classes stay outside business output |
| Constant reflection target | Resolves one unambiguous class literal, method-name literal, and arity to source candidates; dynamic or ambiguous targets stay incomplete |
| Boolean, number, category, string result | Encoded as a typed `DecisionValue` |
| Custom result/evidence type | Requires an explicit `DecisionValueAdapter` |
| Switch statement/expression | Records one exact integral, string, enum, or default case edge |
| Java 21 pattern switch | Records the selected pattern edge and each evaluated guard atom; compiler type-switch helpers stay hidden |
| Boolean ternary expression | Records the selector and only the selected Boolean value path as exact atomic edges |
| `for`, enhanced `for`, `while`, `do while` | Creates a business iteration/choice node and analyzes the result-relevant body; canonical indexed collection loops omit the counter, size check, indexed access, and update |
| Source-proven `try`, multi-catch, and `finally` | Retains primary and alternative business results; handler mechanics and exception types stay outside the business graph |
| Synchronized block | Removes synchronization mechanics and retains result-relevant business logic in the block |
| Try-with-resources | Removes resource mechanics when source proves that `close` has no result-relevant behavior; otherwise reports a located gap |
| Pattern matching | Retains result-relevant type patterns and their bound business facts |
| Sealed type dispatch | Includes each source-visible permitted implementation as a candidate |
| Nested class call | Resolves and expands source-visible nested-class decision logic |
| Direct method reference | Resolves and expands a source-visible referenced decision method |
| Source-unavailable simple Boolean method | Uses a fingerprinted, fail-closed bytecode fallback for one numeric comparison with parameters, configured fields, constants, simple integer calculations, conditional flow, and Boolean returns |
| Other source-unavailable call or reflection | Remains incomplete with the rejected binary construct and call-site location |

## Project and compiler boundary

The graph-entry project selects the compiler mode. A non-modular entry uses a flat source task, even
when a named module depends on that project. A modular entry uses one valid multi-module `javac`
task that contains only connected named source projects. Connected non-modular projects stay on the
binary classpath or module path. If their source logic is not available to that task, the graph gets
an explicit coverage gap.

The JPMS task includes each selected module descriptor, module source path, effective module path,
classpath, release, encoding, and compiler argument. Graph extraction uses the trees and symbols
from this same task. Connected named modules must use compatible release, encoding, and compiler
arguments. External source files without module ownership are rejected for a modular entry. Named
external sources join their declared module source path. Sources for an automatic module use the
Maven binary on the module path and a controlled source patch. Both Maven goals accept ownership
by source identity or source root.
Unsupported forked compilers and annotation-processor configurations fail before graph extraction.
Generated sources keep generated provenance in developer graph V2.

Every annotated decision has one `Start` and one `Stop`. Return paths state the returned business
expression on their edge to Stop. Relevant throws in the entry or an expanded source method use a
`fails` edge to the same Stop and do not rejoin normal continuation.

## Failure and privacy boundary

- Unsupported relevant logic is never reported as complete.
- The binary fallback rejects exception tables, calls, monitors, switches, dynamic instructions,
  native methods, and every shape outside its declared comparison subset.
- A source/class fingerprint mismatch prevents transformation.
- Incomplete exact branch metadata creates a located coverage gap and does not claim an exact edge.
- Mixed, nested, and negated Boolean forms use occurrence-aware atomic bindings. Each evaluated
  atom records one exact edge and typed Boolean evidence. Short-circuited atoms record nothing.
- A result-relevant source-proven catch path records its exact opaque path edge. Unavailable
  exception-triggering logic and finally-overridden returns remain located coverage gaps.
- An exception that leaves an instrumented entry creates one generic failed execution. The agent
  then rethrows the same exception object.
- A failed business record contains no Java exception class, raw message, or stack trace.
- A probe or codec failure never changes the application's return value or exception and is
  available as a developer diagnostic.
- Redaction is mandatory at the value-codec boundary, before evidence enters a decision record.
- Business graphs, explanations, PlantUML, and Mermaid contain opaque IDs and business labels only;
  source paths, Java owners, members, and fingerprints remain in the developer manifest.
- Runtime dispatch first uses an exact registered target, then one unique most-specific assignable
  target. Unknown and ambiguous targets create bounded, deduplicated developer diagnostics.
- Dynamic invocation never guesses a candidate. A proxy delegate, service provider, or reflected
  member must enter one instrumented static candidate. A non-constant reflection target reports a
  source-located reconstruction gap.
- The agent automatically propagates context through standard executor submission,
  `CompletionStage` callbacks, platform threads, and virtual threads. Publication waits for
  captured callbacks. Inactive wrapping preserves callback identity, and each wrapper clears the
  worker context in `finally`.
- Standard asynchronous calls use exact owner, method, descriptor, and callback-position bindings.
  Each call confirms its own callback handle. Nested synchronous callbacks cannot consume another
  call's reservation. Thread constructors bind the handle to the actual `Thread` object.
- Cancellation of supported `Future`, `CompletableFuture`, and `ForkJoinTask` results releases the
  reservation exactly once and keeps the original result object unchanged.
- A required predicate fact with no safe value adapter makes the execution incomplete. A Boolean
  fallback does not hide the missing fact.
- Generic label normalization and the artifact guard remove Java construction, enum-type, and
  helper-role vocabulary. They contain no application-specific terms.

The boundary stays fail-closed. Complex binary control flow, result-relevant resource-close logic
without source, ambiguous reflection, unsupported asynchronous APIs, and unowned modular sources
remain explicit gaps. These gaps do not reduce the supported source and runtime subsets above.
