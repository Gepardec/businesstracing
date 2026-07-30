package fixtures.aggregation;

import at.gepardec.fachtracing.api.FachTracing;

import java.util.ArrayList;
import java.util.List;

public final class StrategyAggregationPolicy {
    interface Rule<T> { List<T> evaluate(int amount); }

    static final class LimitRule implements Rule<String> {
        public List<String> evaluate(int amount) {
            return amount > 100 ? List.of("limit exceeded") : List.of();
        }
    }

    static final class ReviewRule implements Rule<String> {
        public List<String> evaluate(int amount) {
            return amount == 42 ? List.of("manual review") : List.of();
        }
    }

    static final class NumericRule implements Rule<Integer> {
        public List<Integer> evaluate(int amount) {
            return List.of(amount);
        }
    }

    private final List<Rule<String>> rules = List.of(new LimitRule(), new ReviewRule());

    @FachTracing("determine notices")
    public List<String> notices(int amount) {
        List<String> notices = new ArrayList<>();
        rules.forEach(rule -> notices.addAll(rule.evaluate(amount)));
        notices.sort(String::compareTo);
        return notices;
    }
}
