package example;

import at.gepardec.fachtracing.api.FachTracing;

public final class ExternalDecision {
    @FachTracing("external approval")
    public boolean approve(int age, String location) {
        return age >= 24 && "Vienna".equals(location);
    }
}
