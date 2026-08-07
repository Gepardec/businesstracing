# Dependency Audit: Self-Dogfood Business Tracing

**Verified:** 2026-08-07T08:38:16Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

| Package | Version | Ecosystem | Source |
|---------|---------|-----------|--------|
| `org.ow2.asm:asm` | 9.10.1 | Maven | `fachtracing-engine/pom.xml` |
| `org.ow2.asm:asm-tree` | 9.10.1 | Maven | `fachtracing-engine/pom.xml` |
| `org.apache.maven:maven-core` | 3.9.16 | Maven | `fachtracing-maven-plugin/pom.xml` |
| `org.apache.maven:maven-plugin-api` | 3.9.16 | Maven | `fachtracing-maven-plugin/pom.xml` |
| `org.apache.maven.plugin-tools:maven-plugin-annotations` | 3.15.2 | Maven | `fachtracing-maven-plugin/pom.xml` |
| `org.codehaus.plexus:plexus-utils` | 3.6.1 | Maven | `fachtracing-maven-plugin/pom.xml` |
| `org.apache.maven.plugins:maven-jar-plugin` | 3.5.1 | Maven | `fachtracing-agent/pom.xml` |
| `org.apache.maven.plugins:maven-plugin-plugin` | 3.15.2 | Maven | `fachtracing-maven-plugin/pom.xml` |
| `com.h2database:h2` | 2.4.240 | Maven | `fachtracing-storage-jdbc/pom.xml` |
| `org.postgresql:postgresql` | 42.7.13 | Maven | `fachtracing-storage-jdbc/pom.xml` |

## CVE Scan Results

| Advisory | Package | Severity | CVSS | Description | Layer |
|----------|---------|----------|------|-------------|-------|
No advisory was returned for the selected versions.

Exact OSV queries returned no advisories for all packages in the inventory. The upgrade from `plexus-utils:3.5.1` to 3.6.1 removes GHSA-6fmv-xxpf-w3cw / CVE-2025-67030.

## Release Age

Maven Central metadata and artifact headers were checked on 2026-08-07. Each selected stable release was published on or before the 2026-08-04 cutoff. Plexus Utils 3.6.1 was published on 2026-04-01. Version 4.0.3 was not selected because it removes the required `Xpp3Dom` API. The workflow action releases were checked through the GitHub releases API and also meet the cutoff.

## EOL Status

| Product | Version | EOL Date | Status |
|---------|---------|----------|--------|
| Java | 21 | Not assessed in this focused dependency gate | Supported project baseline |
| Maven | 3.9.16 | Not announced | Current supported 3.9.x release |

## Verification Method

- Layer 1 (local audit): Maven has no standard vulnerability audit command; `dependency:tree` supplied the resolved compile inventory.
- Layer 2 (online APIs): Exact OSV queries were used for the direct packages. Maven Central metadata and artifact headers supplied current versions and dates. GitHub release data supplied workflow action versions and dates.
- Layer 3 (LLM fallback): Not used for the blocking decision.

## Allowed Advisories

None.

## Blocking Decision

No advisory meets the blocking threshold after the planned dependency upgrades.
