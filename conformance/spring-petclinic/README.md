# Spring PetClinic conformance

This harness shows what Fachtracing does to a familiar Spring application when a developer only adds three `@FachTracing` annotations. It uses the canonical `spring-projects/spring-petclinic` repository at commit `88e37c15cf6fc8490b01bc3e8e2c800cec1ac272`.

Run `scripts/verify-spring-petclinic.sh`. Set `SPRING_PETCLINIC_DIR` to an existing clean checkout if necessary. The script builds a disposable worktree, applies the annotation-only overlay, analyzes all 30 production Java files, compares three immutable semantic graph oracles, and writes Mermaid, PlantUML, and normalized output under `conformance/spring-petclinic/target/generated`.

The three graphs have increasing detail:

1. A new-entity check shows one complete business predicate.
2. A pet lookup shows complete loop, match, eligibility, and terminal behavior.
3. The new-pet workflow shows the visible result paths and five explicit gaps where compiled framework or persistence effects cannot be proved.

See [the selection rationale](selection.md), [the oracle review method](src/test/resources/oracles/README.md), and [the graph report](conformance-report.md).
