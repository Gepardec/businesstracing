# Design: Integrate Current Main into PR #15

## Approach

Use a normal merge from `origin/main`. For each conflict, compare the merge base, branch version,
and main version. Resolve by semantic union. Do not select one whole side when both sides add valid
behavior.

## Conflict Rules

- Analyzer imports and helper methods: retain both independent additions.
- Analyzer tests and capability docs: retain every executable contract from both branches.
- SpecOps indexes and memory: retain every spec entry, decision, overlap, and context entry.
- Generated repo map: refresh after the final file set is known.

## Verification

Run `./scripts/verify-pr.sh`. Then push the merge commit and wait for `pr-gate` and `postgres`.

## Dependencies

No dependency or build descriptor changes are required.
