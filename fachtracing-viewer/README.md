# Fachtracing Viewer

The viewer is a local Svelte 5 and SvelteKit proof of concept for stored Fachtracing decisions. It lists recorded runs, searches by an arbitrary exact correlation, and shows the complete business graph with the recorded path and evidence.

## Database

Run the JDBC migration before the viewer starts. The viewer uses these server-only variables:

- `FACHTRACING_DATABASE_URL`, or the separate host, port, database, user, and password variables;
- `FACHTRACING_POSTGRES_HOST`;
- `FACHTRACING_POSTGRES_PORT`;
- `FACHTRACING_POSTGRES_DATABASE`;
- `FACHTRACING_POSTGRES_USER`;
- `FACHTRACING_POSTGRES_PASSWORD`.

The server binds to `127.0.0.1` in the documented commands. Do not expose this POC to a shared network. It has no authentication or tenant isolation.

## Graph import

Generate current `fachtracing-developer-graph/v1` JSON files with the Maven plugin. Import them before a user opens a stored run:

```sh
npm ci
npm run import-graphs -- --directory /absolute/path/to/generated/graphs
```

The command validates each document, stores the unchanged UTF-8 bytes and SHA-256 checksum, and rejects different bytes for an existing graph ID and version. An identical import is idempotent. Back up `fachtracing_graph` with the decision and correlation tables. Do not delete a graph while a decision record references its ID and version.

## Preview one graph file

Open `http://127.0.0.1:5173/graphs` and select or drop one current `fachtracing-developer-graph/v1` JSON file. The viewer validates the file in the browser and uses the same top-to-bottom graph canvas as a stored decision. It does not send the file to the server, write it to PostgreSQL, or store it in browser storage. A page reload clears the preview.

The preview accepts JSON files up to 5 MiB. The tested safety profile is 250 nodes and 400 edges. A larger valid graph is not truncated, but it can take longer to arrange. The preview does not support the future binary graph format yet.

## Graph layout

The viewer analyzes the supplied topology before it draws the graph. It puts entries first, outcomes last, direct siblings in one rank, and disconnected components in stable columns. ELK orders the node boxes. A deterministic router then selects north, east, south, or west connection points and draws collision-free orthogonal routes. The router does not add positions or routes to the JSON document.

Every predicate branch shows its outcome near the source. Boolean values display as `Yes` and `No`; the accessible edge name keeps the raw value. Repeated kind-and-label pairs show an occurrence marker, such as `2 of 3`, and keep their exact node IDs in the accessible description.

Four or more routes that enter one node use one presentation-only junction and shared trunk. Each supplied edge stays a separate keyboard focus target. Pointer or keyboard inspection highlights its feeder and the shared trunk. Route crossings use a bridge, not a graph node. Cycles and disconnected components can use neutral structural outlines. These presentation items do not change the supplied node or edge count.

The canvas is read-only. It opens in **Reading** mode at the first declared entry and keeps the local neighborhood readable. Use **Overview** to see the complete topology. Overview reduces node detail when text would be too small to read. Select a node or search for an exact ID or label to return to Reading mode and keep that node visibly selected. Graphs with more than 100 nodes use search guidance instead of a minimap.

To review one or more graph files with the production layout engine, run:

```sh
npm run review:graphs -- /absolute/path/to/graph-a.json /absolute/path/to/graph-b.json
```

The command accepts both V1 graph formats. It prints layout time, dimensions, crossing density, detour, and named quality failures. It returns a non-zero status when a graph fails an objective layout gate. The four-second timing check is a local POC responsiveness gate for graphs with 19 to 55 nodes. It is not a production benchmark.

## Correlation lookup

The search accepts any stored correlation name and its exact canonical value. The viewer does not convert raw application values. If users know a different value, the traced application must store a separate lookup-safe correlation or a separate adapter must define the conversion. Search bodies use HTTP `QUERY`; names and values do not enter URLs or application logs.

## Show Fachtracing tracing itself

From a clean checkout, generate the self-traced developer graphs and five real Java-agent runs:

```sh
./scripts/verify-viewer-dogfood.sh
```

After the PostgreSQL migration, import the generated artifacts:

```sh
cd fachtracing-viewer
npm run import-graphs -- --directory ../target/viewer-dogfood/graphs
npm run import-runs -- --directory ../target/viewer-dogfood/runs
npm run dev
```

Open `http://127.0.0.1:5173/runs` and search for correlation name `application` with exact value `fachtracing`. The results show two Fachtracing production policies and five recorded executions. The script generates every graph, node, edge, observation, and selected path from the analyzer and Java agent. No demonstration topology is stored in the viewer.

The hosted PostgreSQL job runs the same sequence and publishes `fachtracing-viewer.png` as its `fachtracing-viewer-dogfood-*` artifact.

## Development

```sh
npm ci
npm run dev
```

Verification uses `npm run check`, `npm test`, `npm run build`, and `npm run audit`. Browser tests run with `npm run test:browser`; the complete run-detail journey requires `FACHTRACING_DATABASE_URL` and a migrated PostgreSQL database.

Only `fachtracing-developer-graph/v1` and `fachtracing-decision-record/v1` are supported. The graph contract is the merged multi-source V1 shape. Developer source origins, paths, URLs, and fingerprints never enter browser graph responses.
