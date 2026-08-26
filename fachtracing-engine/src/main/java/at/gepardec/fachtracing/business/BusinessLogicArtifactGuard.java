package at.gepardec.fachtracing.business;

import at.gepardec.fachtracing.model.BusinessLogicGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Rejects implementation vocabulary in business-only artifacts. */
public final class BusinessLogicArtifactGuard {
    private static final List<Pattern> PROHIBITED = List.of(
            Pattern.compile("^start$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^stop$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bfor each\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\brepeat while\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bnext item\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bderive\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bevaluate\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bmap\\b|\\bfilter\\b|\\busing lookup\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b[a-z]+ models\\b|\\bsession attribute\\b|\\bstream\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bevaluator\\b|\\brepresentation\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\btemporary\\b|\\btemp(?:orary)? value\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcomp(?:arison)? [a-z]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("decision result path|alternative result|unresolved", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^choose by\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:adapter|repository|mapper|converter|controller|dao) rule\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("redirect:|forward:|/owners/|/pets/|/visits/", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(^|\\s)(true|false)($|\\s)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b[A-Za-z_$][A-Za-z0-9_$]*\\.java\\b"),
            Pattern.compile("java\\.|org\\.springframework\\.|jakarta\\.", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bnull\\b|\\bidentifiers?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bex\\b|to lower case", Pattern.CASE_INSENSITIVE),
            Pattern.compile("==|!=|&&|\\|\\||->|::|[{};]"));
    private static final Pattern NEGATIVE_RULE = Pattern.compile("^not\\s", Pattern.CASE_INSENSITIVE);

    /** Returns all prohibited labels and outcomes. */
    public List<String> violations(BusinessLogicGraph graph) {
        var violations = new ArrayList<String>();
        check("decision", graph.decisionLabel(), violations);
        for (BusinessLogicGraph.Node node : graph.nodes()) {
            check("node " + node.nodeId(), node.label(), violations);
            if (node.kind() == BusinessLogicGraph.NodeKind.RULE
                    && NEGATIVE_RULE.matcher(node.label()).find()) {
                violations.add("node " + node.nodeId() + ": " + node.label());
            }
        }
        for (BusinessLogicGraph.Edge edge : graph.edges()) {
            check("edge " + edge.edgeId(), edge.outcome(), violations);
        }
        return List.copyOf(violations);
    }

    /** Throws when a graph contains prohibited vocabulary. */
    public void requireClean(BusinessLogicGraph graph) {
        List<String> violations = violations(graph);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("business graph contains technical vocabulary: " + violations);
        }
    }

    private static void check(String location, String value, List<String> violations) {
        String text = value.toLowerCase(Locale.ROOT);
        for (Pattern pattern : PROHIBITED) {
            if (pattern.matcher(text).find()) {
                violations.add(location + ": " + value);
                return;
            }
        }
    }
}
