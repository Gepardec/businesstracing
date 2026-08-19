package at.gepardec.fachtracing.storage.jdbc;

import at.gepardec.fachtracing.model.DecisionRecordEnvelope;
import at.gepardec.fachtracing.store.DecisionRecordRepository;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Transactional JDBC adapter with idempotent execution storage and indexed retrieval. */
public final class JdbcDecisionRecordRepository implements DecisionRecordRepository {
    private final DataSource dataSource;
    private final int statementTimeoutSeconds;

    public JdbcDecisionRecordRepository(DataSource dataSource) {
        this(dataSource, Duration.ofSeconds(30));
    }

    /** Creates an adapter with a positive JDBC statement timeout. */
    public JdbcDecisionRecordRepository(DataSource dataSource, Duration statementTimeout) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(statementTimeout, "statementTimeout");
        if (statementTimeout.isZero() || statementTimeout.isNegative()) {
            throw new IllegalArgumentException("statementTimeout must be positive");
        }
        long millis;
        try { millis = statementTimeout.toMillis(); }
        catch (ArithmeticException tooLarge) {
            throw new IllegalArgumentException("statementTimeout is too large", tooLarge);
        }
        long seconds = Math.max(1, Math.floorDiv(millis, 1000) + (millis % 1000 == 0 ? 0 : 1));
        if (seconds > Integer.MAX_VALUE) throw new IllegalArgumentException("statementTimeout is too large");
        this.statementTimeoutSeconds = (int) seconds;
    }

    /** Applies the repeatable storage schema migration. */
    public void migrate() {
        transaction(connection -> {
            execute(connection, """
                    create table if not exists fachtracing_schema_version (
                      version integer primary key, applied_at timestamp with time zone not null)
                    """);
            execute(connection, """
                    create table if not exists fachtracing_decision_record (
                      record_id varchar(200) primary key,
                      execution_id varchar(200) not null unique,
                      graph_id varchar(200) not null,
                      graph_version bigint not null,
                      started_at timestamp with time zone not null,
                      completed_at timestamp with time zone not null,
                      status varchar(20) not null,
                      schema_id varchar(100) not null,
                      payload %s not null)
                    """.formatted(payloadType(connection)));
            execute(connection, """
                    create index if not exists idx_fachtracing_graph_time
                    on fachtracing_decision_record(graph_id, completed_at)
                    """);
            execute(connection, """
                    create index if not exists idx_fachtracing_completed_execution
                    on fachtracing_decision_record(completed_at desc, execution_id desc)
                    """);
            execute(connection, """
                    create table if not exists fachtracing_correlation (
                      record_id varchar(200) not null,
                      correlation_name varchar(200) not null,
                      correlation_value varchar(500) not null,
                      completed_at timestamp with time zone not null,
                      primary key(record_id, correlation_name),
                      foreign key(record_id) references fachtracing_decision_record(record_id) on delete cascade)
                    """);
            execute(connection, """
                    create index if not exists idx_fachtracing_correlation_time
                    on fachtracing_correlation(correlation_name, correlation_value, completed_at)
                    """);
            execute(connection, """
                    create table if not exists fachtracing_graph (
                      graph_id varchar(200) not null,
                      graph_version bigint not null,
                      schema_id varchar(100) not null,
                      media_type varchar(100) not null,
                      payload %s not null,
                      sha256 varchar(64) not null,
                      imported_at timestamp with time zone not null,
                      primary key(graph_id, graph_version))
                    """.formatted(payloadType(connection)));
            recordSchemaVersion(connection, 1);
            recordSchemaVersion(connection, 2);
            return null;
        });
    }

    private void recordSchemaVersion(Connection connection, int version) throws SQLException {
        try (var statement = connection.prepareStatement(
                "insert into fachtracing_schema_version(version, applied_at) values(?, ?)")) {
            timeout(statement);
            statement.setInt(1, version);
            statement.setTimestamp(2, Timestamp.from(Instant.now()));
            try { statement.executeUpdate(); } catch (SQLException duplicate) {
                if (!duplicateKey(duplicate)) throw duplicate;
            }
        }
    }

    @Override public DecisionRecordId save(DecisionRecord record) {
        throw new UnsupportedOperationException("legacy graph records are not stored by the envelope adapter");
    }

    @Override public Optional<DecisionRecord> findById(DecisionRecordId id) { return Optional.empty(); }

    @Override public void saveEnvelope(DecisionRecordEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");
        try {
            insertEnvelope(envelope);
        } catch (JdbcRepositoryException failure) {
            if (!failure.constraintViolation()) throw failure;
            Optional<DecisionRecordEnvelope> existing = findByDurableIdentity(envelope);
            if (existing.filter(envelope::equals).isPresent()) return;
            throw new DecisionRecordConflictException("execution ID or record ID");
        }
    }

    private void insertEnvelope(DecisionRecordEnvelope envelope) {
        transaction(connection -> {
            try (var statement = connection.prepareStatement("""
                    insert into fachtracing_decision_record
                    (record_id, execution_id, graph_id, graph_version, started_at, completed_at, status, schema_id, payload)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                timeout(statement);
                statement.setString(1, envelope.recordId()); statement.setString(2, envelope.execution().executionId());
                statement.setString(3, envelope.execution().graphId()); statement.setLong(4, envelope.execution().graphVersion());
                statement.setTimestamp(5, Timestamp.from(envelope.execution().startedAt()));
                statement.setTimestamp(6, Timestamp.from(envelope.execution().completedAt()));
                statement.setString(7, envelope.status()); statement.setString(8, DecisionRecordEnvelope.SCHEMA);
                statement.setBytes(9, envelope.toJson());
                statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement("""
                    insert into fachtracing_correlation
                    (record_id, correlation_name, correlation_value, completed_at) values (?, ?, ?, ?)
                    """)) {
                timeout(statement);
                for (var entry : envelope.correlationKeys().entrySet()) {
                    statement.setString(1, envelope.recordId()); statement.setString(2, entry.getKey());
                    statement.setString(3, entry.getValue().canonicalValue());
                    statement.setTimestamp(4, Timestamp.from(envelope.execution().completedAt())); statement.addBatch();
                }
                statement.executeBatch();
            }
            return null;
        });
    }

    private Optional<DecisionRecordEnvelope> findByDurableIdentity(DecisionRecordEnvelope envelope) {
        return transaction(connection -> {
            try (var statement = connection.prepareStatement("""
                    select payload from fachtracing_decision_record
                    where execution_id = ? or record_id = ?
                    """)) {
                timeout(statement);
                statement.setString(1, envelope.execution().executionId());
                statement.setString(2, envelope.recordId());
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(read(rows)) : Optional.empty();
                }
            }
        });
    }

    @Override public Optional<DecisionRecordEnvelope> findByExecutionId(String executionId) {
        return transaction(connection -> {
            try (var statement = connection.prepareStatement(
                    "select payload from fachtracing_decision_record where execution_id = ?")) {
                timeout(statement);
                statement.setString(1, executionId);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(read(rows)) : Optional.empty();
                }
            }
        });
    }

    @Override public List<DecisionRecordEnvelope> findByCorrelation(DecisionRecordQuery query) {
        return transaction(connection -> {
            var result = new ArrayList<DecisionRecordEnvelope>();
            try (var statement = connection.prepareStatement("""
                    select r.payload from fachtracing_decision_record r
                    join fachtracing_correlation c on c.record_id = r.record_id
                    where c.correlation_name = ? and c.correlation_value = ?
                      and c.completed_at >= ? and c.completed_at <= ? order by c.completed_at, r.record_id
                    """)) {
                timeout(statement);
                statement.setString(1, query.correlationKey()); statement.setString(2, query.redactedCanonicalValue());
                statement.setTimestamp(3, Timestamp.from(query.completedFrom()));
                statement.setTimestamp(4, Timestamp.from(query.completedTo()));
                try (ResultSet rows = statement.executeQuery()) { while (rows.next()) result.add(read(rows)); }
            }
            return List.copyOf(result);
        });
    }

    @Override public long deleteCompletedBefore(Instant boundary) {
        return transaction(connection -> {
            try (var statement = connection.prepareStatement(
                    "delete from fachtracing_decision_record where completed_at < ?")) {
                timeout(statement);
                statement.setTimestamp(1, Timestamp.from(boundary)); return (long) statement.executeUpdate();
            }
        });
    }

    private static DecisionRecordEnvelope read(ResultSet rows) throws SQLException {
        return DecisionRecordEnvelope.fromJson(rows.getBytes(1));
    }
    private void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) { timeout(statement); statement.execute(sql); }
    }
    private static String payloadType(Connection connection) throws SQLException {
        String product = connection.getMetaData().getDatabaseProductName().toLowerCase(java.util.Locale.ROOT);
        return product.contains("postgresql") ? "bytea" : "blob";
    }
    private void timeout(java.sql.Statement statement) throws SQLException {
        statement.setQueryTimeout(statementTimeoutSeconds);
    }
    private <T> T transaction(SqlWork<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            boolean previous = connection.getAutoCommit(); connection.setAutoCommit(false);
            try { T result = work.run(connection); connection.commit(); return result; }
            catch (Exception failure) { try { connection.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
                if (failure instanceof SQLException sql) throw failure(sql); if (failure instanceof RuntimeException runtime) throw runtime;
                throw new JdbcRepositoryException("JDBC operation failed", false, false, failure); }
            finally { connection.setAutoCommit(previous); }
        } catch (SQLException failure) { throw failure(failure); }
    }
    private static JdbcRepositoryException failure(SQLException failure) {
        String state = failure.getSQLState(); boolean retryable = state != null && (state.startsWith("08") || state.startsWith("40"));
        return new JdbcRepositoryException("JDBC operation failed", retryable, duplicateKey(failure), failure);
    }
    private static boolean duplicateKey(SQLException failure) { return failure.getSQLState() != null && failure.getSQLState().startsWith("23"); }
    @FunctionalInterface private interface SqlWork<T> { T run(Connection connection) throws Exception; }

    public static final class JdbcRepositoryException extends RuntimeException {
        private final boolean retryable;
        private final boolean constraintViolation;
        private JdbcRepositoryException(
                String message, boolean retryable, boolean constraintViolation, Throwable cause) {
            super(message, cause);
            this.retryable = retryable;
            this.constraintViolation = constraintViolation;
        }
        public boolean retryable() { return retryable; }
        private boolean constraintViolation() { return constraintViolation; }
    }
}
