# Design: CDI dispatch selection correctness

## Architecture

The generic engine resolves a source receiver to its injection-point element. The Jakarta EE
adapter remains responsible for CDI annotations and selection semantics.

## Technical Decisions

### Resolve only direct constructor assignments

The source index maps `this.field = parameter` when the parameter belongs to an `@Inject`
constructor. More complex data flow remains unsupported and causes selector abstention.

### Compare qualifier values from compiler metadata

The adapter builds binding-member maps from `AnnotationMirror` data. It uses declared defaults and
excludes members annotated with `jakarta.enterprise.util.Nonbinding`.

### Model implicit default qualification

An injection point with no explicit qualifier requires `@Default`. A bean has `@Default` when it
declares it directly or when it declares no qualifier other than `@Any` or `@Named`.

## Failure Modes

- An unresolved constructor assignment keeps the original receiver and causes safe abstention.
- Missing or malformed annotation metadata causes a non-match instead of a guessed match.
- Multiple selectors retain the existing conflict behavior.

## Dependency Decisions

No new dependency is required. The implementation uses the existing Java compiler model.
