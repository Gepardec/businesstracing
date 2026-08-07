# Spring PetClinic conformance selection

Pinned revision: `88e37c15cf6fc8490b01bc3e8e2c800cec1ac272`.

The canonical Spring Boot and Thymeleaf application is small, familiar, and structurally different from the existing large Quarkus corpus. The selection was made before analyzer changes. It supplies no extraction hints.

| Level | Annotated method | Business label | What it demonstrates |
| --- | --- | --- | --- |
| Entity state | `BaseEntity.isNew` | `determine whether an entity is new` | One result predicate and business normalization of an absent persistence value |
| Domain lookup | `Owner.getPet(String, boolean)` | `find an eligible pet by name` | Enhanced iteration, compound matching, optional exclusion of new pets, early return, and absent result |
| Application workflow | `PetController.processCreationForm` | `register a new pet` | Validation and redirect results plus explicit gaps for unavailable result-relevant framework and persistence effects |

The third graph is intentionally not a completeness success case. It proves that Fachtracing does not present unproved binary behavior as understood business logic. Analyzer remediation for these gaps is outside this suite.
