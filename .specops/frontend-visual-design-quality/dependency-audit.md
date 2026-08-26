# Dependency Audit: Graph Viewer Visual Design Quality

**Verified:** 2026-08-21T11:16:08Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

This bugfix uses the unchanged `fachtracing-viewer/package-lock.json` with SHA-256 `7eb0f30c4f97c391ed4ca43e6a7b33f7e784eeb440ec3e7ca2e217f9329b4e1b`.

The direct Svelte, SvelteKit, Svelte Flow, ELK, Tailwind CSS, shadcn-svelte, Bits UI, Lucide, PostgreSQL, Vite, Vitest, and Playwright dependencies are the same versions that passed the `frontend-flow-explorer` audit on 2026-08-19.

## CVE Scan Results

The local `npm audit --json` request could not reach the npm registry because DNS access is unavailable in this environment. The audit did not report a package finding. The lockfile has not changed since the prior passing audit, and this spec approves no new dependency.

No new offline vulnerability finding is known for the locked direct packages. This statement is based on existing project evidence and model knowledge and may not include advisories published after the prior online audit.

## EOL Status

| Product | Version | Status |
| --- | --- | --- |
| Node.js | 24 | Active project line |

## Verification Method

- Layer 1: attempted; registry DNS failed.
- Layer 2: unavailable because network access failed.
- Layer 3: checked against the prior passing audit and unchanged lockfile.
- New dependency introduction: none.

## Allowed Advisories

None.
