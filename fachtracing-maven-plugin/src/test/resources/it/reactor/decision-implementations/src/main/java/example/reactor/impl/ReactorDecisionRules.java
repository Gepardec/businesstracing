package example.reactor.impl;

import example.reactor.DecisionRule;

final class LocalDecisionRule implements DecisionRule {
    public boolean accepts(int amount) {
        return amount < 20;
    }
}

final class RegionalDecisionRule implements DecisionRule {
    public boolean accepts(int amount) {
        return amount < 100;
    }
}
