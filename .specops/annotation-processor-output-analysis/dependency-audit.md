# Dependency Audit: Analyze Annotation-Processor Output

**Verified:** 2026-08-07T08:43:56Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

| Package | Version | Ecosystem | Source |
| --- | --- | --- | --- |
| ASM | 9.10.1 | Maven | `fachtracing-agent/pom.xml` |
| Maven Plugin API/Core | 3.9.16 | Maven | `fachtracing-maven-plugin/pom.xml` |
| Maven Plugin Tools | 3.15.2 | Maven | `fachtracing-maven-plugin/pom.xml` |
| Plexus Utils | 3.5.1 | Maven | `fachtracing-maven-plugin/pom.xml` |

## CVE Scan Results

No dependency or version changes are in this spec. No known critical or high finding applies to the
affected Maven plugin dependency inventory in the available project audit data.

## EOL Status

| Product | Version | EOL Date | Status |
| --- | --- | --- | --- |
| Java | 21 | Vendor-specific LTS | Active baseline |
| Maven | 3.9.x | Not declared | Supported project baseline |

## Verification Method

- Layer 1 (Local audit): Skipped. Maven has no standard local audit command in the SpecOps policy.
- Layer 2 (Online APIs): Skipped because the spec adds and changes no dependency.
- Layer 3 (LLM fallback): Used with the existing project dependency inventory. It can be less current
  than an advisory service.

## Allowed Advisories

None.
