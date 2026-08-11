# Spring PetClinic business-graph conformance

This harness shows Fachtracing on a familiar Spring application. The source overlay adds only three `@FachTracing` imports and annotations. It does not change application logic.

The test uses `spring-projects/spring-petclinic` commit `88e37c15cf6fc8490b01bc3e8e2c800cec1ac272`. It analyzes these workflows:

- owner search;
- visit booking;
- pet registration.

All three exact analysis graphs and all three business graphs must be `COMPLETE`. Spring request binding and `@Valid` run before each annotated method. The graph therefore treats their results as method inputs.

Run:

```sh
./scripts/verify-spring-petclinic.sh
```

Set `SPRING_PETCLINIC_DIR` if the clean pinned checkout is not at `/tmp/fachtracing-spring-petclinic`.

The script creates a disposable worktree, applies the annotation-only overlay, and analyzes the full production source set. It compares each business JSON file with a reviewed oracle. It also parses each JSON file against `fachtracing-business-graph/v1`.

Generated output is under `conformance/spring-petclinic/target/generated`. Each workflow has business Mermaid, PlantUML, and JSON files. The exact structure files remain as technical developer artifacts.

See [the selection rationale](selection.md), [the oracle review](src/test/resources/oracles/README.md), and [the graph report](conformance-report.md).
