# Architecture Dojo Anspruch conformance

This harness analyzes the unchanged Onion solution in
`Gepardec/ArchitectureDojoAnspruch`. Repository `main` has no `dojo-leistung` application source,
so the harness pins `feature/onion` at commit
`5767ba85bffd82520d7ee7f72c281a9395d1b7ee`.

The harness selects two application boundaries:

- benefit-entitlement checking;
- incapacity-notification submission.

These selections identify source methods only. The generic analyzer derives all nodes, edges,
branches, results, and coverage state. The harness does not contain a diagram or graph topology.

Prepare a clean checkout:

```sh
git clone https://github.com/Gepardec/ArchitectureDojoAnspruch.git \
  /tmp/fachtracing-architecture-dojo-anspruch
```

Run the explicit conformance command:

```sh
./scripts/verify-architecture-dojo-anspruch.sh
```

Set `ARCHITECTURE_DOJO_ANSPRUCH_DIR` when the checkout is elsewhere. The checkout can be on any
branch, but it must be clean and contain the pinned commit. The script creates a detached temporary
worktree and does not change the source repository.

Disposable output is under `conformance/architecture-dojo-anspruch/target/generated`:

- `check-benefit-entitlement-business.json`, `.mmd`, and `.puml`;
- `submit-incapacity-notification-business.json`, `.mmd`, and `.puml`;
- `fachtracing-business-graph-v1.schema.json`.

The JSON documents use the V1 business graph contract and are ready for the Fachtracing viewer.
