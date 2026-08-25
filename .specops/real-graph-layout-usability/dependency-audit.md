# Dependency Audit: Real Graph Layout Usability

**Verified:** 2026-08-25T09:48:51Z  
**Threshold:** medium  
**Result:** PASS

## Dependency Inventory

This specification uses the unchanged `fachtracing-viewer/package-lock.json` with SHA-256 `7eb0f30c4f97c391ed4ca43e6a7b33f7e784eeb440ec3e7ca2e217f9329b4e1b`.

The implementation keeps the existing Svelte 5, SvelteKit, Svelte Flow, ELK, Tailwind CSS v4, shadcn-svelte, Bits UI, Lucide, Vitest, and Playwright dependencies. It adds no package.

## Runtime Status

| Product | Version | Status |
| --- | --- | --- |
| Node.js | 24.18.0 | Active project line |
| npm | 11.16.0 | Current local tool |

## Verification Method

- Confirmed that this spec adds no dependency.
- Confirmed that the lockfile hash matches the prior passing static-graph audit.
- Reused the prior audit evidence for the unchanged lockfile.
- No external advisory lookup was required for this spec-only change.

## Dependency Introduction Decision

No new dependency is approved. GSAP, a second graph engine, a routing package, and a screenshot service are explicitly excluded.

