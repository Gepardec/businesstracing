# Shadcn-Svelte Migration Implementation

## Decisions

| # | Decision | Reason |
| ---: | --- | --- |
| 1 | Use official registry source without compatibility wrappers. | The current flat wrappers imitate shadcn but prevent normal updates and documented composition. |
| 2 | Preserve and extend the current theme instead of running destructive initialization. | The viewer has graph-specific semantic tokens that the initializer would erase. |
| 3 | Use Lucide, a blue primary accent, and compact control sizing. | This preserves product identity and fits an internal analysis tool. |
| 4 | Require before-and-after visual review. | A technically correct migration is insufficient if it looks worse. |
| 5 | Extend the official Badge with success and warning variants. | Run and graph states need stable product semantics that the default registry variants do not supply. |
| 6 | Keep graph-canvas controls application-owned. | Search, presentation modes, edge inspection, and node selection are visualization behavior, not generic form primitives. |

## Result

- Replaced the flat custom wrappers with current registry source in lowercase component folders.
- Converted the decision filters, results, status states, upload surface, error states, theme control, and mobile explanation Sheet.
- Added semantic Tailwind theme mappings while preserving all graph and run colors.
- Added browser assertions that reject legacy primitive classes and require shadcn `data-slot` attributes.
- Added reviewed light, dark, and 390-pixel renders.
