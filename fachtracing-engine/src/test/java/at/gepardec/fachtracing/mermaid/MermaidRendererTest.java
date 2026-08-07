package at.gepardec.fachtracing.mermaid;

import at.gepardec.fachtracing.explain.DecisionExplanationProjectorTest;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Executable structural/execution Mermaid snapshot contracts. */
public final class MermaidRendererTest {
    private MermaidRendererTest() { }

    public static void main(String[] args) throws Exception {
        structuralAndExecutionSnapshotsAreStable();
        redundantNextOutcomeIsHidden();
        incompleteCoverageAndEscapingAreVisible();
    }

    private static void structuralAndExecutionSnapshotsAreStable() throws Exception {
        var renderer = new MermaidRenderer();
        String structure = renderer.structure(DecisionExplanationProjectorTest.graph());
        String execution = renderer.execution(
                DecisionExplanationProjectorTest.graph(), DecisionExplanationProjectorTest.execution());
        assert structure.equals(Files.readString(snapshot("eligibility-structure.mmd"))) : structure;
        assert execution.equals(Files.readString(snapshot("eligibility-execution.mmd"))) : execution;
        assert execution.contains("linkStyle 0,1,3 stroke-width:3px");
        assert execution.contains("linkStyle 2,4 stroke-dasharray:5 5");
        DecisionExplanationProjectorTest.assertNoTechnicalLanguage(structure + execution);
    }

    private static void redundantNextOutcomeIsHidden() {
        var nodes = List.of(
                new BusinessDecisionGraph.DecisionNode("entry", BusinessDecisionGraph.NodeKind.ENTRY,
                        "Start", Map.of()),
                new BusinessDecisionGraph.DecisionNode("first", BusinessDecisionGraph.NodeKind.COMPUTATION,
                        "first", Map.of()),
                new BusinessDecisionGraph.DecisionNode("second", BusinessDecisionGraph.NodeKind.COMPUTATION,
                        "second", Map.of()));
        var graph = new BusinessDecisionGraph("sequence", 1, "sequence", "entry", nodes,
                List.of(
                        new BusinessDecisionGraph.DecisionEdge("ordinary", "entry", "first", "next"),
                        new BusinessDecisionGraph.DecisionEdge("iteration", "first", "second", "next item")),
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());

        String diagram = new MermaidRenderer().structure(graph);
        assert diagram.contains("n1 --> n2") : diagram;
        assert !diagram.contains("|\"next\"|") : diagram;
        assert diagram.contains("n2 -->|\"next item\"| n3") : diagram;
    }

    private static void incompleteCoverageAndEscapingAreVisible() {
        var nodes = List.of(
                new BusinessDecisionGraph.DecisionNode("entry", BusinessDecisionGraph.NodeKind.ENTRY,
                        "begin decision", Map.of()),
                new BusinessDecisionGraph.DecisionNode("gap", BusinessDecisionGraph.NodeKind.COVERAGE_GAP,
                        "amount < limit and label \"special\"", Map.of()));
        var graph = new BusinessDecisionGraph("gap-graph", 1, "approval", "entry", nodes,
                List.of(new BusinessDecisionGraph.DecisionEdge("gap-edge", "entry", "gap", "unresolved")),
                BusinessDecisionGraph.Completeness.INCOMPLETE,
                List.of(new BusinessDecisionGraph.CoverageGap("gap", "repeated scoring affects the decision")));
        String diagram = new MermaidRenderer().structure(graph);
        assert diagram.contains("Incomplete analysis");
        assert diagram.contains("repeated scoring affects the decision");
        assert diagram.contains("&lt;") && diagram.contains("&quot;") : diagram;
    }

    private static Path snapshot(String name) {
        return Path.of("fachtracing-engine/src/test/resources/snapshots", name);
    }
}
