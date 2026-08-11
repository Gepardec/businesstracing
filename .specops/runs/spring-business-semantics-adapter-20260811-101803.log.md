# Run: Spring Business Semantics Adapter

- Started from `codex/business-graph-projection`.
- Added the optional adapter module and Java service registration.
- Added exact Spring utility, validation, page, repository, persistence-failure, and response contracts.
- Added deterministic plugin-realm service loading.
- Verified every signature against real Spring test APIs.
- Verified complete supported fixtures and one incomplete unmatched fixture.
- Passed `./scripts/verify-pr.sh`.
- Local PostgreSQL verification was skipped because no connection was configured.
