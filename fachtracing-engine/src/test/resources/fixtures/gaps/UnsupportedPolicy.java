package fixtures.gaps;

import at.gepardec.fachtracing.api.FachTracing;

public final class UnsupportedPolicy {
    @FachTracing("guarded approval")
    public boolean decide(DecisionService decisions, String value) {
        try {
            return decisions.approves(value);
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}

interface DecisionService {
    boolean approves(String value);
}
