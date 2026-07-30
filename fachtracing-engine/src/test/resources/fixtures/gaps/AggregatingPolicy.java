package fixtures.gaps;

import at.gepardec.fachtracing.api.FachTracing;

public final class AggregatingPolicy {
    @FachTracing("aggregate approval")
    public boolean decide(int[] scores) {
        int total = 0;
        for (int score : scores) {
            total += score;
        }
        return total >= 100;
    }
}
