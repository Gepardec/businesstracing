package fixtures.reactor;

final class RegionalDecisionRule implements DecisionRule {
    public boolean accepts(int amount) {
        return amount < 100;
    }
}
