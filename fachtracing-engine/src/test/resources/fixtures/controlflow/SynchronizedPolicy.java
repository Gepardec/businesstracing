package fixtures.controlflow;

import at.gepardec.fachtracing.api.FachTracing;

public final class SynchronizedPolicy {
    private final Object monitor = new Object();

    @FachTracing("synchronized eligibility")
    public boolean decide(int age, String location) {
        synchronized (monitor) {
            if (age < 24) {
                return false;
            }
            return location.equals("Vienna");
        }
    }
}
