# Implementation: CI Isolated Maven Repository

## Summary

All four tasks are complete. Hosted monitoring found a second CI limit: GitHub canceled the valid
release command at the old 35-minute job timeout. Task 4 gives the unchanged release checks a
60-minute bounded time budget.

## Phase 1 Context Summary

- Config: SpecOps defaults; vertical `infrastructure`; specs directory `.specops`; no task tracker.
- Context recovery: none; all prior specifications are complete.
- Conversion source: accepted inline plan from the PR #5 CI investigation.
- Steering directory: verified.
- Memory directory: verified.
- Vertical: infrastructure for a Java library release workflow.
- Affected files: Maven repository resolver and standard, Mega, PostgreSQL, and release scripts.
- Project state: brownfield.

## Decision Log

- Use one POSIX resolver and preserve the existing release environment variable.
- Give the explicit verification override precedence over the release override.
- Normalize repeated separators because macOS `TMPDIR` ends with `/`.

## Verification

- Focused resolver contract: PASS.
- Standard verification with a fresh isolated Maven repository: PASS.
- Short load gate: 5,000 decisions, 0.298% p95 overhead, zero errors, mismatches, drops, or
  contamination.
- Source-free external activation: PASS.
- Clean release commit: `defe774040f5a5604caffc15838519fd753c0db2`.
- Five Mega Backend graphs: COMPLETE.
- Long load gate: 600,000 decisions at 1,000 RPS, 0.059% p95 overhead, zero errors, mismatches,
  drops, or contamination.
- PostgreSQL: not rerun locally because no connection is configured; the separate PR job remains
  unchanged.

## Hosted Follow-up

- PR run `31086887346`: PostgreSQL passed.
- PR run `31086887346`: GitHub canceled `release-gate` at its exact 35-minute workflow limit.
- The release command reported no test failure before cancellation.
- The corrected job limit is 60 minutes, with a focused contract that requires at least 50.
- Repository integrity, the focused contract, and standard verification pass.
- The standard load check completed 5,000 decisions with 0.176% p95 overhead and zero errors,
  mismatches, drops, or contamination.
- The corrected PR check is monitored after the push.
