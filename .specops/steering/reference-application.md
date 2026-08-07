---
name: "Reference Application"
description: "One brownfield validation corpus for generic business-logic tracing"
inclusion: always
---

## Repository

- Source: `Gepardec/mega-backend`
- Reviewed commit: `782cdec8dfe5b4062eb5c1859e6a9e53afe02770`
- Stack: Java 21, Maven, Quarkus 3.37.1
- Production Java files at the reviewed commit: approximately 420

This repository is a mandatory realistic brownfield conformance corpus, not a source of product
rules. It proves that the generic extractor works on real application structure. The extractor
must work without prior knowledge of Mega's packages, domains, class names, method names, or
business vocabulary, and Mega-specific knowledge must never enter production implementation or
configuration.

## Business Areas

The active hexagonal backend contains `monthend`, `notification`, `project`, `user`, and `worktime` domains. Legacy business rules also exist under `domain/calculation`, `service/impl`, and `service/helper`.

## Polymorphic Pilot

`WarningCalculatorsManager` invokes the generic `WarningCalculationStrategy<T>` interface across time-warning and journey-warning implementations. This path is a useful validation case because static analysis can discover candidate implementations while runtime capture must identify the implementation and branch path actually used. It must not receive special handling in production code.

## Diagram Coverage

PlantUML and Mermaid output must be generated from business decisions reachable from selected, annotated entry points. Generated diagrams must express business conditions and results without exposing Java package, class, or method names to the business-facing representation. Coverage must be reported explicitly so unsupported or unresolved code cannot disappear silently. The same extractor and output pipeline must also pass structurally different, synthetic validation applications to demonstrate generality.

The validation harness may contain a test-only annotation overlay, selected entry points, and
independently reviewed expected graphs. Those artifacts are conformance oracles only: they must
not be consumed as analyzer hints, label dictionaries, branch mappings, allowlists, or runtime
configuration.

## Repository Conventions

- The legacy dependency direction is `rest -> service -> db`; legacy `domain` code remains isolated.
- New `hexagon` code follows DDD and ports-and-adapters boundaries.
- Existing source paths are reference inputs; Fachtracing remains a separate, framework-neutral library unless an integration task explicitly changes the reference repository.

## Spring PetClinic Teaching Corpus

- Source: `spring-projects/spring-petclinic`
- Reviewed commit: `88e37c15cf6fc8490b01bc3e8e2c800cec1ac272`
- Stack: Java 17 source, Maven, Spring Boot, Spring MVC, and Spring Data JPA
- Production Java files at the reviewed commit: 30

This smaller corpus explains generic extraction with three increasing levels of detail. Two source-visible decisions must be complete. One controller workflow must retain explicit gaps for result-relevant compiled framework and persistence effects. PetClinic-specific knowledge must remain in its conformance harness.
