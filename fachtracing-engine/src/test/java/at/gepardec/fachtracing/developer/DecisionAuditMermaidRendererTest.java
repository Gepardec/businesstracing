package at.gepardec.fachtracing.developer;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.business.BusinessGraphProjection;
import at.gepardec.fachtracing.business.BusinessGraphProjector;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Executable contracts for input-driven decision audit Mermaid. */
public final class DecisionAuditMermaidRendererTest {
    private DecisionAuditMermaidRendererTest() { }

    public static void main(String[] args) {
        rendersRecordedAnalysisDecisions();
        rendersRecordedProjectionDecisions();
        followsChangedInputWithoutAFixedDiagram();
    }

    private static void rendersRecordedAnalysisDecisions() {
        AnalysisManifest.AnalysisResult analysis = analysis("account is active");
        var renderer = new DecisionAuditMermaidRenderer();

        String first = renderer.analysis(analysis);
        String second = renderer.analysis(analysis);

        assert first.equals(second) : first + "\n" + second;
        assert first.contains("INCLUDED / CONTROL_DEPENDENCY") : first;
        assert first.contains("EXCLUDED / NO_RESULT_EFFECT") : first;
        assert first.contains("GAP / UNRESOLVED_RELEVANCE") : first;
        assert first.contains("record audit metric") : first;
        assert first.contains("No exact graph node") : first;
        assert first.contains("ad2 -.-> an2") : first;
        assert !first.contains("ad2 --> ae") : first;
        assert first.contains("PREDICATE<br/>account is active") : first;
        assert first.contains("COVERAGE_GAP<br/>unknown policy effect") : first;
    }

    private static void rendersRecordedProjectionDecisions() {
        BusinessDecisionGraph exact = new BusinessDecisionGraph(
                "projection-audit", 1, "authorization", "start",
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        node("derive", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "derive authorization flag"),
                        node("rule", BusinessDecisionGraph.NodeKind.PREDICATE,
                                "account is authorized"),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge("e1", "start", "derive", "next"),
                        edge("e2", "derive", "rule", "next"),
                        edge("e3", "rule", "stop", "true; returns approved"),
                        edge("e4", "rule", "stop", "false; returns denied")),
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
        AnalysisManifest.AnalysisResult analysis = result(exact, List.of());
        BusinessGraphProjection projection = new BusinessGraphProjector().projectWithAudit(analysis);
        var renderer = new DecisionAuditMermaidRenderer();

        String first = renderer.projection(projection);
        String second = renderer.projection(projection);

        assert first.equals(second) : first + "\n" + second;
        assert first.contains("REMOVED / STRUCTURAL_ENTRY") : first;
        assert first.contains("REMOVED / STRUCTURAL_OUTCOME") : first;
        assert first.contains("derive authorization flag") : first;
        assert first.contains("REMOVED / TECHNICAL_CALCULATION") : first;
        assert first.contains("KEPT / BUSINESS_RULE") : first;
        assert first.contains("REPLACED / TERMINAL_RESULT") : first;
        assert first.contains("RESULT<br/>approved") : first;
        assert first.contains("RESULT<br/>denied") : first;
    }

    private static void followsChangedInputWithoutAFixedDiagram() {
        var renderer = new DecisionAuditMermaidRenderer();
        String account = renderer.analysis(analysis("account is active"));
        String order = renderer.analysis(analysis("order has approval"));

        assert !account.equals(order) : account;
        assert order.contains("order has approval") : order;
        assert !order.contains("account is active") : order;
    }

    private static AnalysisManifest.AnalysisResult analysis(String ruleLabel) {
        BusinessDecisionGraph graph = new BusinessDecisionGraph(
                "analysis-audit-" + ruleLabel.replace(' ', '-'), 1, "source audit", "start",
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        node("rule", BusinessDecisionGraph.NodeKind.PREDICATE, ruleLabel),
                        node("gap", BusinessDecisionGraph.NodeKind.COVERAGE_GAP, "unknown policy effect"),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge("e1", "start", "rule", "next"),
                        edge("e2", "rule", "stop", "true; returns accepted"),
                        edge("e3", "rule", "gap", "false"),
                        edge("e4", "gap", "stop", "unresolved")),
                BusinessDecisionGraph.Completeness.INCOMPLETE,
                List.of(new BusinessDecisionGraph.CoverageGap("gap", "unknown policy effect")));
        List<AnalysisManifest.AnalysisDecision> decisions = List.of(
                new AnalysisManifest.AnalysisDecision(
                        AnalysisManifest.AnalysisAction.INCLUDED,
                        AnalysisManifest.AnalysisReason.CONTROL_DEPENDENCY,
                        Path.of("/workspace/Policy.java"), 12, 9, "IF", List.of("rule"), ruleLabel),
                new AnalysisManifest.AnalysisDecision(
                        AnalysisManifest.AnalysisAction.EXCLUDED,
                        AnalysisManifest.AnalysisReason.NO_RESULT_EFFECT,
                        Path.of("/workspace/Policy.java"), 16, 9, "METHOD_INVOCATION", List.of(),
                        "record audit metric"),
                new AnalysisManifest.AnalysisDecision(
                        AnalysisManifest.AnalysisAction.GAP,
                        AnalysisManifest.AnalysisReason.UNRESOLVED_RELEVANCE,
                        Path.of("/workspace/Policy.java"), 20, 9, "METHOD_INVOCATION", List.of("gap"),
                        "unknown policy effect"));
        return result(graph, decisions);
    }

    private static AnalysisManifest.AnalysisResult result(
            BusinessDecisionGraph graph,
            List<AnalysisManifest.AnalysisDecision> decisions) {
        var manifest = new AnalysisManifest(
                graph.graphId(), graph.version(), Map.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), decisions, Map.of());
        return new AnalysisManifest.AnalysisResult(graph, manifest, List.of());
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
