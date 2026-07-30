package at.gepardec.fachtracing.model;

import at.gepardec.fachtracing.api.DecisionValueAdapter;
import at.gepardec.fachtracing.api.DecisionValueRedactor;

import java.math.BigDecimal;
import java.util.List;

/** Plain-Java contract tests which avoid a framework dependency. */
public final class ApiModelTest {
    private enum Category { ELIGIBLE }
    private record Money(BigDecimal amount) { }

    private ApiModelTest() { }

    public static void main(String[] args) {
        builtInsRoundTrip();
        customAdapterAndRedaction();
        collectionsPreserveTypedElementsWithoutArbitraryStringification();
        unknownValuesAreRejectedWithoutStringification();
    }

    private static void collectionsPreserveTypedElementsWithoutArbitraryStringification() {
        var codec = new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none());
        var empty = codec.encode(List.of(), "warnings", "result");
        assert empty.type().equals("collection");
        assert empty.canonicalValue().equals("[]");
        var values = codec.encode(List.of(true, 12, Category.ELIGIBLE), "mixed", "result");
        assert values.canonicalValue().equals("[\"true\",\"12\",\"ELIGIBLE\"]") : values;
    }

    private static void builtInsRoundTrip() {
        var codec = new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none());
        assert codec.encode(true, "eligibility", "result").equals(new DecisionExecution.DecisionValue("boolean", "true", "true"));
        assert codec.encode(new BigDecimal("12.50"), "price", "result").canonicalValue().equals("12.5");
        assert codec.encode(Category.ELIGIBLE, "eligibility", "result").type().equals("category");
        assert codec.encode("accepted", "eligibility", "result").type().equals("string");
    }

    private static void customAdapterAndRedaction() {
        DecisionValueAdapter<Money> adapter = new DecisionValueAdapter<>() {
            public Class<Money> targetType() { return Money.class; }
            public AdaptedValue adapt(Money value) {
                var canonical = value.amount().toPlainString();
                return new AdaptedValue("money", canonical, canonical + " EUR");
            }
        };
        DecisionValueRedactor redactor = (value, context) -> new DecisionValueAdapter.AdaptedValue(
                value.type(), value.canonicalValue(), "REDACTED");
        var encoded = new DecisionExecution.DecisionValueCodec(redactor).register(adapter)
                .encode(new Money(new BigDecimal("9.95")), "price", "result");
        assert encoded.type().equals("money");
        assert encoded.canonicalValue().equals("9.95");
        assert encoded.displayValue().equals("REDACTED");
    }

    private static void unknownValuesAreRejectedWithoutStringification() {
        final class Unknown {
            boolean stringified;
            @Override public String toString() { stringified = true; return "unsafe"; }
        }
        var unknown = new Unknown();
        try {
            new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none())
                    .encode(unknown, "decision", "result");
            throw new AssertionError("unknown value was accepted");
        } catch (IllegalArgumentException expected) {
            assert !unknown.stringified;
        }
    }
}
