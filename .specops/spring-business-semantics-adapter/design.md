# Design: Spring Business Semantics Adapter

## Architecture

`fachtracing-spring` contains one `SpringMethodContractProvider` and a service registration. It has a
production dependency only on `fachtracing-engine`. The provider uses string JVM signatures and does
not import Spring types. Tests use real Spring artifacts with test scope.

## Contract Catalog

- `StringUtils.hasText`: pure predicate.
- `Errors` and `BindingResult`: validation mutation and error predicate.
- Spring Data `Page`: empty, size, content, and iteration/cardinality facts.
- Repository `save` and `saveAndFlush`: persistence action and returned value.
- `DataIntegrityViolationException`: possible persistence failure outcome.
- `RedirectAttributes`: result-independent flash mutation.

## Architecture Decisions

- One provider owns the Spring signature catalog.
- The adapter does not trust whole Spring archives.
- Service loading is additive; explicit analysis request providers remain supported.
- No PetClinic knowledge is permitted.

### Dependency Decisions

| Package | Scope | Decision | Reason |
| --- | --- | --- | --- |
| `fachtracing-engine` | production | Approved | Supplies the generic provider API. |
| Spring Framework and Spring Data APIs | test | Approved | Verify signatures against real APIs without production coupling. |
