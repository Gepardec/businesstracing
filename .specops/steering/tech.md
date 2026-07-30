---
name: "Technology Stack"
description: "Languages, frameworks, tools, and quality infrastructure"
inclusion: always
---

## Core Stack

- Java library and analysis tooling designed to remain independent of application frameworks.
- The reference application is `Gepardec/mega-backend` at commit `782cdec8dfe5b4062eb5c1859e6a9e53afe02770`, using Java 21, Maven, Quarkus 3.37.1, CDI, and both layered and DDD/hexagonal code.
- Java 21 is the initial validation baseline; broader Java-version compatibility remains a separate compatibility decision.
- Static analysis builds the reusable business-logic model; lightweight runtime instrumentation resolves actual execution paths and polymorphic calls.
- Persistence is accessed through an adapter so the core trace protocol does not depend on a specific database.

## Development Tools

- Build system and dependency choices are not yet selected.
- PlantUML source diagrams document the library architecture; generated decision records provide both PlantUML and Mermaid business graphs.

## Quality & Testing

- Tests must cover analysis of representative Java control flow from `mega-backend`, runtime path correlation, polymorphic dispatch, serialization compatibility, and performance at 1,000 application requests per second.
- Performance validation must measure host-application overhead rather than only raw trace serialization throughput.
