# Design: Jakarta platform-call completeness

## Architecture Overview

The analyzer already has one platform-operation boundary. Extend that boundary to the current Jakarta namespace so equivalent Java EE and Jakarta EE value operations receive equivalent analysis behavior.

## Technical Decisions

### Decision 1: Extend the existing namespace classification

**Decision:** Add `jakarta.*` to `isSupportedLibraryOperation`.

**Rationale:** This is the same role that `javax.*` has now. It keeps the fix in the platform classification boundary and does not change bytecode fallback.

### Decision 2: Use a compiled test fixture

**Decision:** Compile a small Jakarta `Response` fixture during the analyzer contract test.

**Rationale:** A source fixture would let the analyzer follow the method body and would not reproduce source-unavailable platform calls.

## Component Design

### Platform operation classifier

**Responsibility:** Decide if a called owner belongs to a platform namespace whose internal control flow is not business decision logic.

### Analyzer contract test

**Responsibility:** Prove that nested Jakarta response-builder calls stay transparent while the source predicate stays visible.

## Testing Strategy

- Run the new analyzer contract test before and after the production change.
- Run the full Maven test suite.
- Run the repository pull-request verification script.
- Add the behavior to the Java capability contract and supported-construct guide.

## Risks & Mitigations

- **Risk:** A broad Jakarta namespace rule can hide framework code that has internal branches. **Mitigation:** Match the existing broad `javax.*` contract and retain application binary fallback tests.

## Dependencies & Blockers

No spec dependency or external blocker exists.

### Dependency Decisions

No new dependencies are introduced.
