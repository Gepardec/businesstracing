# Design: Business Rule Values and Viewer Regressions

## Semantic Value Flow

The source analyzer will use one small value-evidence resolver. It follows attributed source methods
and compatible source implementations. It collects direct scalar literals and immutable
configuration values only when their field or key is read by the relevant method. It returns a
value only when all collected evidence has one distinct value.

The analyzer stores this value in one internal semantic attribute. The business reducer appends it
to the named predicate. The exporter continues to write the existing node label string, so the V1
contract does not change.

```text
attributed predicate call
  -> source method and implementation traversal
  -> zero or one unambiguous scalar value
  -> internal semantic attribute
  -> business label
```

## Viewer Correction

- Keep one matching node height in layout and rendering, with a contract test for the minimum.
- Permit four lines for a single business rule.
- Use one border and one restrained shadow for selection.
- Keep the keyboard focus ring separate and visible only for keyboard focus.
- Reject edge-label candidates that enter endpoint clearance.
- Re-run route planning after the node dimension change.

## Boundaries

- No application noun or package is matched.
- No literal value is built into production code.
- No diagram or graph topology is hard-coded.
- No new dependency, public schema, storage migration, endpoint, or CI change is required.

## Affected Files

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/analysis/StaticDecisionAnalyzer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/model/BusinessSemanticAttributes.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/business/BusinessSemanticReducer.java`
- engine executable contract tests
- `fachtracing-viewer/src/lib/graph/BusinessNode.svelte`
- `fachtracing-viewer/src/lib/graph/layout-definition.ts`
- `fachtracing-viewer/src/lib/graph/route-planner.ts`
- viewer unit and browser tests
