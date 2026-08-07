# Dependency Audit: Conditional Alias and Method-Reference Effects

**Verified:** 2026-08-07T11:25:46Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

| Package | Version | Ecosystem | Source |
| --- | --- | --- | --- |
| `at.gepardec.fachtracing:fachtracing-api` | `0.1.0-rc.1` | Maven | Reactor |
| `org.ow2.asm:asm` | `9.10.1` | Maven | Engine dependency tree |
| `org.ow2.asm:asm-tree` | `9.10.1` | Maven | Engine dependency tree |

## CVE Scan Results

No advisory was returned by OSV for ASM 9.10.1 or ASM Tree 9.10.1.

## EOL Status

Java 21 is the supported project baseline. This fix does not change the runtime or framework.

## Verification Method

- Layer 1: Maven offline dependency tree completed.
- Layer 2: OSV package queries completed for both external engine dependencies.
- Layer 3: Not needed.

## Allowed Advisories

None.

## Dependency Introduction

No new dependency is introduced.
