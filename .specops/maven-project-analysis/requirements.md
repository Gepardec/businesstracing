# Feature: Maven Project Analysis

## Overview

Java developers need to run Fachtracing on an ordinary Maven project without writing an analyzer
launcher or manually assembling source and dependency paths. After annotating decision methods,
the normal Maven lifecycle should generate business diagrams for that module.

## Acceptance Criteria

- [x] WHEN a Maven module executes the Fachtracing `analyze` goal THE SYSTEM SHALL discover all of its main Java source roots and resolved compile classpath without project-specific configuration.
- [x] WHEN annotated methods are discovered THE SYSTEM SHALL generate deterministic Mermaid and PlantUML structural diagrams plus a readable index under `target/fachtracing` by default.
- [x] WHEN a reactor module contains no `@FachTracing` method THE SYSTEM SHALL skip it successfully and clearly report that no decision entry was found.
- [x] IF source attribution or diagram generation fails THEN THE SYSTEM SHALL fail the Maven build with an actionable message.
- [x] WHERE `failOnIncomplete` is enabled THE SYSTEM SHALL fail if any graph reports incomplete static coverage; otherwise it SHALL generate the graph and warn.
- [x] THE SYSTEM SHALL provide a copyable POM configuration after which `mvn process-classes` generates diagrams.
- [x] THE SYSTEM SHALL support a one-off fully qualified Maven command without requiring plugin configuration in the target POM.
- [x] THE SYSTEM SHALL remain domain-neutral and SHALL pass the generic suite and pinned Mega conformance unchanged.

## Scope

This feature generates static structural graphs. Capturing and persisting a path from a particular
runtime execution still requires the existing Java agent and runtime integration. Maven support is
module-oriented: a multi-module reactor executes the plugin once per configured child module and
writes into each module's own build directory.

## Assumptions

- The target project uses Java 21 and Maven 3.9.x.
- The user can consume Fachtracing artifacts from a configured Maven repository or a local install.
