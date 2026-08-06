package at.gepardec.fachtracing.analysis;

/** Removes generic Java construction and helper-role terms from business labels. */
public final class BusinessLabelNormalizer {
    private BusinessLabelNormalizer() { }

    /** Returns a stable business label without Java construction vocabulary. */
    public static String normalize(String value) {
        String label = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        label = label.replaceAll("(?i)\\benum\\s+type\\b", "");
        label = label.replaceAll("(?i)\\bvalidator\\b", "");
        label = label.replaceFirst("(?i)^initialize\\s+(?:new\\s+)?", "");
        label = label.replaceFirst("(?i)^evaluate\\s+create\\s+", "create ");
        label = label.replaceFirst("(?i)\\s+with\\s*$", "");
        label = label.replaceAll("\\s+", " ").trim();
        return label.isBlank() ? "business decision" : label;
    }
}
