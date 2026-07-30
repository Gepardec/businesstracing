package at.gepardec.fachtracing.api;

import java.util.Objects;

/**
 * Redacts adapted evidence before it enters a decision record.
 * Implementations must be deterministic and must not return {@code null}.
 */
@FunctionalInterface
public interface DecisionValueRedactor {
    /** Applies record-boundary redaction to one adapted value. */
    DecisionValueAdapter.AdaptedValue redact(
            DecisionValueAdapter.AdaptedValue value,
            ValueContext context);

    /** Business-only context supplied to a redaction policy. */
    record ValueContext(String decisionLabel, String evidenceLabel) {
        /** Creates a validated context without Java provenance. */
        public ValueContext {
            Objects.requireNonNull(decisionLabel, "decisionLabel");
            Objects.requireNonNull(evidenceLabel, "evidenceLabel");
        }
    }

    /** Returns a redactor that preserves already-adapted values. */
    static DecisionValueRedactor none() {
        return (value, context) -> Objects.requireNonNull(value, "value");
    }
}
