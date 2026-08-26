package at.gepardec.fachtracing.business;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.BusinessSemanticAttributes;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Reduces source-level semantic evidence to business-facing labels and materiality. */
final class BusinessSemanticReducer {
    private static final Pattern INFRASTRUCTURE_TYPE = Pattern.compile(
            ".*(?:adapter|repository|mapper|converter|controller|client|dao|entity|persistence|hibernate|jpa)$",
            Pattern.CASE_INSENSITIVE);
    private static final Set<String> MATERIAL_ACTIONS = Set.of(
            "add", "approve", "assign", "cancel", "create", "delete", "deny", "grant", "issue",
            "notify", "persist", "publish", "record", "register", "reject", "release", "remove",
            "save", "send", "speichere", "store", "submit", "update", "write");

    Reduction reduce(BusinessDecisionGraph.DecisionNode node) {
        String label = BusinessLanguageNormalizer.normalize(node.businessLabel());
        boolean languageRewritten = !label.equals(node.businessLabel().strip().replaceAll("\\s+", " "));
        Map<String, String> attributes = node.attributes();
        if (selector(node.kind(), label)) {
            return new Reduction(label, true, false, false);
        }
        if (node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE) {
            return predicate(label, attributes, languageRewritten);
        }
        if (node.kind() == BusinessDecisionGraph.NodeKind.COMPUTATION) {
            if (materialStatementCall(attributes)) {
                return new Reduction(actionLabel(attributes, label), false, false, false);
            }
            if (booleanBusinessCall(attributes)) {
                return new Reduction(withSourceValue(attributes, namedPredicate(attributes, label)),
                        false, false, true);
            }
            if (infrastructureOwner(attributes) || architectureImplementation(attributes)
                    || attributes.containsKey(BusinessSemanticAttributes.OWNER_TYPE)
                    || implementationWrapper(attributes)) {
                return new Reduction(label, true, false, false);
            }
        }
        if (architectureImplementation(attributes) || infrastructureOwner(attributes)) {
            return new Reduction(label, true, false, false);
        }
        return new Reduction(label, false, false, false);
    }

    private static Reduction predicate(
            String label, Map<String, String> attributes, boolean languageRewritten) {
        String method = attributes.getOrDefault(BusinessSemanticAttributes.CALL_METHOD, "");
        String receiver = businessSubject(attributes.getOrDefault(BusinessSemanticAttributes.RECEIVER, ""));
        String context = businessSubject(attributes.getOrDefault(
                BusinessSemanticAttributes.CONTEXT_SUBJECT, ""));
        String arguments = attributes.getOrDefault(BusinessSemanticAttributes.ARGUMENTS, "")
                .replace('|', ' ').strip();
        boolean negated = Boolean.parseBoolean(attributes.getOrDefault(
                BusinessSemanticAttributes.NEGATED, "false"));

        if (BusinessSemanticAttributes.AGGREGATE.equals(attributes.get(BusinessSemanticAttributes.ROLE))) {
            return new Reduction(label, false, false, false);
        }
        if (method.isBlank() && attributes.getOrDefault(
                BusinessSemanticAttributes.TREE_KIND, "").equals("LOGICAL_COMPLEMENT")) {
            return new Reduction(label, true, false, false);
        }
        if (method.equals("anyMatch") && label.contains(" that ")
                && (label.contains(" has ") || label.contains(" contains "))) {
            return new Reduction(label, false, false, false);
        }
        if (languageRewritten) {
            return new Reduction(withSourceValue(attributes, label), false, false, false);
        }

        if (method.equals("isEmpty") || method.equals("isBlank")) {
            String existence = subject(receiver, label, " is empty") + " exists";
            return new Reduction(withContext(context, existence), false, true, false);
        }
        if (method.equals("isPresent")) {
            String existence = subject(receiver, label, " is present") + " exists";
            return new Reduction(withContext(context, existence), false, false, false);
        }
        if (method.equals("isAfter") && negated && !receiver.isBlank() && !arguments.isBlank()) {
            return new Reduction(receiver + " is on or before " + arguments, false, false, false);
        }
        String lower = label.toLowerCase(Locale.ROOT);
        if (lower.endsWith(" is absent") || lower.endsWith(" is empty")) {
            int suffix = lower.endsWith(" is absent") ? " is absent".length() : " is empty".length();
            return new Reduction(label.substring(0, label.length() - suffix).strip() + " exists",
                    false, true, false);
        }
        if (lower.startsWith("not ")) {
            String positive = withSourceValue(
                    attributes, namedPredicate(attributes, label.substring(4).strip()));
            return new Reduction(positive, false, true, false);
        }
        String predicate = needsSemanticLabel(label) ? namedPredicate(attributes, label) : label;
        return new Reduction(withSourceValue(attributes, predicate),
                false, false, false);
    }

