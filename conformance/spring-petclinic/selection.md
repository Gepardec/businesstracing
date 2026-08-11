# Spring PetClinic conformance selection

Pinned revision: `88e37c15cf6fc8490b01bc3e8e2c800cec1ac272`.

Spring PetClinic is small, familiar, and different from the existing Quarkus corpus. The overlay adds only annotations. It gives the analyzer no extraction hints.

| Workflow | Annotated method | Required business paths |
| --- | --- | --- |
| Owner search | `OwnerController.processFindForm` | no owners, one owner, or multiple owners |
| Visit booking | `VisitController.processNewVisitForm` | invalid date, other validation errors, or successful booking |
| Pet registration | `PetController.processCreationForm` | duplicate name, future birth date, other validation errors, database duplicate, unexpected persistence failure, or success |

These methods include business decisions, mutations, correction paths, and failure paths. The business projection folds helper calls and loop mechanics into business rules. The exact graph stays available for developer and runtime checks.

The conformance test rejects gaps and technical vocabulary in every business artifact. It also rejects PetClinic-specific rules in production modules.
