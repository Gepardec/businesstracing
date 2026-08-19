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
