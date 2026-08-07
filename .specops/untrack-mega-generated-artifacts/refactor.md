# Refactor: Untrack Mega Generated Artifacts

## Rationale

The Mega conformance test commits 18 files that it can reproduce. These files include diagrams,
normalized comparison output, and one execution explanation. They create review noise and can
become stale. The immutable semantic oracles are the reviewed test inputs and must stay in Git.

## Required Behavior

1. THE SYSTEM SHALL write reproducible Mega output under
   `conformance/mega-backend/target/generated`.
2. THE REPOSITORY SHALL NOT track files under `conformance/mega-backend/generated`.
3. THE SYSTEM SHALL keep the five reviewed semantic oracles tracked and compare them exactly.
4. THE REPOSITORY integrity check SHALL reject a later commit of the old generated-output path.
5. THE Mega conformance test SHALL still produce five complete business graphs.

## Unchanged Behavior

- The pinned Mega source and annotation overlay do not change.
- The analyzer and runtime implementation do not change.
- The five semantic oracle files and their hashes do not change.
- PlantUML, Mermaid, semantic comparison, and execution artifacts remain available after a run.

## Acceptance Criteria

- [x] No file under `conformance/mega-backend/generated` is tracked.
- [x] The Mega script writes output under `conformance/mega-backend/target/generated`.
- [x] The output path is ignored by the existing Maven `target/` rule.
- [x] All five reviewed oracle files remain tracked with unchanged hashes.
- [x] Repository integrity rejects tracked files in the former output path.
- [x] Repository integrity and Mega conformance pass.

## Scope Assessment

This is one small test-infrastructure refactor. It changes the output location, repository guard,
and related documentation. It does not change product logic.
