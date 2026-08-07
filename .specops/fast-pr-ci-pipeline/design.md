# Design: Fast Pull-Request CI Pipeline

## Architecture

The workflow has three jobs:

- `pr-gate`: pull-request only, macOS, normal Maven cache, cached pinned Mega checkout;
- `release-gate`: non-pull-request only, macOS, existing clean release command;
- `postgres`: all supported events, Ubuntu with PostgreSQL 18.4.

Workflow-level concurrency groups runs by workflow and Git ref. Cancellation is enabled only for
pull requests, so an obsolete commit stops but a `main`, tag, scheduled, or manual release proof is
not canceled by another run.

## Fast Gate

`verify-pr.sh` runs `verify.sh` and then Mega conformance. `verify.sh` already includes repository
integrity, Java capability, all engine/agent/plugin/storage contracts, external activation, and the
short five-second baseline plus five-second enabled 1,000-RPS check.

Mega conformance gains an opt-in `FACHTRACING_SKIP_PROJECT_BUILD=true` input. Its default remains to
build Fachtracing, so direct local use is unchanged. The fast wrapper sets the input because
`verify.sh` has already installed the reactor.

## Cache Boundaries

- `actions/setup-java` restores and saves `~/.m2/repository` for `pr-gate`.
- `actions/cache` stores only the clean pinned Mega checkout under an immutable key that includes
  its exact commit.
- Generated graphs and Fachtracing build outputs are not cached.
- `release-gate` continues to create its own empty Maven repository and does not consume the fast
  cache as release evidence.

## Event Routing

- Pull request: `pr-gate` plus `postgres`.
- Push to `main`: `release-gate` plus `postgres`.
- Push of `v*` tag: `release-gate` plus `postgres`.
- Nightly schedule and manual dispatch: `release-gate` plus `postgres`.

## Verification

A POSIX workflow contract checks event routing, cache configuration, cancellation, script selection,
the retained 60-minute release budget, and the unchanged long-load command. Standard verification
executes this contract. Then the fast wrapper and standalone Mega gate run locally.

## Security and Dependencies

The workflow handles public repository source and internal test artifacts. Permissions remain
`contents: read`. No secret, credential, production data, or new runtime dependency is introduced.
Only official GitHub actions already accepted by the project are used.
