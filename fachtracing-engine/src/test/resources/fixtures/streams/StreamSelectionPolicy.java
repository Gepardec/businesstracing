package fixtures.streams;

import at.gepardec.fachtracing.api.FachTracing;

import java.util.List;

public final class StreamSelectionPolicy {
    @FachTracing("select long labels")
    public List<String> select(List<String> labels) {
        return labels.stream()
                .filter(label -> label.length() > 3)
                .toList();
    }
}
