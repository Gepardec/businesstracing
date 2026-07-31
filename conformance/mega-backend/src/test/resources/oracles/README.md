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
| `authorize-clarification-resolution.txt` | system-created and ordinary clarification branches; open, lead, involved, subject, and creator conditions; both returns converge on Stop | `18a7047cba3dd024c4058cd2f20411705c3e01b63502e04c5cc6a99c661cc384` | Approved |
| `detect-overlapping-time-entries.txt` | touching boundaries return false; otherwise neither interval lies wholly before/after the other; both paths state their return | `04da12e820de5c3e640af192734261ddbb49f385ef99a573149331068e607ace` | Approved |
| `determine-project-activity-in-month.txt` | month boundary derivations; start-before-end and absent-or-after-start conditions; conjunction return | `53dba8048f35e02c15f6a67b03d0a7e7fb93dc08e980762af01899c262d70993` | Approved |
| `validate-journey-direction.txt` | all enum cases, state updates, default failure, unfinished-journey early return, final return, shared Stop | `56ef5ac56f8a462569d00cbfd81157407e51c71e7c071866cb45be92a02c82ae` | Approved |
| `determine-journey-warnings.txt` | manager collection flow; all three strategy candidates; filtering, indexed and enhanced loops, ternary, validator switch, absence checks, terminal validator failure, warning additions, convergence, final list | `31c59b66a9a7887add4f88c850837cbdbfabdc6e9cfc90561e07103d80398205` | Approved |

The hashes make review drift visible. Exact semantic equality is still the authoritative
executable assertion; hashes document the reviewed revisions rather than replacing comparison.
