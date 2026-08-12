package at.gepardec.fachtracing.agent;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Strict opt-in automatic activation and output paths. */
record AgentOptions(Path activation, Path output) {
    /** Parses empty legacy arguments or the complete automatic-output pair. */
    static Optional<AgentOptions> parse(String arguments) {
        if (arguments == null || arguments.isBlank()) return Optional.empty();
        Map<String, String> values = new LinkedHashMap<>();
        for (String item : arguments.split(",", -1)) {
            int separator = item.indexOf('=');
            if (separator < 1 || separator == item.length() - 1) {
                throw new IllegalArgumentException(
                        "agent options must use activation=<path>,output=<path>");
            }
            String key = item.substring(0, separator).trim();
            String value = item.substring(separator + 1).trim();
            if (!key.equals("activation") && !key.equals("output")) {
                throw new IllegalArgumentException("unknown Fachtracing agent option: " + key);
            }
            if (values.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("duplicate Fachtracing agent option: " + key);
            }
        }
        if (!values.keySet().equals(java.util.Set.of("activation", "output"))) {
            throw new IllegalArgumentException(
                    "agent options must include both activation and output paths");
        }
        return Optional.of(new AgentOptions(
                normalized(values.get("activation")), normalized(values.get("output"))));
    }

    private static Path normalized(String value) {
        if (value.isBlank()) throw new IllegalArgumentException("agent option path must not be blank");
        return Path.of(value).toAbsolutePath().normalize();
    }
}
