# Dependency Audit: Configured Endpoint Business Tracing

**Verified:** 2026-08-12T16:19:29Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

| Package | Version | Ecosystem | Source |
| --- | --- | --- | --- |
| ASM | 9.10.1 | Maven | existing project POM files |

## CVE Scan Results

No spec-introduced package exists. This spec changes no POM dependency or plugin version.

## EOL Status

| Product | Version | EOL Date | Status |
| --- | --- | --- | --- |
| Java | 21 | not evaluated in this spec | Active project baseline |

## Verification Method

- Layer 1 (Local audit): skipped; Maven has no standard audit command in this project.
- Layer 2 (Online APIs): not needed; the spec introduces no dependency.
- Layer 3 (LLM fallback): reviewed the existing scoped dependency set; no new finding applies to this spec.

## Allowed Advisories

None.
