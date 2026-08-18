package at.gepardec.fachtracing.business;

import at.gepardec.fachtracing.model.BusinessLogicGraph;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** One business graph plus in-memory traceability to its exact source graph. */
public record BusinessGraphProjection(
        BusinessLogicGraph graph,
        Map<String, String> businessNodeIdsByExactNodeId,
        Map<String, String> businessResultNodeIdsByExactEdgeId,
        Map<String, List<List<String>>> exactEdgePathsByBusinessEdgeId) {

    /** Creates one immutable and internally consistent projection. */
    public BusinessGraphProjection {
        graph = Objects.requireNonNull(graph, "graph");
        businessNodeIdsByExactNodeId = immutableTextMap(
                businessNodeIdsByExactNodeId, "exact node ID", "business node ID");
        businessResultNodeIdsByExactEdgeId = immutableTextMap(
                businessResultNodeIdsByExactEdgeId, "exact edge ID", "business result node ID");
        exactEdgePathsByBusinessEdgeId = immutablePaths(exactEdgePathsByBusinessEdgeId);

        var nodeIds = new HashSet<String>();
        graph.nodes().forEach(node -> nodeIds.add(node.nodeId()));
        if (!nodeIds.containsAll(businessNodeIdsByExactNodeId.values())) {
            throw new IllegalArgumentException("exact node mapping must reference business graph nodes");
        }
        for (String resultNodeId : businessResultNodeIdsByExactEdgeId.values()) {
            if (!nodeIds.contains(resultNodeId)
                    || graph.node(resultNodeId).kind() != BusinessLogicGraph.NodeKind.RESULT) {
                throw new IllegalArgumentException("terminal edge mapping must reference business result nodes");
            }
        }
        var edgeIds = new HashSet<String>();
        graph.edges().forEach(edge -> edgeIds.add(edge.edgeId()));
        if (!edgeIds.equals(exactEdgePathsByBusinessEdgeId.keySet())) {
            throw new IllegalArgumentException("each business edge must have exact path traceability");
        }
    }

    private static Map<String, String> immutableTextMap(
            Map<String, String> source, String keyName, String valueName) {
        Objects.requireNonNull(source, "source");
        var copy = new LinkedHashMap<String, String>();
        source.forEach((key, value) -> copy.put(
                requireText(key, keyName), requireText(value, valueName)));
        return Collections.unmodifiableMap(copy);
    }

    private static Map<String, List<List<String>>> immutablePaths(
            Map<String, List<List<String>>> source) {
        Objects.requireNonNull(source, "source");
        var copy = new LinkedHashMap<String, List<List<String>>>();
        source.forEach((businessEdgeId, alternatives) -> {
            requireText(businessEdgeId, "business edge ID");
            Objects.requireNonNull(alternatives, "exact path alternatives");
            if (alternatives.isEmpty()) {
                throw new IllegalArgumentException("business edge must have an exact path");
            }
            List<List<String>> pathCopies = alternatives.stream().map(path -> {
                Objects.requireNonNull(path, "exact edge path");
                if (path.isEmpty()) throw new IllegalArgumentException("exact edge path must not be empty");
                return path.stream().map(edgeId -> requireText(edgeId, "exact edge ID")).toList();
            }).toList();
            copy.put(businessEdgeId, pathCopies);
        });
        return Collections.unmodifiableMap(copy);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
