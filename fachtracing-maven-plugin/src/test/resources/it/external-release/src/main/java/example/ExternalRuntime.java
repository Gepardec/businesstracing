package example;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.model.DecisionRecordEnvelope;
import at.gepardec.fachtracing.runtime.DecisionRecordDelivery;
import at.gepardec.fachtracing.storage.jdbc.JdbcDecisionRecordRepository;
import org.h2.jdbcx.JdbcDataSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ExternalRuntime {
    public static void main(String[] args) throws Exception {
        var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:external;DB_CLOSE_DELAY=-1");
        var repository = new JdbcDecisionRecordRepository(dataSource);
        repository.migrate();
        Instant now = Instant.now();
        var execution = new DecisionExecution("external-execution", "external-graph", 1,
                now.minusMillis(1), now, List.of(), DecisionExecution.DecisionValue.of(true),
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
        var envelope = new DecisionRecordEnvelope("external-record", execution, "external-boundary",
                Map.of("case", new DecisionExecution.DecisionValue("string", "redacted-hash", "REDACTED")),
                "external-policy");
        try (var delivery = new DecisionRecordDelivery(repository, 16,
                DecisionRecordDelivery.AdmissionPolicy.FAIL_OPEN, 2, Duration.ofMillis(1))) {
            if (!delivery.offer(envelope)) throw new IllegalStateException("record was not accepted");
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (repository.findByExecutionId("external-execution").isEmpty()
                    && System.nanoTime() < deadline) Thread.sleep(1);
        }
        if (repository.findByExecutionId("external-execution").isEmpty()) {
            throw new IllegalStateException("record was not persisted");
        }
        System.out.println("EXTERNAL_RUNTIME_OK");
    }
}
