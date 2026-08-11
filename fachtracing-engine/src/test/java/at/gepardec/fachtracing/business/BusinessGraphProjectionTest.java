package at.gepardec.fachtracing.business;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.BusinessLogicGraph;

import java.util.List;
import java.util.Map;

/** Executable contracts for the business graph projection and formats. */
public final class BusinessGraphProjectionTest {
    private BusinessGraphProjectionTest() { }

    public static void main(String[] args) {
        foldsLoopMechanicsIntoOneRule();
        preservesRulesActionsReturnsAndFailures();
        preservesIncompleteAnalysisAsBusinessGap();
        acceptsBusinessVocabularyThatContainsStructuralWords();
        rejectsTechnicalVocabulary();
        exportsAllFormatsWithOneTopology();
    }

    private static void foldsLoopMechanicsIntoOneRule() {
        BusinessDecisionGraph exact = graph("pet search", BusinessDecisionGraph.Completeness.COMPLETE,
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        node("loop", BusinessDecisionGraph.NodeKind.PREDICATE, "for each pet in pets"),
                        node("derive", BusinessDecisionGraph.NodeKind.COMPUTATION, "derive comp name as pet name"),
                        node("match", BusinessDecisionGraph.NodeKind.PREDICATE,
                                "comp name equals ignore case requested name"),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge("e1", "start", "loop", "next"),
                        edge("e2", "loop", "derive", "item"),
                        edge("e3", "derive", "match", "next"),
                        edge("e4", "match", "stop", "true; returns pet"),
                        edge("e5", "match", "loop", "next item"),
                        edge("e6", "loop", "stop", "done; returns absent")),
                List.of());
        AnalysisManifest.AnalysisResult analysis = analysis(exact);

        BusinessLogicGraph projected = new BusinessGraphProjector().project(analysis);

