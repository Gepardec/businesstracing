package at.gepardec.fachtracing.store;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.model.DecisionExplanation;
import at.gepardec.fachtracing.model.DecisionRecordEnvelope;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Storage-neutral persistence port for complete or explicitly incomplete decision records. */
public interface DecisionRecordRepository {
    /** Saves one immutable record and returns its opaque ID. */
    DecisionRecordId save(DecisionRecord record);

    /** Finds a record by opaque ID without exposing storage implementation details. */
    Optional<DecisionRecord> findById(DecisionRecordId id);

    /** Saves one V1 protocol envelope; legacy repositories can opt in without breaking callers. */
    default void saveEnvelope(DecisionRecordEnvelope envelope) {
        throw new UnsupportedOperationException("decision envelope storage is not supported");
    }

    /** Finds a protocol envelope by its application execution ID. */
    default Optional<DecisionRecordEnvelope> findByExecutionId(String executionId) { return Optional.empty(); }

    /** Finds envelopes by one redacted correlation value and inclusive completion-time range. */
    default List<DecisionRecordEnvelope> findByCorrelation(DecisionRecordQuery query) { return List.of(); }

    /** Deletes records completed strictly before the retention boundary. */
    default long deleteCompletedBefore(Instant boundary) { return 0; }

    /** Storage-neutral query over already-redacted indexed values. */
    record DecisionRecordQuery(
            String correlationKey,
            String redactedCanonicalValue,
            Instant completedFrom,
            Instant completedTo) {
        public DecisionRecordQuery {
            Objects.requireNonNull(correlationKey, "correlationKey");
            Objects.requireNonNull(redactedCanonicalValue, "redactedCanonicalValue");
            Objects.requireNonNull(completedFrom, "completedFrom");
            Objects.requireNonNull(completedTo, "completedTo");
            if (completedTo.isBefore(completedFrom)) throw new IllegalArgumentException("invalid time range");
        }
    }

    /** Opaque record identifier. */
    record DecisionRecordId(String value) {
        /** Creates a validated identifier. */
        public DecisionRecordId {
            Objects.requireNonNull(value, "value");
            if (value.isBlank()) throw new IllegalArgumentException("value must not be blank");
        }
    }

    /** Immutable record containing reusable structure and one actual execution projection. */
    record DecisionRecord(
            DecisionRecordId id,
            BusinessDecisionGraph graph,
            DecisionExecution execution,
            DecisionExplanation explanation,
            String structurePlantUml,
            String executionPlantUml,
            String structureMermaid,
            String executionMermaid) {
        /** Compatibility constructor for records created before Mermaid output was available. */
        public DecisionRecord(
                DecisionRecordId id,
                BusinessDecisionGraph graph,
                DecisionExecution execution,
                DecisionExplanation explanation,
                String structurePlantUml,
                String executionPlantUml) {
            this(id, graph, execution, explanation, structurePlantUml, executionPlantUml, "", "");
        }

        /** Creates a version-consistent record. */
        public DecisionRecord {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(graph, "graph");
            Objects.requireNonNull(execution, "execution");
            Objects.requireNonNull(explanation, "explanation");
            Objects.requireNonNull(structurePlantUml, "structurePlantUml");
            Objects.requireNonNull(executionPlantUml, "executionPlantUml");
            Objects.requireNonNull(structureMermaid, "structureMermaid");
            Objects.requireNonNull(executionMermaid, "executionMermaid");
            if (!graph.graphId().equals(execution.graphId()) || graph.version() != execution.graphVersion()) {
                throw new IllegalArgumentException("graph and execution versions do not correlate");
            }
        }
    }
}
