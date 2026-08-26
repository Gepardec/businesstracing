# Dependency Audit: Business Graph Semantic Explanation

## Decision

No new dependency is required.

The implementation can use attributed compiler symbols, the existing exact method contract model
for source-unavailable methods, the existing graph projection audit, and current Java collections.
It must not add a project glossary parser or application-specific semantic dependency.

## Boundaries

- No LLM or remote service.
- No translation service.
- No viewer dependency.
- No JSON V2 library or schema.
- No CI configuration change.
