package fixtures.gaps;

import at.gepardec.fachtracing.api.FachTracing;

public final class OpaqueValuePolicy {
    @FachTracing("opaque external value")
    public Object decide(ValueSource source) {
        return source.value();
    }
}

interface ValueSource {
    Object value();
}
