package at.gepardec.fachtracing.diagram;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Resolves observed and inferred connecting edges for diagram renderers. */
public final class ExecutionPathResolver {
    private ExecutionPathResolver() { }

    /** Returns the static edge IDs traversed by one ordered execution. */
    public static Set<String> visitedEdges(BusinessDecisionGraph graph, DecisionExecution execution) {
        var visited = new HashSet<String>();
        String previous = null;
        for (var observation : execution.observations().stream()
                .sorted(java.util.Comparator.comparingLong(DecisionExecution.NodeObservation::sequence)).toList()) {
            if (previous != null) visited.addAll(shortestPath(graph, previous, observation.nodeId()));
            if (observation.selectedEdgeId() != null) {
                visited.add(observation.selectedEdgeId());
                previous = graph.edges().stream()
                        .filter(edge -> edge.edgeId().equals(observation.selectedEdgeId()))
                        .map(BusinessDecisionGraph.DecisionEdge::toNodeId)
                        .findFirst().orElse(observation.nodeId());
            } else {
                previous = observation.nodeId();
            }
        }
        return Set.copyOf(visited);
    }

    private static List<String> shortestPath(BusinessDecisionGraph graph, String start, String destination) {
        if (start.equals(destination)) return List.of();
        var queue = new ArrayDeque<String>();
        var predecessor = new HashMap<String, BusinessDecisionGraph.DecisionEdge>();
        var seen = new HashSet<String>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && !seen.contains(destination)) {
            String node = queue.remove();
            graph.edges().stream().filter(edge -> edge.fromNodeId().equals(node)).forEach(edge -> {
                if (seen.add(edge.toNodeId())) {
                    predecessor.put(edge.toNodeId(), edge);
                    queue.add(edge.toNodeId());
                }
            });
        }
        if (!seen.contains(destination)) return List.of();
        var reversed = new ArrayList<String>();
        for (String node = destination; !node.equals(start); ) {
            var edge = predecessor.get(node);
            reversed.add(edge.edgeId());
            node = edge.fromNodeId();
        }
        Collections.reverse(reversed);
        return reversed;
    }
}
