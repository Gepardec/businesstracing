package at.gepardec.fachtracing.store;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe in-memory repository for tests and local embedding. */
public final class InMemoryDecisionRecordRepository implements DecisionRecordRepository {
    private final ConcurrentHashMap<DecisionRecordId, DecisionRecord> records = new ConcurrentHashMap<>();

    @Override
    public DecisionRecordId save(DecisionRecord record) {
        records.put(record.id(), record);
        return record.id();
    }

    @Override
    public Optional<DecisionRecord> findById(DecisionRecordId id) {
        return Optional.ofNullable(records.get(id));
    }

    /** Returns the current record count for operational verification. */
    public int size() {
        return records.size();
    }
}
