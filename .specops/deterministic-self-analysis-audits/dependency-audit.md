# Dependency Audit: Deterministic Self-Analysis Audit Graphs

**Verified:** 2026-08-14T09:13:23Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

This feature introduces no dependency. It uses the current Java 21 and Maven reactor contracts.

## CVE Scan Results

The dependency inventory is unchanged from the current-build audit completed on 2026-08-14. That
audit queried the official OSV service for all 14 exact direct external Maven versions and returned
no advisory.

## Verification Method

- Layer 1: the current Maven dependency inventory and lock-free POM declarations are unchanged.
- Layer 2: reuse the exact same-day OSV result from `self-runtime-tracing/dependency-audit.md`.
- Layer 3: not required.

## Allowed Advisories

None.

## Blocking Decision

No new dependency exists and the current same-day exact inventory has no OSV advisory. The gate
passes.
