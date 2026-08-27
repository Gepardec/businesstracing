---
specId: "real-graph-layout-usability"
startedAt: "2026-08-25T19:06:32Z"
completedAt: "2026-08-25T19:31:00Z"
finalStatus: "completed"
phases:
  - understand-context
  - revise-specification
  - implementation
  - verification
---

# SpecOps Run: real-graph-layout-usability

## Phase 1: Understand Context

- Reopened the completed specification after the user rejected the final screenshots.
- Confirmed that geometry tests do not prove visual or explanatory quality.
- Confirmed that business graph V1 contains the entry, node, edge outcome, and result labels needed for the correction.

## Phase 2: Revise Specification

- Added RB-10 and Task 12 for a persistent explanation workspace.
- Prohibited invented visible path labels.
- Required complete sequence contents and source-derived continuation navigation.
- Set the implementation evaluation and specification status back to failing and implementing.

## Phase 3: Implementation

- Removed synthetic `Path N` labels from unlabelled edges.
- Added a pure explanation model and the static-preview-only `GraphGuide` panel.
- Added current-step details, ordered sequence members, incoming context, and all immediate continuations.
- Made continuation rows select and frame their source-derived target.
- Replaced the count summary with start, first-alternative, and possible-result sentences.
- Removed the duplicate summary above the graph.
- Reserved desktop and narrow safe areas for the explanation panel.
- Corrected a narrow-screen overlap found during browser review.

## Phase 4: Verification

- `npm test`: 17 files and 76 tests passed.
- `npm run check`: 0 errors and 0 warnings.
- `npm run build`: passed with existing third-party warnings only.
- All three supplied graph files passed the local objective review.
- Eleven applicable Playwright journeys passed. Five external-fixture journeys skipped because their PostgreSQL or generated-dogfood inputs are not configured.
- Visual review covered initial, selected, Full detail, Overview, light, dark, and narrow states.
- Source scans found no supplied graph IDs, business labels, coordinates, routes, or hard-coded diagrams.
