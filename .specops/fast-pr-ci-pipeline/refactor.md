# Refactor: Fast Pull-Request CI Pipeline

## Rationale

Pull requests currently run the complete release gate. That gate contains a 60-second baseline and
a 600-second enabled load test, so it cannot finish within the desired two-to-three-minute feedback
window. It also creates an empty Maven repository and clones the project again, which intentionally
bypasses the Maven cache.

The full gate is valuable release evidence and must remain unchanged. The workflow must separate
fast change feedback from the clean release proof.

## Current Behavior

- Each pull request runs `verify-release.sh` on macOS.
- The script creates a clean clone and an empty Maven repository.
- Standard, external, Mega Backend, and 600-second load checks run in one job.
- PostgreSQL runs separately and usually completes in less than one minute.
- A superseded pull-request run continues after a newer commit is pushed.

## Required Behavior

1. WHEN a pull request is opened or updated, THE SYSTEM SHALL run standard verification, external
   activation, pinned Mega Backend conformance, the short 1,000-RPS check, and PostgreSQL.
2. WHILE the pull-request fast gate runs, THE SYSTEM SHALL use the Maven cache and an immutable
   cache for the pinned Mega Backend checkout.
3. WHEN a newer commit updates the same pull request, THE SYSTEM SHALL cancel its older workflow run.
4. WHEN `main` or a release tag is pushed, or a scheduled or manual run starts, THE SYSTEM SHALL run
   the existing clean-clone, empty-Maven-repository, 600-second release gate.
5. THE SYSTEM SHALL NOT run the 600-second load gate for a normal pull-request event.
6. THE SYSTEM SHALL avoid rebuilding Fachtracing inside Mega conformance after the fast gate has
   already installed the same reactor artifacts.

## Unchanged Behavior

- The full release gate still proves 600,000 decisions at 1,000 RPS.
- The release gate still uses a clean clone and empty Maven repository.
- Pull requests still run all functional, external activation, Mega, short performance, and
  PostgreSQL contracts.
- The workflow keeps read-only repository permission and official GitHub actions.
- Mega remains a pinned conformance corpus and does not enter production behavior.

## Acceptance Criteria

- [x] A workflow contract fails against the old pull-request release layout.
- [x] Pull requests select only the cached fast gate and PostgreSQL job.
- [x] Main, release tags, schedule, and manual events select the full release gate.
- [x] Superseded pull-request runs use `cancel-in-progress`.
- [x] Fast verification uses Maven and pinned Mega checkout caches.
- [x] Mega conformance can reuse a completed root build without changing its default standalone use.
- [x] Standard verification and pinned Mega conformance pass.
- [x] Workflow documentation explains fast versus full evidence.

## Scope Assessment

This is one infrastructure refactor. Workflow routing, the fast wrapper, and workflow contracts form
one coupled delivery. No decomposition is needed.
