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

The static overview uses the selected method as its semantic boundary. It keeps every
caller-visible rule, action, failure, and result path. A source-unavailable Boolean decision is one
atomic business rule. A source-unavailable statement call is one atomic business action. A
collaborator lookup is not a business step. These rules come from attributed Java use-sites and do
not contain a Keycloak selector or method list.

The generated static overview has zero coverage gaps. It contains no runtime observation and does
not select one endpoint call. It also does not expand the internal control flow of dependency
methods. The business projection converts collection mechanics and implementation type names to
plain actions before it renders Mermaid. The exact developer record retains source mappings and
technical evidence outside the business file.
