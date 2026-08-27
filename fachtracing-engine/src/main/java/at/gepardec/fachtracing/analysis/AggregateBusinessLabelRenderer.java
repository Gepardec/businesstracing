package at.gepardec.fachtracing.analysis;

/** Joins source-derived aggregate roles without adding a language-specific sentence grammar. */
final class AggregateBusinessLabelRenderer {
    private AggregateBusinessLabelRenderer() { }

    static String render(String subject, String collection, String condition, String qualifier) {
        String rule = collection.strip() + ": " + condition.strip();
        if (qualifier != null && !qualifier.isBlank()) rule += " — " + qualifier.strip();
        return subject == null || subject.isBlank() ? rule : subject.strip() + " — " + rule;
    }
}
