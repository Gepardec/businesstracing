package at.gepardec.fachtracing.business;

import at.gepardec.fachtracing.model.BusinessLogicGraph;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Holds one final business graph and the developer-only decisions that produced it. */
public record BusinessGraphAudit(
        BusinessLogicGraph graph,
        List<BusinessGraphProjection.Decision> decisions) {

    /** Creates one immutable and internally consistent final audit. */
    public BusinessGraphAudit {
        graph = Objects.requireNonNull(graph, "graph");
        decisions = List.copyOf(Objects.requireNonNull(decisions, "decisions"));
        var nodeIds = new HashSet<String>();
        graph.nodes().forEach(node -> nodeIds.add(node.nodeId()));
        for (BusinessGraphProjection.Decision decision : decisions) {
            if (!nodeIds.containsAll(decision.businessNodeIds())) {
                throw new IllegalArgumentException("audit decision must reference final graph nodes");
            }
        }
    }
}
