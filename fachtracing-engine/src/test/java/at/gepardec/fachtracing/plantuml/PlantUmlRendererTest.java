package at.gepardec.fachtracing.plantuml;

import at.gepardec.fachtracing.explain.DecisionExplanationProjectorTest;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Executable structural/execution PlantUML snapshot contracts. */
public final class PlantUmlRendererTest {
    private PlantUmlRendererTest() { }

    public static void main(String[] args) throws Exception {
        structuralAndExecutionSnapshotsAreStable();
        redundantNextOutcomeIsHidden();
        incompleteCoverageIsVisible();
    }

    private static void structuralAndExecutionSnapshotsAreStable() throws Exception {
        var renderer = new PlantUmlRenderer();
        String structure = renderer.structure(DecisionExplanationProjectorTest.graph());
        String execution = renderer.execution(
                DecisionExplanationProjectorTest.graph(), DecisionExplanationProjectorTest.execution());
        assert structure.equals(Files.readString(snapshot("eligibility-structure.puml"))) : structure;
        assert execution.equals(Files.readString(snapshot("eligibility-execution.puml"))) : execution;
        assert execution.contains("#2E7D32");
        assert execution.contains("#9E9E9E");
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

        String diagram = new PlantUmlRenderer().structure(graph);
        assert diagram.contains("n1 --> n2\n") : diagram;
        assert !diagram.contains("n1 --> n2 : next") : diagram;
        assert diagram.contains("n2 --> n3 : next item") : diagram;
    }

    private static void incompleteCoverageIsVisible() {
        var nodes = List.of(
                new BusinessDecisionGraph.DecisionNode("entry", BusinessDecisionGraph.NodeKind.ENTRY,
                        "begin decision", Map.of()),
                new BusinessDecisionGraph.DecisionNode("gap", BusinessDecisionGraph.NodeKind.COVERAGE_GAP,
                        "analysis incomplete: repeated scoring", Map.of()));
        var graph = new BusinessDecisionGraph("gap-graph", 1, "aggregate approval", "entry", nodes,
                List.of(new BusinessDecisionGraph.DecisionEdge("gap-edge", "entry", "gap", "unresolved")),
                BusinessDecisionGraph.Completeness.INCOMPLETE,
                List.of(new BusinessDecisionGraph.CoverageGap("gap", "repeated scoring affects the decision")));
        String diagram = new PlantUmlRenderer().structure(graph);
        assert diagram.contains("This decision graph is incomplete");
        assert diagram.contains("repeated scoring affects the decision");
    }

    private static Path snapshot(String name) {
        return Path.of("fachtracing-engine/src/test/resources/snapshots", name);
    }
}
