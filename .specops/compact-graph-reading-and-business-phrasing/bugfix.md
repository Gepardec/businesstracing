# Bug Fix: Compact Graph Reading and Business Phrasing

## Problem Statement

The Explore view can fit seven nearby cards into a compact safe area by reducing the zoom to about
50 percent. The graph stays inside the guide boundary, but its text is too small to read. Generated
aggregate rules also join source nouns with English filler words. A label such as
`svnr has versicherungszeiten that begruendet anspruch on stichtag` is hard to read and mixes two
language grammars.

## Root Cause Analysis

- `directNeighborhood` includes every direct predecessor and successor without a compact node
  budget.
- `readingViewport` permits a 0.48 zoom floor when the safe area is compact.
- The initial static analyzer renders aggregate subject, collection, condition, and qualifier as
  one English sentence before the business projection receives them.

## Impact Assessment

- **Severity:** Medium.
- **Users affected:** All users who inspect a converging decision on a compact screen and users who
  read source terms that are not English.
- **Frequency:** Often for business decisions with several inputs.

## Dependencies and Blockers

The completed business semantic reduction and viewer regression specifications are required. There
are no external blockers and no new dependency is required.

## Reproduction Steps

1. Load the supplied entitlement graph.
2. Select the entitlement predicate and resize the viewer to 860 by 900 pixels.
3. Observe seven cards at about half size.
4. Inspect the aggregate entitlement labels.
5. Actual: cards are too small and labels contain `has`, `that`, and `on` between German nouns.
6. Expected: a smaller complete decision neighborhood stays readable and labels use neutral role
   separators.

## Regression Risk Analysis

### Blast Radius

- `graph-viewport.ts` selects the nodes and viewport for all Explore views.
- `FlowCanvas.svelte` creates and relays out the local graph after selection and resize.
- `StaticDecisionAnalyzer.aggregateMatchLabel` creates labels for all proven `Stream.anyMatch`
  predicates.
- Real-graph browser tests exercise compact layout, guide navigation, and branch alternatives.

### Behavior Inventory

- Wide Explore view shows every direct predecessor and successor.
- Compact Explore view must keep all direct outgoing alternatives so no branch appears terminal.
- The guide keeps the complete incoming and outgoing connection list.
- Full JSON V1 and exact graph mappings stay unchanged.
- Unsafe or material aggregate callbacks stay expanded.

### Risk Tier

| Behavior | Tier | Reason |
| --- | --- | --- |
| Compact topology selection and resize relayout | Must-Test | It directly changes visible nodes. |
| All outgoing alternatives remain visible | Must-Test | Missing alternatives make the graph false. |
| Aggregate callback safety and JSON V1 | Must-Test | The change must only alter display text. |
| Wide direct neighborhood | Nice-To-Test | Compact limits must not reduce desktop context. |

## Proposed Fix

Add one pure compact-neighborhood selector. It keeps the selected node and all direct successors.
It adds direct predecessors in stable graph order only while a three-card context budget remains.
The guide remains the complete navigation index. Recreate the local layout when the safe area
crosses the compact threshold. Remove the 0.48 zoom fallback and keep cards at the normal
neighborhood reading floor.

Render aggregate roles with punctuation instead of inserted sentence grammar:
`<subject> — <collection>: <condition> (<qualifier>)`. Derive every term from source evidence. Do not
add a domain dictionary, translation, application name, or fixed diagram.

## Unchanged Behavior

- WHEN the viewer has enough safe space THE SYSTEM SHALL CONTINUE TO show the complete direct
  neighborhood.
- WHEN a selected rule has several outgoing alternatives THE SYSTEM SHALL CONTINUE TO show every
  direct alternative.
- WHEN an aggregate callback is not safe to collapse THE SYSTEM SHALL CONTINUE TO keep its detail.
- WHEN business JSON is exported THE SYSTEM SHALL CONTINUE TO use
  `fachtracing-business-graph/v1` without new fields.

## Testing Plan

### Current Behavior

- At 860 by 900 pixels, the selected real graph currently renders cards below the existing
  160-pixel browser reading threshold.
- Aggregate labels currently contain mixed `has … that … on …` grammar.

### Expected Behavior

- Unit tests prove the compact node budget and preservation of all outgoing alternatives.
- Browser tests prove compact cards are at least 160 pixels wide, stay above the guide, and show
  fewer cards than the wide local view when the direct neighborhood is large.
- Analyzer tests prove neutral aggregate role formatting in unrelated source fixtures.
- The two supplied graphs show source-derived terms and the value `18` without English filler.

### Unchanged Behavior

- Run viewer unit, Svelte, build, layout-review, and real-graph browser checks.
- Run the executable static analyzer and business projection contracts.
- Regenerate and inspect both supplied graphs.

## Acceptance Criteria

- [x] Regression Risk Analysis is complete for Medium severity.
- [x] Current behavior is reproduced from the prior compact screenshot and generated JSON.
- [x] WHEN the safe graph area is compact THE SYSTEM SHALL render a bounded local neighborhood at a
  card width of at least 160 pixels.
- [x] WHEN the selected node has direct outgoing alternatives THE SYSTEM SHALL show all of them in
  compact Explore view.
- [x] WHEN aggregate business evidence is rendered THE SYSTEM SHALL use source-derived terms with
  neutral separators and SHALL NOT insert mixed-language sentence filler.
- [x] THE SYSTEM SHALL keep business JSON V1 and exact graph detail unchanged.
- [x] All Must-Test unchanged behaviors shall pass.

## Team Conventions

- Use ASD-STE100 Simplified Technical English in project documentation and messages.
- Keep components single-purpose.
- Do not hard-code diagrams or application-specific business rules.
