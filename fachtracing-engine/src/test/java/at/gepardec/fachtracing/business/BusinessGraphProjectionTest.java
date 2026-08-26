package at.gepardec.fachtracing.business;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.BusinessSemanticAttributes;
import at.gepardec.fachtracing.model.BusinessLogicGraph;
import at.gepardec.fachtracing.model.DecisionExecution;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Executable contracts for the business graph projection and formats. */
public final class BusinessGraphProjectionTest {
    private BusinessGraphProjectionTest() { }

    public static void main(String[] args) {
        removesArchitectureAndSelectorMechanics();
        statesNegativeChecksAsPositiveBusinessRules();
        keepsOnlyMaterialCallerActions();
        foldsLoopMechanicsIntoOneRule();
        usesTheFoldedLoopResultForTheFollowingBusinessBranch();
        preservesRulesActionsReturnsAndFailures();
        preservesIncompleteAnalysisAsBusinessGap();
        acceptsBusinessVocabularyThatContainsStructuralWords();
        removesJavaExpressionsFromBusinessProjection();
        removesTechnicalDataBuildingFromBusinessProjection();
        rewritesGenericCollectionOperationsForBusinessReaders();
        preservesTraceabilityAcrossHiddenExactNodes();
        collapsesConnectedGapRegionsAndEquivalentStates();
        selectsDifferentBusinessFlowsForDifferentExecutions();
        showsOnlyGapsOnTheSelectedPath();
        connectsObservedSegmentsThroughASafeGap();
        keepsResultTerminalWhenRuntimeGapHasNoRules();
        changesGeneratedOutputWhenBusinessBehaviorChanges();
        rejectsExecutionFromAnotherGraphVersion();
        rejectsTechnicalVocabulary();
        producesStableBusinessIdsForEquivalentExactGraphs();
        exportsAllFormatsWithOneTopology();
    }

    private static void removesArchitectureAndSelectorMechanics() {
        BusinessDecisionGraph exact = graph("generic eligibility", BusinessDecisionGraph.Completeness.COMPLETE,
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        semanticNode("dispatch", BusinessDecisionGraph.NodeKind.DISPATCH,
                                "select applicable decision rule", Map.of()),
                        semanticNode("adapter", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "person adapter rule", Map.of(
                                        BusinessSemanticAttributes.OWNER_TYPE, "example.PersonAdapter",
                                        BusinessSemanticAttributes.ROLE, BusinessSemanticAttributes.IMPLEMENTATION)),
                        semanticNode("choice", BusinessDecisionGraph.NodeKind.CHOICE, "choose by person number",
                                Map.of(BusinessSemanticAttributes.OWNER_TYPE, "example.PersonAdapter")),
                        semanticNode("rule", BusinessDecisionGraph.NodeKind.PREDICATE, "person exists", Map.of(
                                BusinessSemanticAttributes.OWNER_TYPE, "example.Eligibility",
                                BusinessSemanticAttributes.ENCLOSING_METHOD, "isEligible")),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge("e1", "start", "dispatch", "next"),
                        edge("e2", "dispatch", "adapter", "selected rule"),
                        edge("e3", "adapter", "choice", "next"),
                        edge("e4", "choice", "rule", "selected"),
                        edge("e5", "rule", "stop", "true; returns approved"),
                        edge("e6", "rule", "stop", "false; returns declined")), List.of());

        BusinessLogicGraph projected = new BusinessGraphProjector().project(analysis(exact));

