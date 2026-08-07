# Design: Spring PetClinic Conformance

## Architecture Overview

The suite mirrors the established external-corpus boundary. It pins a clean canonical PetClinic checkout, creates a disposable worktree, builds the unmodified application, applies an annotation-only overlay, analyzes all main Java sources, and compares normalized graph topology with tracked reviewed oracles. A report uses those same graph results as a product explanation.

## Technical Decisions

### Decision 1: Use Three Increasing Levels of Detail

**Decision:** Annotate one simple persistence-state method, one domain collection lookup, and one Spring MVC creation workflow.

**Rationale:** The sequence shows the product clearly. The first graph explains predicate extraction. The second explains loops, compound conditions, and terminal convergence. The third explains honest coverage gaps at unavailable framework and persistence boundaries.

### Decision 2: Keep PetClinic Knowledge in the Harness

**Decision:** Store the revision, source paths, annotations, expected labels, and oracles only under `conformance/spring-petclinic` and its verification script.

**Rationale:** The conformance corpus must test the generic analyzer. It must not shape production behavior.

### Decision 3: Compare Normalized Semantics

**Decision:** Compare node kinds, business labels, edges, outcomes, completeness, and gaps without opaque node identifiers.

**Rationale:** The oracle records reviewed business topology. It does not couple the test to implementation-only identifiers or source positions.

### Decision 4: Run the Suite in Pull-Request and Release Gates

**Decision:** Cache a clean pinned checkout in GitHub Actions and pass its path to the same local verification script.

**Rationale:** The suite is small enough for the fast gate and must detect analyzer drift before merge. The release gate reuses the same contract in a clean clone.

## Component Design

### Annotation Overlay

**Responsibility:** Add only the public annotation import and one business label to each selected method.
**Interface:** `conformance/spring-petclinic/annotation-overlay.patch`.

### Conformance Test

**Responsibility:** Analyze the corpus, enforce expected decisions and completeness, compare oracles, guard business output, and write disposable renderings.
**Interface:** A plain-Java executable test under `conformance/spring-petclinic/src/test/java`.

### Verification Script

**Responsibility:** Validate the clean pin, create a disposable worktree, build PetClinic, apply the overlay, compile the test, and execute it.
**Interface:** `./scripts/verify-spring-petclinic.sh`.

### Report

**Responsibility:** Explain the three graphs and their confidence boundary in plain language.
**Interface:** `conformance/spring-petclinic/conformance-report.md`.

## Data Flow

1. The script checks the external repository and exact commit.
2. The script builds the unchanged PetClinic source and dependency classpath.
3. The script applies the annotation-only overlay to a disposable worktree.
4. The analyzer discovers exactly three annotated decisions.
5. The test renders graphs, normalizes semantics, and compares tracked oracles.
6. The report explains the reviewed results.

## Testing Strategy

- Run the PetClinic conformance script against the pinned local checkout.
- Verify exact decision labels, graph counts, completeness states, guard results, and semantic equality.
- Verify repository integrity, immutable hashes, ignored generated output, CI setup, and fast-gate wiring.
- Run the full pull-request verification before commit.

## Risks & Mitigations

- **Risk:** Upstream source moves. **Mitigation:** Use an exact commit and a clean detached worktree.
- **Risk:** A framework call is presented as understood application logic. **Mitigation:** Require the application workflow to stay incomplete with explicit coverage gaps.
- **Risk:** A report becomes stale. **Mitigation:** Copy only reviewed normalized graph semantics and protect their hashes.
- **Risk:** CI time increases. **Mitigation:** Cache the pinned checkout and reuse the already built Fachtracing reactor in the fast gate.

### Dependency Decisions

No project dependency is introduced. The external pinned PetClinic dependency set is built only as conformance input and is not packaged or exposed by Fachtracing.

## Future Enhancements

- Add a runtime PetClinic request only after a separate spec defines Spring activation and stable test data.
