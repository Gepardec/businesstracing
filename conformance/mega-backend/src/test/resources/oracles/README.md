# Reviewed semantic graph oracles

These normalized topologies are immutable verification inputs for the pinned
`Gepardec/mega-backend` commit `782cdec8dfe5b4062eb5c1859e6a9e53afe02770`.
They are never supplied to static analysis and the conformance runner has no update mode.

## Independent derivation method

The engineering review was performed from the pinned Java source, separately from analyzer
execution. For each annotated method, the reviewer walked every result-affecting statement,
expanded directly reachable source methods, enumerated true/false, switch, loop, early-return,
throw, and dispatch paths, then compared that source-derived inventory with the normalized file.
Opaque identifiers and source positions are deliberately absent. A node or edge not justified by
the source, or a missing source-derived node or edge, rejects the oracle.

The five reviewed decisions cover four business areas and include the real three-strategy
journey-warning manager. Review anchors and results:

| Oracle | Direct source-derived checks | SHA-256 | Outcome |
| --- | --- | --- | --- |
| `authorize-clarification-resolution.txt` | atomic system-created and ordinary clarification branches; open, lead, involved, subject, and creator conditions; expanded predicate returns; both returns converge on Stop | `530467312f889f544526d2bd1ad8aad06db190551d30de074827cb09edc98319` | Approved |
| `detect-overlapping-time-entries.txt` | atomic touching-boundary checks return false; otherwise each interval position is tested and both paths state their return | `cccbb57b3ac143b86565c50ffeefb536d17ed6d2671feb7cf1ff2d553ee54198` | Approved |
| `determine-project-activity-in-month.txt` | month boundary derivations; atomic start-before-end and absent-or-after-start conditions; conjunction return | `0d4a30c9cc47e99913852f9546c7e3bc849b54c5b866490fda2cbfc3b1f11e38` | Approved |
| `validate-journey-direction.txt` | reachable absent warning initializer; all enum cases; atomic state and completion predicates; state updates; expanded helper returns; default failure; early return; final return; shared Stop | `ae47cca416a4f3b3d2cd67d2616d87cd32af00aff5933bf61d6cd987a981045a` | Approved |
| `determine-journey-warnings.txt` | manager collection flow; all three selected strategy rules; reachable warning and working-location initializers; business-safe entry iteration and aggregate task check; aliased helper and JDK collection mutations; atomic filtering, direction, absence, and warning predicates; expanded return evidence; convergence and final list | `ca63251bdfb18b5ef1f0bb368ebbf9acd015e850acf2eb330ef5da0b135337f6` | Approved |

The hashes make review drift visible. Exact semantic equality is still the authoritative
executable assertion; hashes document the reviewed revisions rather than replacing comparison.
