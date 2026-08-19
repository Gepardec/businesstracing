package at.gepardec.fachtracing.business;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts analysis labels to clear business language. */
final class BusinessLanguageNormalizer {
    private static final Pattern MODEL_COLLECTION = Pattern.compile("\\b([a-z]+) models\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMPTY_NEGATION = Pattern.compile("(?i)^not (.+) is empty$");
    private static final Pattern FILTER_PERMISSION = Pattern.compile("(?i)^filter (.+) by can (.+)$");
    private static final Pattern FILTER_RULE = Pattern.compile("(?i)^filter (.+) by (.+)$");
    private static final Pattern MAPPED_TRANSFER =
            Pattern.compile("(?i)^add map ([a-z]+) \\1 (.+) to (.+)$");
    private static final Pattern MAP_SETTER = Pattern.compile("(?i)^map (.+) using set (.+)$");
    private static final Pattern MAP_CALLBACK = Pattern.compile("(?i)^map (.+) using (.+)$");
    private static final Pattern SEARCH_STREAM = Pattern.compile("(?i)^search for (.+) stream$");
    private static final Pattern SPLIT_TERMS = Pattern.compile("(?i)^.+ split terms (.+)$");
    private static final Pattern SET_VALUE = Pattern.compile("(?i)^set (.+) to (.+)$");
    private static final Pattern GRANT_WITHOUT_PERMISSION =
            Pattern.compile("(?i)^grant(?: (.+))? if no permission$");

    private BusinessLanguageNormalizer() { }

    static String normalize(String label) {
        String value = Objects.requireNonNull(label, "label").strip()
                .replaceAll("(?i)^analysis incomplete:\\s*", "")
                .replaceAll("(?i)\\bcomp(?:arison)?\\s+", "")
                .replaceAll("\\s+", " ");
        value = value.replaceAll("(?i)^not (.+) is after local date now$", "$1 is today or earlier")
                .replaceAll("(?i)^(.+) is after current date$", "$1 is in the future")
                .replaceAll("(?i)^is not duplicate .+ violation ex$",
                        "persistence failure is not a duplicate record");
        value = replaceModelCollections(value);

        Matcher match = EMPTY_NEGATION.matcher(value);
        if (match.matches()) return existence(match.group(1));
        match = FILTER_PERMISSION.matcher(value);
        if (match.matches()) return "keep " + match.group(1) + " with " + match.group(2) + " permission";
        match = FILTER_RULE.matcher(value);
        if (match.matches()) return "keep " + match.group(1) + " that satisfy the " + match.group(2) + " rule";
        match = MAPPED_TRANSFER.matcher(value);
        if (match.matches()) {
            return "add converted " + match.group(1) + " " + match.group(2) + " to " + match.group(3);
        }
        match = MAP_SETTER.matcher(value);
        if (match.matches()) return "set " + match.group(2) + " for " + inputItems(match.group(1));
        match = MAP_CALLBACK.matcher(value);
        if (match.matches()) return callbackAction(match.group(2), inputItems(match.group(1)));
        match = SEARCH_STREAM.matcher(value);
        if (match.matches()) return "search for " + plural(match.group(1));
        match = SET_VALUE.matcher(value);
        if (match.matches() && sameSubject(match.group(1), match.group(2))) return "set " + match.group(1);
        match = GRANT_WITHOUT_PERMISSION.matcher(value);
        if (match.matches()) {
            String subject = match.group(1) == null ? "" : match.group(1) + " ";
            return "grant " + subject + "access when no explicit permission applies";
        }
        if (value.isBlank()) return "business condition";
        return value;
    }

    private static String replaceModelCollections(String value) {
        Matcher matcher = MODEL_COLLECTION.matcher(value);
        var result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(plural(matcher.group(1))));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String inputItems(String value) {
        Matcher matcher = SPLIT_TERMS.matcher(value);
        return matcher.matches() ? matcher.group(1) + " terms" : value;
    }

    private static String callbackAction(String callback, String input) {
        return callback.equalsIgnoreCase("lookup")
                ? "look up " + input
                : "apply " + callback + " to " + input;
    }

    private static String existence(String subject) {
        String lower = subject.toLowerCase(java.util.Locale.ROOT);
        boolean plural = lower.endsWith("criteria") || lower.endsWith("s")
                && !lower.endsWith("ss") && !lower.endsWith("us") && !lower.endsWith("is");
        return subject + (plural ? " exist" : " exists");
    }

    private static boolean sameSubject(String first, String second) {
        return singular(first).equalsIgnoreCase(singular(second));
    }

    private static String singular(String value) {
        return value.endsWith("s") && value.length() > 1
                ? value.substring(0, value.length() - 1) : value;
    }

    private static String plural(String value) {
        if (value.endsWith("s")) return value;
        if (value.endsWith("y") && value.length() > 1
                && "aeiou".indexOf(Character.toLowerCase(value.charAt(value.length() - 2))) < 0) {
            return value.substring(0, value.length() - 1) + "ies";
        }
        return value + "s";
    }
}
