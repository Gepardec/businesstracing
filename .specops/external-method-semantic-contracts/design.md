# Design: External Method Semantic Contracts

## Architecture

Add immutable contracts in the engine analysis package:

- `ExternalMethodReference` identifies one exact JVM method.
- `ExternalMethodContract` describes result semantics, side effects, and declared exception outcomes.
- `ExternalMethodContractProvider` supplies a collection of contracts.
- `ExternalMethodContractRegistry` validates providers and resolves zero or one exact contract.

`AnalysisRequest` receives an optional provider list through a compatibility constructor. The analyzer
checks source availability first. It then resolves a contract. An ambiguous resolution creates one
normal coverage gap at the call site. Opaque-library handling remains the next fallback.

## Contract Facts

A contract contains a business operation kind and label, result behavior, receiver and argument
effects, and possible exception type names. Contract facts are immutable. They do not execute code.
Exception facts only affect a path when the source method contains a compatible catch clause.

## Architecture Decisions

- Use exact keys. Do not use package prefixes or priority rules.
- Pass providers explicitly in analysis requests. Framework adapters can also expose providers with
  Java `ServiceLoader` at integration boundaries.
- Keep provider conflict detection in one registry.
- Keep source-tree handling in the analyzer and contract validation in the registry.
- Add no dependency.

### Dependency Decisions

| Package | Decision | Reason |
| --- | --- | --- |
| Java standard library | Approved | Records and collections are sufficient. |
