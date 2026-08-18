package at.gepardec.fachtracing.business;

import at.gepardec.fachtracing.model.BusinessLogicGraph;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Renders one selected business graph as ordered plain text. */
public final class BusinessExecutionTextRenderer {

    /** Renders decision, named result, selected flow, and coverage. */
    public String render(BusinessLogicGraph graph) {
        Objects.requireNonNull(graph, "graph");
        new BusinessLogicArtifactGuard().requireClean(graph);
        List<BusinessLogicGraph.Node> results = graph.nodes().stream()
                .filter(node -> node.kind() == BusinessLogicGraph.NodeKind.RESULT).toList();
        if (results.size() != 1) {
            throw new IllegalArgumentException("an evaluated business flow must have one result");
        }

        var output = new StringBuilder()
                .append("Decision: ").append(graph.decisionLabel()).append('\n')
                .append("Result: ").append(results.getFirst().label()).append('\n')
                .append("Flow:\n");
        for (BusinessLogicGraph.Node node : orderedNodes(graph)) {
            if (node.kind() == BusinessLogicGraph.NodeKind.RESULT) continue;
            output.append("- ").append(nodeLabel(node.kind())).append(": ").append(node.label());
            if (node.kind() == BusinessLogicGraph.NodeKind.RULE) {
                List<String> outcomes = graph.edges().stream()
                        .filter(edge -> edge.fromNodeId().equals(node.nodeId()))
                        .map(BusinessLogicGraph.Edge::outcome)
                        .filter(value -> !value.isBlank()).distinct().toList();
                if (!outcomes.isEmpty()) output.append(" — ").append(String.join(", ", outcomes));
            }
            output.append('\n');
        }
        output.append("Coverage: ")
                .append(graph.completeness().name().toLowerCase(java.util.Locale.ROOT)).append('\n');
        return output.toString();
    }

    private static List<BusinessLogicGraph.Node> orderedNodes(BusinessLogicGraph graph) {
        var orderedIds = new LinkedHashSet<String>();
        var queue = new ArrayDeque<>(graph.entryNodeIds());
        while (!queue.isEmpty()) {
            String nodeId = queue.removeFirst();
            if (!orderedIds.add(nodeId)) continue;
            graph.edges().stream().filter(edge -> edge.fromNodeId().equals(nodeId))
                    .map(BusinessLogicGraph.Edge::toNodeId).forEach(queue::addLast);
        }
        graph.nodes().stream().map(BusinessLogicGraph.Node::nodeId).forEach(orderedIds::add);
        return orderedIds.stream().map(graph::node).toList();
    }

    private static String nodeLabel(BusinessLogicGraph.NodeKind kind) {
        return switch (kind) {
            case RULE -> "Rule";
            case ACTION -> "Action";
            case GAP -> "Gap";
            case RESULT -> "Result";
        };
    }
}
