package fixtures.gaps;

import at.gepardec.fachtracing.api.FachTracing;

public final class UnsupportedPolicy {
    @FachTracing("guarded approval")
    public boolean decide(String value) {
        try {
            return Integer.parseInt(value) > 10;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
