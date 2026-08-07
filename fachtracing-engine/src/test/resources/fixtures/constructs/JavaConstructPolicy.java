package fixtures.constructs;

import at.gepardec.fachtracing.api.FachTracing;
import java.util.List;

public final class JavaConstructPolicy {
    @FachTracing("try resource decision")
    public boolean tryResource(String category) throws Exception {
        try (var resource = new TestResource()) {
            return category.length() > 3;
        }
    }

    @FachTracing("pattern decision")
    public boolean pattern(Object category) {
        return category instanceof String text && text.length() > 3;
    }

    @FachTracing("sealed decision")
    public boolean sealed(Rule rule, int amount) {
        return rule.accepts(amount);
    }

    @FachTracing("nested decision")
    public boolean nested(int age) {
        return AgeRule.accepts(age);
    }

    @FachTracing("method reference decision")
    public boolean reference(List<Integer> amounts) {
        return amounts.stream().anyMatch(this::eligible);
    }

    private boolean eligible(Integer amount) { return amount < 100; }

    public sealed interface Rule permits LocalRule, RegionalRule { boolean accepts(int amount); }
    public static final class LocalRule implements Rule {
        public boolean accepts(int amount) { return amount < 20; }
    }
    public static final class RegionalRule implements Rule {
        public boolean accepts(int amount) { return amount < 100; }
    }
    private static final class AgeRule {
        private static boolean accepts(int age) { return age >= 24; }
    }
    private static final class TestResource implements AutoCloseable {
        @Override public void close() { }
    }
}
