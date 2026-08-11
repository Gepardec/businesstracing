# Run: Spring PetClinic Business Conformance

- Replaced the technical PetClinic examples with owner search, visit booking, and pet registration.
- Kept the source overlay limited to three imports and three annotations.
- Added general contextual Spring Data page-query contracts with fail-closed subtype and method matching.
- Folded duplicate-name loop mechanics into one ordered business rule without changing exact Mega graphs.
- Added reviewed business JSON oracles and JSON Schema validation.
- Passed `./scripts/verify.sh` and `./scripts/verify-pr.sh`.
- Passed the PostgreSQL 18.4 storage test locally with `POSTGRES_JDBC_OK`.
- Hosted CI remains pending because GitHub pull-request creation is not authorized in this environment.
