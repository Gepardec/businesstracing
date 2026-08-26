# Bug Fix: Business Rule Values and Viewer Regressions

## Problem

The reduced business graph now shows the relevant checks, but it can omit a source-proven rule
value. For example, the supplied entitlement graph says `svnr ist unter altersgrenze` although the
application supplies the value `18`. The viewer also truncates a material rule, draws several
selection outlines, and can put an edge label on a node port.

## Scope

This correction has two linked outputs:

- carry one unambiguous source-proven scalar value into a business-rule label;
- keep node text, selection, ports, and edge labels readable in the graph viewer.

The business JSON schema stays `fachtracing-business-graph/v1`. The exact graph stays authoritative.
The implementation must not contain insurance terms, a project glossary, a hard-coded diagram, or
an assumed age limit.

## Root Cause

1. Semantic node attributes contain the business method and subject but not one scalar value proved
   by the source call chain.
2. The business reducer cannot render a value that it does not receive.
3. The viewer uses a 92 px node height for labels that need three or four lines.
4. Selection uses a thick border and an outer ring at the same time.
5. Edge-label placement checks node boxes but does not reserve space around route endpoints.

## Impact

- **Severity:** Medium.
- **Affected behavior:** Static business explanations and graph readability.
- **Data and compatibility:** No stored data or public JSON shape changes.

## Regression Risk

| Area | Behavior to keep | Risk |
| --- | --- | --- |
| Exact analysis | Exact nodes and source mappings stay unchanged except for internal attributes. | Must-Test |
| Business projection | Values appear only when the analyzer finds one unambiguous scalar. | Must-Test |
| Generic analysis | Unrelated applications do not receive guessed values. | Must-Test |
| Layout | Routes remain collision-free after the node height change. | Must-Test |
| Interaction | Run-path and keyboard-focus states remain distinct from selection. | Must-Test |

## Acceptance Criteria

- WHEN a business predicate depends on one unambiguous scalar value proved by source analysis THE
  SYSTEM SHALL include that value in the visible business rule.
- IF source analysis finds no value or more than one possible value THEN THE SYSTEM SHALL omit the
  value and SHALL NOT guess.
- THE SYSTEM SHALL keep the V1 business JSON field set unchanged.
- WHEN a node is selected THE SYSTEM SHALL show one clear selection treatment.
- WHEN a business label needs four lines THE SYSTEM SHALL show the complete label in its node.
- WHEN an edge has a visible label THE SYSTEM SHALL keep the label clear of source and target ports.
- WHEN the two supplied insurance graphs are rendered at normal and overview zoom THE SYSTEM SHALL
  show complete outcomes, readable branches, and no new node or label collisions.

## Testing Plan

### Current Behavior

- Prove that the source-backed value is absent from the entitlement rule.
- Prove that the current node height truncates the supplied long rule.
- Prove that the label placer accepts a position inside endpoint clearance.

### Expected Behavior

- Add a generic source fixture with a configurable threshold and verify the projected value.
- Add an ambiguous-value fixture and verify that no value is added.
- Add viewer unit and browser geometry checks for node text, selection, and endpoint clearance.

### Unchanged Behavior

- Run the executable analyzer and business-projection contracts.
- Run viewer checks, unit tests, and the real-graph browser review.
- Regenerate and inspect both supplied business graphs.

