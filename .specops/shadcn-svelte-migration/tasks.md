# Shadcn-Svelte Migration Tasks

## Task 1 — Install and configure official primitives

**Status:** Complete

- [x] Add current registry Button, Badge, Input, Card, Sheet, Alert, Label, Select, Switch, and Table source.
- [x] Update `components.json` for Lucide and current registry use.
- [x] Merge complete semantic tokens into the existing theme.

## Task 2 — Convert application consumers

**Status:** Complete

- [x] Convert Button, Badge, Input, Select, Switch, and Table imports and variants.
- [x] Convert Card users to documented composition.
- [x] Replace the custom Sheet with official Sheet composition.
- [x] Remove obsolete primitive wrappers and CSS.

## Task 3 — Prove behavior and visual improvement

**Status:** Complete

- [x] Pass check, unit, build, and applicable browser tests.
- [x] Render both themes and narrow state.
- [x] Review control consistency, card hierarchy, sheet accessibility, and graph integrity.
