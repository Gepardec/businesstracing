package at.gepardec.fachtracing.store;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.model.DecisionRecordEnvelope;
import at.gepardec.fachtracing.runtime.DecisionRecordDelivery;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** Executable contracts for V1 serialization, lookup, retention, and async delivery. */
public final class DecisionRecordProtocolTest {
    private DecisionRecordProtocolTest() { }

    public static void main(String[] args) throws Exception {
        roundTripsDeterministicallyAndIgnoresUnknownFields();
        queriesOnlyRedactedCorrelationValuesAndRetainsBoundaries();
        retriesOutsideTheApplicationThreadWithCounters();
    }

    private static void roundTripsDeterministicallyAndIgnoresUnknownFields() {
        DecisionRecordEnvelope envelope = envelope("record-1", "execution-1", Instant.parse("2026-01-01T00:00:01Z"));
        byte[] first = envelope.toJson();
        var decoded = DecisionRecordEnvelope.fromJson(first);
        assert decoded.equals(envelope) : new String(first, java.nio.charset.StandardCharsets.UTF_8);
        assert java.util.Arrays.equals(first, decoded.toJson());
        String extended = new String(first, java.nio.charset.StandardCharsets.UTF_8).trim();
        extended = extended.substring(0, extended.length() - 1) + ",\"futureField\":true}";
        assert DecisionRecordEnvelope.fromJson(extended.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .equals(envelope);
    }

    private static void queriesOnlyRedactedCorrelationValuesAndRetainsBoundaries() {
        var repository = new InMemoryDecisionRecordRepository();
        var older = envelope("record-1", "execution-1", Instant.parse("2026-01-01T00:00:01Z"));
        var newer = envelope("record-2", "execution-2", Instant.parse("2026-01-02T00:00:01Z"));
        repository.saveEnvelope(older); repository.saveEnvelope(newer);
        var query = new DecisionRecordRepository.DecisionRecordQuery(
                "case", "hash-123", Instant.parse("2026-01-01T12:00:00Z"), Instant.parse("2026-01-03T00:00:00Z"));
        assert repository.findByCorrelation(query).equals(List.of(newer));
        assert repository.findByExecutionId("execution-1").orElseThrow().equals(older);
        assert repository.deleteCompletedBefore(Instant.parse("2026-01-01T12:00:00Z")) == 1;
        assert repository.findByExecutionId("execution-1").isEmpty();
    }

    private static void retriesOutsideTheApplicationThreadWithCounters() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        String applicationThread = Thread.currentThread().getName();
        var repository = new DecisionRecordRepository() {
            @Override public DecisionRecordId save(DecisionRecord record) { return record.id(); }
            @Override public Optional<DecisionRecord> findById(DecisionRecordId id) { return Optional.empty(); }
            @Override public void saveEnvelope(DecisionRecordEnvelope envelope) {
                assert !Thread.currentThread().getName().equals(applicationThread);
                if (attempts.getAndIncrement() < 2) throw new IllegalStateException("temporary outage");
            }
        };
        try (var delivery = new DecisionRecordDelivery(repository, 4,
                DecisionRecordDelivery.AdmissionPolicy.FAIL_OPEN, 3, Duration.ofMillis(1))) {
            assert delivery.offer(envelope("record-3", "execution-3", Instant.now()));
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (delivery.counters().saved() == 0 && System.nanoTime() < deadline) Thread.sleep(1);
            var counters = delivery.counters();
            assert counters.accepted() == 1 && counters.saved() == 1 && counters.retried() == 2 : counters;
        }
    }

    private static DecisionRecordEnvelope envelope(String recordId, String executionId, Instant completed) {
        Instant started = completed.minusMillis(10);
        var observations = List.of(new DecisionExecution.NodeObservation(
                0, "predicate", "true", Map.of("age", DecisionExecution.DecisionValue.of(24)), "edge-true"));
        var execution = new DecisionExecution(executionId, "graph", 3, started, completed, observations,
                DecisionExecution.DecisionValue.of("approved"), BusinessDecisionGraph.Completeness.COMPLETE, List.of());
        return new DecisionRecordEnvelope(recordId, execution, "boundary-sha256",
                Map.of("case", new DecisionExecution.DecisionValue("string", "hash-123", "REDACTED")),
                "policy-v1");
    }
}
