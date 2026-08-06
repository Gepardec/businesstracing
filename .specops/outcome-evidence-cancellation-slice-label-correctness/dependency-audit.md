# Dependency Audit: Outcome Evidence, Cancellation Reach, Slice, and Label Correctness

**Verified:** 2026-08-06T21:05:22Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

| Package | Version | Ecosystem | Source |
| --- | --- | --- | --- |
| Java | 21 | JVM | root `pom.xml` |
| ASM | 9.10.1 | Maven | `fachtracing-engine/pom.xml` and `fachtracing-agent/pom.xml` |
| Maven Plugin API/Core | 3.9.16 | Maven | `fachtracing-maven-plugin/pom.xml` |

## CVE Scan Results

No new dependency is introduced. No known critical or high advisory applies to the dependencies in
this change based on the existing project audit and offline review.

## EOL Status

| Product | Version | Status |
| --- | --- | --- |
| Java | 21 | Active LTS |
| Maven | 3.9.x | Active |

## Verification Method

- Layer 1: Java has no standard local audit command in the SpecOps matrix.
- Layer 2: not repeated because this spec changes no dependency and the project audit is current.
- Layer 3: offline review used; it can miss advisories published after model training.

## Allowed Advisories

None.

