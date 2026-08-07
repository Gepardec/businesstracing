package example.reactor;

public final class ExternalDecisionRule implements DecisionRule {
    @Override public boolean accepts(int amount) {
        return amount < 500;
    }
}
