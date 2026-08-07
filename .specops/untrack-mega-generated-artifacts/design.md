# Design: Untrack Mega Generated Artifacts

## Artifact Boundary

The conformance harness has two artifact classes:

- reviewed inputs: five semantic oracles under `src/test/resources/oracles`, tracked in Git;
- reproducible outputs: diagrams, normalized semantic text, and execution explanations, written
  under the module `target/generated` directory and ignored by Git.

The Java conformance program already accepts the output directory as an argument. The shell entry
point changes that argument only. No Java implementation change is necessary.

## Regression Guard

The repository integrity script checks that Git has no tracked file under the old generated path.
This check prevents review noise from returning. Existing tracked-file and SHA-256 checks continue
to protect the reviewed oracles.

## Verification

Run repository integrity first. Then run Mega conformance and confirm that it produces five
complete graphs under the ignored target path. Standard pull-request verification covers the same
contracts in CI.

## Dependencies and Security

This refactor adds no dependency and changes no data boundary. The output contains test evidence
only and stays in the local or CI workspace.
