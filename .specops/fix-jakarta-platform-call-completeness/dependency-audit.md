# Dependency Audit: Jakarta platform-call completeness

**Verified:** 2026-08-07T11:13:49Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

| Package | Version | Ecosystem | Source |
| --- | --- | --- | --- |
| org.ow2.asm:asm | 9.10.1 | Maven | `fachtracing-engine/pom.xml` |
| org.ow2.asm:asm-tree | 9.10.1 | Maven | `fachtracing-engine/pom.xml` |
| org.apache.maven:maven-core | 3.9.16 | Maven | `fachtracing-maven-plugin/pom.xml` |
| org.apache.maven:maven-plugin-api | 3.9.16 | Maven | `fachtracing-maven-plugin/pom.xml` |
| org.apache.maven.plugin-tools:maven-plugin-annotations | 3.15.2 | Maven | `fachtracing-maven-plugin/pom.xml` |
| org.codehaus.plexus:plexus-utils | 3.6.1 | Maven | `fachtracing-maven-plugin/pom.xml` |

## CVE Scan Results

OSV returned zero advisories for each listed package and version on 2026-08-07.

## EOL Status

Java 21 is required. This spec does not change the runtime or framework version.

## Verification Method

- Layer 1 (Local audit): skipped; Java/Maven has no standard command in the SpecOps gate.
- Layer 2 (Online APIs): OSV queries completed for the direct external Maven dependencies.
- Layer 3 (LLM fallback): not needed.

## Allowed Advisories

None.
