# Dependency Audit: Interactive Flow and Run Explorer

**Verified:** 2026-08-19T10:52:40Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

| Package | Version | Ecosystem | Source |
| --- | --- | --- | --- |
| `svelte` | stable 5.x compatible with selected Kit release | Node.js | approved new dependency |
| `@sveltejs/kit` | 2.70.2 | Node.js | npm registry metadata |
| `@sveltejs/adapter-node` | 5.5.7 | Node.js | npm registry metadata |
| `@xyflow/svelte` | 1.6.2 | Node.js | npm registry metadata |
| `elkjs` | 0.12.0 | Node.js | npm registry metadata |
| `pg` | 8.22.0 | Node.js | npm registry metadata |
| `vitest` | 4.1.10 | Node.js | npm registry metadata |
| `@playwright/test` | 1.62.1 | Node.js | npm registry metadata |

## CVE Scan Results

No lockfile exists because implementation has not started. Registry metadata shows active current releases, but it cannot prove the future transitive dependency set. Phase 3 must generate the lockfile and run `npm audit --audit-level=high`; high or critical findings block implementation completion.

## EOL Status

| Product | Version | EOL Date | Status |
| --- | --- | --- | --- |
| Node.js | To be selected from an active LTS line during implementation | — | Must be Active or Maintenance LTS |

## Verification Method

- Layer 1 (Local audit): skipped — no Node manifest or lockfile exists yet.
- Layer 2 (Online APIs): npm package metadata checked for release activity, direct dependency count, and licenses.
- Layer 3 (LLM fallback): used only to note that the lockfile audit is still required; no offline vulnerability finding is asserted.

## Allowed Advisories

None.
