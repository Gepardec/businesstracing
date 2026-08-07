package at.gepardec.fachtracing.storage.jdbc;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.model.DecisionRecordEnvelope;
import at.gepardec.fachtracing.store.DecisionRecordRepository;
import org.h2.jdbcx.JdbcDataSource;

import java.time.Instant;
import java.time.Duration;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.sql.DataSource;

/** H2 reference contracts for migration, save, lookup, query, idempotency, and retention. */
public final class JdbcDecisionRecordRepositoryTest {
    private JdbcDecisionRecordRepositoryTest() { }

    public static void main(String[] args) {
        var source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:fachtracing;DB_CLOSE_DELAY=-1");
        var tracking = new TimeoutTrackingDataSource(source);
        var repository = new JdbcDecisionRecordRepository(tracking, Duration.ofMillis(1500));
        repository.migrate(); repository.migrate();
        var first = envelope("record-1", "execution-1", Instant.parse("2026-01-01T00:00:01Z"));
        var second = envelope("record-2", "execution-2", Instant.parse("2026-01-02T00:00:01Z"));
        repository.saveEnvelope(first); repository.saveEnvelope(first); repository.saveEnvelope(second);
        assertConflict(() -> repository.saveEnvelope(envelope(
                "record-conflict", "execution-1", Instant.parse("2026-01-03T00:00:01Z"))));
        assertConflict(() -> repository.saveEnvelope(envelope(
                "record-1", "execution-conflict", Instant.parse("2026-01-03T00:00:01Z"))));
        assert repository.findByExecutionId("execution-1").orElseThrow().equals(first);
        assert repository.findByExecutionId("execution-conflict").isEmpty();
        assert repository.findByExecutionId("missing").isEmpty();
        var query = new DecisionRecordRepository.DecisionRecordQuery(
                "case", "hash-123", Instant.parse("2026-01-01T12:00:00Z"), Instant.parse("2026-01-03T00:00:00Z"));
        assert repository.findByCorrelation(query).equals(List.of(second));
        assert repository.deleteCompletedBefore(Instant.parse("2026-01-01T12:00:00Z")) == 1;
        assert repository.findByExecutionId("execution-1").isEmpty();
        assert repository.findByExecutionId("execution-2").isPresent();
        assert !tracking.timeouts.isEmpty();
        assert tracking.timeouts.stream().allMatch(timeout -> timeout == 2) : tracking.timeouts;
    }

    private static void assertConflict(Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("conflicting record was accepted");
        } catch (DecisionRecordRepository.DecisionRecordConflictException expected) {
            assert expected.getMessage().contains("already belongs") : expected.getMessage();
        }
    }

    private static DecisionRecordEnvelope envelope(String recordId, String executionId, Instant completed) {
        var execution = new DecisionExecution(executionId, "graph", 1, completed.minusMillis(1), completed,
                List.of(), DecisionExecution.DecisionValue.of(true),
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
        return new DecisionRecordEnvelope(recordId, execution, "boundary",
                Map.of("case", new DecisionExecution.DecisionValue("string", "hash-123", "REDACTED")), "policy-v1");
    }

    private static final class TimeoutTrackingDataSource implements DataSource {
        private final DataSource delegate;
        private final List<Integer> timeouts = new ArrayList<>();
        private TimeoutTrackingDataSource(DataSource delegate) { this.delegate = delegate; }
        @Override public Connection getConnection() throws SQLException { return connection(delegate.getConnection()); }
        @Override public Connection getConnection(String user, String password) throws SQLException {
            return connection(delegate.getConnection(user, password));
        }
        private Connection connection(Connection target) {
            return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                    Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        try {
                            Object result = method.invoke(target, args);
                            return result instanceof Statement statement ? statement(statement) : result;
                        } catch (java.lang.reflect.InvocationTargetException failure) {
                            throw failure.getCause();
                        }
                    });
        }
        private Statement statement(Statement target) {
            Class<?> type = target instanceof java.sql.PreparedStatement
                    ? java.sql.PreparedStatement.class : Statement.class;
            return (Statement) java.lang.reflect.Proxy.newProxyInstance(
                    Statement.class.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
                        if (method.getName().equals("setQueryTimeout")) timeouts.add((Integer) args[0]);
                        try { return method.invoke(target, args); }
                        catch (java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
                    });
        }
        @Override public PrintWriter getLogWriter() throws SQLException { return delegate.getLogWriter(); }
        @Override public void setLogWriter(PrintWriter writer) throws SQLException { delegate.setLogWriter(writer); }
        @Override public void setLoginTimeout(int seconds) throws SQLException { delegate.setLoginTimeout(seconds); }
        @Override public int getLoginTimeout() throws SQLException { return delegate.getLoginTimeout(); }
        @Override public Logger getParentLogger() { return Logger.getGlobal(); }
        @Override public <T> T unwrap(Class<T> type) throws SQLException { return delegate.unwrap(type); }
        @Override public boolean isWrapperFor(Class<?> type) throws SQLException { return delegate.isWrapperFor(type); }
    }
}