    private static String withSourceValue(Map<String, String> attributes, String label) {
        String value = attributes.getOrDefault(BusinessSemanticAttributes.SOURCE_VALUE, "").strip();
        if (value.isBlank() || Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(value)
                + "(?![\\p{L}\\p{N}])").matcher(label).find()) return label;
        return label + " (" + value + ")";
    }

    private static String namedPredicate(Map<String, String> attributes, String fallback) {
        String method = attributes.getOrDefault(BusinessSemanticAttributes.CALL_METHOD, "");
        if (method.isBlank() || genericLibraryPredicate(method)) return fallback;
        String predicate = words(method);
        String context = businessSubject(attributes.getOrDefault(
                BusinessSemanticAttributes.CONTEXT_SUBJECT, ""));
        if (!context.isBlank()) {
            String argumentType = Arrays.stream(attributes.getOrDefault(
                            BusinessSemanticAttributes.ARGUMENT_TYPES, "").split("\\|"))
                    .map(BusinessSemanticReducer::simpleName).map(BusinessSemanticReducer::words)
                    .filter(value -> !value.isBlank() && !value.startsWith("local date"))
                    .findFirst().orElse("");
            String object = argumentType.isBlank() || predicate.startsWith("ist ")
                    || predicate.startsWith("is ") || predicate.startsWith("hat ")
                    || predicate.startsWith("has ") ? "" : argumentType + " ";
            return context + " " + object + predicate;
        }
        String receiver = businessSubject(attributes.getOrDefault(BusinessSemanticAttributes.RECEIVER, ""));
        String arguments = attributes.getOrDefault(BusinessSemanticAttributes.ARGUMENTS, "")
                .replace('|', ' ').strip();
        boolean receiverRepeatsPredicate = !receiver.isBlank() && sharesBusinessWord(receiver, predicate);
        if (receiverRepeatsPredicate && !arguments.isBlank()) {
            receiver = businessSubject(arguments);
        }
        if (receiverRepeatsPredicate && !receiver.isBlank() && !predicate.contains(" ")) {
            return receiver + " has " + predicate;
        }
        if (!receiver.isBlank()) return receiver + " " + predicate;
        if (!arguments.isBlank()) return businessSubject(arguments.split(" ", 2)[0]) + " " + predicate;
        String owner = ownerSubject(attributes);
        return owner.isBlank() ? predicate : owner + " " + predicate;
    }

    private static boolean sharesBusinessWord(String left, String right) {
        Set<String> rightWords = Arrays.stream(right.split("\\s+")).collect(Collectors.toSet());
        return Arrays.stream(left.split("\\s+")).filter(word -> word.length() > 3).anyMatch(rightWords::contains);
    }

    private static boolean genericLibraryPredicate(String method) {
        return Set.of("equals", "contains", "isAfter", "isBefore", "isEqual", "isEmpty", "isBlank",
                "isPresent").contains(method);
    }

    private static String ownerSubject(Map<String, String> attributes) {
        String owner = simpleName(attributes.getOrDefault(BusinessSemanticAttributes.OWNER_TYPE, ""));
        String method = attributes.getOrDefault(BusinessSemanticAttributes.ENCLOSING_METHOD, "");
        String subject = words(owner);
        String role = words(method);
        if (!role.isBlank()) {
            subject = subject.replaceAll("(?i)(?:^|\\s)" + Pattern.quote(role) + "(?:$|\\s)", " ").strip();
        }
        return subject.replaceFirst("(?i)\\s+(?:rule|policy|service|implementation|impl)$", "").strip();
    }

    private static boolean selector(BusinessDecisionGraph.NodeKind kind, String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        return (kind == BusinessDecisionGraph.NodeKind.CHOICE && (lower.startsWith("choose by ")
                || lower.startsWith("select ")))
                || (kind == BusinessDecisionGraph.NodeKind.DISPATCH && lower.startsWith("select "));
    }

    private static boolean architectureImplementation(Map<String, String> attributes) {
        return BusinessSemanticAttributes.IMPLEMENTATION.equals(attributes.get(BusinessSemanticAttributes.ROLE))
                && infrastructureOwner(attributes);
    }

    private static boolean implementationWrapper(Map<String, String> attributes) {
        return BusinessSemanticAttributes.IMPLEMENTATION.equals(attributes.get(BusinessSemanticAttributes.ROLE));
    }

    private static boolean infrastructureOwner(Map<String, String> attributes) {
        String owner = simpleName(attributes.getOrDefault(BusinessSemanticAttributes.OWNER_TYPE, ""));
        return INFRASTRUCTURE_TYPE.matcher(owner).matches();
    }

    private static boolean materialStatementCall(Map<String, String> attributes) {
        if (!Boolean.parseBoolean(attributes.getOrDefault(BusinessSemanticAttributes.STATEMENT_CALL, "false"))) {
            return false;
        }
        String method = attributes.getOrDefault(BusinessSemanticAttributes.CALL_METHOD, "");
        String callOwner = attributes.getOrDefault(BusinessSemanticAttributes.CALL_OWNER_TYPE, "");
        if (callOwner.startsWith("java.") || callOwner.startsWith("javax.")
                || callOwner.startsWith("jakarta.")) return false;
        String firstWord = words(method).split(" ", 2)[0];
        return MATERIAL_ACTIONS.contains(firstWord);
    }

    private static boolean booleanBusinessCall(Map<String, String> attributes) {
        if (!attributes.getOrDefault(BusinessSemanticAttributes.CALL_RETURN_TYPE, "").equals("boolean")) {
            return false;
        }
        String callOwner = attributes.getOrDefault(BusinessSemanticAttributes.CALL_OWNER_TYPE, "");
        return !callOwner.startsWith("java.") && !callOwner.startsWith("javax.")
                && !callOwner.startsWith("jakarta.");
    }

    private static String actionLabel(Map<String, String> attributes, String fallback) {
        if (!needsSemanticLabel(fallback)) return fallback;
        String method = words(attributes.getOrDefault(BusinessSemanticAttributes.CALL_METHOD, ""));
        if (method.isBlank()) return fallback;
        String argumentTypes = attributes.getOrDefault(BusinessSemanticAttributes.ARGUMENT_TYPES, "");
        String object = Arrays.stream(argumentTypes.split("\\|"))
                .map(BusinessSemanticReducer::simpleName)
                .map(BusinessSemanticReducer::words)
                .filter(value -> !value.isBlank())
                .distinct().collect(Collectors.joining(" and "));
        if (object.isBlank()) {
            object = Arrays.stream(attributes.getOrDefault(BusinessSemanticAttributes.ARGUMENTS, "").split("\\|"))
                    .map(BusinessSemanticReducer::businessSubject).filter(value -> !value.isBlank())
                    .distinct().collect(Collectors.joining(" and "));
        }
        return object.isBlank() ? method : method + " " + object;
    }

    private static String subject(String receiver, String label, String suffix) {
        String lower = label.toLowerCase(Locale.ROOT);
        if (lower.endsWith(suffix)) {
            return label.substring(0, label.length() - suffix.length()).strip();
        }
        return receiver.isBlank() ? label : receiver;
    }

    private static boolean needsSemanticLabel(String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        return lower.startsWith("evaluate ") || lower.startsWith("derive ")
                || lower.startsWith("initialize ") || lower.startsWith("use ")
                || lower.equals("condition") || lower.equals("business condition");
    }

    private static String withContext(String context, String label) {
        return context.isBlank() || label.startsWith(context + " ") ? label : context + " " + label;
    }

    private static String businessSubject(String value) {
        return words(simpleName(value)).replaceFirst(
                "(?i)\\s+(?:port|adapter|repository|mapper|converter|controller|client|dao)$", "").strip();
    }

    private static String simpleName(String value) {
        String simple = value == null ? "" : value.strip();
        int generic = simple.indexOf('<');
        if (generic >= 0) simple = simple.substring(0, generic);
        int separator = Math.max(simple.lastIndexOf('.'), simple.lastIndexOf('$'));
        return separator < 0 ? simple : simple.substring(separator + 1);
    }

    private static String words(String value) {
        if (value == null || value.isBlank()) return "";
        return value.replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replace('_', ' ').replaceAll("\\s+", " ").toLowerCase(Locale.ROOT).strip();
    }

    record Reduction(String label, boolean technical, boolean invertOutcome, boolean promoteRule) { }
}
