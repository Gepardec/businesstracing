package at.gepardec.fachtracing.model;

import at.gepardec.fachtracing.api.DecisionValueAdapter;
import at.gepardec.fachtracing.api.DecisionValueRedactor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable evidence captured for one decision invocation. */
public record DecisionExecution(
        String executionId,
        String graphId,
        long graphVersion,
        Instant startedAt,
        Instant completedAt,
        List<NodeObservation> observations,
        TerminalStatus terminalStatus,
        DecisionValue finalResult,
        FailureData failure,
        BusinessDecisionGraph.Completeness completeness,
        List<String> coverageGaps) {

    /** Creates a defensive execution snapshot. */
    public DecisionExecution {
        executionId = requireText(executionId, "executionId");
        graphId = requireText(graphId, "graphId");
        if (graphVersion < 1) throw new IllegalArgumentException("graphVersion must be positive");
        startedAt = Objects.requireNonNull(startedAt, "startedAt");
        completedAt = Objects.requireNonNull(completedAt, "completedAt");
        if (completedAt.isBefore(startedAt)) throw new IllegalArgumentException("completion precedes start");
        observations = List.copyOf(observations);
        terminalStatus = Objects.requireNonNull(terminalStatus, "terminalStatus");
        if (terminalStatus == TerminalStatus.SUCCEEDED) {
            Objects.requireNonNull(finalResult, "finalResult");
            if (failure != null) throw new IllegalArgumentException("successful execution must not have failure data");
        } else {
            if (finalResult != null) throw new IllegalArgumentException("failed execution must not have a final result");
            Objects.requireNonNull(failure, "failure");
        }
        completeness = Objects.requireNonNull(completeness, "completeness");
        coverageGaps = List.copyOf(coverageGaps);
    }

    /** Creates a successful execution with the original constructor contract. */
    public DecisionExecution(
            String executionId,
            String graphId,
            long graphVersion,
            Instant startedAt,
            Instant completedAt,
            List<NodeObservation> observations,
            DecisionValue finalResult,
            BusinessDecisionGraph.Completeness completeness,
            List<String> coverageGaps) {
        this(executionId, graphId, graphVersion, startedAt, completedAt, observations,
                TerminalStatus.SUCCEEDED, finalResult, null, completeness, coverageGaps);
    }

    /** Terminal state for one completed invocation. */
    public enum TerminalStatus { SUCCEEDED, FAILED }

    /** Generic business-safe failure data with no technical exception details. */
    public record FailureData(String canonicalValue, String displayValue) {
        /** Creates validated failure data. */
        public FailureData {
            canonicalValue = requireText(canonicalValue, "canonicalValue");
            displayValue = requireText(displayValue, "displayValue");
        }

        /** Returns the standard failed-invocation value. */
        public static FailureData genericFailure() {
            return new FailureData("FAILED", "Decision failed");
        }
    }

    /** One ordered visit to a graph node. */
    public record NodeObservation(
            long sequence,
            String nodeId,
            String outcome,
            Map<String, DecisionValue> evidence,
            String selectedEdgeId) {
        /** Creates a defensive observation. */
        public NodeObservation {
            if (sequence < 0) throw new IllegalArgumentException("sequence must be non-negative");
            nodeId = requireText(nodeId, "nodeId");
            outcome = Objects.requireNonNull(outcome, "outcome");
            evidence = Map.copyOf(evidence);
        }
    }

    /** Tagged, already-redacted value safe for decision records. */
    public record DecisionValue(String type, String canonicalValue, String displayValue) {
        /** Creates a validated record value. */
        public DecisionValue {
            type = requireText(type, "type");
            canonicalValue = Objects.requireNonNull(canonicalValue, "canonicalValue");
            displayValue = Objects.requireNonNull(displayValue, "displayValue");
        }

        /** Creates a Boolean decision value. */
        public static DecisionValue of(boolean value) {
            return new DecisionValue("boolean", Boolean.toString(value), Boolean.toString(value));
        }

        /** Creates a canonical arbitrary-precision numeric decision value. */
        public static DecisionValue of(Number value) {
            Objects.requireNonNull(value, "value");
            var canonical = value instanceof BigDecimal decimal
                    ? decimal.stripTrailingZeros().toPlainString()
                    : new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
            return new DecisionValue("number", canonical, canonical);
        }

        /** Creates a string decision value. */
        public static DecisionValue of(String value) {
            return new DecisionValue("string", Objects.requireNonNull(value, "value"), value);
        }

        /** Creates a category value from the stable enum constant name. */
        public static DecisionValue category(Enum<?> value) {
            Objects.requireNonNull(value, "value");
            return new DecisionValue("category", value.name(), value.name());
        }
    }

    /** Registry which denies unknown objects and applies redaction before returning a value. */
    public static final class DecisionValueCodec {
        private final Map<Class<?>, DecisionValueAdapter<?>> adapters = new LinkedHashMap<>();
        private final DecisionValueRedactor redactor;

        /** Creates a codec using the supplied mandatory record-boundary redactor. */
        public DecisionValueCodec(DecisionValueRedactor redactor) {
            this.redactor = Objects.requireNonNull(redactor, "redactor");
        }

        /** Registers an exact-type adapter and returns this codec. */
        public <T> DecisionValueCodec register(DecisionValueAdapter<T> adapter) {
            Objects.requireNonNull(adapter, "adapter");
            adapters.put(adapter.targetType(), adapter);
            return this;
        }

        /** Adapts and redacts a supported value without arbitrary stringification. */
        public DecisionValue encode(Object value, String decisionLabel, String evidenceLabel) {
            Objects.requireNonNull(value, "value");
            DecisionValueAdapter.AdaptedValue adapted = builtIn(value);
            if (adapted == null) adapted = adaptCustom(value);
            if (adapted == null) {
                throw new IllegalArgumentException("No decision value adapter registered for supplied type");
            }
            var redacted = Objects.requireNonNull(redactor.redact(adapted,
                    new DecisionValueRedactor.ValueContext(decisionLabel, evidenceLabel)), "redacted value");
            return new DecisionValue(redacted.type(), redacted.canonicalValue(), redacted.displayValue());
        }

        private DecisionValueAdapter.AdaptedValue builtIn(Object value) {
            if (value instanceof Boolean bool) return adapted(DecisionValue.of(bool));
            if (value instanceof Number number) return adapted(DecisionValue.of(number));
            if (value instanceof String string) return adapted(DecisionValue.of(string));
            if (value instanceof Enum<?> category) return adapted(DecisionValue.category(category));
            if (value instanceof Collection<?> collection) return adaptCollection(collection);
            return null;
        }

        private DecisionValueAdapter.AdaptedValue adaptCollection(Collection<?> collection) {
            var canonical = new ArrayList<String>();
            var display = new ArrayList<String>();
            for (Object item : collection) {
                if (item == null) throw new IllegalArgumentException("Null collection results require a custom adapter");
                DecisionValueAdapter.AdaptedValue adapted = builtIn(item);
                if (adapted == null) adapted = adaptCustom(item);
                if (adapted == null) {
                    throw new IllegalArgumentException("Collection element has no decision value adapter");
                }
                canonical.add(quote(adapted.canonicalValue()));
                display.add(adapted.displayValue());
            }
            return new DecisionValueAdapter.AdaptedValue(
                    "collection", "[" + String.join(",", canonical) + "]", "[" + String.join(", ", display) + "]");
        }

        private static String quote(String value) {
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }

        @SuppressWarnings("unchecked")
        private DecisionValueAdapter.AdaptedValue adaptCustom(Object value) {
            var adapter = (DecisionValueAdapter<Object>) adapters.get(value.getClass());
            return adapter == null ? null : Objects.requireNonNull(adapter.adapt(value), "adapted value");
        }

        private static DecisionValueAdapter.AdaptedValue adapted(DecisionValue value) {
            return new DecisionValueAdapter.AdaptedValue(value.type(), value.canonicalValue(), value.displayValue());
        }
    }

    /** Creates a mutable evidence map which preserves insertion order before record construction. */
    public static Map<String, DecisionValue> evidence() {
        return new LinkedHashMap<>();
    }

    /** Returns a mutable observation list intended only for collector assembly. */
    public static List<NodeObservation> newObservations() {
        return new ArrayList<>();
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
