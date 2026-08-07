package example.reactor;

import at.gepardec.fachtracing.api.FachTracing;

public final class ImportedDecision {
    @FachTracing("imported approval")
    public boolean approve(int amount) {
        return amount < 250;
    }
}
