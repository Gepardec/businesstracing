# Dependency Audit: Interactive Flow and Run Explorer

**Verified:** 2026-08-19T17:48:57Z
**Threshold:** high
**Result:** PASS

## Dependency Inventory

| Package | Version | Ecosystem | Source |
| --- | --- | --- | --- |
| `svelte` | 5.56.9 | Node.js | audited lockfile |
| `@sveltejs/kit` | 2.70.3 | Node.js | audited lockfile |
| `@sveltejs/adapter-node` | 5.5.7 | Node.js | npm registry metadata |
| `tailwindcss` | 4.1.13 | Node.js | audited lockfile |
| `@tailwindcss/vite` | 4.1.13 | Node.js | audited lockfile |
| `shadcn-svelte` | 1.0.8 | Node.js | audited lockfile |
| `bits-ui` | 2.14.0 | Node.js | audited lockfile |
| `@lucide/svelte` | 1.33.0 | Node.js | audited lockfile; maintained replacement for deprecated `lucide-svelte` |
| `tailwind-variants` | 3.1.1 | Node.js | audited lockfile |
| `clsx` | 2.1.1 | Node.js | audited lockfile |
| `tailwind-merge` | 3.3.1 | Node.js | audited lockfile |
| `@xyflow/svelte` | 1.6.2 | Node.js | npm registry metadata |
| `elkjs` | 0.12.0 | Node.js | npm registry metadata |
| `pg` | 8.16.3 | Node.js | audited lockfile |
| `vite` | 7.3.6 | Node.js | audited lockfile |
| `vitest` | 4.1.11 | Node.js | audited lockfile |
| `@playwright/test` | 1.62.1 | Node.js | audited lockfile |

## CVE Scan Results

The committed npm lockfile contains 184 audited packages. `npm audit --audit-level=high` reports no high or critical vulnerability and exits successfully. It reports six low-severity findings through SvelteKit's current `cookie` dependency. The registry suggests an invalid breaking downgrade, so no forced remediation is accepted.

The initial requested point versions contained high and critical advisories in SvelteKit, Vite, Vitest, and Playwright. Implementation selected patched same-major versions before feature code was accepted. `lucide-svelte` emitted a deprecation warning, so the implementation uses its maintained `@lucide/svelte` replacement.

## EOL Status

| Product | Version | EOL Date | Status |
| --- | --- | --- | --- |
| Node.js | 24 | — | Active line used by local and hosted verification |

## Verification Method

- Layer 1 (Local audit): `npm audit --audit-level=high` passed against `package-lock.json`.
- Layer 2 (Online APIs): npm metadata confirmed the maintained Lucide replacement and patched direct versions.
- Layer 3 (LLM fallback): not used.

## Allowed Advisories

None.
