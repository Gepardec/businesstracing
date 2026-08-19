# Dependency Audit: Interactive Flow and Run Explorer

**Verified:** 2026-08-19T11:58:25Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

| Package | Version | Ecosystem | Source |
| --- | --- | --- | --- |
| `svelte` | stable 5.x compatible with selected Kit release | Node.js | approved new dependency |
| `@sveltejs/kit` | 2.70.2 | Node.js | npm registry metadata |
| `@sveltejs/adapter-node` | 5.5.7 | Node.js | npm registry metadata |
| `tailwindcss` | stable 4.x compatible with selected SvelteKit release | Node.js | approved new build dependency |
| `@tailwindcss/vite` | stable 4.x compatible with selected Tailwind release | Node.js | approved new build dependency |
| `shadcn-svelte` | latest stable compatible with Svelte 5 and Tailwind CSS v4 | Node.js | approved development scaffolding |
| `bits-ui` | compatible stable release selected by shadcn-svelte | Node.js | approved generated-component dependency |
| `lucide-svelte` | compatible stable release selected by shadcn-svelte | Node.js | approved icon dependency |
| `tailwind-variants` | compatible stable release selected by shadcn-svelte | Node.js | approved generated-component dependency |
| `clsx` | compatible stable release selected by shadcn-svelte | Node.js | approved generated-component dependency |
| `tailwind-merge` | compatible stable release selected by shadcn-svelte | Node.js | approved generated-component dependency |
| `@xyflow/svelte` | 1.6.2 | Node.js | npm registry metadata |
| `elkjs` | 0.12.0 | Node.js | npm registry metadata |
| `pg` | 8.22.0 | Node.js | npm registry metadata |
| `vitest` | 4.1.10 | Node.js | npm registry metadata |
| `@playwright/test` | 1.62.1 | Node.js | npm registry metadata |

## CVE Scan Results

No lockfile exists because implementation has not started. Registry metadata shows active current releases, but it cannot prove the future transitive dependency set. Phase 3 must generate the lockfile and run `npm audit --audit-level=high`; high or critical findings block implementation completion.

The shadcn-svelte documentation confirms that its current line targets Svelte 5 and Tailwind CSS v4 and that its CLI writes component source into the project. The implementation must record the exact generated dependency set before it accepts the lockfile. A package that is not in this approved inventory requires a new dependency gate.

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
