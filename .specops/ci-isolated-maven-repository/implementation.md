# Implementation: CI Isolated Maven Repository

## Summary

All three tasks are complete. The verified commits are ready to push to PR #5.

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
