# Requirements: CI Isolated Maven Repository

## Overview

PR #5 fails in the `release-gate` job because verification builds dependencies in an isolated
Maven repository but starts Java processes with dependency paths from `$HOME/.m2/repository`. A
clean runner cannot load ASM from that unrelated location.

## Root Cause

`verify-release.sh` sets `FACHTRACING_RELEASE_MAVEN_REPOSITORY` and configures Maven with
`-Dmaven.repo.local`. The manual classpaths in `verify.sh` and `verify-mega-backend.sh` ignore that
location. The PostgreSQL verifier has the same hard-coded default path.

## Impact

- Severity: High. The required release job cannot reach the conformance and load gates.
- Affected entry points: standard verification, Mega conformance, and PostgreSQL verification.
- Product runtime behavior and stored decision data are not affected.

## Regression Risk

| Behavior | Risk | Required evidence |
| --- | --- | --- |
| Default local runs use the normal Maven repository | Must-Test | Resolver default contract |
| Release runs use their isolated Maven repository | Must-Test | Resolver override contract and clean release gate |
| Standard, Mega, and PostgreSQL scripts share one rule | Must-Test | No hard-coded repository paths in consumers |
| Fachtracing business results and graphs stay unchanged | Nice-To-Test | Existing verification and Mega conformance |

## Expected Behavior

- WHEN the release repository environment variable is set THE SYSTEM SHALL load every manually
  referenced Maven artifact from that repository.
- WHEN no repository override is set THE SYSTEM SHALL use `$HOME/.m2/repository`.
- THE SYSTEM SHALL use the same repository resolution rule in standard, Mega, and PostgreSQL
  verification scripts.
- WHEN a clean isolated repository contains the Maven build artifacts THE SYSTEM SHALL complete
  standard verification without reading dependency jars from the home repository.

## Constraints

- The fix must add no dependency.
- The fix must not change application or analyzer behavior.
- The GitHub pull-request release gate must remain fail-closed.

## Definition of Done

- [x] A focused shell contract proves override, explicit, and default repository resolution.
- [x] Verification scripts contain no direct `$HOME/.m2/repository` dependency path.
- [x] Standard verification passes.
- [x] The clean release gate passes from the committed revision.
- [x] PR #5 receives the pushed fix and its checks are monitored.
