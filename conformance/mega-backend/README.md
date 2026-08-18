# Mega Backend conformance

This harness treats `Gepardec/mega-backend` as an opaque brownfield reference. It verifies the
same generic Fachtracing artifacts used by synthetic fixtures; no reference name, package,
method, or domain vocabulary is allowed in production source or generic configuration.

Run `scripts/verify-mega-backend.sh`. Set `MEGA_BACKEND_DIR` to an existing checkout if desired.
The checkout must be clean and exactly at commit `782cdec8dfe5b4062eb5c1859e6a9e53afe02770`.
The script creates a temporary worktree, compiles the pinned application without source changes,
selects five exact Java methods from harness configuration, compares the immutable reviewed semantic oracles, and
writes disposable PlantUML, Mermaid, comparison, and execution output under
`conformance/mega-backend/target/generated`. Maven's `target/` ignore rule keeps this reproducible
output out of Git.

The same entry-point contract can select public or private methods. The harness supplies the exact
owner, method, erased parameter types, and business label. Mega does not depend on Fachtracing and
does not receive test annotations.

The harness also enforces the business projection contract: one shared Start and Stop, convergence
of every terminal path, explicit return statements, and no standalone identifier or Java null
vocabulary in business-facing output.

The oracle review method, source-derived checks, approval outcomes, and immutable hashes are in
`src/test/resources/oracles/README.md`; the complete evidence summary is in `conformance-report.md`.
