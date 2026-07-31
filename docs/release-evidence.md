# Release evidence

Release candidate `0.1.0-rc.1` uses `scripts/verify-release.sh` as its mandatory release gate. The
gate refuses a dirty source tree, creates a non-local clone, uses an empty Maven local repository,
and runs repository integrity, Java capability, generic engine, agent, Maven, JDBC, isolated external
release, and pinned Mega tests.

It then runs the persistence-enabled load harness for a 60-second disabled baseline and 600 seconds
at 1,000 decisions per second. The enabled run sends each accepted completed record through bounded
asynchronous delivery to a fault-injecting repository. The repository creates repeatable transient
fault windows. The gate requires zero trace-caused errors, result mismatches, contamination, dropped
captures, or accepted-but-unsaved records, and less than 10% p95 overhead.

The same analyzer artifacts verify the non-Mega eligibility, pricing, strategy, records, streams,
and external-approval domains plus the pinned Mega brownfield oracles. The generic-source guard scans
production implementation and configuration for Mega-specific hints. The command writes exact
output, commit IDs, Java and Maven versions, and the capability matrix SHA-256 to
`target/release-evidence.txt`.

Run:

```sh
./scripts/verify-release.sh
```

The specification can be marked complete only after this command prints `RELEASE_GATE_OK` and its
evidence file contains successful generic, external, Mega, and long-load results.
