# Runtime integration

The release-candidate coordinates are `at.gepardec.fachtracing:*:0.1.0-rc.1`. A build must resolve
them from the configured release repository. The verified external flow does not use a Fachtracing
source checkout or a pre-populated local Maven repository.

Generate diagrams after compilation:

```sh
mvn compile at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-rc.1:analyze
```

Start the application with the matching agent artifact:

```sh
java -javaagent:/path/to/fachtracing-agent-0.1.0-rc.1.jar -jar application.jar
```

At startup, read `target/fachtracing/activation.json` with
`RuntimeActivationBundle.fromJson`, call `FachtracingAgent.configure(bundle)`, and register each
bundle graph in `RuntimeCollector` with an application-owned redaction policy. The activation path
does not need Java source or a compiler. The bundle's `javaAgentOption` gives the exact JVM option.
Activation V3 includes the JVM descriptor for each runtime binding, so annotated overloads remain
separate. The reader accepts activation V2 as a legacy name-only plan. Regenerate a V2 bundle before
you use overloads or before you require exact overload-safe instrumentation.
Send completed `DecisionRecordEnvelope` values to `DecisionRecordDelivery`, backed by
`JdbcDecisionRecordRepository`. Call `migrate()` during controlled startup. Retrieve a record with
`findByExecutionId` and project its opaque path with the matching graph version.

The RC policy keeps graph IDs opaque and graph versions explicit. Additive V1 JSON fields are safe
for old V1 readers, which ignore unknown fields. A schema meaning change requires a new schema ID.
Upgrade the plugin, engine, and agent together. Roll back those three artifacts together and keep
the V1 database schema; V1 records remain readable. Do not activate a manifest when its graph
version or class fingerprint does not match the running class.

`scripts/verify-external-release.sh` publishes the RC to a temporary isolated file repository,
uses an empty Maven local repository, resolves an exact external source artifact, and repeats that
source analysis offline from cache. It generates Mermaid and PlantUML, starts a JVM with the released
agent, loads only the generated activation bundle, invokes the annotated method, verifies the captured path and
business explanation, persists the actual execution through JDBC, and retrieves and explains it.