        assert projected.nodes().stream().map(BusinessLogicGraph.Node::label).toList()
                .equals(List.of("person exists", "approved", "declined")) : projected.nodes();
    }

    private static void statesNegativeChecksAsPositiveBusinessRules() {
        BusinessDecisionGraph exact = graph("submission window", BusinessDecisionGraph.Completeness.COMPLETE,
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        semanticNode("empty", BusinessDecisionGraph.NodeKind.PREDICATE, "employment is empty", Map.of(
                                BusinessSemanticAttributes.CALL_METHOD, "isEmpty",
                                BusinessSemanticAttributes.RECEIVER, "employment")),
                        semanticNode("date", BusinessDecisionGraph.NodeKind.PREDICATE,
                                "not submitted at is after deadline", Map.of(
                                        BusinessSemanticAttributes.CALL_METHOD, "isAfter",
                                        BusinessSemanticAttributes.RECEIVER, "submitted at",
                                        BusinessSemanticAttributes.ARGUMENTS, "deadline",
                                        BusinessSemanticAttributes.NEGATED, "true")),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge("e1", "start", "empty", "next"),
                        edge("e2", "empty", "stop", "true; returns not eligible"),
                        edge("e3", "empty", "date", "false"),
                        edge("e4", "date", "stop", "true; returns eligible"),
                        edge("e5", "date", "stop", "false; returns too late")), List.of());

        BusinessLogicGraph projected = new BusinessGraphProjector().project(analysis(exact));

        assert projected.nodes().stream().anyMatch(node -> node.label().equals("employment exists"))
                : projected.nodes();
        assert projected.nodes().stream().anyMatch(node -> node.label().equals(
                "submitted at is on or before deadline")) : projected.nodes();
        String employment = projected.nodes().stream().filter(node -> node.label().equals("employment exists"))
                .map(BusinessLogicGraph.Node::nodeId).findFirst().orElseThrow();
        assert projected.edges().stream().anyMatch(edge -> edge.fromNodeId().equals(employment)
                && edge.outcome().equals("no")) : projected.edges();
    }

    private static void keepsOnlyMaterialCallerActions() {
        BusinessDecisionGraph exact = graph("notification decision", BusinessDecisionGraph.Completeness.COMPLETE,
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        semanticNode("unit", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "convert duration to seconds", Map.of(
                                        BusinessSemanticAttributes.OWNER_TYPE, "example.DurationConverter")),
                        semanticNode("query", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "execute hibernate query", Map.of(
                                        BusinessSemanticAttributes.OWNER_TYPE, "example.NotificationRepository")),
                        semanticNode("save", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "evaluate save", Map.of(
                                        BusinessSemanticAttributes.CALL_METHOD, "save",
                                        BusinessSemanticAttributes.RECEIVER, "notification port",
                                        BusinessSemanticAttributes.ARGUMENTS, "notification",
                                        BusinessSemanticAttributes.ARGUMENT_TYPES, "example.Notification",
                                        BusinessSemanticAttributes.STATEMENT_CALL, "true")),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge("e1", "start", "unit", "next"),
                        edge("e2", "unit", "query", "next"),
                        edge("e3", "query", "save", "next"),
                        edge("e4", "save", "stop", "returns saved")), List.of());

        BusinessLogicGraph projected = new BusinessGraphProjector().project(analysis(exact));

        assert projected.nodes().stream().map(BusinessLogicGraph.Node::label).toList()
                .equals(List.of("save notification", "saved")) : projected.nodes();
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
                "failure ex", "message to lower case", "users evaluator::can view",
                "map orders using lookup", "user models", "account stream", "user representation")) {
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
        for (String prohibited : List.of(
                "choose by route id", "not submitted after deadline", "person adapter rule")) {
            var graph = new BusinessLogicGraph("semantic-guard", 1, "guard", List.of("node"),
                    List.of(new BusinessLogicGraph.Node(
                            "node", BusinessLogicGraph.NodeKind.RULE, prohibited)),
                    List.of(), BusinessLogicGraph.Completeness.COMPLETE);
            assert !new BusinessLogicArtifactGuard().violations(graph).isEmpty() : prohibited;
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

    private static void rewritesGenericCollectionOperationsForBusinessReaders() {
        BusinessDecisionGraph exact = graph("prepare records", BusinessDecisionGraph.Completeness.COMPLETE,
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        node("terms", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "map parser split terms request using lookup"),
                        node("view", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "filter account models by can view"),
                        node("access", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "map order models using set access"),
                        node("groups", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "set groups to group"),
                        node("search", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "search for account stream"),
                        node("grant", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "grant if no permission"),
                        node("attributes", BusinessDecisionGraph.NodeKind.PREDICATE,
                                "not request attributes is empty"),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge("e1", "start", "terms", "next"),
                        edge("e2", "terms", "view", "next"),
                        edge("e3", "view", "access", "next"),
                        edge("e4", "access", "groups", "next"),
                        edge("e5", "groups", "search", "next"),
                        edge("e6", "search", "grant", "next"),
                        edge("e7", "grant", "attributes", "next"),
                        edge("e8", "attributes", "stop", "true; returns prepared"),
                        edge("e9", "attributes", "stop", "false; returns unchanged")),
                List.of());

        BusinessLogicGraph projected = new BusinessGraphProjector().project(analysis(exact));
        List<String> labels = projected.nodes().stream().map(BusinessLogicGraph.Node::label).toList();

        assert labels.containsAll(List.of(
                "look up request terms",
                "keep accounts with view permission",
                "set access for orders",
                "set groups",
                "search for accounts",
                "grant access when no explicit permission applies",
                "request attributes exist")) : labels;
        new BusinessLogicArtifactGuard().requireClean(projected);
    }

    private static void preservesTraceabilityAcrossHiddenExactNodes() {
        BusinessDecisionGraph exact = graph("shipment release", BusinessDecisionGraph.Completeness.COMPLETE,
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        node("rule", BusinessDecisionGraph.NodeKind.PREDICATE, "shipment is approved"),
                        node("temporary-a", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "derive first temporary route value"),
                        node("temporary-b", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "derive second temporary route value"),
                        node("release", BusinessDecisionGraph.NodeKind.COMPUTATION, "release shipment"),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge("e1", "start", "rule", "next"),
                        edge("e2", "rule", "temporary-a", "true"),
                        edge("e3", "temporary-a", "release", "next"),
                        edge("e4", "rule", "temporary-b", "true"),
                        edge("e5", "temporary-b", "release", "next"),
                        edge("e6", "release", "stop", "returns released"),
                        edge("e7", "rule", "stop", "false; returns held")),
                List.of());

        BusinessGraphProjection projection = new BusinessGraphProjector().projectTraceable(exact);
        BusinessLogicGraph graph = projection.graph();
        String rule = projection.businessNodeIdsByExactNodeId().get("rule");
        String action = projection.businessNodeIdsByExactNodeId().get("release");
        String released = projection.businessResultNodeIdsByExactEdgeId().get("e6");
        String held = projection.businessResultNodeIdsByExactEdgeId().get("e7");

        assert rule != null && graph.node(rule).label().equals("shipment is approved") : projection;
        assert action != null && graph.node(action).label().equals("release shipment") : projection;
        assert !projection.businessNodeIdsByExactNodeId().containsKey("temporary-a") : projection;
        assert !projection.businessNodeIdsByExactNodeId().containsKey("temporary-b") : projection;
        assert released != null && graph.node(released).label().equals("released") : projection;
        assert held != null && graph.node(held).label().equals("held") : projection;
        BusinessLogicGraph.Edge ruleToAction = graph.edges().stream()
                .filter(edge -> edge.fromNodeId().equals(rule) && edge.toNodeId().equals(action))
                .findFirst().orElseThrow();
        assert projection.exactEdgePathsByBusinessEdgeId().get(ruleToAction.edgeId())
                .equals(List.of(List.of("e2", "e3"), List.of("e4", "e5"))) : projection;
        BusinessLogicGraph.Edge actionToResult = graph.edges().stream()
                .filter(edge -> edge.fromNodeId().equals(action) && edge.toNodeId().equals(released))
                .findFirst().orElseThrow();
        assert projection.exactEdgePathsByBusinessEdgeId().get(actionToResult.edgeId())
                .equals(List.of(List.of("e6"))) : projection;
    }

    private static void collapsesConnectedGapRegionsAndEquivalentStates() {
        var graph = new BusinessLogicGraph("summary", 1, "fulfil order", List.of("rule-a", "rule-b"),
                List.of(
                        businessNode("rule-a", BusinessLogicGraph.NodeKind.RULE, "order is eligible"),
                        businessNode("rule-b", BusinessLogicGraph.NodeKind.RULE, "order is eligible"),
                        businessNode("gap-a", BusinessLogicGraph.NodeKind.GAP,
                                "analysis could not determine a required rule"),
                        businessNode("gap-b", BusinessLogicGraph.NodeKind.GAP,
                                "analysis could not determine a required rule"),
                        businessNode("gap-c", BusinessLogicGraph.NodeKind.GAP,
                                "analysis could not determine a required rule"),
                        businessNode("result", BusinessLogicGraph.NodeKind.RESULT, "order accepted")),
                List.of(
                        businessEdge("e1", "rule-a", "gap-a", "yes"),
                        businessEdge("e2", "rule-b", "gap-a", "yes"),
                        businessEdge("e3", "gap-a", "gap-b", ""),
                        businessEdge("e4", "gap-a", "gap-c", ""),
                        businessEdge("e5", "gap-b", "result", ""),
                        businessEdge("e6", "gap-c", "result", "")),
                BusinessLogicGraph.Completeness.INCOMPLETE);

        BusinessLogicGraph summary = new BusinessGraphSummarizer().summarize(graph);

        assert summary.nodes().stream().filter(node -> node.kind() == BusinessLogicGraph.NodeKind.RULE)
                .count() == 1 : summary;
        assert summary.nodes().stream().filter(node -> node.kind() == BusinessLogicGraph.NodeKind.GAP)
                .count() == 1 : summary;
        String rule = summary.nodes().stream().filter(node -> node.kind() == BusinessLogicGraph.NodeKind.RULE)
                .findFirst().orElseThrow().nodeId();
        String gap = summary.nodes().stream().filter(node -> node.kind() == BusinessLogicGraph.NodeKind.GAP)
                .findFirst().orElseThrow().nodeId();
        assert summary.entryNodeIds().equals(List.of(rule)) : summary.entryNodeIds();
        assert summary.edges().stream().anyMatch(edge -> edge.fromNodeId().equals(rule)
                && edge.toNodeId().equals(gap) && edge.outcome().equals("yes")) : summary.edges();
        assert summary.edges().stream().anyMatch(edge -> edge.fromNodeId().equals(gap)
                && edge.toNodeId().equals("result")) : summary.edges();
    }

    private static void selectsDifferentBusinessFlowsForDifferentExecutions() {
        BusinessDecisionGraph exact = executionSelectionGraph();
        var projector = new BusinessExecutionGraphProjector();
        BusinessLogicGraph released = projector.project(exact, execution(exact, List.of(
                observation(0, "start", null),
                observation(1, "rule", "e2"),
                observation(2, "release", "e4"),
                observation(3, "stop", null))));
        BusinessLogicGraph held = projector.project(exact, execution(exact, List.of(
                observation(0, "start", null),
                observation(1, "rule", "e5"),
                observation(2, "stop", null))));

        List<String> releasedLabels = released.nodes().stream().map(BusinessLogicGraph.Node::label).toList();
        List<String> heldLabels = held.nodes().stream().map(BusinessLogicGraph.Node::label).toList();
        assert releasedLabels.containsAll(List.of("shipment is approved", "release shipment", "released"))
                : released;
        assert !releasedLabels.contains("held") : released;
        assert heldLabels.containsAll(List.of("shipment is approved", "held")) : held;
        assert !heldLabels.contains("release shipment") && !heldLabels.contains("released") : held;
        assert !new BusinessMermaidRenderer().render(released)
                .equals(new BusinessMermaidRenderer().render(held));
    }

    private static void showsOnlyGapsOnTheSelectedPath() {
        BusinessDecisionGraph exact = graph("parcel routing", BusinessDecisionGraph.Completeness.INCOMPLETE,
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        node("rule", BusinessDecisionGraph.NodeKind.PREDICATE, "parcel needs special handling"),
                        node("gap-a", BusinessDecisionGraph.NodeKind.COVERAGE_GAP,
                                "analysis incomplete: first unavailable rule"),
                        node("gap-b", BusinessDecisionGraph.NodeKind.COVERAGE_GAP,
                                "analysis incomplete: second unavailable rule"),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge("e1", "start", "rule", "next"),
                        edge("e2", "rule", "gap-a", "true"),
                        edge("e3", "gap-a", "gap-b", "next"),
                        edge("e4", "gap-b", "stop", "returns special route"),
                        edge("e5", "rule", "stop", "false; returns normal route")),
                List.of(
                        new BusinessDecisionGraph.CoverageGap("gap-a", "first unavailable rule"),
                        new BusinessDecisionGraph.CoverageGap("gap-b", "second unavailable rule")));
        var projector = new BusinessExecutionGraphProjector();
        BusinessLogicGraph special = projector.project(exact, incompleteExecution(exact, List.of(
                observation(0, "start", null),
                observation(1, "rule", "e2"),
                observation(2, "gap-a", "e3"),
                observation(3, "gap-b", "e4"),
                observation(4, "stop", null))));
        BusinessLogicGraph normal = projector.project(exact, incompleteExecution(exact, List.of(
                observation(0, "start", null),
                observation(1, "rule", "e5"),
                observation(2, "stop", null))));

        assert special.completeness() == BusinessLogicGraph.Completeness.INCOMPLETE : special;
        assert special.nodes().stream().filter(node -> node.kind() == BusinessLogicGraph.NodeKind.GAP)
                .count() == 1 : special;
        assert normal.completeness() == BusinessLogicGraph.Completeness.COMPLETE : normal;
        assert normal.nodes().stream().noneMatch(node -> node.kind() == BusinessLogicGraph.NodeKind.GAP) : normal;
        assert normal.nodes().stream().anyMatch(node -> node.label().equals("normal route")) : normal;
    }

    private static void connectsObservedSegmentsThroughASafeGap() {
        BusinessLogicGraph complete = new BusinessLogicGraph(
                "partially-observed-route", 1, "partially observed route", List.of("first-rule"),
                List.of(
                        businessNode("first-rule", BusinessLogicGraph.NodeKind.RULE, "request has a filter"),
                        businessNode("unobserved-rule", BusinessLogicGraph.NodeKind.RULE,
                                "temporary filter is present"),
                        businessNode("second-rule", BusinessLogicGraph.NodeKind.RULE, "permission is granted"),
                        businessNode("second-rule-copy", BusinessLogicGraph.NodeKind.RULE,
                                "permission is granted"),
                        businessNode("result", BusinessLogicGraph.NodeKind.RESULT, "allowed")),
                List.of(
                        businessEdge("first-to-unobserved", "first-rule", "unobserved-rule", "no"),
                        businessEdge("unobserved-to-second", "unobserved-rule", "second-rule", "no"),
                        businessEdge("second-to-result", "second-rule", "result", "yes"),
                        businessEdge("second-copy-to-result", "second-rule-copy", "result", "yes")),
                BusinessLogicGraph.Completeness.COMPLETE);
        var projection = new BusinessGraphProjection(
                complete,
                Map.of(
                        "exact-first", "first-rule",
                        "exact-unproved", "unobserved-rule",
                        "exact-second", "second-rule-copy"),
                Map.of(),
                Map.of(
                        "first-to-unobserved", List.of(List.of("entered-hidden-path")),
                        "unobserved-to-second", List.of(List.of("hidden-path")),
                        "second-to-result", List.of(List.of("other-result-path")),
                        "second-copy-to-result", List.of(List.of("result-path"))));
        DecisionExecution partial = new DecisionExecution(
                "partial", "exact-graph", 1, Instant.EPOCH, Instant.EPOCH.plusSeconds(1),
                List.of(
                        new DecisionExecution.NodeObservation(
                                0, "exact-first", "false", Map.of(), null),
                        new DecisionExecution.NodeObservation(
                                1, "exact-unproved", "", Map.of(), null),
                        new DecisionExecution.NodeObservation(
                                2, "exact-second", "true", Map.of(), null)),
                DecisionExecution.DecisionValue.of(true),
                BusinessDecisionGraph.Completeness.INCOMPLETE,
                List.of("hidden path has no exact edge evidence"));

        ObservedBusinessSegmentConnector.Selection connected = new ObservedBusinessSegmentConnector().connect(
                complete, projection, partial,
                List.of(complete.node("first-rule"), complete.node("unobserved-rule"),
                        complete.node("second-rule-copy"), complete.node("result")),
                List.of(complete.edges().get(0), complete.edges().get(3)));
        BusinessLogicGraph selected = new BusinessLogicGraph(
                complete.graphId(), complete.version(), complete.decisionLabel(), List.of("first-rule"),
                connected.nodes(), connected.edges(), BusinessLogicGraph.Completeness.INCOMPLETE);
        String first = selected.nodes().stream().filter(node -> node.label().equals("request has a filter"))
                .findFirst().orElseThrow().nodeId();
        String second = selected.nodes().stream().filter(node -> node.label().equals("permission is granted"))
                .findFirst().orElseThrow().nodeId();
        String gap = selected.nodes().stream().filter(node -> node.kind() == BusinessLogicGraph.NodeKind.GAP)
                .findFirst().orElseThrow().nodeId();

        assert selected.entryNodeIds().equals(List.of(first)) : selected;
        assert selected.edges().stream().anyMatch(edge -> edge.fromNodeId().equals(first)
                && edge.toNodeId().equals(gap) && edge.outcome().equals("no")) : selected;
        assert selected.edges().stream().anyMatch(edge -> edge.fromNodeId().equals(gap)
                && edge.toNodeId().equals(second)) : selected;
        assert selected.nodes().stream().noneMatch(node -> node.nodeId().equals("unobserved-rule")) : selected;
        assert selected.completeness() == BusinessLogicGraph.Completeness.INCOMPLETE : selected;
    }

    private static void changesGeneratedOutputWhenBusinessBehaviorChanges() {
        BusinessLogicGraph released = oneRuleBusinessGraph("released");
        BusinessLogicGraph cancelled = oneRuleBusinessGraph("cancelled");
        var summarizer = new BusinessGraphSummarizer();
        String releasedDiagram = new BusinessMermaidRenderer().render(summarizer.summarize(released));
        String cancelledDiagram = new BusinessMermaidRenderer().render(summarizer.summarize(cancelled));

        assert !releasedDiagram.equals(cancelledDiagram) : releasedDiagram;
        assert releasedDiagram.contains("released") : releasedDiagram;
        assert cancelledDiagram.contains("cancelled") : cancelledDiagram;
    }

    private static void keepsResultTerminalWhenRuntimeGapHasNoRules() {
        BusinessDecisionGraph exact = graph("status check", BusinessDecisionGraph.Completeness.COMPLETE,
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(edge("e1", "start", "stop", "returns available")),
                List.of());
        DecisionExecution incomplete = new DecisionExecution(
                "execution", exact.graphId(), exact.version(),
                Instant.parse("2026-08-14T09:00:00Z"), Instant.parse("2026-08-14T09:00:01Z"),
                List.of(observation(0, "start", "e1"), observation(1, "stop", null)),
                DecisionExecution.DecisionValue.of(true),
                BusinessDecisionGraph.Completeness.INCOMPLETE,
                List.of("runtime result value is unavailable"));

        BusinessLogicGraph selected = new BusinessExecutionGraphProjector().project(exact, incomplete);
        String gap = selected.nodes().stream()
                .filter(node -> node.kind() == BusinessLogicGraph.NodeKind.GAP)
                .findFirst().orElseThrow().nodeId();
        String result = selected.nodes().stream()
                .filter(node -> node.kind() == BusinessLogicGraph.NodeKind.RESULT)
                .findFirst().orElseThrow().nodeId();

        assert selected.entryNodeIds().equals(List.of(gap)) : selected;
        assert selected.edges().stream().anyMatch(edge -> edge.fromNodeId().equals(gap)
                && edge.toNodeId().equals(result)) : selected;
        assert selected.edges().stream().noneMatch(edge -> edge.fromNodeId().equals(result)) : selected;
    }

    private static void rejectsExecutionFromAnotherGraphVersion() {
        BusinessDecisionGraph exact = executionSelectionGraph();
        DecisionExecution wrongVersion = new DecisionExecution(
                "wrong-version", exact.graphId(), 2,
                Instant.parse("2026-08-14T09:00:00Z"), Instant.parse("2026-08-14T09:00:01Z"),
                List.of(), DecisionExecution.DecisionValue.of(true),
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
        try {
            new BusinessExecutionGraphProjector().project(exact, wrongVersion);
            throw new AssertionError("execution from another graph version was accepted");
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage().contains("versions") : expected;
        }
    }

    private static BusinessDecisionGraph executionSelectionGraph() {
        return graph("shipment decision", BusinessDecisionGraph.Completeness.COMPLETE,
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        node("rule", BusinessDecisionGraph.NodeKind.PREDICATE, "shipment is approved"),
                        node("temporary", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "derive temporary route value"),
                        node("release", BusinessDecisionGraph.NodeKind.COMPUTATION, "release shipment"),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge("e1", "start", "rule", "next"),
                        edge("e2", "rule", "temporary", "true"),
                        edge("e3", "temporary", "release", "next"),
                        edge("e4", "release", "stop", "returns released"),
                        edge("e5", "rule", "stop", "false; returns held")),
                List.of());
    }

    private static DecisionExecution execution(
            BusinessDecisionGraph graph, List<DecisionExecution.NodeObservation> observations) {
        return new DecisionExecution("execution", graph.graphId(), graph.version(),
                Instant.parse("2026-08-14T09:00:00Z"), Instant.parse("2026-08-14T09:00:01Z"),
                observations, DecisionExecution.DecisionValue.of(true),
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
    }

    private static DecisionExecution incompleteExecution(
            BusinessDecisionGraph graph, List<DecisionExecution.NodeObservation> observations) {
        return new DecisionExecution("execution", graph.graphId(), graph.version(),
                Instant.parse("2026-08-14T09:00:00Z"), Instant.parse("2026-08-14T09:00:01Z"),
                observations, DecisionExecution.DecisionValue.of(true),
                BusinessDecisionGraph.Completeness.INCOMPLETE,
                graph.coverageGaps().stream().map(BusinessDecisionGraph.CoverageGap::description).toList());
    }

    private static DecisionExecution.NodeObservation observation(
            long sequence, String nodeId, String selectedEdgeId) {
        return new DecisionExecution.NodeObservation(sequence, nodeId, "evaluated", Map.of(), selectedEdgeId);
    }

    private static BusinessLogicGraph oneRuleBusinessGraph(String resultLabel) {
        return new BusinessLogicGraph("one-rule-" + resultLabel, 1, "shipment decision", List.of("rule"),
                List.of(
                        businessNode("rule", BusinessLogicGraph.NodeKind.RULE, "shipment is approved"),
                        businessNode("result", BusinessLogicGraph.NodeKind.RESULT, resultLabel)),
                List.of(businessEdge("edge", "rule", "result", "yes")),
                BusinessLogicGraph.Completeness.COMPLETE);
    }

    private static BusinessLogicGraph.Node businessNode(
            String id, BusinessLogicGraph.NodeKind kind, String label) {
        return new BusinessLogicGraph.Node(id, kind, label);
    }

    private static BusinessLogicGraph.Edge businessEdge(
            String id, String from, String to, String outcome) {
        return new BusinessLogicGraph.Edge(id, from, to, outcome);
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

    private static BusinessDecisionGraph.DecisionNode semanticNode(
            String id, BusinessDecisionGraph.NodeKind kind, String label, Map<String, String> attributes) {
        return new BusinessDecisionGraph.DecisionNode(id, kind, label, attributes);
    }

    private static BusinessDecisionGraph.DecisionEdge edge(
            String id, String from, String to, String outcome) {
        return new BusinessDecisionGraph.DecisionEdge(id, from, to, outcome);
    }
}
