# Design: Generic aggregate construct integration

## Architecture Overview

The fix stays in the engine. The Maven adapter already resolves exact opaque dependency coordinates
and passes the selected archives to the analyzer. The analyzer must combine that contract with
source catch paths and method-local effect scope. The integration proof also includes generated
source dispatch.

## Technical Decisions

### Decision 1: Reuse supported-operation evidence for caught calls

**Decision:** A source-unavailable call in a relevant `try` block is an unavailable exception trigger
only when it is neither source-visible nor accepted by the existing supported platform or explicit
opaque-library operation contracts.

**Rationale:** The catch block and its runtime control target already represent the result path. The
same operation must not be accepted for normal flow but rejected only because it is inside `try`.

### Decision 2: Keep effect scans inside one executable body

**Decision:** Dependency and mutation scans for one method do not descend into a nested or anonymous
class body. The nested methods remain separate indexed methods.

**Rationale:** A nested class has separate receivers and parameters. Its calls cannot mutate the
enclosing method only because its syntax is nested there.

### Decision 3: Keep the existing generated-source dispatch contract

**Decision:** Do not change dispatch selection. Run compile and aggregate analysis in one Maven
invocation so javac-generated Java is in the registered source roots.

**Rationale:** The existing compiler subtype and source-index checks select the generated mapper.
A standalone goal in a new Maven session does not have the generated source root and is outside the
documented annotation-processor workflow.

## Component Design

### DependencyGraphBuilder

**Responsibility:** Collect dependencies and effects for one executable body only.

### StaticDecisionAnalyzer

**Responsibility:** Apply supported-call evidence to catch completeness and keep each method-local
scan inside one executable body.

### StaticDecisionAnalyzerTest

**Responsibility:** Prove the integrated generic construct and the fail-closed counterexamples.

## Testing Strategy

- Add a focused source and dependency-JAR fixture that models the three generic constructs.
- Run the executable `StaticDecisionAnalyzerTest` with assertions enabled.
- Install the current plugin and run strict aggregate analysis against the real Hogajama checkout.
- Run `./scripts/verify-pr.sh` and `git diff --check`.

## Risks and Mitigations

- **Risk:** A platform call with unknown exception behavior can be accepted too broadly.
  **Mitigation:** Require the existing supported-operation contract and keep unsupported calls
  incomplete.
- **Risk:** Skipping nested classes can omit a callback body.
  **Mitigation:** Lambdas remain in the enclosing executable; nested class methods remain indexed and
  are analyzed when called.
- **Risk:** A direct standalone aggregate goal can omit generated mapper source.
  **Mitigation:** Keep the documented same-invocation `compile ... analyze-reactor` command in the
  real validation.

## Dependencies and Blockers

The required `explicit-opaque-library-boundaries` and `path-sensitive-definition-integration` specs
are complete. No new package is required.

### Dependency Decisions

No new dependency is introduced.
