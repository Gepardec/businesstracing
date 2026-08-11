# Design: Spring PetClinic Business Conformance

## Entry Methods

- Owner search: normalize the optional surname search, then distinguish no owner, one owner, and
  several owners.
- Visit booking: reject a date before today, respect incoming validation errors, or save the visit.
- Pet registration: reject a duplicate name or future birth date, respect validation errors, handle
  a duplicate database constraint, rethrow other persistence failures, or save successfully.

## Harness

The overlay adds only `@FachTracing` annotations and imports. The harness loads the generic Spring
provider, runs the normal analyzer and projector, writes disposable artifacts under `target`, and
compares normalized business JSON with committed oracles. The report embeds the generated Mermaid
source and describes the named results.

## Architecture Decisions

- Replace the current technical teaching targets.
- Require complete business graphs. Do not accept generic coverage gaps.
- Keep exact developer topology checks separate from business-oracle checks.
- Scan every production module for PetClinic tokens.

### Dependency Decisions

| Package | Decision | Reason |
| --- | --- | --- |
| `fachtracing-spring` | Approved | Supplies reusable Spring method contracts. |
| Pinned PetClinic dependencies | Approved for conformance only | Compile and attribute the reference source. |
