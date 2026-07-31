package fixtures.reactor;

import at.gepardec.fachtracing.api.FachTracing;

final class LocalDecisionRule implements DecisionRule {
    public boolean accepts(int amount) {
        return amount < 20;
    }

    @FachTracing("sibling entry")
    boolean siblingEntry(int amount) {
        return amount == 10;
    }
}
