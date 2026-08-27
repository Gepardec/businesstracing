# Reviewed business graph oracles

These JSON files are immutable verification inputs for `spring-projects/spring-petclinic` commit `88e37c15cf6fc8490b01bc3e8e2c800cec1ac272`. They are not analyzer input. The conformance runner cannot update them.

## Review method

The reviewer inspected each annotated method and each source-visible helper. The review listed every result-relevant rule, action, correction path, failure path, and successful result. Spring request binding and `@Valid` results were treated as method inputs.

The reviewer then checked these business meanings:

- Owner search states the name and result-page checks positively, searches records, records an
  empty-result error, and distinguishes zero, one, and multiple results.
- Visit booking rejects a missing or non-future date, respects other validation errors, saves a valid visit, and adds the success message.
- Pet registration checks required text and new-record state, folds the pet loop into the
  duplicate-name rule, checks the birth date, respects other validation errors, and distinguishes
  success, database duplicate, and the source-derived registration failure.

Each JSON file was parsed against `fachtracing-business-graph/v1`. Each Mermaid and JSON artifact was also checked for gaps and prohibited technical terms.

| Oracle | SHA-256 | Outcome |
| --- | --- | --- |
| `owner-search-business.json` | `dcfb51795211da3c71538072ee4323ad85d9fbb522977f3d00d4a8376c7bf2ce` | Approved complete graph with positive rule labels |
| `pet-registration-business.json` | `450a0df304fd18624fd453b6e3d9f076ecf0515415047a0f58aca8097fef689d` | Approved complete graph with an explicit failure result |
| `visit-booking-business.json` | `e0148a8cf3ec8f42210b0df765310c3e58d88cbf7bf6e428a5343822c2c587b3` | Approved complete business graph |

Exact JSON equality is the executable assertion. The hashes make review drift visible.
