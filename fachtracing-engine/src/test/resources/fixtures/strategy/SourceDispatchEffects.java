package fixtures.strategy;

import at.gepardec.fachtracing.api.FachTracing;
import java.util.List;

public final class SourceDispatchEffects {
    @FachTracing("source dispatch effects")
    public boolean decide(ValueSource source, Request request) {
        for (Integer value : source.values(request)) {
            if (value > 0) return true;
        }
        return false;
    }
}

interface ValueSource {
    List<Integer> values(Request request);
}

final class FixedValueSource implements ValueSource {
    @Override
    public List<Integer> values(Request request) {
        return List.of(request.value());
    }
}

record Request(int value) { }
