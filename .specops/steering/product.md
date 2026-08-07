---
name: "Product Context"
description: "What this project builds, for whom, and how it is positioned"
inclusion: always
---

## Product Overview

Fachtracing is an embeddable, domain-neutral Java capability that derives business-logic trees from annotated decision methods it has never seen before and enriches them at runtime with the path and evidence needed to explain a specific result. `Gepardec/mega-backend` and Spring PetClinic are validation corpora, not sources of product-specific rules.

## Target Users

Java developers integrate the tracing capability and mark business-decision entry points. Non-technical business users consume persisted decision records to understand why a result occurred without reading source code, stack traces, class names, or method names.

## Key Differentiators

- Static analysis supplies a reusable, domain-neutral model of the reachable business logic instead of requiring developers to hand-code every trace step.
- Lightweight runtime capture adds actual inputs, evaluated branches, and dynamically selected implementations where static analysis cannot resolve polymorphism.
- The exported record preserves business meaning while excluding technical execution details.
- The extractor must not contain `mega-backend`-specific packages, method lists, vocabularies, or rule mappings. Its diagrams for that repository must be generated from the same generic mechanism used for other Java applications.
- The smaller Spring PetClinic corpus must teach the same generic behavior, including explicit coverage gaps when result-relevant framework behavior cannot be proved.
