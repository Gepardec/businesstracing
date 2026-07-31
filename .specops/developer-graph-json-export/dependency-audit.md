# Dependency Audit: Developer Graph JSON Export

**Verified:** 2026-07-31T08:37:01Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

| Package | Version | Ecosystem | Source |
| --- | --- | --- | --- |
| `fachtracing-api` | project version | Maven/Java | `fachtracing-engine/pom.xml` |

## CVE Scan Results

No external dependency is introduced or changed by this spec. The affected engine module has only an internal project dependency.

## EOL Status

| Product | Version | EOL Date | Status |
| --- | --- | --- | --- |
| Java | 21 | Not evaluated locally | Supported project baseline |

## Verification Method

- Layer 1 (Local audit): skipped — Maven/Java has no standard audit command in the SpecOps matrix and the affected module adds no package
- Layer 2 (Online APIs): not needed — no net-new package
- Layer 3 (LLM fallback): used only to confirm Java 21 remains the declared project baseline; no vulnerability claim made

## Allowed Advisories

None
