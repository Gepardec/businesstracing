# Supported Java constructs

Static relevance is determined by backwards data/control dependence from returned values. The
analyzer does not classify logging, metrics, packages, frameworks, or method names as
“technical”; work disappears only when it cannot affect the decision result.

| Construct | Walking-skeleton behavior |
| --- | --- |
| `@FachTracing` method | Discovers every decision entry in deterministic source order and its optional business label |
| `if / else` and comparison | Produces result-relevant predicate nodes; a complete Java 21 `javac` Boolean binding records the exact `true` or `false` edge |
| `value == null`, `value != null` | Preserves result-relevant optionality as “is absent” or “exists”; Java `null` is never business output |
| `&&`, `||`, `!` | Retains compound business conditions and correlates evaluated short-circuit operands |
| Local initialization and assignment | Retained only when it can influence a return |
| Returned mutable collection | Retains result-affecting mutations through calls and lambda bodies |
| Direct method call | Follows a source-available callee and includes its relevant slice |
| Generic interface or abstract dispatch | Uses erased subtype identity to include all source-visible candidates, including implementations in sibling modules of the active Maven reactor; runtime evidence selects the expected call site's opaque edge |
| Boolean, number, category, string result | Encoded as a typed `DecisionValue` |
| Custom result/evidence type | Requires an explicit `DecisionValueAdapter` |
| Switch statement/expression | Creates a choice node with per-case/default topology and terminal throws |
| Ternary expression | Retains its result-relevant condition as a predicate |
| `for`, enhanced `for`, `while`, `do while` | Creates a business iteration/choice node and analyzes the result-relevant body |
| Relevant `try` or synchronized block | Produces an explicit coverage gap and an incomplete graph |
| Source-unavailable call or reflection | Remains outside the complete subset and must be surfaced by the broader extractor |

Every annotated decision has one `Start` and one `Stop`. Return paths state the returned business
expression on their edge to Stop. Relevant throws in the entry or an expanded source method use a
`fails` edge to the same Stop and do not rejoin normal continuation.

## Failure and privacy boundary

- Unsupported relevant logic is never reported as complete.
- A source/class fingerprint mismatch prevents transformation.
- Incomplete branch metadata uses an evaluated-node probe and does not claim an exact edge.
- Flat homogeneous `&&` and `||` predicates use occurrence-aware exact bindings. Mixed, nested,
  negated-compound, ternary, switch-expression, and ambiguous synthetic-method forms use
  evaluated-node probes.
- An exception that leaves an instrumented entry creates one generic failed execution. The agent
  then rethrows the same exception object.
- A failed business record contains no Java exception class, raw message, or stack trace.
- A probe or codec failure never changes the application's return value or exception and is
  available as a developer diagnostic.
- Redaction is mandatory at the value-codec boundary, before evidence enters a decision record.
- Business graphs, explanations, PlantUML, and Mermaid contain opaque IDs and business labels only;
  source paths, Java owners, members, and fingerprints remain in the developer manifest.

This boundary is intentionally conservative. Full Java support—including exception-flow
slicing, reflection, libraries without source, and precise atomic short-circuit
operands—belongs to the follow-on extractor and conformance specifications.
