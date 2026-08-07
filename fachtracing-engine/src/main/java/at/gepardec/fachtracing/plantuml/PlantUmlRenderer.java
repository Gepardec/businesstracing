package at.gepardec.fachtracing.plantuml;

import at.gepardec.fachtracing.diagram.ExecutionPathResolver;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;

import java.util.HashMap;
import java.util.Set;

/** Produces dependency-free deterministic PlantUML source for structure and one execution. */
public final class PlantUmlRenderer {
    /** Renders every static node and edge, including visible coverage gaps. */
    public String structure(BusinessDecisionGraph graph) {
        return render(graph, null);
    }

    /** Renders the full graph while distinguishing visited and unvisited paths. */
    public String execution(BusinessDecisionGraph graph, DecisionExecution execution) {
        if (!graph.graphId().equals(execution.graphId()) || graph.version() != execution.graphVersion()) {
            throw new IllegalArgumentException("graph and execution versions do not match");
        }
        return render(graph, execution);
    }

    private String render(BusinessDecisionGraph graph, DecisionExecution execution) {
        var aliases = new HashMap<String, String>();
        var output = new StringBuilder("@startuml\n")
                .append("title ").append(escape(graph.decisionLabel())).append("\n")
                .append("left to right direction\n");
        for (int index = 0; index < graph.nodes().size(); index++) {
            var node = graph.nodes().get(index);
            String alias = "n" + (index + 1);
            aliases.put(node.nodeId(), alias);
            output.append(shape(node.kind())).append(" \"").append(escape(node.businessLabel()))
                    .append("\" as ").append(alias).append("\n");
        }

        Set<String> visitedEdges = execution == null ? Set.of()
                : ExecutionPathResolver.visitedEdges(graph, execution);
        for (var edge : graph.edges()) {
            output.append(aliases.get(edge.fromNodeId())).append(' ');
            if (execution == null) output.append("-->");
            else if (visitedEdges.contains(edge.edgeId())) output.append("-[#2E7D32,thickness=3]->");
            else output.append("-[#9E9E9E,dashed]->");
            output.append(' ').append(aliases.get(edge.toNodeId()));
            String outcome = displayOutcome(edge.outcome());
            if (!outcome.isBlank()) output.append(" : ").append(escape(outcome));
            output.append('\n');
        }
        if (graph.completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE) {
            output.append("note bottom\nThis decision graph is incomplete");
            for (var gap : graph.coverageGaps()) output.append("\n- ").append(escape(gap.description()));
            output.append("\nend note\n");
        }
        return output.append("@enduml\n").toString();
    }

    private static String displayOutcome(String outcome) {
        return outcome.equals("next") ? "" : outcome;
    }

    private static String shape(BusinessDecisionGraph.NodeKind kind) {
        return switch (kind) {
            case ENTRY, OUTCOME -> "usecase";
            case PREDICATE, CHOICE, DISPATCH -> "diamond";
            case COVERAGE_GAP -> "hexagon";
            case COMPUTATION -> "rectangle";
        };
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", " ").replace("\n", "\\n");
    }
}
