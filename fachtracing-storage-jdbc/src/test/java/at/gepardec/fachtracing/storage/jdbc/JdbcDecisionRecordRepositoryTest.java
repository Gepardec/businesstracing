package at.gepardec.fachtracing.storage.jdbc;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.model.DecisionRecordEnvelope;
import at.gepardec.fachtracing.store.DecisionRecordRepository;
import org.h2.jdbcx.JdbcDataSource;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** H2 reference contracts for migration, save, lookup, query, idempotency, and retention. */
public final class JdbcDecisionRecordRepositoryTest {
    private JdbcDecisionRecordRepositoryTest() { }

    public static void main(String[] args) {
        var source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:fachtracing;DB_CLOSE_DELAY=-1");
        var repository = new JdbcDecisionRecordRepository(source);
        repository.migrate(); repository.migrate();
        var first = envelope("record-1", "execution-1", Instant.parse("2026-01-01T00:00:01Z"));
        var second = envelope("record-2", "execution-2", Instant.parse("2026-01-02T00:00:01Z"));
        repository.saveEnvelope(first); repository.saveEnvelope(first); repository.saveEnvelope(second);
        assert repository.findByExecutionId("execution-1").orElseThrow().equals(first);
        assert repository.findByExecutionId("missing").isEmpty();
        var query = new DecisionRecordRepository.DecisionRecordQuery(
                "case", "hash-123", Instant.parse("2026-01-01T12:00:00Z"), Instant.parse("2026-01-03T00:00:00Z"));
        assert repository.findByCorrelation(query).equals(List.of(second));
        assert repository.deleteCompletedBefore(Instant.parse("2026-01-01T12:00:00Z")) == 1;
        assert repository.findByExecutionId("execution-1").isEmpty();
        assert repository.findByExecutionId("execution-2").isPresent();
    }

    private static DecisionRecordEnvelope envelope(String recordId, String executionId, Instant completed) {
        var execution = new DecisionExecution(executionId, "graph", 1, completed.minusMillis(1), completed,
                List.of(), DecisionExecution.DecisionValue.of(true),
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
        return new DecisionRecordEnvelope(recordId, execution, "boundary",
                Map.of("case", new DecisionExecution.DecisionValue("string", "hash-123", "REDACTED")), "policy-v1");
    }
}
