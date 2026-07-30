package at.gepardec.fachtracing.store;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.model.DecisionExplanation;

import java.util.Objects;
import java.util.Optional;

/** Storage-neutral persistence port for complete or explicitly incomplete decision records. */
public interface DecisionRecordRepository {
    /** Saves one immutable record and returns its opaque ID. */
    DecisionRecordId save(DecisionRecord record);

    /** Finds a record by opaque ID without exposing storage implementation details. */
    Optional<DecisionRecord> findById(DecisionRecordId id);

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
