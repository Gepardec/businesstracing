# Design: Bounded CI Feedback

## Approach

Keep `pr-gate` as the core verification job. Keep Mega Backend, Spring PetClinic, Jakarta EE,
viewer verification, and PostgreSQL integration as five additional independent jobs. Run all six
jobs for pull requests, pushes to `main`, version tags, schedules, and manual dispatches.

Set `timeout-minutes: 3` on the five independent jobs. Set `timeout-minutes: 5` on PostgreSQL. Each
macOS job uses the Maven cache. Each conformance job
uses only its immutable source cache, builds the Fachtracing artifacts, and runs one pinned corpus.
The standalone viewer gate uses only Node. The PostgreSQL job keeps storage, generated dogfood, and
the browser integration journey. The workflow does not call `verify-release.sh`. That script
remains an optional manual command for long evidence.

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

Each job fails closed at its configured limit. Other parallel jobs can finish and show which
independent check failed. The workflow fails if any required job fails or reaches its limit.

## Verification

1. Update the budget contract first and prove that the 10-, 15-, and 90-minute workflow fails.
2. Split the jobs, set the five independent limits to three minutes and PostgreSQL to five minutes,
   and run the focused contracts.
3. Run core, Mega, PetClinic, Jakarta EE, viewer, dogfood, and PostgreSQL-related contracts.
4. Push the PR and confirm that all hosted jobs pass within their configured limits.
5. Merge the PR and confirm the same result on `main`.

The PostgreSQL job keeps all 17 browser tests. The larger limit does not reduce product coverage.
It accounts for database startup, dogfood generation, viewer installation, viewer build, and the
browser journey in one integration boundary.

## Dependency Decisions

No dependency is added, removed, or updated.
