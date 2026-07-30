package at.gepardec.fachtracing.api;

import java.util.Objects;

/**
 * Converts an explicitly supported application value into a non-technical representation.
 * Unknown objects are rejected unless an adapter is registered for their exact type.
 *
 * @param <T> supported application type
 */
public interface DecisionValueAdapter<T> {
    /** Returns the exact application type handled by this adapter. */
    Class<T> targetType();

    /** Converts a value without invoking arbitrary {@code toString()} implementations. */
    AdaptedValue adapt(T value);

    /** A safe tagged value before the redaction stage. */
    record AdaptedValue(String type, String canonicalValue, String displayValue) {
        /** Creates a validated adapted value. */
        public AdaptedValue {
            type = requireText(type, "type");
            canonicalValue = Objects.requireNonNull(canonicalValue, "canonicalValue");
            displayValue = Objects.requireNonNull(displayValue, "displayValue");
        }

        private static String requireText(String value, String name) {
            Objects.requireNonNull(value, name);
            if (value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value;
        }
    }
}
