# Bug Fix: CI Business Projection Regression

## Problem Statement

The pull request is not ready because four required CI jobs fail. The current business projection
emits call-like aggregate labels, removes one explicit failure result from Spring PetClinic, and
expects a removed technical choice to remain in the self-tracing business graph. These failures
break the business-output contract and stop the release gate.

## Root Cause Analysis

- `aggregateQualifier` adds source arguments inside parentheses. Mega conformance rejects call
  syntax in business labels.
- `BusinessGraphProjector.technicalInfrastructureFailure` removes a terminal failure when the
  source owner name ends with an infrastructure role. This heuristic removes the externally
  visible `operation failed` result from pet registration.
- `BusinessSemanticReducer` applies an enclosing owner suffix before direct predicate and action
  evidence. It also replaces clear source labels with lower-quality call metadata. A controller
  can contain business rules and actions, so owner naming is weaker evidence than node semantics.
- `verify-self-tracing.sh` still searches the business graph for `choose by node kind`, although
  the semantic reducer now classifies this node as a technical choice and removes it. The
  projection audit already records that removal.
- The Postgres job fails after the same self-tracing assertion, so it does not produce dogfood
  artifacts.
- After the first Mega assertion is corrected, the corpus exposes three stale reviewed oracles.
  The branch intentionally adds source-visible aggregate checks and return evidence, but the
  normalized source-derived inventories still describe the older topology.
- After all product gates pass, remote run `33007667194` cancels PostgreSQL during its browser
  journey. The job runs an independent 50-second viewer gate before database integration and
  exceeds the existing three-minute budget.
- After the split, remote run `33008949036` proves that all five independent jobs pass within three
  minutes. PostgreSQL starts all 17 browser tests but exposes two stale UI assertions and reaches
  its limit before Playwright can complete its retry.

**Affected components:**

- Static aggregate label generation.
- Business terminal-result projection.
- Self-tracing release assertions and viewer dogfood verification.

**Error symptoms:**

- Mega rejects `project time list — project entries: is task (entry task)`.
- PetClinic produces two results instead of the required three results, including the source-derived
  `pet registration could not be completed` failure.
- The pull-request and Postgres jobs exit before their success markers.

## Impact Assessment

- **Severity:** High.
- **Users affected:** All users of the pull-request branch and users who inspect failure paths.
- **Frequency:** Always for the affected conformance examples.

## Dependencies and Blockers

This fix extends the completed `compact-graph-reading-and-business-phrasing` and
`business-graph-semantic-explanation` specifications. It also supplies hosted evidence to the
existing `release-gate-timeout-budget` specification. It has no external blocker and adds no
software dependency.

## Reproduction Steps

1. Run the Mega conformance gate.
2. Actual: the aggregate label contains call-like parentheses.
3. Run the Spring PetClinic conformance gate.
4. Actual: pet registration has no `pet registration could not be completed` result.
5. Run `FACHTRACING_SKIP_PROJECT_BUILD=true ./scripts/verify-self-tracing.sh`.
6. Actual: the script stops at the stale technical-choice assertion.
7. Expected: all three gates pass and the pull request reports all required checks as successful.

## Regression Risk Analysis

### Blast Radius

- `StaticDecisionAnalyzer` supplies aggregate labels to exact and business graph output.
- `AggregateBusinessLabelRenderer` joins source-derived aggregate roles.
- `BusinessGraphProjector` creates one business result for each exact terminal edge.
- `verify-self-tracing.sh` validates the separation between exact structure and business output.
- Mega, PetClinic, pull-request, and Postgres CI jobs exercise these contracts.
- Three Mega normalized topology oracles verify the changed exact analyzer evidence.

### Behavior Inventory

- Aggregate labels keep the source subject, collection, condition, and qualifier.
- Source values such as `18` remain visible when they explain a rule.
- Each exact success, correction, and failure terminal remains a business result.
- Technical selectors remain absent from the business graph and visible in developer audit output.
- Business JSON V1 and exact graph topology do not change.

### Test Coverage Assessment

- **Covered:** aggregate formatting -> `StaticDecisionAnalyzerTest` and Mega conformance.
- **Covered:** success, correction, and failure results -> `BusinessGraphProjectionTest`.
- **Gap:** the result test does not assign an infrastructure owner to the failure source.
- **Covered:** technical-choice reduction -> self-tracing generated structure and projection audit.
- **Covered:** integrated output -> PetClinic, viewer dogfood, and pull-request gates.

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| Aggregate qualifier remains source-derived | Must-Test | The punctuation change must not remove business evidence. |
| Infrastructure-owned terminal failure remains visible | Must-Test | The current heuristic deletes this result. |
| Technical choice stays outside business output | Must-Test | A stale gate must not reverse the reducer contract. |
| Business JSON and source-value labels stay compatible | Must-Test | These are public business-output behaviors. |

### Scope Escalation Check

**Scope:** Contained. The fix removes one invalid terminal filter, changes one label separator,
restores evidence precedence in the semantic reducer, and aligns one release assertion with the
existing projection contract.

## Proposed Fix

Return the aggregate qualifier as plain source evidence and let the aggregate renderer join it
with an em dash. Do not infer terminal materiality from an owner-name suffix: keep each exact
terminal edge and let the existing result-label logic describe its outcome. Update self-tracing to
find the technical choice in the exact structure and its `TECHNICAL_CHOICE` removal in the audit.
Refresh only the three Mega inventories whose source review confirms the new aggregate or return
evidence, and update their documented hashes.

## Unchanged Behavior

- WHEN an aggregate callback is not safe to collapse THE SYSTEM SHALL CONTINUE TO keep its detail.
- WHEN a predicate has a source value THE SYSTEM SHALL CONTINUE TO show that value.
- WHEN a graph has successful or correction results THE SYSTEM SHALL CONTINUE TO keep them.
- WHEN a technical selector is reduced THE SYSTEM SHALL CONTINUE TO exclude it from business
  output and record the decision in the audit.
- WHEN business JSON is exported THE SYSTEM SHALL CONTINUE TO use
  `fachtracing-business-graph/v1`.

## Testing Plan

### Current Behavior

- Mega, PetClinic, pull-request, and Postgres jobs fail on the current pull-request commit.

### Expected Behavior

- The focused analyzer contract emits `collection: condition — qualifier` without call syntax.
- The focused projector contract keeps a failure result from an infrastructure-owned source.
- Self-tracing proves the technical choice is structural input and an audited removal.
- The exact local pull-request gate passes.
- All required GitHub checks pass after the fix is pushed.

### Unchanged Behavior

- Run the engine contracts, Mega, PetClinic, viewer dogfood, and the full pull-request verifier.
- Check generated artifacts for exact structure, business output, audit evidence, and stable JSON.

## Acceptance Criteria

- [x] Regression Risk Analysis is complete for High severity.
- [x] The current CI failures are reproduced and tied to concrete assertions.
- [x] WHEN aggregate evidence includes a qualifier THE SYSTEM SHALL render it without parentheses
  or call syntax and SHALL retain every source-derived role.
- [x] WHEN an exact terminal edge represents failure THE SYSTEM SHALL create a business result even
  when the source owner has an infrastructure-role suffix.
- [x] WHEN a technical choice is reduced THE SYSTEM SHALL keep it out of the business graph and
  SHALL record `REMOVED / TECHNICAL_CHOICE` in the projection audit.
- [x] All Must-Test unchanged behaviors pass.
- [x] All required pull-request checks are successful on the pushed commit.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Give each component one responsibility.
- Do not hard-code diagrams or application-specific rules.
