# SpecOps Run: frontend-flow-explorer

- Run ID: `20260819-214951`
- Started: `2026-08-19T21:49:51Z`
- SpecOps version: `1.8.0`
- Status: completed

## Objective

Repair graph JSON compatibility, exact edge routing, and visual quality after user acceptance found functional and visual defects.

## Evidence Found

- The local preview accepts only developer graph V1 and rejects the stable business graph V1 root field `graphId`.
- ELK computes obstacle-safe orthogonal sections, but the layout adapter discards them.
- The edge component recomputes smooth-step paths, which makes skip edges cross nodes and parallel edges overlap.
- The read-only canvas exposes connection handles and combines type, path, and current borders.

## Completion

Task 9 is complete. Real exported fixtures, geometry-based browser checks, inspected light and dark proof, the 250-node safety profile, and hosted CI all passed.

## Local Verification

- Svelte diagnostics: 0 errors and 0 warnings.
- Unit tests: 21 passed across 9 files.
- Browser tests: 10 passed with local PostgreSQL and generated Fachtracing artifacts.
- Browser geometry: sampled SVG routes do not enter unrelated nodes; parallel routes are distinct; labels do not cover nodes or other labels; read-only handles are invisible.
- Real formats: generated developer graph V1 and checked-in Spring PetClinic business graph V1 both render.
- Visual review: inspected light, dark, 1,440-pixel, 1,024-pixel, 390-pixel Sheet, node-grammar, branch-routing, dogfood, and focused 250-node images.
- Repository verification: passed with JDK 21.
- PostgreSQL storage contract: passed with JDK 21.
- Dependency audit: no high or critical findings; six low findings remain in the current SvelteKit cookie dependency.

## Hosted Verification

Run `32307869463` passed all four jobs at commit `1770f4f`:

- PostgreSQL viewer journey and proof upload: passed.
- Core repository gate: passed.
- Spring PetClinic conformance: passed.
- Mega Backend conformance: passed.

Task 9 and specification version 14 are complete.
