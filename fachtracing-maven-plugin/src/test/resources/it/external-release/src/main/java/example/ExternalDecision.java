package example;

import at.gepardec.fachtracing.api.FachTracing;

public final class ExternalDecision {
    @FachTracing("external approval")
    public boolean approve(int age, String location) {
        if (age < 24) return false;
        if ("Vienna".equals(location)) return true;
        return false;
    }
}
