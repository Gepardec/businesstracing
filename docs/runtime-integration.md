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

At startup, load the build-time manifest and matching class fingerprints, call
`FachtracingAgent.configure`, and activate the graph with an application-owned redaction policy.
Send completed `DecisionRecordEnvelope` values to `DecisionRecordDelivery`, backed by
`JdbcDecisionRecordRepository`. Call `migrate()` during controlled startup. Retrieve a record with
`findByExecutionId` and project its opaque path with the matching graph version.

The RC policy keeps graph IDs opaque and graph versions explicit. Additive V1 JSON fields are safe
for old V1 readers, which ignore unknown fields. A schema meaning change requires a new schema ID.
Upgrade the plugin, engine, and agent together. Roll back those three artifacts together and keep
the V1 database schema; V1 records remain readable. Do not activate a manifest when its graph
version or class fingerprint does not match the running class.

`scripts/verify-external-release.sh` publishes the RC to a temporary isolated file repository,
uses an empty Maven local repository, generates Mermaid and PlantUML from one annotation, starts a
JVM with the released agent, persists through the released JDBC adapter, and retrieves the record.
