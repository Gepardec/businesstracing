# Dependency Audit: Developer Graph JSON Schema

**Verified:** 2026-08-07T12:43:13Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

This spec adds no dependency. It uses the existing Java 21 standard library, engine model, Maven generator, and test parser.

## CVE Scan Results

The same repository dependency set passed exact OSV checks earlier on 2026-08-07 in `.specops/self-dogfood-business-tracing/dependency-audit.md`. This spec does not change that set.

## EOL Status

Java 21 and Maven 3.9.x remain the supported project baselines.

## Verification Method

- Layer 1 (local audit): no new package or version to resolve
- Layer 2 (online APIs): reused the same-day exact dependency audit because the manifest is unchanged
- Layer 3 (LLM fallback): not used for the blocking decision

## Allowed Advisories

None.

## Blocking Decision

No dependency change can introduce a new blocking advisory in this spec.
