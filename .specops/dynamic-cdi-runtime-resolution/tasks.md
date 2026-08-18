# Tasks: Dynamic CDI runtime resolution

## Task 1: Retain unresolved dispatch candidates

**Status:** Completed

- Change unresolved framework selection to emit compatible dispatch candidates.
- Preserve one visible static coverage gap.
- Add focused analyzer assertions.

## Task 2: Add real CDI runtime conformance

**Status:** Completed

- Add test-scoped Weld SE.
- Generate a fingerprinted activation bundle from the conformance source.
- Run qualified dynamic selection with the Java agent.
- Assert exact selected paths and no unresolved runtime target.

## Task 3: Verify and publish

**Status:** Completed

- Run focused tests and the full pull-request gate.
- Update documentation and SpecOps records.
- Commit, push, open the stacked pull request, and verify CI.
