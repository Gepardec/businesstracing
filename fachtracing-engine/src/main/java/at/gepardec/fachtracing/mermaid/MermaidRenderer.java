package at.gepardec.fachtracing.mermaid;

import at.gepardec.fachtracing.diagram.ExecutionPathResolver;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Produces deterministic dependency-free Mermaid flowchart source. */
public final class MermaidRenderer {
    /** Renders every static business node and edge with visible coverage gaps. */
    public String structure(BusinessDecisionGraph graph) {
        return render(graph, null);
    }

    /** Renders the full graph while emphasizing the path taken by one execution. */
    public String execution(BusinessDecisionGraph graph, DecisionExecution execution) {
        if (!graph.graphId().equals(execution.graphId()) || graph.version() != execution.graphVersion()) {
            throw new IllegalArgumentException("graph and execution versions do not match");
        }
        return render(graph, execution);
    }

    private String render(BusinessDecisionGraph graph, DecisionExecution execution) {
        var aliases = new HashMap<String, String>();
        var output = new StringBuilder("flowchart LR\n");
        for (int index = 0; index < graph.nodes().size(); index++) {
            var node = graph.nodes().get(index);
            String alias = "n" + (index + 1);
            aliases.put(node.nodeId(), alias);
            output.append("    ").append(node(alias, node.kind(), node.businessLabel())).append('\n');
        }

        Set<String> visitedEdges = execution == null ? Set.of()
                : ExecutionPathResolver.visitedEdges(graph, execution);
        var visitedNodes = new HashSet<String>();
        var visitedLinks = new ArrayList<Integer>();
        var unvisitedLinks = new ArrayList<Integer>();
        for (int index = 0; index < graph.edges().size(); index++) {
            var edge = graph.edges().get(index);
            boolean visited = visitedEdges.contains(edge.edgeId());
            (visited ? visitedLinks : unvisitedLinks).add(index);
            if (visited) {
                visitedNodes.add(aliases.get(edge.fromNodeId()));
                visitedNodes.add(aliases.get(edge.toNodeId()));
            }
            output.append("    ").append(aliases.get(edge.fromNodeId())).append(" -->");
            if (!edge.outcome().isBlank()) output.append("|\"").append(escape(edge.outcome())).append("\"|");
            output.append(' ').append(aliases.get(edge.toNodeId())).append('\n');
        }
        if (graph.completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE) {
            output.append("    coverage[\"Incomplete analysis");
            for (var gap : graph.coverageGaps()) output.append("<br/>- ").append(escape(gap.description()));
            output.append("\"]\n");
        }
        if (execution != null) {
            if (!visitedNodes.isEmpty()) output.append("    class ")
                    .append(visitedNodes.stream().sorted().collect(Collectors.joining(",")))
                    .append(" visited\n");
            if (!visitedLinks.isEmpty()) output.append("    linkStyle ").append(join(visitedLinks))
                    .append(" stroke-width:3px\n");
            if (!unvisitedLinks.isEmpty()) output.append("    linkStyle ").append(join(unvisitedLinks))
                    .append(" stroke-dasharray:5 5,opacity:0.45\n");
            output.append("    classDef visited stroke-width:3px\n");
        }
        return output.toString();
    }

    private static String node(String alias, BusinessDecisionGraph.NodeKind kind, String label) {
        String text = "\"" + escape(label) + "\"";
        return switch (kind) {
            case ENTRY, OUTCOME -> alias + "([" + text + "])";
            case PREDICATE, CHOICE, DISPATCH -> alias + "{" + text + "}";
            case COVERAGE_GAP -> alias + "{{" + text + "}}";
            case COMPUTATION -> alias + "[" + text + "]";
        };
    }

    private static String join(java.util.List<Integer> indices) {
        return indices.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;")
                .replace("<", "&lt;").replace(">", "&gt;")
                .replace("\r", " ").replace("\n", "<br/>");
    }
}
