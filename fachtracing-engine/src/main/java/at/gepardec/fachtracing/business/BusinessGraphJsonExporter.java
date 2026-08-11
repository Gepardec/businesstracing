package at.gepardec.fachtracing.business;

import at.gepardec.fachtracing.model.BusinessLogicGraph;

/** Exports deterministic dependency-free business graph JSON. */
public final class BusinessGraphJsonExporter {
    /** Stable business graph schema identifier. */
    public static final String SCHEMA = "fachtracing-business-graph/v1";

    /** Exports one graph document. */
    public String export(BusinessLogicGraph graph) {
        new BusinessLogicArtifactGuard().requireClean(graph);
        var output = new StringBuilder(2048).append('{');
        field(output, "schema", SCHEMA).append(',');
        field(output, "graphId", graph.graphId()).append(',');
        output.append("\"version\":").append(graph.version()).append(',');
        field(output, "decision", graph.decisionLabel()).append(',');
        field(output, "completeness", graph.completeness().name()).append(',');
        output.append("\"entryNodeIds\":[");
        for (int index = 0; index < graph.entryNodeIds().size(); index++) {
            if (index > 0) output.append(',');
            string(output, graph.entryNodeIds().get(index));
        }
        output.append("],\"nodes\":[");
        for (int index = 0; index < graph.nodes().size(); index++) {
            if (index > 0) output.append(',');
            BusinessLogicGraph.Node node = graph.nodes().get(index);
            output.append('{');
            field(output, "id", node.nodeId()).append(',');
            field(output, "kind", node.kind().name()).append(',');
            field(output, "label", node.label());
            output.append('}');
        }
        output.append("],\"edges\":[");
        for (int index = 0; index < graph.edges().size(); index++) {
            if (index > 0) output.append(',');
            BusinessLogicGraph.Edge edge = graph.edges().get(index);
            output.append('{');
            field(output, "id", edge.edgeId()).append(',');
            field(output, "from", edge.fromNodeId()).append(',');
            field(output, "to", edge.toNodeId()).append(',');
            field(output, "outcome", edge.outcome());
            output.append('}');
        }
        return output.append("]}\n").toString();
    }

    private static StringBuilder field(StringBuilder output, String name, String value) {
        string(output, name).append(':');
        return string(output, value);
    }

    private static StringBuilder string(StringBuilder output, String value) {
        output.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (character < 0x20) output.append(String.format("\\u%04x", (int) character));
                    else output.append(character);
                }
            }
        }
        return output.append('"');
    }
}
