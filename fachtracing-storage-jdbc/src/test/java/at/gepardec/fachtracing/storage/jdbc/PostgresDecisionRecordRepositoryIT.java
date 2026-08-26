package at.gepardec.fachtracing.storage.jdbc;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.model.DecisionRecordEnvelope;
import at.gepardec.fachtracing.store.DecisionRecordRepository;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** PostgreSQL migration, idempotency, lookup, query, and retention contract. */
public final class PostgresDecisionRecordRepositoryIT {
    private PostgresDecisionRecordRepositoryIT() { }

    public static void main(String[] args) throws SQLException {
        String url = required("FACHTRACING_POSTGRES_URL");
        var source = new PGSimpleDataSource();
        source.setURL(url);
        source.setUser(required("FACHTRACING_POSTGRES_USER"));
        source.setPassword(required("FACHTRACING_POSTGRES_PASSWORD"));
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            statement.execute("drop table if exists fachtracing_graph");
            statement.execute("drop table if exists fachtracing_correlation");
            statement.execute("drop table if exists fachtracing_decision_record");
            statement.execute("drop table if exists fachtracing_schema_version");
        }

        var repository = new JdbcDecisionRecordRepository(source, Duration.ofSeconds(1));
        repository.migrate();
        repository.migrate();
        try (var connection = source.getConnection(); var statement = connection.createStatement();
             var rows = statement.executeQuery("select count(*) from fachtracing_schema_version")) {
            assert rows.next() && rows.getInt(1) == 2;
        }
        var first = envelope("postgres-record-1", "postgres-execution-1",
                Instant.parse("2026-08-05T10:00:00Z"));
        var second = envelope("postgres-record-2", "postgres-execution-2",
                Instant.parse("2026-08-05T10:01:00Z"));
        repository.saveEnvelope(first);
        repository.saveEnvelope(first);
        repository.saveEnvelope(second);
        assertConflict(() -> repository.saveEnvelope(envelope(
                "postgres-record-conflict", "postgres-execution-1",
                Instant.parse("2026-08-05T10:02:00Z"))));
        assertConflict(() -> repository.saveEnvelope(envelope(
                "postgres-record-1", "postgres-execution-conflict",
                Instant.parse("2026-08-05T10:03:00Z"))));
        var invalid = envelope("postgres-rollback", "postgres-rollback-execution",
                Instant.parse("2026-08-05T10:04:00Z"), "x".repeat(501));
        try {
            repository.saveEnvelope(invalid);
            throw new AssertionError("invalid correlation value was accepted");
        } catch (JdbcDecisionRecordRepository.JdbcRepositoryException expected) {
            assert repository.findByExecutionId("postgres-rollback-execution").isEmpty();
        }
        assert repository.findByExecutionId(first.execution().executionId()).orElseThrow().equals(first);
        var query = new DecisionRecordRepository.DecisionRecordQuery(
                "case", "redacted-1", Instant.parse("2026-08-05T09:00:00Z"),
                Instant.parse("2026-08-05T11:00:00Z"));
        assert repository.findByCorrelation(query).equals(List.of(first, second));
        assert repository.deleteCompletedBefore(Instant.parse("2026-08-05T10:00:30Z")) == 1;
        assert repository.findByExecutionId(first.execution().executionId()).isEmpty();
        assert repository.findByExecutionId(second.execution().executionId()).isPresent();
        assertQueryTimeout(source, repository);
        System.out.println("POSTGRES_JDBC_OK");
    }

    private static void assertQueryTimeout(
            PGSimpleDataSource source, JdbcDecisionRecordRepository repository) throws SQLException {
        try (var lock = source.getConnection(); var statement = lock.createStatement()) {
            lock.setAutoCommit(false);
            statement.execute("lock table fachtracing_decision_record in access exclusive mode");
            long started = System.nanoTime();
            try {
                repository.findByExecutionId("postgres-execution-2");
                throw new AssertionError("blocked PostgreSQL query ignored its timeout");
            } catch (JdbcDecisionRecordRepository.JdbcRepositoryException expected) {
                long elapsedMillis = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - started);
                assert elapsedMillis >= 500 && elapsedMillis < 5_000 : elapsedMillis;
            } finally {
                lock.rollback();
            }
        }
    }

    private static void assertConflict(Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("conflicting durable key was accepted");
        } catch (DecisionRecordRepository.DecisionRecordConflictException expected) {
            assert expected.getMessage().contains("already belongs") : expected.getMessage();
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
        return value;
    }

    private static DecisionRecordEnvelope envelope(String recordId, String executionId, Instant completed) {
        return envelope(recordId, executionId, completed, "redacted-1");
    }

    private static DecisionRecordEnvelope envelope(
            String recordId, String executionId, Instant completed, String correlationValue) {
        var execution = new DecisionExecution(executionId, "postgres-graph", 1,
                completed.minusMillis(1), completed, List.of(), DecisionExecution.DecisionValue.of(true),
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
        return new DecisionRecordEnvelope(recordId, execution, "postgres-boundary",
                Map.of("case", new DecisionExecution.DecisionValue("string", correlationValue, "REDACTED")),
                "policy-v1");
    }
}
