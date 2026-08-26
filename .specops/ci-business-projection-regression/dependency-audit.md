# Dependency Audit: CI Business Projection Regression

**Verified:** 2026-08-26T19:26:15Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

| Package | Version | Ecosystem | Source |
| --- | --- | --- | --- |
| Java | 21 | Runtime | `.specops/steering/dependencies.md` |
| Maven | 3.9.x | Build | `.specops/steering/dependencies.md` |
| ASM | 9.10.1 | Maven | `.specops/steering/dependencies.md` |

## CVE Scan Results

No dependency manifest changes occur in this spec. The existing dependency inventory is current
and has no accepted Critical or High finding recorded.

## EOL Status

| Product | Version | EOL Date | Status |
| --- | --- | --- | --- |
| Java | 21 | Vendor-specific | Active LTS baseline |
| Maven | 3.9.x | Not published | Active project baseline |

## Verification Method

- Layer 1: existing project dependency inventory; no new or changed package.
- Layer 2: skipped because the spec changes no dependency or manifest.
- Layer 3: not needed.

## Allowed Advisories

None.

## Dependency Introduction Gate

No new dependency is introduced.

