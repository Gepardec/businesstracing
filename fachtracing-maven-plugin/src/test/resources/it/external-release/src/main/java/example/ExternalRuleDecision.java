package example;

import at.gepardec.fachtracing.api.FachTracing;
import example.rules.ExternalRules;

public final class ExternalRuleDecision {
    @FachTracing("external rule approval")
    public boolean approve(int amount) {
        return ExternalRules.accepts(amount);
    }
}
