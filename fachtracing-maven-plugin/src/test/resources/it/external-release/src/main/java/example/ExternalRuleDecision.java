package example;

import at.gepardec.fachtracing.api.FachTracing;

public final class ExternalRuleDecision {
    @FachTracing("external rule approval")
    public boolean approve(ExternalRule rule, int amount) {
        return rule.accepts(amount);
    }
}
