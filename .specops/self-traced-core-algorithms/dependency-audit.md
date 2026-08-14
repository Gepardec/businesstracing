# Dependency Audit: Self-Traced Core Algorithms

**Verified:** 2026-08-14T09:41:42Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

This feature introduces no dependency. It uses the current Java 21, engine, API annotation, and
Maven reactor contracts.

## CVE Scan Results

The dependency inventory is unchanged from the exact current-build audit completed on 2026-08-14.
That audit queried the official OSV service for all 14 exact direct external Maven versions and
returned no advisory.

## Verification Method

- Layer 1: the current Maven dependency inventory is unchanged.
- Layer 2: reuse the same-day exact OSV result from
  `deterministic-self-analysis-audits/dependency-audit.md`.
- Layer 3: not required.

## Allowed Advisories

None.

## Blocking Decision

No new dependency exists. The same-day exact inventory has no OSV advisory. The gate passes.
