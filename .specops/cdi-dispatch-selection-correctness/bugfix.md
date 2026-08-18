# Bugfix: CDI dispatch selection correctness

## Problem

The Jakarta EE dispatch selector can include CDI beans that the container cannot inject. It also
does not recognize dependencies assigned from an injected constructor parameter.

## Root Cause Analysis

- The selector checks `@Inject` only on the method-call receiver element. A constructor-injected
  dependency is usually called through a field, not through the constructor parameter.
- An injection point with no explicit qualifier is treated as if it has no required qualifier.
  CDI instead gives it the implicit `@Default` qualifier.
- Qualifier matching compares only annotation type names. It ignores binding member values and
  `@Nonbinding` members.

## Impact Assessment

**Severity:** High

The analyzer can produce impossible dynamic-dispatch paths. It can also retain all generic paths
for a constructor-injected dependency, which reduces the precision promised by the adapter.

## Regression Risk Analysis

### Blast Radius

- `DynamicDispatchTargetSelector.DispatchTarget`: carries the source injection point to adapters.
- `StaticDecisionAnalyzer.SourceIndex`: indexes constructor-parameter assignments to fields.
- `StaticDecisionAnalyzer` dynamic dispatch: supplies the resolved injection point.
- `CdiDispatchTargetSelector`: applies CDI injection and qualifier rules.
- Jakarta EE adapter tests and fixtures: verify the supported source forms.

### Behavior Inventory

| Behavior | Risk | Coverage |
| --- | --- | --- |
| Generic dispatch stays unchanged when no selector applies | Must-Test | Existing engine test |
| Injected fields select scoped, qualified beans | Must-Test | Existing adapter test |
| Constructor-injected fields use the constructor parameter qualifiers | Must-Test | Add regression test |
| Unqualified injection requires a default-qualified bean | Must-Test | Add regression test |
| Binding qualifier values must match | Must-Test | Add regression test |
| `@Nonbinding` qualifier values do not affect selection | Must-Test | Add regression test |

## Proposed Fix

Index direct assignments from injected constructor parameters to fields. Give the resolved
injection-point element to the framework selector. Compare CDI qualifiers by type and binding
member values, and implement the implicit `@Default` rules.

## Acceptance Criteria

- [x] WHEN a field receives a dependency from an `@Inject` constructor parameter THE SYSTEM SHALL
  use that parameter and its qualifiers for dispatch selection.
- [x] WHEN an injection point has no explicit qualifier THE SYSTEM SHALL select only beans that
  have the CDI `@Default` qualifier.
- [x] WHEN qualifiers have binding members THE SYSTEM SHALL require equal binding values and SHALL
  ignore members annotated with `@Nonbinding`.
- [x] IF no framework selector applies THEN THE SYSTEM SHALL keep generic dispatch behavior.

## Testing Plan

### Current Behavior

- Reproduce constructor fallback, implicit-default over-selection, and qualifier-value
  over-selection with one source fixture.

### Expected Behavior

- Assert the exact selected owner for each CDI injection form.
- Assert that a different binding value is excluded.
- Assert that a different nonbinding value remains eligible.

### Unchanged Behavior

- Run the existing engine dispatch-selector test.
- Run all Jakarta EE contract and conformance verification.
