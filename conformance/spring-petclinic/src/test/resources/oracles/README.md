# Reviewed semantic graph oracles

These normalized topologies are immutable verification inputs for `spring-projects/spring-petclinic` commit `88e37c15cf6fc8490b01bc3e8e2c800cec1ac272`. They are not analyzer input, and the conformance runner cannot update them.

## Independent review method

The reviewer inspected each pinned Java method and its source-visible calls. The review listed every result-relevant predicate, loop route, early result, final result, and unavailable call effect. The normalized graph was then checked against that source-derived list. Opaque graph identifiers and source positions are excluded.

| Oracle | Source-derived checks | SHA-256 | Outcome |
| --- | --- | --- | --- |
| `determine-whether-an-entity-is-new.txt` | one absence predicate; true and false results converge on Stop | `72c426152fd2e4aea025d4758da7d85faf3b7a1ac47824ba9f56d83df247b5b0` | Approved complete graph |
| `find-an-eligible-pet-by-name.txt` | pet iteration; name existence and case-insensitive match; new-pet eligibility; found and absent results | `ccb613cd380454d5aaf939c210adab10d40e89fdbda312fe985a436bc13bed9d` | Approved complete graph |
| `register-a-new-pet.txt` | visible validation result and terminal views; five unavailable result-relevant effects remain explicit | `888019a73253cb98abad87b79867da763bc833abe002e386fb6dad572c8e9a85` | Approved incomplete graph |

Exact semantic equality is the executable assertion. The hashes make review drift visible.
