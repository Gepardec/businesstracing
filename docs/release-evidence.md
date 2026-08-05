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

Pull requests run the same clean-clone release gate on Java 21. A separate PostgreSQL 18.4 service
runs `scripts/verify-postgres.sh`. The workflow has read-only repository permissions and no
application database or Mega credentials. It also supports a manual dispatch.

## RC 1 corrected result

- Release gate commit: `6b52e86ab25d06494b176057bfaa486ddf754cb2`
- Java: OpenJDK 21.0.2
- Maven: 3.9.16
- Mega revision: `782cdec8dfe5b4062eb5c1859e6a9e53afe02770`
- Capability matrix SHA-256: `406ecc89f2ffbcbd9ef924fa86f53acc86ee6ceba379f7622b72f910acd96fc4`
- Long run: 60-second baseline and 600 seconds enabled at 1,000 RPS
- Result: 600,000 completed; 0 errors; 0 mismatches; 0 drops; 0 contamination
- Latency: 12,530.000 µs baseline p95; 12,538.875 µs enabled p95; 0.071% overhead
- Gate result: `RELEASE_GATE_OK`

## RC 1 activation V2 result

- Release gate commit: `d63d37de8dcc6f794569ee6be1b30917f6a709aa`
- Java: OpenJDK 21.0.2
- Maven: 3.9.16
- Mega revision: `782cdec8dfe5b4062eb5c1859e6a9e53afe02770`
- Capability matrix SHA-256: `122bf93232cb423577cc2c0ade2d3bc640a230df5a58d3f6ee0af08bf7069593`
- Runtime proof: activation V2 loaded without Java source or a runtime compiler
- Mega result: five complete graphs from 420 source files
- Long run: 60-second baseline and 600 seconds enabled at 1,000 RPS
- Result: 600,000 completed; 0 errors; 0 mismatches; 0 drops; 0 contamination
- Latency: 14,288.459 µs baseline p95; 14,028.834 µs enabled p95; -1.817% overhead
- Gate result: `RELEASE_GATE_OK`

## RC 1 remediation V4 result

- Release gate commit: `facd1daf052f4e3ffae42c48a876dc46e4dd9576`
- Java: OpenJDK 21.0.2
- Maven: 3.9.16
- Mega revision: `782cdec8dfe5b4062eb5c1859e6a9e53afe02770`
- Capability matrix SHA-256: `122bf93232cb423577cc2c0ade2d3bc640a230df5a58d3f6ee0af08bf7069593`
- Runtime proof: activation V3 separated annotated overloads and overloaded lambda targets
- Mega result: five complete graphs from 420 source files
- Long run: 60-second baseline and 600 seconds enabled at 1,000 RPS
- Result: 600,000 completed; 0 errors; 0 mismatches; 0 drops; 0 contamination
- Latency: 15,023.625 µs baseline p95; 15,035.291 µs enabled p95; 0.078% overhead
- Gate result: `RELEASE_GATE_OK`
