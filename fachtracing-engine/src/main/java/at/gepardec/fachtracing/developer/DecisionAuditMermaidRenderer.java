package at.gepardec.fachtracing.developer;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.business.BusinessGraphAudit;
import at.gepardec.fachtracing.business.BusinessGraphProjection;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.BusinessLogicGraph;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Formats recorded analysis and projection decisions as compact deterministic Mermaid. */
public final class DecisionAuditMermaidRenderer {
    private static final int EXAMPLE_LIMIT = 3;
    private static final int EXAMPLE_LENGTH = 90;

    /** Renders source-relevance decisions and their exact graph relations. */
    public String analysis(AnalysisManifest.AnalysisResult analysis) {
        requireMatchingAnalysis(analysis);
        Map<String, BusinessDecisionGraph.DecisionNode> exactNodes = new LinkedHashMap<>();
        analysis.graph().nodes().forEach(node -> exactNodes.put(node.nodeId(), node));
        var diagram = new AuditDiagram(
                "Source constructs", "Analysis decisions", "Exact result-relevant graph");
        for (AnalysisManifest.AnalysisDecision decision : analysis.manifest().analysisDecisions()) {
            String sourceKey = sourceKey(decision);
            diagram.source(sourceKey, sourceHeading(decision), sourceExample(decision));
            String decisionKey = decision.action() + " / " + decision.reason();
            diagram.decision(decisionKey, decisionKey, analysisExample(decision, exactNodes));
            diagram.leftRelation(sourceKey, decisionKey);
            if (decision.nodeIds().isEmpty()) {
                diagram.target("NONE", "Not in exact graph", "",
                        decision.source() + ":" + decision.line() + ":" + decision.column());
                diagram.rightRelation(decisionKey, "NONE");
                continue;
            }
            for (String nodeId : decision.nodeIds()) {
                BusinessDecisionGraph.DecisionNode node = exactNodes.get(nodeId);
                if (node == null) {
                    throw new IllegalArgumentException(
                            "analysis decision refers to an unknown exact node: " + nodeId);
                }
                String targetKey = node.kind().name();
                diagram.target(targetKey, targetKey, node.businessLabel(), nodeId);
                diagram.rightRelation(decisionKey, targetKey);
            }
        }
        return diagram.render();
    }

    /** Renders exact-to-business projection decisions and their final output relations. */
    public String projection(BusinessGraphAudit audit) {
        Map<String, BusinessLogicGraph.Node> businessNodes = new LinkedHashMap<>();
        audit.graph().nodes().forEach(node -> businessNodes.put(node.nodeId(), node));
        var diagram = new AuditDiagram(
                "Exact graph inputs", "Projection decisions", "Final business graph");
        for (BusinessGraphProjection.Decision decision : audit.decisions()) {
            String sourceKey = decision.subjectKind() + " / " + decision.sourceKind();
            diagram.source(sourceKey, sourceKey, decision.sourceLabel());
            String decisionKey = decision.action() + " / " + decision.reason();
            diagram.decision(decisionKey, decisionKey, decision.sourceLabel());
            diagram.leftRelation(sourceKey, decisionKey);
            if (decision.businessNodeIds().isEmpty()) {
                diagram.target("NONE", "Not in business graph", "", decision.subjectId());
                diagram.rightRelation(decisionKey, "NONE");
                continue;
            }
            for (String nodeId : decision.businessNodeIds()) {
                BusinessLogicGraph.Node node = businessNodes.get(nodeId);
                if (node == null) {
                    throw new IllegalArgumentException(
                            "projection decision refers to an unknown business node: " + nodeId);
                }
                String targetKey = node.kind().name();
                diagram.target(targetKey, targetKey, node.label(), nodeId);
                diagram.rightRelation(decisionKey, targetKey);
            }
        }
        return diagram.render();
    }

    private static String sourceKey(AnalysisManifest.AnalysisDecision decision) {
        Path fileName = decision.source().getFileName();
        return (fileName == null ? decision.source() : fileName) + " / " + decision.constructKind();
    }

    private static String sourceHeading(AnalysisManifest.AnalysisDecision decision) {
        Path fileName = decision.source().getFileName();
        return (fileName == null ? decision.source() : fileName) + " / " + decision.constructKind();
    }

    private static String sourceExample(AnalysisManifest.AnalysisDecision decision) {
        String location = "line " + decision.line();
        return decision.subject().isBlank() ? location : location + ": " + decision.subject();
    }

