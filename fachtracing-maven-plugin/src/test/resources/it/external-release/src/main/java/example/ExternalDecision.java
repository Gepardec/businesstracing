package example;

import at.gepardec.fachtracing.api.FachTracing;

public final class ExternalDecision {
    private java.util.concurrent.Future<?> pending;

    @FachTracing("external approval")
    public boolean approve(int age, String location) {
        if (age < 24) return false;
        if ("Vienna".equals(location)) return true;
        return false;
    }

    @FachTracing("external cancellation")
    public boolean submit(java.util.concurrent.ExecutorService executor, int age) {
        pending = executor.submit(() -> age >= 24);
        return age >= 24;
    }

    public java.util.concurrent.Future<?> pending() {
        return pending;
    }
}
