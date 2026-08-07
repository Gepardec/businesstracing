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
| `authorize-clarification-resolution.txt` | atomic system-created and ordinary clarification branches; open, lead, involved, subject, and creator conditions; both returns converge on Stop | `192cd18116ed3522a7bd4ab07dc6cd4000cfbc4b42afaf830e4ed620ca3f3848` | Approved |
| `detect-overlapping-time-entries.txt` | atomic touching-boundary checks return false; otherwise each interval position is tested and both paths state their return | `cccbb57b3ac143b86565c50ffeefb536d17ed6d2671feb7cf1ff2d553ee54198` | Approved |
| `determine-project-activity-in-month.txt` | month boundary derivations; atomic start-before-end and absent-or-after-start conditions; conjunction return | `0d4a30c9cc47e99913852f9546c7e3bc849b54c5b866490fda2cbfc3b1f11e38` | Approved |
| `validate-journey-direction.txt` | all enum cases, atomic state and completion predicates, state updates, default failure, early return, final return, shared Stop | `1684955f4aa81930040a8d9df919be77df0f953f1fd4a419d70b6621a9f6c36e` | Approved |
| `determine-journey-warnings.txt` | manager collection flow; all three selected strategy rules; business-safe entry iteration; aliased helper and JDK collection mutations; atomic filtering, direction, absence, and warning predicates; convergence and final list | `368391bfdb748f2d453d78439386083b83b2be4363201915589963aa878ede0a` | Approved |

The hashes make review drift visible. Exact semantic equality is still the authoritative
executable assertion; hashes document the reviewed revisions rather than replacing comparison.
