package fixtures.gaps;

import at.gepardec.fachtracing.api.FachTracing;

public final class ExternalDecisionPolicy {
    @FachTracing("external score decision")
    public boolean decide(ScoreService scores, String customerId) {
        return scores.approves(customerId);
    }
}

interface ScoreService {
    boolean approves(String customerId);
}
