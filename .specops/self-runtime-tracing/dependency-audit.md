# Dependency Audit: Self Runtime Tracing

**Verified:** 2026-08-14T08:45:21Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

This feature introduces no dependency. The current direct external Maven inventory is:

| Package | Version | Scope in project |
| --- | --- | --- |
| `org.ow2.asm:asm` | 9.10.1 | Compile |
| `org.ow2.asm:asm-tree` | 9.10.1 | Compile |
| `org.apache.maven:maven-plugin-api` | 3.9.16 | Provided |
| `org.apache.maven:maven-core` | 3.9.16 | Provided |
| `org.apache.maven.plugin-tools:maven-plugin-annotations` | 3.15.2 | Provided |
| `org.codehaus.plexus:plexus-utils` | 3.6.1 | Provided |
| `org.springframework:spring-core` | 7.0.8 | Test |
| `org.springframework:spring-context` | 7.0.8 | Test |
| `org.springframework:spring-tx` | 7.0.8 | Test |
| `org.springframework:spring-webmvc` | 7.0.8 | Test |
| `org.springframework.data:spring-data-commons` | 4.1.0 | Test |
| `org.springframework.data:spring-data-jpa` | 4.1.0 | Test |
| `com.h2database:h2` | 2.4.240 | Test |
| `org.postgresql:postgresql` | 42.7.13 | Test |

## CVE Scan Results

The exact OSV batch query returned an empty result for all 14 selected versions. No advisory meets
the medium blocking threshold.

## Verification Method

- Layer 1: the Maven test-scope dependency tree resolved successfully.
- Layer 2: the official OSV query-batch API checked every exact direct external version on
  2026-08-14.
- Layer 3: not required because the exact online result was available.

## Allowed Advisories

None.

## Blocking Decision

No dependency change exists and no selected direct version has an OSV advisory. Implementation can
continue.
