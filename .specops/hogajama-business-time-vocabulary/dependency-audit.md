# Dependency Audit: Hogajama business time vocabulary

**Verified:** 2026-08-11T10:33:26Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

| Package | Version | Ecosystem | Source |
| --- | --- | --- | --- |
| `at.gepardec.fachtracing:fachtracing-engine` | `0.1.0-rc.1` | Maven | reactor |
| `org.ow2.asm:asm` | `9.10.1` | Maven | existing dependency steering |
| `org.ow2.asm:asm-tree` | `9.10.1` | Maven | existing dependency steering |

## CVE Scan Results

This fix introduces no package and changes no package version. The existing project audit records no
blocking advisory for the listed compile dependencies.

## EOL Status

Java 21 is the project baseline. This guard fix does not change the runtime or framework.

## Verification Method

- Layer 1: Maven has no standard vulnerability audit goal.
- Layer 2: Reused the current dependency steering because this spec introduces no dependency.
- Layer 3: Not needed for a new package decision.

## Allowed Advisories

None.
