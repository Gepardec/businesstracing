package fixtures.strategy;

import at.gepardec.fachtracing.api.FachTracing;

public final class StrategyDecisionService {
    @FachTracing("delivery eligibility")
    public boolean decide(DecisionRule rule, int distance) {
        return rule.accepts(distance);
    }

    @FachTracing("scoped delivery eligibility")
    public boolean decideScoped(ScopedBaseRule rule, int distance) {
        return rule.accepts(distance);
    }
}

sealed interface DecisionRule permits AbstractDecisionRule, RegionalRule {
    boolean accepts(int distance);
}

abstract non-sealed class AbstractDecisionRule implements DecisionRule { }

final class LocalRule extends AbstractDecisionRule {
    public boolean accepts(int distance) {
        return distance < 20;
    }
}

final class RegionalRule implements DecisionRule {
    public boolean accepts(int distance) {
        return distance < 100;
    }
}

sealed interface ScopedRule permits ScopedBaseRule, RemoteScopedRule {
    boolean accepts(int distance);
}

abstract non-sealed class ScopedBaseRule implements ScopedRule { }

final class LocalScopedRule extends ScopedBaseRule {
    public boolean accepts(int distance) {
        return distance < 20;
    }
}

final class RemoteScopedRule implements ScopedRule {
    public boolean accepts(int distance) {
        return distance < 200;
    }
}
