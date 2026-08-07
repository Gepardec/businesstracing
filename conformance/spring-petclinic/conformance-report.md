# Spring PetClinic conformance report

Status: **passed**
Pinned source: `spring-projects/spring-petclinic@88e37c15cf6fc8490b01bc3e8e2c800cec1ac272`
Run date: 2026-08-07, Java 21

## What this test shows

Fachtracing finds methods marked with one public annotation. It reads the reachable Java logic and creates a business graph. It removes Java names that do not add business meaning. If the analyzer cannot prove a result-relevant effect, it adds a visible coverage gap.

The PetClinic suite shows this behavior at three levels. No Spring integration or PetClinic-specific production configuration exists.

## 1. A simple state decision

The source returns whether the persistence value is absent. Fachtracing converts this into one complete business predicate with both results connected to one Stop.

```mermaid
flowchart LR
    n1(["Start"])
    n2{"value is absent"}
    n3(["Stop"])
    n1 --> n2
    n2 -->|"true; returns whether value is absent"| n3
    n2 -->|"false; returns whether value is absent"| n3
```

Result: **complete**, 3 nodes, 3 edges.

## 2. A domain lookup

The source searches the owner's pets by name. It can exclude a matching pet that is not saved yet. Fachtracing shows the loop, the atomic name checks, the eligibility checks, the early found result, and the absent result after the loop.

```mermaid
flowchart LR
    n1(["Start"])
    n2{"for each pet in pets"}
    n3["derive comp name as pet name"]
    n4{"comp name exists"}
    n5{"comp name equals ignore case name"}
    n6["evaluate is new"]
    n7{"value is absent"}
    n8{"not ignore new"}
    n9{"not pet is new"}
    n10(["Stop"])
    n1 --> n2
    n2 -->|"item"| n3
    n4 -->|"true"| n5
    n3 --> n4
    n5 -->|"true"| n6
    n6 --> n7
    n8 -->|"false"| n9
    n7 -->|"true"| n8
    n7 -->|"false"| n8
    n8 -->|"true; returns pet"| n10
    n9 -->|"true; returns pet"| n10
    n9 -->|"next item"| n2
    n4 -->|"next item"| n2
    n5 -->|"next item"| n2
    n2 -->|"done; returns absent"| n10
```

Result: **complete**, 10 nodes, 15 edges.

## 3. An application workflow with proof limits

The new-pet controller uses compiled Spring validation objects and persistence calls. Fachtracing can prove the visible error-result predicate, the duplicate-name exception check, the escaping failure, and the terminal view or redirect results. It cannot reconstruct five result-relevant binary side effects. The graph shows these limits as explicit gap nodes.

```mermaid
flowchart LR
    n1(["Start"])
    n2{{"analysis incomplete: a possible side effect on the returned decision cannot be reconstructed"}}
    n3{{"analysis incomplete: a possible side effect on the returned decision cannot be reconstructed"}}
    n4{{"analysis incomplete: binary method contains an unsupported call, monitor, switch, or dynamic instruction"}}
    n5{"result has errors"}
    n6(["Stop"])
    n7{"select decision result path"}
    n8{{"analysis incomplete: a possible side effect on the returned decision cannot be reconstructed"}}
    n9["evaluate is duplicate pet name violation"]
    n10["derive message as ex message"]
    n11{"message exists"}
    n12{"message to lower case contains unique_owner_pet_name"}
    n13{"is not duplicate pet name violation ex"}
    n14["decision cannot continue"]
    n15{{"analysis incomplete: a possible side effect on the returned decision cannot be reconstructed"}}
    n1 -->|"unresolved"| n2
    n2 -->|"unresolved"| n3
    n3 -->|"unresolved"| n4
    n4 --> n5
    n5 -->|"true; returns views pets create or update form"| n6
    n5 -->|"false"| n7
    n7 -->|"unresolved"| n8
    n7 -->|"alternative result 1"| n9
    n9 --> n10
    n10 --> n11
    n11 -->|"true"| n12
    n11 -->|"false"| n13
    n12 -->|"true"| n13
    n12 -->|"false"| n13
    n13 -->|"true"| n14
    n14 -->|"fails"| n6
    n13 -->|"unresolved"| n15
    n15 -->|"returns views pets create or update form"| n6
    n8 -->|"returns redirect:/owners/{ownerId}"| n6
    coverage["Incomplete analysis<br/>- a possible side effect on the returned decision cannot be reconstructed affects the decision<br/>- a possible side effect on the returned decision cannot be reconstructed affects the decision<br/>- binary method contains an unsupported call, monitor, switch, or dynamic instruction affects the decision<br/>- a possible side effect on the returned decision cannot be reconstructed affects the decision<br/>- a possible side effect on the returned decision cannot be reconstructed affects the decision"]
```

Result: **incomplete**, 15 nodes, 19 edges, 5 explicit gaps.

This is a conformance success. The graph tells the reader which paths are proved and which paths need more source or supported bytecode semantics.

## Reproduce the result

Clone the pinned PetClinic repository to `/tmp/fachtracing-spring-petclinic`, or set `SPRING_PETCLINIC_DIR` to its location. Then run:

```sh
./scripts/verify-spring-petclinic.sh
```

The command checks the clean revision, applies only `annotation-overlay.patch`, analyzes all 30 main Java files, compares immutable semantic oracles, and writes disposable output under `target/generated`.
