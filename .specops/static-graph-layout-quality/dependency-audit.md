# Dependency Audit: Static Graph Layout Quality

**Verified:** 2026-08-25T07:39:57Z  
**Threshold:** medium  
**Result:** PASS

## Dependency Inventory

This bugfix uses the unchanged `fachtracing-viewer/package-lock.json` with SHA-256 `7eb0f30c4f97c391ed4ca43e6a7b33f7e784eeb440ec3e7ca2e217f9329b4e1b`.

The direct Svelte 5, SvelteKit, Svelte Flow, ELK, Tailwind CSS, shadcn-svelte, Bits UI, Lucide, PostgreSQL, Vite, Vitest, and Playwright dependencies are the same locked versions that passed the `frontend-flow-explorer` dependency audit on 2026-08-19. The last lockfile change is commit `daf83c49de4a5160dfd59232f07d7aed10c23159` from 2026-08-19.

## CVE Scan Results

The local `npm audit --json` request could not resolve the npm registry in the restricted environment. An unrestricted request was not approved because it would send dependency metadata outside the workspace. The command returned no package finding.

The lockfile SHA-256 matches the prior passing audit, and this specification adds no dependency. No new offline vulnerability finding is known for the locked direct packages. This statement uses existing project evidence and can omit advisories published after the prior online audit.

## EOL Status

| Product | Version | Status |
| --- | --- | --- |
| Node.js | 24 | Active project line |

## Verification Method

- Layer 1: attempted; registry DNS resolution failed.
- Layer 2: not available because network access was restricted.
- Layer 3: checked the prior passing audit, unchanged lockfile SHA-256, and lockfile history.
- New dependency introduction: none.

## Allowed Advisories

None.

