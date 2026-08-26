# Tasks: Architecture Dojo Anspruch conformance

## Task 1: Add the pinned conformance adapter

**Status:** Completed

1. [x] Add the source-generated Java conformance harness.
2. [x] Add the pinned checkout and build script.
3. [x] Document the selected branch, entry points, command, and outputs.

## Task 2: Generate and verify the graphs

**Status:** Completed

1. [x] Run the pinned external project tests.
2. [x] Generate both business graph JSON files and the shared schema.
3. [x] Validate the JSON documents and inspect their business artifact guard result.
4. [x] Verify viewer contract compatibility.
5. [x] Run the full pull-request gate.

## Completion rule

Both tasks and all six acceptance criteria must pass before the specification is completed.

## Version 2: Regenerate from the newer solution branch

**Status:** Completed

1. [x] Pin `feature/solution1` and update its application entry points.
2. [x] Regenerate and validate both graphs.
3. [x] Run the viewer and repository gates.
