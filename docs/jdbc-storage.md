# JDBC storage

Add `at.gepardec.fachtracing:fachtracing-storage-jdbc` and construct
`JdbcDecisionRecordRepository` with the application's `DataSource`. Call `migrate()` during
controlled application startup, before accepting decision traffic. The migration is repeatable and
records schema version 1.

The adapter uses standard JDBC in production. H2 2.4.240 is test scope only and is the reference
database for executable contracts. H2 uses the MPL 2.0 or EPL 1.0 dual license. Deployments must run
the migration and integration suite against their selected JDBC database before release.

Records use an execution-ID unique constraint and idempotent duplicate save. Correlation values are
stored in a separate indexed table by name, already-redacted canonical value, and completion time.
Queries use an inclusive time range. Retention deletes only records completed strictly before its
boundary; foreign-key cascade deletes their correlation rows in the same transaction.

Each operation owns one transaction and rolls back on failure. SQL states in connection class `08`
and transaction rollback class `40` are marked retryable. `DecisionRecordDelivery` performs retries
outside the decision thread. The adapter applies a positive query timeout to every JDBC statement.
The default is 30 seconds. Use the constructor with `Duration` to set a deployment-specific value.
If a driver ignores interruption or finishes a commit after the delivery wait ends, delivery reports
the record as unknown and stops that worker. It does not claim that the record was dropped. Operators
must query the execution ID before they retry an unknown record.
Credentials remain in the configured `DataSource` and never enter logs,
payloads, diagrams, or developer JSON.
