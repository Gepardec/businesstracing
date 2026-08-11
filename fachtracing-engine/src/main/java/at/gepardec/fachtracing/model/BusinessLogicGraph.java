package at.gepardec.fachtracing.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Immutable build-time graph for business readers. */
public record BusinessLogicGraph(
        String graphId,
        long version,
        String decisionLabel,
        List<String> entryNodeIds,
        List<Node> nodes,
        List<Edge> edges,
        Completeness completeness) {

    /** Creates and validates one graph snapshot. */
    public BusinessLogicGraph {
        graphId = requireText(graphId, "graphId");
        if (version < 1) throw new IllegalArgumentException("version must be positive");
        decisionLabel = requireText(decisionLabel, "decisionLabel");
        entryNodeIds = List.copyOf(entryNodeIds);
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
        completeness = Objects.requireNonNull(completeness, "completeness");
        if (entryNodeIds.isEmpty()) throw new IllegalArgumentException("entryNodeIds must not be empty");

        var nodeIds = new HashSet<String>();
        for (Node node : nodes) {
            if (!nodeIds.add(node.nodeId())) throw new IllegalArgumentException("duplicate node ID");
        }
        if (!nodeIds.containsAll(entryNodeIds)) {
            throw new IllegalArgumentException("entryNodeIds must reference graph nodes");
        }
        var edgeIds = new HashSet<String>();
        for (Edge edge : edges) {
            if (!edgeIds.add(edge.edgeId())) throw new IllegalArgumentException("duplicate edge ID");
            if (!nodeIds.contains(edge.fromNodeId()) || !nodeIds.contains(edge.toNodeId())) {
                throw new IllegalArgumentException("edge must reference graph nodes");
            }
        }
        boolean hasGap = nodes.stream().anyMatch(node -> node.kind() == NodeKind.GAP);
        if (completeness == Completeness.COMPLETE && hasGap) {
            throw new IllegalArgumentException("a complete graph must not contain a gap");
        }
    }

    /** Returns a node by its opaque ID. */
    public Node node(String nodeId) {
        return nodes.stream().filter(node -> node.nodeId().equals(nodeId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown node ID"));
    }

    /** One business rule, action, result, or analysis gap. */
    public record Node(String nodeId, NodeKind kind, String label) {
        /** Creates a validated node. */
        public Node {
            nodeId = requireText(nodeId, "nodeId");
            kind = Objects.requireNonNull(kind, "kind");
            label = requireText(label, "label");
        }
    }

    /** One directed business relationship. */
    public record Edge(String edgeId, String fromNodeId, String toNodeId, String outcome) {
        /** Creates a validated edge. */
        public Edge {
            edgeId = requireText(edgeId, "edgeId");
            fromNodeId = requireText(fromNodeId, "fromNodeId");
            toNodeId = requireText(toNodeId, "toNodeId");
            outcome = Objects.requireNonNull(outcome, "outcome");
        }
    }

    /** Business-only node kinds. */
    public enum NodeKind { RULE, ACTION, RESULT, GAP }

    /** Whether all relevant method paths have known semantics. */
    public enum Completeness { COMPLETE, INCOMPLETE }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
