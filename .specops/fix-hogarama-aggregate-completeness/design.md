# Design: External archive reference-operation boundary

## Architecture Overview

The Maven adapter already gives the engine an ordered compilation classpath. A new engine component resolves the first classpath origin for one binary owner. The analyzer uses that origin to distinguish external archive APIs from application class-directory binaries.

The boundary applies only to reference-returning operations. It does not apply to Boolean decisions. This keeps the existing controlled bytecode fallback and fail-closed behavior for binary business rules.

## Technical Decisions

### Decision 1: Resolve the exact binary owner from the ordered classpath

**Decision:** Add `BinaryTypeOriginResolver` with `ARCHIVE`, `DIRECTORY`, and `UNAVAILABLE` results.

**Rationale:** Package names do not prove ownership. The compiler classpath order is the available target-neutral ownership evidence.

### Decision 2: Limit the opaque boundary to reference results

**Decision:** Accept array, declared, type-variable, wildcard, and intersection results from an archive. Do not accept primitive or void results.

**Rationale:** Hogarama uses collection, query, criteria, and options references. Boolean and numeric result rules can directly carry hidden business decisions and must stay fail-closed.

### Decision 3: Preserve external receiver effects

**Decision:** Treat an instance reference operation at the archive boundary as an opaque effect on its receiver. Treat a static reference operation as effect-free.

**Rationale:** Fluent persistence APIs change query and options objects. The receiver effect retains source-visible application predicates that control those changes. Static collection value functions do not change their input.

### Decision 4: Accept archive Boolean calls only in source control conditions

**Decision:** Treat a Boolean archive call as transparent only when it is inside an `if`, loop, or conditional-expression condition.

**Rationale:** The source call site already becomes an exact graph predicate in this context. A direct returned Boolean call has no source control boundary and must continue through bytecode analysis or create a gap.

## Component Design

### BinaryTypeOriginResolver

**Responsibility:** Resolve and cache the first ordered classpath location that contains an exact binary type.

### StaticDecisionAnalyzer

**Responsibility:** Apply the archive reference-operation contract to invocation flow and effect slicing.

### Analyzer contract test

**Responsibility:** Compile external APIs into a JAR, analyze source-only application logic, and prove both corrected and unchanged behavior.

## Testing Strategy

- Add the failing compiled-archive fixture first.
- Run `StaticDecisionAnalyzerTest` before and after the fix.
- Run strict aggregate analysis against the real Hogarama reproduction.
- Run the full Maven suite, Java capability verifier, repository integrity verifier, and pull-request gate.

## Risks & Mitigations

- **Risk:** Archive business logic can be hidden. **Mitigation:** Exclude primitive and void results, and add an explicit Boolean dependency regression.
- **Risk:** Receiver effects can add irrelevant source logic. **Mitigation:** Add only the receiver root and let the existing backward slicer decide relevance.
- **Risk:** Repeated archive scans can be expensive. **Mitigation:** Cache one result for each binary owner during an analysis run.

## Dependencies & Blockers

This spec relates to `fix-jakarta-platform-call-completeness` and `conditional-alias-method-reference-effects`. It does not depend on an unfinished spec.

### Dependency Decisions

No new dependency is introduced.
