# Dependency Audit: Context-aware operation labels

**Verified:** 2026-08-07T11:11:11Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

| Package | Version | Ecosystem | Source |
| --- | --- | --- | --- |
| `at.gepardec.fachtracing:fachtracing-api` | `0.1.0-rc.1` | Maven | reactor |
| `org.ow2.asm:asm` | `9.10.1` | Maven | Maven dependency tree |
| `org.ow2.asm:asm-tree` | `9.10.1` | Maven | Maven dependency tree |

## CVE Scan Results

No advisory was returned by OSV for ASM or ASM Tree 9.10.1 on 2026-08-07.

## EOL Status

Java 21 is the project baseline. This label-only change does not change the runtime or framework.

## Verification Method

- Layer 1: Maven dependency tree completed; Maven has no standard vulnerability audit goal.
- Layer 2: OSV queries for both external compile dependencies returned no advisory.
- Layer 3: Not needed.

## Allowed Advisories

None.