    private static String analysisExample(
            AnalysisManifest.AnalysisDecision decision,
            Map<String, BusinessDecisionGraph.DecisionNode> exactNodes) {
        if (!decision.subject().isBlank()) return decision.subject();
        return decision.nodeIds().stream().map(exactNodes::get).filter(java.util.Objects::nonNull)
                .map(BusinessDecisionGraph.DecisionNode::businessLabel).findFirst().orElse("");
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

    private static String bounded(String value) {
        String clean = value.replaceAll("\\s+", " ").strip();
        if (clean.length() <= EXAMPLE_LENGTH) return clean;
        return clean.substring(0, EXAMPLE_LENGTH - 3).stripTrailing() + "...";
    }

    private static final class AuditDiagram {
        private final String sourceTitle;
        private final String decisionTitle;
        private final String targetTitle;
        private final Map<String, Bucket> sources = new LinkedHashMap<>();
        private final Map<String, Bucket> decisions = new LinkedHashMap<>();
        private final Map<String, Bucket> targets = new LinkedHashMap<>();
        private final Map<Relation, Integer> leftRelations = new LinkedHashMap<>();
        private final Map<Relation, Integer> rightRelations = new LinkedHashMap<>();

        private AuditDiagram(String sourceTitle, String decisionTitle, String targetTitle) {
            this.sourceTitle = sourceTitle;
            this.decisionTitle = decisionTitle;
            this.targetTitle = targetTitle;
        }

        private void source(String key, String heading, String example) {
            sources.computeIfAbsent(key, ignored -> new Bucket(heading, "inputs")).add(example);
        }

        private void decision(String key, String heading, String example) {
            decisions.computeIfAbsent(key, ignored -> new Bucket(heading, "decisions")).add(example);
        }

        private void target(String key, String heading, String example, String identity) {
            String noun = key.equals("NONE") ? "decisions" : "nodes";
            targets.computeIfAbsent(key, ignored -> new Bucket(heading, noun))
                    .addUnique(example, identity);
        }

        private void leftRelation(String source, String decision) {
            leftRelations.merge(new Relation(source, decision), 1, Integer::sum);
        }

        private void rightRelation(String decision, String target) {
            rightRelations.merge(new Relation(decision, target), 1, Integer::sum);
        }

        private String render() {
            var sourceAliases = aliases("s", sources);
            var decisionAliases = aliases("d", decisions);
            var targetAliases = aliases("t", targets);
            var output = new StringBuilder("flowchart TD\n");
            appendStage(output, "sources", sourceTitle, sources, sourceAliases, false);
            appendStage(output, "decisions", decisionTitle, decisions, decisionAliases, true);
            appendStage(output, "targets", targetTitle, targets, targetAliases, false);
            leftRelations.forEach((relation, count) -> appendRelation(
                    output, sourceAliases.get(relation.from()), decisionAliases.get(relation.to()), count));
            rightRelations.forEach((relation, count) -> appendRelation(
                    output, decisionAliases.get(relation.from()), targetAliases.get(relation.to()), count));
            return output.toString();
        }

        private static Map<String, String> aliases(String prefix, Map<String, Bucket> buckets) {
            var aliases = new LinkedHashMap<String, String>();
            int index = 0;
            for (String key : buckets.keySet()) aliases.put(key, prefix + (++index));
            return aliases;
        }

        private static void appendStage(
                StringBuilder output,
                String id,
                String title,
                Map<String, Bucket> buckets,
                Map<String, String> aliases,
                boolean decisionShape) {
            output.append("    subgraph ").append(id).append("[\"")
                    .append(escape(title)).append("\"]\n");
            if (buckets.isEmpty()) {
                output.append("        ").append(id).append("_empty([\"No recorded decisions\"])\n");
            } else {
                buckets.forEach((key, bucket) -> {
                    String alias = aliases.get(key);
                    output.append("        ").append(alias)
                            .append(decisionShape ? "{\"" : "[\"")
                            .append(bucket.label())
                            .append(decisionShape ? "\"}" : "\"]").append('\n');
                });
            }
            output.append("    end\n");
        }

        private static void appendRelation(
                StringBuilder output, String from, String to, int count) {
            output.append("    ").append(from).append(" -->|\"")
                    .append(count).append("\"| ").append(to).append('\n');
        }
    }

    private static final class Bucket {
        private final String heading;
        private final String noun;
        private final LinkedHashSet<String> examples = new LinkedHashSet<>();
        private final LinkedHashSet<String> identities = new LinkedHashSet<>();
        private int count;

        private Bucket(String heading, String noun) {
            this.heading = heading;
            this.noun = noun;
        }

        private void add(String example) {
            count++;
            String clean = bounded(example);
            if (!clean.isBlank() && examples.size() < EXAMPLE_LIMIT) examples.add(clean);
        }

        private void addUnique(String example, String identity) {
            if (!identities.add(identity)) return;
            add(example);
        }

        private String label() {
            var lines = new ArrayList<String>();
            lines.add(escape(heading));
            lines.add(noun + ": " + count);
            examples.forEach(example -> lines.add("example: " + escape(example)));
            if (count > examples.size()) lines.add("+ " + (count - examples.size()) + " more");
            return String.join("<br/>", lines);
        }
    }

    private record Relation(String from, String to) { }
}
