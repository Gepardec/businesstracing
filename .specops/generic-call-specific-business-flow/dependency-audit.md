# Dependency Audit: Generic Call-Specific Business Flow

## Scope

The design adds graph records, graph algorithms, renderers, tests, and conformance checks inside current Java and shell modules.

## Existing Dependencies Used

- Java 21 collections, records, hashing, and file APIs.
- Existing Fachtracing engine, agent, Maven, and conformance modules.
- Existing ASM runtime dependency remains unchanged and is not used by the new graph summary code.

## New Dependencies

No new dependency is introduced.

## Decision

The dependency introduction gate passes. The design and tasks contain no install command, new Maven coordinate, or external runtime service.
