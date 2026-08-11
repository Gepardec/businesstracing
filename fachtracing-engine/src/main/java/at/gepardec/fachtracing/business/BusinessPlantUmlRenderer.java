package at.gepardec.fachtracing.business;

import at.gepardec.fachtracing.model.BusinessLogicGraph;

import java.util.LinkedHashMap;

/** Renders one business-only graph as PlantUML text. */
public final class BusinessPlantUmlRenderer {
    /** Renders all business nodes and relationships. */
    public String render(BusinessLogicGraph graph) {
        new BusinessLogicArtifactGuard().requireClean(graph);
        var aliases = new LinkedHashMap<String, String>();
        var output = new StringBuilder("@startuml\n")
                .append("title ").append(escape(graph.decisionLabel())).append('\n')
                .append("left to right direction\n");
        for (int index = 0; index < graph.nodes().size(); index++) {
            BusinessLogicGraph.Node node = graph.nodes().get(index);
            String alias = "n" + (index + 1);
            aliases.put(node.nodeId(), alias);
            output.append(shape(node.kind())).append(" \"").append(escape(node.label()))
                    .append("\" as ").append(alias).append('\n');
        }
        for (BusinessLogicGraph.Edge edge : graph.edges()) {
            output.append(aliases.get(edge.fromNodeId())).append(" --> ")
                    .append(aliases.get(edge.toNodeId()));
            if (!edge.outcome().isBlank()) output.append(" : ").append(escape(edge.outcome()));
            output.append('\n');
        }
        return output.append("@enduml\n").toString();
    }

    private static String shape(BusinessLogicGraph.NodeKind kind) {
        return switch (kind) {
            case RULE -> "diamond";
            case ACTION -> "rectangle";
            case RESULT -> "usecase";
            case GAP -> "hexagon";
        };
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", " ").replace("\n", "\\n");
    }
}
