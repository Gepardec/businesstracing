package fixtures.controlflow;

import at.gepardec.fachtracing.api.FachTracing;

public final class ResourceFailurePolicy {
    @FachTracing("resource failure decision")
    public boolean decide(boolean closeFails) {
        try (var ignored = new DecisionResource(closeFails)) {
            return true;
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    private record DecisionResource(boolean closeFails) implements AutoCloseable {
        @Override public void close() {
            if (closeFails) throw new IllegalStateException();
        }
    }
}
