# Dependency Audit: Runtime Decision Path Capture

**Verified:** 2026-07-31T09:20:33Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

| Package | Version | Ecosystem | Source |
| --- | --- | --- | --- |
| `org.ow2.asm:asm` | 9.10.1 | Maven | `fachtracing-agent/pom.xml` |
| Maven Plugin API/Core | 3.9.16 | Maven | Current `main` build baseline; unchanged by this spec |
| Maven Plugin Tools | 3.15.2 | Maven | Current `main` build baseline; unchanged by this spec |

## CVE Scan Results

No new dependency is introduced by this spec. The implementation uses the current `main` dependency set.

## EOL Status

| Product | Version | Status |
| --- | --- | --- |
| Java | 21 | Active LTS baseline |

## Verification Method

- Layer 1 (local audit): skipped; the Java ecosystem has no configured standard audit command in this project.
- Layer 2 (online APIs): not required for a net-new package because this spec adds none.
- Layer 3 (offline review): used for the unchanged build files and the current `main` dependency inventory.

## Allowed Advisories

None.
