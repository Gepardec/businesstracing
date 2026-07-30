package at.gepardec.fachtracing.analysis;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;

import java.nio.file.Path;
import java.util.List;

/** Executable dependency-free contract tests for the static analyzer. */
public final class StaticDecisionAnalyzerTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path FIXTURES = ROOT.resolve("fachtracing-engine/src/test/resources/fixtures");
    private static final List<Path> CLASSPATH = List.of(ROOT.resolve("fachtracing-api/target/classes"));

    private StaticDecisionAnalyzerTest() { }

    public static void main(String[] args) {
        supportedConstructsAcrossDomains();
        excludesResultIndependentWork();
        followsDirectCallsAcrossDomains();
        representsDynamicDispatchWithoutGuessing();
        exposesRelevantCoverageGaps();
        sourceUnavailableDecisionLogicIsNeverReportedComplete();
        analyzesEveryAnnotatedEntry();
        treatsPlatformValueOperationsAsDecisionFacts();
        supportsCollectionFactsAndRecordEquality();
        followsStrategiesThatMutateReturnedCollectionsInsideLambdas();
        streamPredicatesStayBusinessFacing();
        usesOneBusinessStartAndStopWithExplicitReturns();
        removesIdentifierAndNullImplementationVocabulary();
    }

    private static void supportedConstructsAcrossDomains() {
        for (var fixture : List.of("eligibility/EligibilityPolicy.java", "pricing/PricingPolicy.java")) {
            var graph = analyze(fixture).graph();
            assert graph.completeness() == BusinessDecisionGraph.Completeness.COMPLETE : fixture;
            assert hasKind(graph, BusinessDecisionGraph.NodeKind.ENTRY) : fixture;
            assert hasKind(graph, BusinessDecisionGraph.NodeKind.PREDICATE) : fixture;
            assert hasKind(graph, BusinessDecisionGraph.NodeKind.OUTCOME) : fixture;
        }
    }

    private static void excludesResultIndependentWork() {
        var result = analyze("eligibility/EligibilityPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE;
        assert hasKind(result.graph(), BusinessDecisionGraph.NodeKind.PREDICATE);
        assert hasKind(result.graph(), BusinessDecisionGraph.NodeKind.OUTCOME);
        var predicate = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE)
                .findFirst().orElseThrow();
        var branchOutcomes = result.graph().edges().stream()
                .filter(edge -> edge.fromNodeId().equals(predicate.nodeId()))
                .map(BusinessDecisionGraph.DecisionEdge::outcome).toList();
        assert branchOutcomes.stream().anyMatch(value -> value.startsWith("true; returns "))
                && branchOutcomes.stream().anyMatch(value -> value.startsWith("false; returns "))
                : result.graph().edges();
        assert result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.OUTCOME)
                .noneMatch(outcome -> result.graph().edges().stream()
                        .anyMatch(edge -> edge.fromNodeId().equals(outcome.nodeId()))) : result.graph().edges();
        String labels = result.graph().nodes().toString();
        assert !labels.contains("diagnosticOnly") : labels;
        assert !labels.contains("System.getProperty") : labels;
        result.graph().nodes().forEach(node -> {
            assert !node.businessLabel().contains("(") : node;
            assert !node.businessLabel().contains(")") : node;
            assert !node.businessLabel().contains(".equals") : node;
        });
    }

    private static void followsDirectCallsAcrossDomains() {
        var result = analyze("pricing/PricingPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE;
        long predicates = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE)
                .count();
        assert predicates >= 1 : result.graph().nodes();
        assert result.graph().nodes().stream()
                .anyMatch(node -> node.businessLabel().contains("preferred")) : result.graph().nodes();
        assert result.graph().nodes().stream()
                .anyMatch(node -> node.businessLabel().equals("evaluate fixed adjustment"))
                : "single-return arithmetic helper was mistaken for a projection: " + result.graph().nodes();
        var outcome = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.OUTCOME)
                .findFirst().orElseThrow();
        var resultPaths = result.graph().edges().stream()
                .filter(edge -> edge.toNodeId().equals(outcome.nodeId()))
                .map(BusinessDecisionGraph.DecisionEdge::outcome).toList();
        assert resultPaths.size() == 2
                && resultPaths.stream().anyMatch(value -> value.startsWith("true; returns "))
                && resultPaths.stream().anyMatch(value -> value.startsWith("false; returns "))
                : result.graph().edges();
    }

    private static void representsDynamicDispatchWithoutGuessing() {
        var result = analyze("strategy/StrategyDecisionService.java");
        var dispatch = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.DISPATCH)
                .findFirst().orElseThrow();
        long alternatives = result.graph().edges().stream()
                .filter(edge -> edge.fromNodeId().equals(dispatch.nodeId()))
                .filter(edge -> edge.outcome().startsWith("candidate "))
                .count();
        assert alternatives == 2 : result.graph().edges();
        long implementationPredicates = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE)
                .count();
        assert implementationPredicates == 2 : result.graph().nodes();
    }

    private static void exposesRelevantCoverageGaps() {
        var loopResult = analyze("gaps/AggregatingPolicy.java");
        assert loopResult.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE
                : loopResult.diagnostics();
        assert hasKind(loopResult.graph(), BusinessDecisionGraph.NodeKind.CHOICE) : loopResult.graph().nodes();

        var result = analyze("gaps/UnsupportedPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE;
        assert hasKind(result.graph(), BusinessDecisionGraph.NodeKind.COVERAGE_GAP);
        assert !result.diagnostics().isEmpty();
        assert result.diagnostics().getFirst().line() > 0;
    }

    private static void sourceUnavailableDecisionLogicIsNeverReportedComplete() {
        var result = analyze("gaps/ExternalDecisionPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE;
        assert result.graph().coverageGaps().stream()
                .anyMatch(gap -> gap.description().contains("implementations are unavailable"));
    }

    private static void analyzesEveryAnnotatedEntry() {
        var results = new StaticDecisionAnalyzer().analyzeAll(AnalysisRequest.of(
                List.of(
                        FIXTURES.resolve("eligibility/EligibilityPolicy.java"),
                        FIXTURES.resolve("pricing/PricingPolicy.java")),
                CLASSPATH));
        assert results.size() == 2 : results;
        assert results.stream().map(result -> result.graph().decisionLabel()).distinct().count() == 2 : results;
        assert results.stream().allMatch(result ->
                result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE) : results;
    }

    private static void treatsPlatformValueOperationsAsDecisionFacts() {
        var result = analyze("calendar/CalendarPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : result.diagnostics();
        assert result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE)
                .count() >= 1 : result.graph().nodes();
    }

    private static void supportsCollectionFactsAndRecordEquality() {
        var result = analyze("authorization/RecordAuthorizationPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : result.diagnostics();
        assert result.graph().nodes().stream()
                .anyMatch(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE
                        && node.businessLabel().contains("approvers")) : result.graph().nodes();
        assert result.graph().nodes().stream()
                .noneMatch(node -> node.kind() == BusinessDecisionGraph.NodeKind.DISPATCH) : result.graph().nodes();
    }

    private static void followsStrategiesThatMutateReturnedCollectionsInsideLambdas() {
        var result = analyze("aggregation/StrategyAggregationPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : result.diagnostics();
        long alternatives = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.DISPATCH).count();
        assert alternatives >= 1 : result.graph().nodes();
        long candidates = result.graph().edges().stream()
                .filter(edge -> edge.outcome().startsWith("candidate ")).count();
        assert candidates == 2 : result.graph().edges();
        assert result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE).count() >= 2
                : result.graph().nodes();
    }

    private static void streamPredicatesStayBusinessFacing() {
        var result = analyze("streams/StreamSelectionPolicy.java");
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : result.diagnostics();
        assert result.graph().nodes().stream()
                .anyMatch(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE
                        && node.businessLabel().contains("label length is above 3")) : result.graph().nodes();
        result.graph().nodes().forEach(node -> {
            String label = node.businessLabel();
            assert !label.contains("->") && !label.contains("::") && !label.contains(".stream")
                    && !label.contains("instanceof") : node;
        });
    }

    private static void usesOneBusinessStartAndStopWithExplicitReturns() {
        var result = analyze("eligibility/EligibilityPolicy.java");
        var entries = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.ENTRY).toList();
        var outcomes = result.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.OUTCOME).toList();
        assert entries.size() == 1 && entries.getFirst().businessLabel().equals("Start") : entries;
        assert outcomes.size() == 1 && outcomes.getFirst().businessLabel().equals("Stop") : outcomes;
        var terminalNodes = result.graph().nodes().stream()
                .filter(node -> result.graph().edges().stream()
                        .noneMatch(edge -> edge.fromNodeId().equals(node.nodeId())))
                .map(BusinessDecisionGraph.DecisionNode::nodeId)
                .collect(java.util.stream.Collectors.toSet());
        assert terminalNodes.equals(java.util.Set.of(outcomes.getFirst().nodeId())) : terminalNodes;
        assert result.graph().edges().stream().filter(edge -> edge.toNodeId().equals(outcomes.getFirst().nodeId()))
                .allMatch(edge -> edge.outcome().contains("returns")) : result.graph().edges();
        var returnProbes = result.manifest().probeSites().stream()
                .filter(probe -> probe.kind() == AnalysisManifest.ProbeKind.OUTCOME).toList();
        assert returnProbes.size() == 2 : returnProbes;
        assert returnProbes.stream().map(AnalysisManifest.ProbeSite::nodeId).distinct().count() == 1 : returnProbes;
    }

    private static void removesIdentifierAndNullImplementationVocabulary() {
        var graph = analyze("authorization/RecordAuthorizationPolicy.java").graph();
        String businessText = graph.nodes().stream().map(BusinessDecisionGraph.DecisionNode::businessLabel)
                .collect(java.util.stream.Collectors.joining(" ")) + " "
                + graph.edges().stream().map(BusinessDecisionGraph.DecisionEdge::outcome)
                .collect(java.util.stream.Collectors.joining(" "));
        assert !businessText.matches("(?is).*\\bids?\\b.*") : businessText;
        assert !businessText.matches("(?is).*\\bnull\\b.*") : businessText;
        assert businessText.contains("creator exists") : businessText;
        assert businessText.contains("employee") : businessText;
    }

    private static AnalysisManifest.AnalysisResult analyze(String relativeFixture) {
        return new StaticDecisionAnalyzer().analyze(AnalysisRequest.of(
                List.of(FIXTURES.resolve(relativeFixture)), CLASSPATH));
    }

    private static boolean hasKind(BusinessDecisionGraph graph, BusinessDecisionGraph.NodeKind kind) {
        return graph.nodes().stream().anyMatch(node -> node.kind() == kind);
    }
}
