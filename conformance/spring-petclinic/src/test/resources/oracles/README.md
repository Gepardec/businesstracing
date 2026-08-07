# Reviewed semantic graph oracles

These normalized topologies are immutable verification inputs for `spring-projects/spring-petclinic` commit `88e37c15cf6fc8490b01bc3e8e2c800cec1ac272`. They are not analyzer input, and the conformance runner cannot update them.

## Independent review method

The reviewer inspected each pinned Java method and its source-visible calls. The review listed every result-relevant predicate, loop route, early result, final result, and unavailable call effect. The normalized graph was then checked against that source-derived list. Opaque graph identifiers and source positions are excluded.

| Oracle | Source-derived checks | SHA-256 | Outcome |
| --- | --- | --- | --- |
| `determine-whether-an-entity-is-new.txt` | one absence predicate; true and false results converge on Stop | `72c426152fd2e4aea025d4758da7d85faf3b7a1ac47824ba9f56d83df247b5b0` | Approved complete graph |
| `find-an-eligible-pet-by-name.txt` | pet iteration; name existence and case-insensitive match; new-pet eligibility; found and absent results | `ccb613cd380454d5aaf939c210adab10d40e89fdbda312fe985a436bc13bed9d` | Approved complete graph |
| `register-a-new-pet.txt` | visible validation result; duplicate-name check; escaping non-duplicate failure; terminal views; five unavailable result-relevant effects remain explicit | `27db9b109a2e479ec26d76e843dab9b4d3339cd786664ac3fe7b046e4f1eac02` | Approved incomplete graph |

Exact semantic equality is the executable assertion. The hashes make review drift visible.
