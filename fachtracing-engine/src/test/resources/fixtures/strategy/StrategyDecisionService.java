package fixtures.strategy;

import at.gepardec.fachtracing.api.FachTracing;

public final class StrategyDecisionService {
    @FachTracing("delivery eligibility")
    public boolean decide(DecisionRule rule, int distance) {
        return rule.accepts(distance);
    }
}

interface DecisionRule {
    boolean accepts(int distance);
}

final class LocalRule implements DecisionRule {
    public boolean accepts(int distance) {
        return distance < 20;
    }
}

final class RegionalRule implements DecisionRule {
    public boolean accepts(int distance) {
        return distance < 100;
    }
}
