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
        usesTheFoldedLoopResultForTheFollowingBusinessBranch();
        preservesRulesActionsReturnsAndFailures();
        preservesIncompleteAnalysisAsBusinessGap();
        acceptsBusinessVocabularyThatContainsStructuralWords();
        removesJavaExpressionsFromBusinessProjection();
        removesTechnicalDataBuildingFromBusinessProjection();
        rejectsTechnicalVocabulary();
        producesStableBusinessIdsForEquivalentExactGraphs();
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

    private static void usesTheFoldedLoopResultForTheFollowingBusinessBranch() {
        BusinessDecisionGraph exact = graph("pet registration", BusinessDecisionGraph.Completeness.COMPLETE,
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        node("loop", BusinessDecisionGraph.NodeKind.PREDICATE, "for each pet in pets"),
                        node("match", BusinessDecisionGraph.NodeKind.PREDICATE,
                                "pet name equals requested name"),
                        node("wrapper", BusinessDecisionGraph.NodeKind.PREDICATE,
                                "pet name and true exists"),
                        node("duplicate", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "record field validation error"),
                        node("date", BusinessDecisionGraph.NodeKind.PREDICATE,
                                "pet birth date is in the future"),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge("e1", "start", "loop", "next"),
                        edge("e2", "loop", "match", "item"),
                        edge("e3", "match", "wrapper", "true"),
                        edge("e4", "match", "loop", "next item"),
                        edge("e5", "loop", "wrapper", "done"),
                        edge("e6", "wrapper", "duplicate", "true"),
                        edge("e7", "wrapper", "date", "false"),
                        edge("e8", "duplicate", "date", "next"),
                        edge("e9", "date", "stop", "true; returns correction required"),
                        edge("e10", "date", "stop", "false; returns completed")),
                List.of());

        BusinessLogicGraph projected = new BusinessGraphProjector().project(analysis(exact));
        String loop = projected.nodes().stream()
                .filter(node -> node.label().equals("a pet with this name exists"))
                .map(BusinessLogicGraph.Node::nodeId).findFirst().orElseThrow();
        String duplicate = projected.nodes().stream()
                .filter(node -> node.label().equals("record field validation error"))
                .map(BusinessLogicGraph.Node::nodeId).findFirst().orElseThrow();
        String date = projected.nodes().stream()
                .filter(node -> node.label().equals("pet birth date is in the future"))
                .map(BusinessLogicGraph.Node::nodeId).findFirst().orElseThrow();
        assert projected.edges().stream().anyMatch(edge -> edge.fromNodeId().equals(loop)
                && edge.toNodeId().equals(duplicate) && edge.outcome().equals("yes")) : projected.edges();
        assert projected.edges().stream().anyMatch(edge -> edge.fromNodeId().equals(loop)
                && edge.toNodeId().equals(date) && edge.outcome().equals("no")) : projected.edges();
        assert projected.edges().stream().noneMatch(edge -> edge.fromNodeId().equals(loop)
                && edge.toNodeId().equals(duplicate) && edge.outcome().equals("no")) : projected.edges();
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
                "redirect:/owners/12", "true", "Policy.java", "null", "identifier",
                "failure ex", "message to lower case", "users evaluator::can view")) {
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

    private static void producesStableBusinessIdsForEquivalentExactGraphs() {
        String first = new BusinessGraphJsonExporter().export(
                new BusinessGraphProjector().project(analysis(equivalentExactGraph("first"))));
        String second = new BusinessGraphJsonExporter().export(
                new BusinessGraphProjector().project(analysis(equivalentExactGraph("second"))));
        assert first.equals(second) : first + "\n" + second;
    }

    private static BusinessDecisionGraph equivalentExactGraph(String prefix) {
        return new BusinessDecisionGraph(prefix + "-graph", 1, "approval", prefix + "-start",
                List.of(
                        node(prefix + "-start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        node(prefix + "-rule", BusinessDecisionGraph.NodeKind.PREDICATE,
                                "application is eligible"),
                        node(prefix + "-stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge(prefix + "-e1", prefix + "-start", prefix + "-rule", "next"),
                        edge(prefix + "-e2", prefix + "-rule", prefix + "-stop", "true; returns approved"),
                        edge(prefix + "-e3", prefix + "-rule", prefix + "-stop", "false; returns declined")),
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
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

    private static void removesJavaExpressionsFromBusinessProjection() {
        BusinessDecisionGraph exact = graph("search users", BusinessDecisionGraph.Completeness.COMPLETE,
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        node("first", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "set first result to first result != absent ? first result : -1"),
                        node("maximum", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "set max results to max results != absent ? max results : constants default max results"),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge("e1", "start", "first", "next"),
                        edge("e2", "first", "maximum", "next"),
                        edge("e3", "maximum", "stop",
                                "returns search for user new hash map<> and realm and evaluator and first result and max results and false")),
                List.of());

        BusinessLogicGraph projected = new BusinessGraphProjector().project(analysis(exact));

        assert projected.nodes().stream().map(BusinessLogicGraph.Node::label).toList()
                .equals(List.of("search users completed")) : projected.nodes();
        new BusinessLogicArtifactGuard().requireClean(projected);
    }

    private static void removesTechnicalDataBuildingFromBusinessProjection() {
        BusinessDecisionGraph exact = graph("search users", BusinessDecisionGraph.Completeness.COMPLETE,
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        node("attributes", BusinessDecisionGraph.NodeKind.COMPUTATION, "attributes"),
                        node("put", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "put attributes with email and email"),
                        node("filter", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "set user models to items in user models match users evaluator::can view"),
                        node("rule", BusinessDecisionGraph.NodeKind.PREDICATE, "search exists"),
                        node("hold", BusinessDecisionGraph.NodeKind.COMPUTATION, "put order on hold"),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge("e1", "start", "attributes", "next"),
                        edge("e2", "attributes", "put", "next"),
                        edge("e3", "put", "filter", "next"),
                        edge("e4", "filter", "rule", "next"),
                        edge("e5", "rule", "hold", "true"),
                        edge("e6", "hold", "stop", "returns matching users"),
                        edge("e7", "rule", "stop", "false; returns all users")),
                List.of());

        BusinessLogicGraph projected = new BusinessGraphProjector().project(analysis(exact));
        List<String> labels = projected.nodes().stream().map(BusinessLogicGraph.Node::label).toList();

        assert labels.containsAll(List.of("search exists", "put order on hold")) : labels;
        assert labels.stream().noneMatch(label -> label.equals("attributes")
                || label.startsWith("put attributes with ") || label.contains("::")) : labels;
        new BusinessLogicArtifactGuard().requireClean(projected);
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
