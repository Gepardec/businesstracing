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
the REST layer consumes that stream. Thus, the trace explains query selection, filters, permission
checks, and other work performed inside `getUsers`. It does not claim to explain later stream
consumption or response serialization. The safe final result is `Result not recorded`, and runtime
coverage is incomplete for that result type.
