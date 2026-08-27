# Shadcn-Svelte Migration Design

## Component strategy

Install official registry source for Button, Badge, Input, Card, Sheet, Alert, Label, Select, Switch, and Table. Keep `CopyValue` as an application component because it represents product behavior, not a primitive. Move all imports to `$lib/components/ui/<component>` barrels.

Use the official component APIs without compatibility wrappers. Rename `danger` to `destructive`, convert badge tones to semantic variants, and compose Card and Sheet regions explicitly. This makes future registry updates reviewable and keeps each primitive responsible for one concern.

## Theme strategy

Preserve `src/app.css` as the only global stylesheet. Add the complete shadcn semantic token set and Tailwind `@theme inline` mapping. Keep the existing blue primary accent and all graph-specific tokens. Remove global `.button`, `.input`, `.card`, and `.badge` primitive styling after consumers use official components.

Use the compact Nova product scale: a 0.625rem base radius, 32-pixel default controls, 28-pixel small controls, quiet borders, and restrained shadows. Page and graph layout CSS remains application-owned.

## Migration sequence

1. Add official source components through the shadcn-svelte CLI.
2. Reconcile theme tokens without overwriting graph tokens.
3. Convert one primitive and all its consumers at a time.
4. Convert Card and Sheet composition.
5. Remove obsolete wrappers and primitive CSS.
6. Run behavior tests and compare browser renders in both themes.

## Risk controls

- Do not run destructive `init` or `add --overwrite` against customized files.
- Keep the migration separate from graph layout and data contracts.
- Use existing tests for control names and accessible roles.
- Review generated registry source before use.
