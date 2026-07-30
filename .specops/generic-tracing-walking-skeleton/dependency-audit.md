# Dependency Audit: Generic Fachtracing Walking Skeleton

**Verified:** 2026-07-24T08:34:34Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

The root Maven reactor contains API, engine, and agent modules. The API has no external
dependency, the engine depends on the API and Java 21 `jdk.compiler`, and the agent adds the
approved `org.ow2.asm:asm:9.10.1`. Maven resolved the declared versions successfully. The
project does not yet contain a dependency lock file.

## CVE Scan Results

The net-new ASM decision was checked through the dependency-introduction gate; OSV returned no
advisories for `org.ow2.asm:asm:9.10.1` at specification time. No fresh completion-time online
CVE scan was performed; this audit does not imply a permanent advisory-free guarantee.

## EOL Status

| Product | Version | EOL Date | Status |
| --- | --- | --- | --- |
| Java validation baseline | 21 | Not evaluated as an installed project runtime | Baseline selected from reference application |

## Verification Method

- Layer 1 (Local audit): Maven build and `jdeps` module-boundary verification passed
- Layer 2 (Online APIs): used at specification time for the proposed ASM dependency
- Layer 3 (LLM fallback): not needed

## Allowed Advisories

None.
