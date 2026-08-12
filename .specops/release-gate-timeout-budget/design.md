# Design: Three-Minute CI Budget

## Approach

Keep `pr-gate` as the core verification job. Move Mega Backend and Spring PetClinic conformance to
two independent jobs. Keep PostgreSQL as a fourth independent job. Run all four jobs for pull
requests, pushes to `main`, version tags, schedules, and manual dispatches.

Set `timeout-minutes: 3` on all four jobs. Each macOS job uses the Maven cache. Each conformance job
uses only its immutable source cache, builds the Fachtracing artifacts, and runs one pinned corpus.
The workflow does not call `verify-release.sh`. That script remains an optional manual command for
long evidence.

Change the budget contract from a minimum release timeout to a maximum timeout for every required
job. Change the workflow contract to reject serial corpus work in `pr-gate`, a `release-gate` job,
or a hosted call to `verify-release.sh`.

## Boundaries

- Measure job execution after GitHub assigns a runner. GitHub queue time is outside repository
  control.
- Keep read-only workflow permissions and current concurrency behavior.
- Keep the test content. Only the ten-minute load proof moves off the required path.
- Add no dependency.
- Keep the optional clean-clone release command and historical evidence.

## Failure Behavior

Each job fails closed after three minutes. Other parallel jobs can finish and show which independent
check failed. The workflow fails if any required job fails or reaches its limit.

## Verification

1. Update the budget contract first and prove that the 10-, 15-, and 90-minute workflow fails.
2. Split the jobs, set each limit to three minutes, and run the focused contracts.
3. Run core, Mega, and PetClinic verification locally.
4. Push the PR and confirm that all hosted jobs pass within three minutes.
5. Merge the PR and confirm the same result on `main`.

## Dependency Decisions

No dependency is added, removed, or updated.
