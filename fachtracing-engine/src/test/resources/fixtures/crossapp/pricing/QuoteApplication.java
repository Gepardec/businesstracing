package fixtures.crossapp.pricing;

import at.gepardec.fachtracing.api.FachTracing;

import java.util.Comparator;

public final class QuoteApplication {
    @FachTracing("quote ranking")
    public int rank(boolean lowestFirst) {
        Comparator<Quote> cmp = lowestFirst
                ? Comparator.comparingInt(Quote::amount)
                : Comparator.comparingInt(Quote::amount).reversed();
        return cmp.compare(new Quote(10), new Quote(20));
    }

    private record Quote(int amount) { }
}
