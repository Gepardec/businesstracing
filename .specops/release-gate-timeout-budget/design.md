# Design: Release Gate Timeout Budget

## Approach

Change `release-gate.timeout-minutes` from 60 to 90. Change
`MINIMUM_TIMEOUT_MINUTES` in the focused shell contract from 50 to 90.

Use a temporary named pipe in `capture-gate-output.sh`. Run `tee` as the pipe reader, run the release
producer as the writer, wait for both processes, and return the producer status first. This streams
one byte sequence to the terminal and evidence file without relying on non-POSIX `PIPESTATUS`.

The 90-minute value is evidence-based. The clean release gate needed about 45 minutes before the
Spring PetClinic work. Current runs now reach 60 minutes without a reported test failure. Ninety
minutes gives the additional corpus and cold-run variance 30 minutes beyond the observed limit.

## Boundaries

- Keep `pr-gate.timeout-minutes` at 10.
- Keep PostgreSQL at 15 minutes.
- Keep `verify-release.sh` and all sub-gates unchanged.
- Keep event routing, concurrency, permissions, runner images, and actions unchanged.
- Add no dependency.
- Preserve exact producer failures and report a `tee` failure when the producer succeeds.

## Failure Behavior

The workflow remains fail-closed. GitHub cancels it if it exceeds 90 minutes. Local and PR
verification fail if a later workflow edit lowers the release limit below 90 minutes.

## Verification

1. Run the focused contract against the old 60-minute workflow after raising its minimum; expect a
   failure.
2. Set the workflow to 90 and rerun the focused and event-routing contracts; expect success.
3. Run the complete pull-request gate.
4. Push a focused PR, require hosted PR and PostgreSQL success, merge it, and monitor the final
   `main` release job to completion.
5. If the job does not complete, use its live log to identify the exact blocking sub-gate.

## Dependency Decisions

No new dependency is required.
