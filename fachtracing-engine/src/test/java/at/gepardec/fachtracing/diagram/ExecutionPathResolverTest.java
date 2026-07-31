package at.gepardec.fachtracing.diagram;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Executable contracts for observed-edge precedence. */
public final class ExecutionPathResolverTest {
    private ExecutionPathResolverTest() { }

    public static void main(String[] args) {
        explicitEdgeSelectsOneAmbiguousRoute();
    }

    private static void explicitEdgeSelectsOneAmbiguousRoute() {
        var nodes = List.of(
                node("entry", BusinessDecisionGraph.NodeKind.ENTRY),
                node("predicate", BusinessDecisionGraph.NodeKind.PREDICATE),
                node("true-node", BusinessDecisionGraph.NodeKind.COMPUTATION),
                node("false-node", BusinessDecisionGraph.NodeKind.COMPUTATION),
                node("outcome", BusinessDecisionGraph.NodeKind.OUTCOME));
        var edges = List.of(
                edge("entry-edge", "entry", "predicate", "next"),
                edge("true-edge", "predicate", "true-node", "true"),
                edge("false-edge", "predicate", "false-node", "false"),
                edge("true-result", "true-node", "outcome", "result"),
                edge("false-result", "false-node", "outcome", "result"));
        var graph = new BusinessDecisionGraph(
                "graph", 1, "decision", "entry", nodes, edges,
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
        Instant time = Instant.parse("2026-07-31T00:00:00Z");
        var execution = new DecisionExecution(
                "execution", "graph", 1, time, time,
                List.of(
                        new DecisionExecution.NodeObservation(0, "entry", "started", Map.of(), null),
                        new DecisionExecution.NodeObservation(1, "predicate", "true", Map.of(), "true-edge"),
                        new DecisionExecution.NodeObservation(2, "outcome", "result", Map.of(), null)),
                DecisionExecution.DecisionValue.of(true),
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());

        Set<String> visited = ExecutionPathResolver.visitedEdges(graph, execution);
        assert visited.equals(Set.of("entry-edge", "true-edge", "true-result")) : visited;
        assert !visited.contains("false-edge") && !visited.contains("false-result") : visited;
    }

    private static BusinessDecisionGraph.DecisionNode node(
            String id, BusinessDecisionGraph.NodeKind kind) {
        return new BusinessDecisionGraph.DecisionNode(id, kind, id, Map.of());
    }

    private static BusinessDecisionGraph.DecisionEdge edge(
            String id, String from, String to, String outcome) {
        return new BusinessDecisionGraph.DecisionEdge(id, from, to, outcome);
    }
}
