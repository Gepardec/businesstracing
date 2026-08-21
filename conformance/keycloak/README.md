# Keycloak endpoint business tracing

This example traces the pinned Keycloak user-search endpoint without changing Keycloak source. It
generates a non-technical Mermaid graph and a runtime activation file for `search users`.

The harness creates the Mermaid graph from the generic static business projection of the endpoint
analysis. It checks required rule and action anchors in both the exact and projected graphs. These
anchors are assertions only; they do not supply nodes or edges to the diagram. The static graph has
all method paths and does not use a runtime call. The runtime activation keeps the exact graph for
optional later path recording.

## Generate the graph and activation

Prepare a clean checkout:

```sh
git clone https://github.com/keycloak/keycloak.git /tmp/fachtracing-keycloak
git -C /tmp/fachtracing-keycloak checkout eba869ee597b933efc8fa2c84713db9e6c0983cf
```

Then run:

```sh
./scripts/verify-keycloak.sh
```

Set `KEYCLOAK_DIR` when the checkout is elsewhere. The command builds the Keycloak `services`
module and can take longer than the three-minute pull-request gate. It is an explicit external
conformance command and is not part of pull-request CI.

The command writes these disposable files under `conformance/keycloak/target/generated`:

- `search-users-business.mmd`: the generated all-path, non-technical method overview.
- `search-users-business.json`: the same all-path overview in the V1 business graph contract.
- `fachtracing-business-graph-v1.schema.json`: the JSON Schema for the V1 business graph contract.
- `search-users-evaluated-example.mmd`: one concise successful path selected from the analyzed graph.
- `activation.json`: exact probes and class fingerprints for the pinned build.

The static overview is the primary proof for method usability. It has zero coverage gaps. The
evaluated example is not a fixed reviewed diagram and is not a recorded HTTP call. The harness
finds a successful exact path in the generated graph, creates exact edge evidence for that path, and
passes it through the same generic runtime projector as the agent sink. This proves the mapping in a
repeatable gate. A live call supplies its own observed edges and can select a different path.

## Call the endpoint and get the evaluated flow

Use a Keycloak distribution built from the pinned revision. Build this Fachtracing repository first.
Make the matching API, engine, agent, and ASM JARs visible to the bootstrap loader because the agent
JAR is not an aggregate JAR. The exact distribution start command can differ by Keycloak build.
For a local development distribution, the JVM options have this form:

```sh
FACHTRACING_ROOT=/path/to/fachtracing
FACHTRACING_OUTPUT=/tmp/keycloak-business-traces
FACHTRACING_ACTIVATION="$FACHTRACING_ROOT/conformance/keycloak/target/generated/activation.json"
FACHTRACING_BOOT="$FACHTRACING_ROOT/fachtracing-api/target/fachtracing-api-0.1.0-rc.1.jar:$FACHTRACING_ROOT/fachtracing-engine/target/fachtracing-engine-0.1.0-rc.1.jar:$HOME/.m2/repository/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar:$HOME/.m2/repository/org/ow2/asm/asm-tree/9.10.1/asm-tree-9.10.1.jar"
export KC_BOOTSTRAP_ADMIN_USERNAME=admin
export KC_BOOTSTRAP_ADMIN_PASSWORD=change-me
export JAVA_OPTS_APPEND="-Xbootclasspath/a:$FACHTRACING_BOOT -javaagent:$FACHTRACING_ROOT/fachtracing-agent/target/fachtracing-agent-0.1.0-rc.1.jar=activation=$FACHTRACING_ACTIVATION,output=$FACHTRACING_OUTPUT"
bin/kc.sh start-dev
```

In another shell, request an administrator token and call the endpoint:

```sh
TOKEN=$(curl --fail --silent \
  --data client_id=admin-cli \
  --data username=admin \
  --data password=change-me \
  --data grant_type=password \
  http://localhost:8080/realms/master/protocol/openid-connect/token | jq -r .access_token)
curl --fail --silent \
  --header "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/realms/master/users?search=admin"
```

Each call creates one `.txt` explanation and one `.mmd` evaluated path in
`/tmp/keycloak-business-traces`. Both files use one generated call-specific business graph. They
contain only the rules, outcomes, named result, and gaps selected for that call. They do not contain
Java owners, methods, source paths, request values, result values, tokens, or exception details. The
lazy result keeps the generated `search users completed` business result. If runtime evidence is
not sufficient to select an exact path, that runtime-only limit stays visible as a business-safe
gap. The files do not expose developer diagnostics.

## Non-Java review check

Give only `search-users-business.mmd` to a reviewer who does not know Java. Do not give the source,
exact graph, developer record, or runtime call. The review passes when the reviewer can answer all
four questions from the diagram alone:

1. Where does the method flow start?
2. Which rules can change the flow?
3. Which business actions can occur?
4. Which failure and completion results can occur, and is any rule unknown?

The feature definition of done passes only when all four answers agree with the nodes, edges,
results, and complete coverage state in the generated static graph.

The overview, generated evaluated example, and live call are separate checks. The conformance
command verifies the pinned source selection, static summary, exact-to-business runtime mapping,
activation, and class fingerprints. A live Keycloak call is optional for this review because it needs a running
distribution, an administrator account, a free port, and startup time outside the repository CI
budget. See [the exact selection](selection.md) for the static method boundary.
