# Keycloak conformance selection

Pinned revision: `eba869ee597b933efc8fa2c84713db9e6c0983cf`.

| HTTP endpoint | Configured Java method | Business label |
| --- | --- | --- |
| `GET /admin/realms/{realm}/users` | `org.keycloak.services.resources.admin.UsersResource.getUsers` | `search users` |

The owner and method name identify one method at this revision, so the example omits parameter
types. If Keycloak adds an overload, Fachtracing stops and asks for the complete erased parameter
type list. The selection is in the conformance harness only. Production engine, agent, and Maven
plugin classes contain no Keycloak name.

The selected method returns a lazy stream. The runtime trace ends when the method returns, before
the REST layer consumes that stream. Thus, a source-unavailable lazy callback appears as a
configured filter or mapping action. The trace does not claim that this callback ran before the
endpoint returned. It also does not claim to explain later response serialization.

The static overview has three gap regions. They cover unavailable permission state, session state,
and user-search data. The source-visible query rules, prefix rule, date actions, permission feature
rule, and lazy callback actions do not create duplicate gaps. The evaluated example has one gap on
its selected path. Live calls can select two separate permission-boundary gaps. The exact developer
record keeps the source locations and causes. The business files do not expose those technical
diagnostics.
