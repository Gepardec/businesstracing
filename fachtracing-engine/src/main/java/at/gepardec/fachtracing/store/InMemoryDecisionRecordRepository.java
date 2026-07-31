package at.gepardec.fachtracing.store;

import at.gepardec.fachtracing.model.DecisionRecordEnvelope;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe in-memory repository for tests and local embedding. */
public final class InMemoryDecisionRecordRepository implements DecisionRecordRepository {
    private final ConcurrentHashMap<DecisionRecordId, DecisionRecord> records = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DecisionRecordEnvelope> envelopes = new ConcurrentHashMap<>();

    @Override
    public DecisionRecordId save(DecisionRecord record) {
        records.put(record.id(), record);
        return record.id();
    }

    @Override
    public Optional<DecisionRecord> findById(DecisionRecordId id) {
        return Optional.ofNullable(records.get(id));
    }

    @Override public void saveEnvelope(DecisionRecordEnvelope envelope) {
        envelopes.putIfAbsent(envelope.execution().executionId(), envelope);
    }

    @Override public Optional<DecisionRecordEnvelope> findByExecutionId(String executionId) {
        return Optional.ofNullable(envelopes.get(executionId));
    }

    @Override public List<DecisionRecordEnvelope> findByCorrelation(DecisionRecordQuery query) {
        return envelopes.values().stream()
                .filter(envelope -> !envelope.execution().completedAt().isBefore(query.completedFrom())
                        && !envelope.execution().completedAt().isAfter(query.completedTo()))
                .filter(envelope -> Optional.ofNullable(envelope.correlationKeys().get(query.correlationKey()))
                        .map(value -> value.canonicalValue().equals(query.redactedCanonicalValue())).orElse(false))
                .sorted(java.util.Comparator.comparing(item -> item.execution().completedAt())).toList();
    }

    @Override public long deleteCompletedBefore(Instant boundary) {
        long before = envelopes.size();
        envelopes.entrySet().removeIf(entry -> entry.getValue().execution().completedAt().isBefore(boundary));
        return before - envelopes.size();
    }

    /** Returns the current record count for operational verification. */
    public int size() {
        return records.size();
    }
}
