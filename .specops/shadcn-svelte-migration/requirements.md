# Shadcn-Svelte Migration Requirements

## SM-1 — Official component source

- [x] The viewer SHALL use current official shadcn-svelte source components for Button, Badge, Input, Card, Sheet, Alert, Label, Select, Switch, and Table.
- [x] Components SHALL use lowercase component folders with `index.ts` barrels and documented named or namespace imports.
- [x] The migration SHALL keep Svelte 5, Tailwind CSS 4, Bits UI, Lucide, and the current SvelteKit architecture.
- [x] The migration SHALL remove the superseded custom UI wrappers after all consumers move.

## SM-2 — Visual quality

- [x] The interface SHALL have one consistent control height, radius scale, border treatment, focus treatment, and disabled treatment.
- [x] Cards SHALL use the official Root, Header, Title, Description, Content, and Footer composition where those regions exist.
- [x] Status badges SHALL use semantic variants. Destructive controls SHALL use the documented destructive variant.
- [x] The existing blue product accent, graph node colors, graph routing colors, run highlights, and light and dark themes SHALL remain intentional and legible.
- [x] The migrated run list, run explanation, graph upload, graph preview, and error state SHALL look at least as polished as the baseline renders.

## SM-3 — Behavior and compatibility

- [x] Existing button, link, form submission, file upload, sheet, theme, search, graph, and run behavior SHALL remain unchanged.
- [x] The Sheet SHALL include an accessible title and description and SHALL keep keyboard and overlay behavior from Bits UI.
- [x] The graph JSON contracts, HTTP APIs, database behavior, and graph layout SHALL remain unchanged.
- [x] No diagram or application-specific graph SHALL be hardcoded.

## SM-4 — Verification

- [x] Svelte diagnostics, unit tests, production build, and applicable browser tests SHALL pass.
- [x] Browser screenshots SHALL cover the runs list, graph upload or preview, light theme, dark theme, and a narrow viewport. The database-backed suite SHALL retain run explanation and Sheet coverage.
- [x] Human review SHALL reject the migration if visual hierarchy, spacing, contrast, or interaction states regress.
