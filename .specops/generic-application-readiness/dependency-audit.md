# Dependency Audit: Generic Application Readiness

**Verified:** 2026-08-05T07:31:02Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

- Existing production baseline: Java 21 compiler APIs, ASM 9.10.1, Maven 3.9.16 provided APIs, and
  Maven Plugin Tools 3.15.2.
- Planned standard APIs: JDBC and existing deterministic JSON encoding.
- Existing test baseline: H2 2.4.240.

## CVE Scan Results

OSV returned no advisories for ASM 9.10.1, H2 2.4.240, Maven Core and Plugin API 3.9.16,
Maven Plugin Tools 3.15.2, or Maven JAR Plugin 3.5.0. Iteration 4 introduces no package and no
installation command.

## EOL Status

Java 21 remains an LTS baseline. The checked Oracle JDK 21 cycle has support through September 2028.
The Apache Maven 3.9 cycle is active, and 3.9.16 is the current checked release.

## Verification Method

- Layer 1: Java has no standard local audit command; Maven declarations and scopes were inspected.
- Layer 2: OSV and endoflife.date checks completed on 2026-08-05.
- Layer 3: not needed. No new dependency is introduced.

## Allowed Advisories

None.
