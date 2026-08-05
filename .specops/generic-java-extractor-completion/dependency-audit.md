# Dependency Audit: Generic Java Extractor Completion

**Verified:** 2026-08-05T08:40:07Z  
**Threshold:** medium  
**Result:** PASS

## Existing Production Dependencies

- Java 21 compiler APIs.
- ASM 9.10.1 in the agent and engine-adjacent test paths.
- Maven Core and Plugin API 3.9.16 with Maven Plugin Tools 3.15.2.
- Standard JDBC APIs.

## Proposed Test and CI Dependencies

- `org.postgresql:postgresql:42.7.13`, test scope only.
- PostgreSQL server 18.4 as a GitHub Actions service.
- Official `actions/checkout@v7` and `actions/setup-java@v5`.

## Security and Lifecycle Evidence

- The official pgJDBC download page lists 42.7.13 as the current Java 8+ driver.
- OSV returned no advisory for `org.postgresql:postgresql:42.7.13` on 2026-08-05.
- PostgreSQL 18.4 is a supported release. PostgreSQL 18 support ends in November 2030.
- PostgreSQL 18.4 contains the current May 2026 security fixes for supported server lines.
- The official GitHub action repositories document checkout V7 and setup-java V5 as current majors.

## Decision

Approve pgJDBC 42.7.13 only as a test dependency. Do not add a database driver to any published
runtime artifact. Use only official GitHub actions and read-only workflow permissions.

## Allowed Advisories

None.

