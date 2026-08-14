package at.gepardec.fachtracing.business;

import at.gepardec.fachtracing.model.BusinessLogicGraph;

import java.util.LinkedHashMap;

/** Renders one business-only graph as Mermaid flowchart text. */
public final class BusinessMermaidRenderer {
    /** Renders all business nodes and relationships. */
    public String render(BusinessLogicGraph graph) {
        new BusinessLogicArtifactGuard().requireClean(graph);
        var aliases = new LinkedHashMap<String, String>();
        var output = new StringBuilder("flowchart LR\n")
                .append("    subgraph decision[\"")
                .append(escape(graph.decisionLabel())).append("\"]\n");
        for (int index = 0; index < graph.nodes().size(); index++) {
            BusinessLogicGraph.Node node = graph.nodes().get(index);
            String alias = "n" + (index + 1);
            aliases.put(node.nodeId(), alias);
            output.append("        ").append(shape(alias, node.kind(), node.label())).append('\n');
        }
        for (BusinessLogicGraph.Edge edge : graph.edges()) {
            output.append("        ").append(aliases.get(edge.fromNodeId())).append(" -->");
            if (!edge.outcome().isBlank()) {
                output.append("|\"").append(escape(edge.outcome())).append("\"|");
            }
            output.append(' ').append(aliases.get(edge.toNodeId())).append('\n');
        }
        output.append("    end\n");
        return output.toString();
    }

    private static String shape(String alias, BusinessLogicGraph.NodeKind kind, String label) {
        String text = "\"" + escape(label) + "\"";
        return switch (kind) {
            case RULE -> alias + "{" + text + "}";
            case ACTION -> alias + "[" + text + "]";
            case RESULT -> alias + "([" + text + "])";
            case GAP -> alias + "{{" + text + "}}";
        };
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;")
                .replace("<", "&lt;").replace(">", "&gt;")
                .replace("\r", " ").replace("\n", "<br/>");
    }
}
