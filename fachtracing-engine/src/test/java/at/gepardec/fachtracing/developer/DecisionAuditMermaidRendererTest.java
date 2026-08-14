package at.gepardec.fachtracing.developer;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.business.BusinessGraphAudit;
import at.gepardec.fachtracing.business.BusinessGraphProjector;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Executable contracts for compact input-driven decision audit Mermaid. */
public final class DecisionAuditMermaidRendererTest {
    private DecisionAuditMermaidRendererTest() { }

    public static void main(String[] args) {
        rendersGroupedAnalysisDecisions();
        rendersGroupedProjectionDecisions();
        followsChangedInputWithoutAFixedDiagram();
    }

    private static void rendersGroupedAnalysisDecisions() {
        AnalysisManifest.AnalysisResult analysis = analysis("account is active");
        var renderer = new DecisionAuditMermaidRenderer();

        String first = renderer.analysis(analysis);
        String second = renderer.analysis(analysis);

        assert first.equals(second) : first + "\n" + second;
        assert first.startsWith("flowchart TD\n") : first;
        assert first.contains("Policy.java / IF") : first;
        assert first.contains("INCLUDED / CONTROL_DEPENDENCY") : first;
        assert first.contains("EXCLUDED / NO_RESULT_EFFECT") : first;
        assert first.contains("GAP / UNRESOLVED_RELEVANCE") : first;
        assert first.contains("Not in exact graph") : first;
        assert first.contains("example: account is active") : first;
        assert first.contains("example: record audit metric") : first;
        assert first.contains("PREDICATE") : first;
        assert first.contains("COVERAGE_GAP") : first;
        assert first.contains("decisions: 2") : first;
    }

    private static void rendersGroupedProjectionDecisions() {
        AnalysisManifest.AnalysisResult analysis = analysis("account is active");
        BusinessGraphAudit audit = new BusinessGraphProjector().projectWithAudit(analysis);
        var renderer = new DecisionAuditMermaidRenderer();

        String diagram = renderer.projection(audit);

        assert diagram.contains("REMOVED / STRUCTURAL_ENTRY") : diagram;
        assert diagram.contains("REMOVED / STRUCTURAL_OUTCOME") : diagram;
        assert diagram.contains("REMOVED / TECHNICAL_CALCULATION") : diagram;
        assert diagram.contains("KEPT / BUSINESS_RULE") : diagram;
        assert diagram.contains("KEPT / COVERAGE_GAP") : diagram;
        assert diagram.contains("REPLACED / TERMINAL_RESULT") : diagram;
        assert diagram.contains("RULE") : diagram;
        assert diagram.contains("GAP") : diagram;
        assert diagram.contains("RESULT") : diagram;
        assert diagram.contains("Not in business graph") : diagram;
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
                "audit-" + ruleLabel.replace(' ', '-'), 1, "authorization", "start",
                List.of(
                        node("start", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                        node("derive", BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "derive authorization flag"),
                        node("rule", BusinessDecisionGraph.NodeKind.PREDICATE, ruleLabel),
                        node("gap", BusinessDecisionGraph.NodeKind.COVERAGE_GAP,
                                "unknown policy effect"),
                        node("stop", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop")),
                List.of(
                        edge("e1", "start", "derive", "next"),
                        edge("e2", "derive", "rule", "next"),
                        edge("e3", "rule", "stop", "true; returns approved"),
                        edge("e4", "rule", "gap", "false"),
                        edge("e5", "gap", "stop", "returns pending")),
                BusinessDecisionGraph.Completeness.INCOMPLETE,
                List.of(new BusinessDecisionGraph.CoverageGap("gap", "unknown policy effect")));
        List<AnalysisManifest.AnalysisDecision> decisions = List.of(
                decision(AnalysisManifest.AnalysisAction.INCLUDED,
                        AnalysisManifest.AnalysisReason.CONTROL_DEPENDENCY,
                        12, "IF", List.of("rule"), ""),
                decision(AnalysisManifest.AnalysisAction.INCLUDED,
                        AnalysisManifest.AnalysisReason.CONTROL_DEPENDENCY,
                        18, "IF", List.of("gap"), ""),
                decision(AnalysisManifest.AnalysisAction.EXCLUDED,
                        AnalysisManifest.AnalysisReason.NO_RESULT_EFFECT,
                        16, "METHOD_INVOCATION", List.of(), "record audit metric"),
                decision(AnalysisManifest.AnalysisAction.GAP,
                        AnalysisManifest.AnalysisReason.UNRESOLVED_RELEVANCE,
                        20, "METHOD_INVOCATION", List.of("gap"), "unknown policy effect"));
        var manifest = new AnalysisManifest(
                graph.graphId(), graph.version(), Map.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), decisions, Map.of());
        return new AnalysisManifest.AnalysisResult(graph, manifest, List.of());
    }

    private static AnalysisManifest.AnalysisDecision decision(
            AnalysisManifest.AnalysisAction action,
            AnalysisManifest.AnalysisReason reason,
            long line,
            String kind,
            List<String> nodeIds,
            String subject) {
        return new AnalysisManifest.AnalysisDecision(
                action, reason, Path.of("/workspace/Policy.java"), line, 9,
                kind, nodeIds, subject);
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
