package at.gepardec.fachtracing.developer;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.business.BusinessGraphProjection;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.BusinessLogicGraph;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Formats recorded analysis and projection decisions as deterministic Mermaid. */
public final class DecisionAuditMermaidRenderer {
    /** Renders source-relevance decisions and their exact graph relations. */
    public String analysis(AnalysisManifest.AnalysisResult analysis) {
        requireMatchingAnalysis(analysis);
        Map<String, BusinessDecisionGraph.DecisionNode> exactNodes = new LinkedHashMap<>();
        analysis.graph().nodes().forEach(node -> exactNodes.put(node.nodeId(), node));
        var output = new StringBuilder("flowchart LR\n");
        for (int index = 0; index < analysis.manifest().analysisDecisions().size(); index++) {
            AnalysisManifest.AnalysisDecision decision =
                    analysis.manifest().analysisDecisions().get(index);
            String source = "as" + (index + 1);
            String classification = "ad" + (index + 1);
            output.append("    ").append(source).append("[\"")
                    .append(analysisSourceLabel(decision)).append("\"]\n")
                    .append("    ").append(classification).append("{\"")
                    .append(decision.action()).append(" / ").append(decision.reason())
                    .append("\"}\n")
                    .append("    ").append(source).append(" --> ").append(classification).append('\n');
            if (decision.nodeIds().isEmpty()) {
                String target = "an" + (index + 1);
                output.append("    ").append(target).append("([\"No exact graph node\"])\n")
                        .append("    ").append(classification).append(" -.-> ").append(target).append('\n');
                continue;
            }
            for (int targetIndex = 0; targetIndex < decision.nodeIds().size(); targetIndex++) {
                String nodeId = decision.nodeIds().get(targetIndex);
                BusinessDecisionGraph.DecisionNode node = exactNodes.get(nodeId);
                if (node == null) {
                    throw new IllegalArgumentException("analysis decision refers to an unknown exact node: " + nodeId);
                }
                String target = "ae" + (index + 1) + "_" + (targetIndex + 1);
                output.append("    ").append(target).append("[\"")
                        .append(escape(node.kind().name())).append("<br/>")
                        .append(escape(node.businessLabel())).append("\"]\n")
                        .append("    ").append(classification).append(" --> ").append(target).append('\n');
            }
        }
        return output.toString();
    }

    /** Renders exact-to-business projection decisions and their output relations. */
    public String projection(BusinessGraphProjection projection) {
        Map<String, BusinessLogicGraph.Node> businessNodes = new LinkedHashMap<>();
        projection.graph().nodes().forEach(node -> businessNodes.put(node.nodeId(), node));
        var output = new StringBuilder("flowchart LR\n");
        for (int index = 0; index < projection.decisions().size(); index++) {
            BusinessGraphProjection.Decision decision = projection.decisions().get(index);
            String source = "ps" + (index + 1);
            String classification = "pd" + (index + 1);
            output.append("    ").append(source).append("[\"")
                    .append(escape(decision.sourceKind())).append("<br/>")
                    .append(escape(decision.sourceLabel()))
                    .append("\"]\n")
                    .append("    ").append(classification).append("{\"")
                    .append(decision.action()).append(" / ").append(decision.reason())
                    .append("\"}\n")
                    .append("    ").append(source).append(" --> ").append(classification).append('\n');
            if (decision.businessNodeIds().isEmpty()) {
                String target = "pn" + (index + 1);
                output.append("    ").append(target).append("([\"Not in business graph\"])\n")
                        .append("    ").append(classification).append(" -.-> ").append(target).append('\n');
                continue;
            }
            for (int targetIndex = 0; targetIndex < decision.businessNodeIds().size(); targetIndex++) {
                String nodeId = decision.businessNodeIds().get(targetIndex);
                BusinessLogicGraph.Node node = businessNodes.get(nodeId);
                if (node == null) {
                    throw new IllegalArgumentException(
                            "projection decision refers to an unknown business node: " + nodeId);
                }
                String target = "pb" + (index + 1) + "_" + (targetIndex + 1);
                output.append("    ").append(target).append("[\"")
                        .append(escape(node.kind().name())).append("<br/>")
                        .append(escape(node.label())).append("\"]\n")
                        .append("    ").append(classification).append(" --> ").append(target).append('\n');
            }
        }
        return output.toString();
    }

    private static String analysisSourceLabel(AnalysisManifest.AnalysisDecision decision) {
        Path fileName = decision.source().getFileName();
        String location = (fileName == null ? decision.source() : fileName)
                + ":" + decision.line() + ":" + decision.column();
        String label = escape(decision.constructKind()) + "<br/>" + escape(location);
        if (decision.subject().isBlank()) return label;
        return label + "<br/>" + escape(decision.subject());
    }

    private static void requireMatchingAnalysis(AnalysisManifest.AnalysisResult analysis) {
        if (!analysis.graph().graphId().equals(analysis.manifest().graphId())
                || analysis.graph().version() != analysis.manifest().graphVersion()) {
            throw new IllegalArgumentException("graph and analysis manifest versions do not match");
        }
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;")
                .replace("<", "&lt;").replace(">", "&gt;")
                .replace("\r", " ").replace("\n", "<br/>");
    }
}
