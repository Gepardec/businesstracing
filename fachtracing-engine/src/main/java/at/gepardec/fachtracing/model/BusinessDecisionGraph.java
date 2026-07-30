package at.gepardec.fachtracing.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable, reusable business-decision graph containing no Java source coordinates. */
public record BusinessDecisionGraph(
        String graphId,
        long version,
        String decisionLabel,
        String entryNodeId,
        List<DecisionNode> nodes,
        List<DecisionEdge> edges,
        Completeness completeness,
        List<CoverageGap> coverageGaps) {

    /** Creates a defensive graph snapshot. */
    public BusinessDecisionGraph {
        graphId = requireText(graphId, "graphId");
        if (version < 1) throw new IllegalArgumentException("version must be positive");
        decisionLabel = requireText(decisionLabel, "decisionLabel");
        entryNodeId = requireText(entryNodeId, "entryNodeId");
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
        completeness = Objects.requireNonNull(completeness, "completeness");
        coverageGaps = List.copyOf(coverageGaps);
        var validatedEntryNodeId = entryNodeId;
        if (nodes.stream().noneMatch(node -> node.nodeId().equals(validatedEntryNodeId))) {
            throw new IllegalArgumentException("entryNodeId must reference a graph node");
        }
    }

    /** Returns a graph node by opaque ID. */
    public DecisionNode node(String nodeId) {
        return nodes.stream().filter(node -> node.nodeId().equals(nodeId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown node ID"));
    }

    /** Business graph node. Labels contain domain expressions, not Java symbols. */
    public record DecisionNode(
            String nodeId,
            NodeKind kind,
            String businessLabel,
            Map<String, String> attributes) {
        /** Creates a defensive node snapshot. */
        public DecisionNode {
            nodeId = requireText(nodeId, "nodeId");
            kind = Objects.requireNonNull(kind, "kind");
            businessLabel = requireText(businessLabel, "businessLabel");
            attributes = Map.copyOf(attributes);
        }
    }

    /** Directed graph edge with an opaque ID and a business outcome label. */
    public record DecisionEdge(String edgeId, String fromNodeId, String toNodeId, String outcome) {
        /** Creates a validated edge. */
        public DecisionEdge {
            edgeId = requireText(edgeId, "edgeId");
            fromNodeId = requireText(fromNodeId, "fromNodeId");
            toNodeId = requireText(toNodeId, "toNodeId");
            outcome = Objects.requireNonNull(outcome, "outcome");
        }
    }

    /** Safe marker for an unsupported result-relevant graph location. */
    public record CoverageGap(String nodeId, String description) {
        /** Creates a business-visible gap marker. */
        public CoverageGap {
            nodeId = requireText(nodeId, "nodeId");
            description = requireText(description, "description");
        }
    }

    /** Graph node kinds supported by the public record model. */
    public enum NodeKind { ENTRY, PREDICATE, CHOICE, COMPUTATION, DISPATCH, OUTCOME, COVERAGE_GAP }

    /** Whether every result-relevant operation was analyzed or observed. */
    public enum Completeness { COMPLETE, INCOMPLETE }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
