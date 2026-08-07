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
    private final ConcurrentHashMap<String, String> recordExecutions = new ConcurrentHashMap<>();

    @Override
    public DecisionRecordId save(DecisionRecord record) {
        records.put(record.id(), record);
        return record.id();
    }

    @Override
    public Optional<DecisionRecord> findById(DecisionRecordId id) {
        return Optional.ofNullable(records.get(id));
    }

    @Override public synchronized void saveEnvelope(DecisionRecordEnvelope envelope) {
        String executionId = envelope.execution().executionId();
        DecisionRecordEnvelope existing = envelopes.get(executionId);
        if (existing != null && !existing.equals(envelope)) {
            throw new DecisionRecordConflictException("execution ID");
        }
        String existingExecution = recordExecutions.get(envelope.recordId());
        if (existingExecution != null && !existingExecution.equals(executionId)) {
            throw new DecisionRecordConflictException("record ID");
        }
        envelopes.putIfAbsent(executionId, envelope);
        recordExecutions.putIfAbsent(envelope.recordId(), executionId);
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

    @Override public synchronized long deleteCompletedBefore(Instant boundary) {
        var removed = envelopes.entrySet().stream()
                .filter(entry -> entry.getValue().execution().completedAt().isBefore(boundary))
                .toList();
        removed.forEach(entry -> {
            envelopes.remove(entry.getKey(), entry.getValue());
            recordExecutions.remove(entry.getValue().recordId(), entry.getKey());
        });
        return removed.size();
    }

    /** Returns the current record count for operational verification. */
    public int size() {
        return records.size();
    }
}