        assert analysis.graph().equals(exact) : "projection changed the exact graph";
        assert projected.completeness() == BusinessLogicGraph.Completeness.COMPLETE : projected;
        assert projected.nodes().stream().filter(node -> node.kind() == BusinessLogicGraph.NodeKind.RULE)
                .map(BusinessLogicGraph.Node::label).toList()
                .equals(List.of("a pet with this name exists")) : projected.nodes();
        assert projected.nodes().stream().filter(node -> node.kind() == BusinessLogicGraph.NodeKind.RESULT)
                .map(BusinessLogicGraph.Node::label).toList()
                .equals(List.of("pet", "no matching result")) : projected.nodes();
        assert projected.edges().stream().map(BusinessLogicGraph.Edge::outcome).toList()
                .containsAll(List.of("yes", "no")) : projected.edges();
    }

    private static void preservesRulesActionsReturnsAndFailures() {
        BusinessDecisionGraph exact = graph("visit booking", BusinessDecisionGraph.Completeness.COMPLETE,
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        node("errors", BusinessDecisionGraph.NodeKind.PREDICATE, "visit has validation errors"),
                        node("save", BusinessDecisionGraph.NodeKind.COMPUTATION, "save visit"),
                        node("failure", BusinessDecisionGraph.NodeKind.COMPUTATION, "decision cannot continue"),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge("e1", "start", "errors", "next"),
                        edge("e2", "errors", "stop", "true; returns correction required"),
                        edge("e3", "errors", "save", "false"),
                        edge("e4", "save", "stop", "returns booking confirmed"),
                        edge("e5", "start", "failure", "next"),
                        edge("e6", "failure", "stop", "fails")),
                List.of());

        BusinessLogicGraph projected = new BusinessGraphProjector().project(analysis(exact));

        assert projected.nodes().stream().map(BusinessLogicGraph.Node::kind).toList()
                .containsAll(List.of(BusinessLogicGraph.NodeKind.RULE,
                        BusinessLogicGraph.NodeKind.ACTION, BusinessLogicGraph.NodeKind.RESULT));
        assert projected.nodes().stream().anyMatch(node -> node.kind() == BusinessLogicGraph.NodeKind.ACTION
                && node.label().equals("save visit")) : projected.nodes();
        assert projected.nodes().stream().filter(node -> node.kind() == BusinessLogicGraph.NodeKind.RESULT)
                .map(BusinessLogicGraph.Node::label).toList()
                .containsAll(List.of("correction required", "booking confirmed", "operation failed"))
                : projected.nodes();
    }

    private static void preservesIncompleteAnalysisAsBusinessGap() {
        BusinessDecisionGraph exact = graph("registration", BusinessDecisionGraph.Completeness.INCOMPLETE,
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        node("gap", BusinessDecisionGraph.NodeKind.COVERAGE_GAP,
                                "analysis incomplete: binary method contains an unsupported call instruction"),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(edge("e1", "start", "gap", "unresolved"),
                        edge("e2", "gap", "stop", "returns response")),
                List.of(new BusinessDecisionGraph.CoverageGap("gap", "unsupported bytecode")));

        BusinessLogicGraph projected = new BusinessGraphProjector().project(analysis(exact));

        assert projected.completeness() == BusinessLogicGraph.Completeness.INCOMPLETE : projected;
        assert projected.nodes().stream().anyMatch(node -> node.kind() == BusinessLogicGraph.NodeKind.GAP
                && node.label().equals("analysis could not determine a required rule")) : projected.nodes();
    }

    private static void rejectsTechnicalVocabulary() {
        for (String prohibited : List.of("Start", "for each item", "derive temporary value",
                "redirect:/owners/12", "true", "Policy.java")) {
            var graph = new BusinessLogicGraph("guard", 1, "guard", List.of("node"),
                    List.of(new BusinessLogicGraph.Node(
                            "node", BusinessLogicGraph.NodeKind.RESULT, prohibited)),
                    List.of(), BusinessLogicGraph.Completeness.COMPLETE);
            try {
                new BusinessLogicArtifactGuard().requireClean(graph);
                throw new AssertionError("technical label was accepted: " + prohibited);
            } catch (IllegalArgumentException expected) {
                assert expected.getMessage().contains(prohibited) : expected.getMessage();
            }
        }
    }

    private static void acceptsBusinessVocabularyThatContainsStructuralWords() {
        var graph = new BusinessLogicGraph("business-words", 1, "business words", List.of("time"),
                List.of(
                        new BusinessLogicGraph.Node("time", BusinessLogicGraph.NodeKind.ACTION,
                                "set from date to today start time"),
                        new BusinessLogicGraph.Node("location", BusinessLogicGraph.NodeKind.RULE,
                                "preferred bus stop is open")),
                List.of(), BusinessLogicGraph.Completeness.COMPLETE);

        new BusinessLogicArtifactGuard().requireClean(graph);
    }

    private static void exportsAllFormatsWithOneTopology() {
        BusinessDecisionGraph exact = graph("approval", BusinessDecisionGraph.Completeness.COMPLETE,
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        node("rule", BusinessDecisionGraph.NodeKind.PREDICATE, "application is eligible"),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge("yes", "start", "rule", "next"),
                        edge("approved", "rule", "stop", "true; returns approved"),
                        edge("declined", "rule", "stop", "false; returns declined")),
                List.of());
        BusinessLogicGraph graph = new BusinessGraphProjector().project(analysis(exact));
        String mermaid = new BusinessMermaidRenderer().render(graph);
        String plantUml = new BusinessPlantUmlRenderer().render(graph);
        String json = new BusinessGraphJsonExporter().export(graph);
        String schema = new BusinessGraphJsonSchema().generate();

        for (BusinessLogicGraph.Node node : graph.nodes()) {
            assert mermaid.contains(node.label()) : mermaid;
            assert plantUml.contains(node.label()) : plantUml;
            assert json.contains("\"id\":\"" + node.nodeId() + "\"") : json;
        }
        assert json.startsWith("{\"schema\":\"fachtracing-business-graph/v1\"") : json;
        assert schema.contains("\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"") : schema;
        assert schema.contains("\"$id\": \"fachtracing-business-graph/v1\"") : schema;
        assert !mermaid.contains("Start") && !mermaid.contains("Stop") : mermaid;
        assert !json.contains("\"kind\":\"ENTRY\"") : json;
    }

    private static AnalysisManifest.AnalysisResult analysis(BusinessDecisionGraph graph) {
        var manifest = new AnalysisManifest(graph.graphId(), graph.version(), Map.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
        return new AnalysisManifest.AnalysisResult(graph, manifest, List.of());
    }

    private static BusinessDecisionGraph graph(
            String label,
            BusinessDecisionGraph.Completeness completeness,
            List<BusinessDecisionGraph.DecisionNode> nodes,
            List<BusinessDecisionGraph.DecisionEdge> edges,
            List<BusinessDecisionGraph.CoverageGap> gaps) {
        return new BusinessDecisionGraph("graph-" + label.replace(' ', '-'), 1, label, "start",
                nodes, edges, completeness, gaps);
    }

    private static BusinessDecisionGraph.DecisionNode node(
            String id, BusinessDecisionGraph.NodeKind kind, String label) {
        return new BusinessDecisionGraph.DecisionNode(id, kind, label, Map.of());
    }

    private static BusinessDecisionGraph.DecisionEdge edge(
            String id, String from, String to, String outcome) {
        return new BusinessDecisionGraph.DecisionEdge(id, from, to, outcome);
    }
}
