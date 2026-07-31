package example.reactor;

import at.gepardec.fachtracing.api.FachTracing;

public final class DecisionEntry {
    @FachTracing("reactor approval")
    public boolean approve(DecisionRule rule, int amount) {
        return rule.accepts(amount);
    }
}
