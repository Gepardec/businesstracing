# Dependency Audit: Reactor-wide Implementation Resolution

**Verified:** 2026-07-31T09:22:40Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

| Package | Version | Ecosystem | Source |
| --- | --- | --- | --- |
| Maven Plugin API/Core | 3.9.16 | Java/Maven | `fachtracing-maven-plugin/pom.xml` |
| Maven Plugin Tools | 3.15.2 | Java/Maven | `fachtracing-maven-plugin/pom.xml` |
| ASM | 9.10.1 | Java/Maven | `fachtracing-agent/pom.xml` |

## CVE Scan Results

No blocking finding was identified. Maven has no standard audit command in this workflow. The existing build resolved the approved dependency set, and the offline review found no known critical or high issue in the versions used. This result can be older than current advisory data.

## EOL Status

| Product | Version | EOL Date | Status |
| --- | --- | --- | --- |
| Java | 21 | Not checked online | Active LTS baseline |
| Maven | 3.9.x | Not checked online | Active project baseline |

## Verification Method

- Layer 1 (Local audit): Maven build and dependency resolution passed; no standard Maven CVE audit command is available.
- Layer 2 (Online APIs): skipped because this change adds no dependency.
- Layer 3 (LLM fallback): used for the approved existing versions; may not reflect the latest advisories.

## Allowed Advisories

None.
