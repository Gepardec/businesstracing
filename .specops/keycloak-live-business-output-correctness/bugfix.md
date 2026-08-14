# Bug Fix: Keycloak Live Business Output Correctness

## Overview

The live Keycloak proof selects and calls the configured user-search endpoint. The generated files
do not show two executed input decisions, and they expose analyzer diagnostics to business readers.
The automatic output must show each observed business decision and must use business-safe wording.

## Root Cause Analysis

The analyzer changes a predicate outcome to `unresolved` when the selected path enters a coverage
gap. The runtime branch map then cannot bind the true or false predicate path.

The automatic file sink also uses the developer explanation without a presentation boundary. This
passes source, bytecode, implementation, and value-adapter diagnostics to the business output.

## Impact Assessment

- Severity: High. A live endpoint call can create a misleading and technical business flow.
- Runtime behavior: The endpoint result and control flow are unchanged.
- Privacy: The fixed output keeps the current redaction contract.
- Blast radius: Static gap edges, runtime branch binding, automatic text output, the Keycloak
  conformance contract, and related focused tests are in scope.

## Required Behavior

- A predicate path that enters a coverage gap SHALL keep its observed true or false outcome.
- The gap SHALL stay explicit and SHALL NOT become a guessed business rule.
- Automatic text output SHALL NOT include source, bytecode, class, implementation, or adapter
  diagnostics.
- A successful endpoint call with an unsupported result type SHALL report `Completed` without
  reading or changing the result.
- The exact developer graph and developer diagnostics SHALL stay available.
- The pinned Keycloak user-search call SHALL show the observed `search exists` and `prefix exists`
  decisions.

## Testing Plan

1. Add a static-analysis regression for a predicate path that enters a gap.
2. Add an automatic-output regression with an unsupported successful result and technical gaps.
3. Strengthen the pinned Keycloak conformance assertions.
4. Repeat the real Keycloak token and user-search HTTP calls with the Java agent.
5. Run the complete repository verification and hosted CI.

## Acceptance Criteria

- [x] Runtime branch targets include predicate outcomes that lead to an explicit gap.
- [x] Automatic output reports a successful unsupported result as `Completed`.
- [x] Automatic output replaces technical gap details with one business-safe coverage statement.
- [x] Automatic output stays redacted.
- [x] The live Keycloak output shows `search exists` as true and `prefix exists` as false.
- [x] The live Keycloak output contains no technical diagnostics.
- [x] Core, Keycloak conformance, and complete repository checks pass. Hosted checks run after push.

## Scope Assessment

This is one output-correctness fix. Branch binding and the business presentation boundary are both
needed for the same live acceptance flow. No decomposition is needed.
