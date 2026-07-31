# Dependency Audit: Maven Developer Graph Export

**Verified:** 2026-07-31T09:24:03Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

The change reuses the existing engine, Maven plugin APIs, Java standard library, and executable contract style.

## CVE Scan Results

No dependency is added or upgraded by this spec.

## EOL Status

Java 21 and the repository's Maven 3.9.x baseline are unchanged.

## Verification Method

- Layer 1 (Local audit): skipped; Maven has no standard dependency vulnerability command and no dependency changes occur.
- Layer 2 (Online APIs): skipped; no new or changed package requires verification.
- Layer 3 (LLM fallback): reviewed the unchanged dependency surface; no new finding applies.

## Allowed Advisories

None.
