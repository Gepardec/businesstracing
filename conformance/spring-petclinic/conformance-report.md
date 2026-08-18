# Spring PetClinic business-graph conformance report

- Status: **passed locally**
- Pinned source: `spring-projects/spring-petclinic@88e37c15cf6fc8490b01bc3e8e2c800cec1ac272`
- Run date: 2026-08-11, Java 21

## What the test shows

The source overlay adds only three `@FachTracing` annotations. Fachtracing combines source analysis with the optional Spring contract adapter. It then creates a separate business graph. The exact analysis graph stays unchanged for developer checks and runtime tracing.

All three workflows are complete for the annotated Java method. There are no business `GAP` nodes. Spring request binding and `@Valid` results are method inputs.

## Owner search

```mermaid
flowchart LR
    n1{"last name is absent"}
    n2{"result page is empty"}
    n3["record field validation error"]
    n4{"total result count equals 1"}
    n5(["no matching records"])
    n6(["one matching record"])
    n7(["multiple matching records"])
    n1 -->|"yes"| n2
    n1 -->|"no"| n2
    n2 -->|"yes"| n3
    n2 -->|"no"| n4
    n3 --> n5
    n4 -->|"yes"| n6
    n4 -->|"no"| n7
```

Result: **complete**, 7 business nodes and 7 business edges.

## Visit booking

```mermaid
flowchart LR
    n1{"visit date exists"}
    n2{"visit date is today or earlier"}
    n3["record field validation error"]
    n4{"validation has errors"}
    n5["save record"]
    n6["add response message"]
    n7(["correction required"])
    n8(["visit booking completed"])
    n1 -->|"yes"| n2
    n2 -->|"yes"| n3
    n3 --> n4
    n1 -->|"no"| n4
    n2 -->|"no"| n4
    n4 -->|"no"| n5
    n5 --> n6
    n4 -->|"yes"| n7
    n6 --> n8
```

Result: **complete**, 8 business nodes and 9 business edges.

## Pet registration

```mermaid
flowchart LR
    n1{"text is present"}
    n2{"pet is new"}
    n3{"a pet with this name exists"}
    n4["record field validation error"]
    n5{"pet birth date exists"}
    n6{"pet birth date is in the future"}
    n7["record field validation error"]
    n8{"validation has errors"}
    n9["save record"]
    n10{"persistence failure is not a duplicate record"}
    n11["record field validation error"]
    n12["add response message"]
    n13(["correction required"])
    n14(["operation failed"])
    n15(["pet registration completed"])
    n1 -->|"yes"| n2
    n2 -->|"yes"| n3
    n3 -->|"yes"| n4
    n4 --> n5
    n1 -->|"no"| n5
    n2 -->|"no"| n5
    n3 -->|"no"| n5
    n5 -->|"yes"| n6
    n6 -->|"yes"| n7
    n7 --> n8
    n5 -->|"no"| n8
    n6 -->|"no"| n8
    n8 -->|"no"| n9
    n8 -->|"no"| n10
    n10 -->|"no"| n11
    n9 --> n12
    n8 -->|"yes"| n13
    n10 -->|"yes"| n14
    n11 --> n13
    n12 --> n15
```

Result: **complete**, 15 business nodes and 20 business edges. The two equivalent correction results use one result node.

## Reproduce the result

```sh
./scripts/verify-spring-petclinic.sh
```

The command checks the clean pinned revision, applies the annotation-only overlay, analyzes the full main source set, compares the reviewed JSON oracles, validates the JSON schema, and writes generated artifacts under `conformance/spring-petclinic/target/generated`.
