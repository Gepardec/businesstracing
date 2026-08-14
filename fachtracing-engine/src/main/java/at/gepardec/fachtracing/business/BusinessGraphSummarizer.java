package at.gepardec.fachtracing.business;

import at.gepardec.fachtracing.model.BusinessLogicGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Condenses a business graph using only node and edge semantics. */
public final class BusinessGraphSummarizer {

    /** Collapses connected gap regions and behaviorally equivalent states. */
    public BusinessLogicGraph summarize(BusinessLogicGraph graph) {
        return summarizeTraceable(graph).graph();
    }

    /** Summarizes a graph and maps each input node to its final representative. */
    public Summary summarizeTraceable(BusinessLogicGraph graph) {
        Objects.requireNonNull(graph, "graph");
        new BusinessLogicArtifactGuard().requireClean(graph);
        Rewrite collapsed = collapseGapRegions(graph);
        Rewrite summarized = mergeEquivalentStates(collapsed.graph());
        var finalNodeIds = new LinkedHashMap<String, String>();
        graph.nodes().forEach(node -> {
            String collapsedId = collapsed.representativeNodeIds().get(node.nodeId());
            finalNodeIds.put(node.nodeId(), summarized.representativeNodeIds().get(collapsedId));
        });
        new BusinessLogicArtifactGuard().requireClean(summarized.graph());
        return new Summary(summarized.graph(), finalNodeIds);
    }

    private static Rewrite collapseGapRegions(BusinessLogicGraph graph) {
        var gapIds = new LinkedHashSet<String>();
        graph.nodes().stream().filter(node -> node.kind() == BusinessLogicGraph.NodeKind.GAP)
                .map(BusinessLogicGraph.Node::nodeId).forEach(gapIds::add);
        if (gapIds.size() < 2) return rewrite(graph, identityMapping(graph));

        var parents = new LinkedHashMap<String, String>();
        gapIds.forEach(id -> parents.put(id, id));
        for (BusinessLogicGraph.Edge edge : graph.edges()) {
            if (gapIds.contains(edge.fromNodeId()) && gapIds.contains(edge.toNodeId())) {
                union(parents, edge.fromNodeId(), edge.toNodeId());
            }
        }

        var representativeByRoot = new LinkedHashMap<String, String>();
        for (BusinessLogicGraph.Node node : graph.nodes()) {
            if (gapIds.contains(node.nodeId())) {
                representativeByRoot.putIfAbsent(find(parents, node.nodeId()), node.nodeId());
            }
        }
        var mapping = identityMapping(graph);
        gapIds.forEach(id -> mapping.put(id, representativeByRoot.get(find(parents, id))));
        return rewrite(graph, mapping);
    }

    private static Rewrite mergeEquivalentStates(BusinessLogicGraph graph) {
        Map<String, Integer> partitions = initialPartitions(graph);
        while (true) {
            var signatures = new LinkedHashMap<String, Integer>();
            var next = new LinkedHashMap<String, Integer>();
            for (BusinessLogicGraph.Node node : graph.nodes()) {
                String signature = behaviorSignature(graph, node, partitions);
                int partition = signatures.computeIfAbsent(signature, ignored -> signatures.size());
                next.put(node.nodeId(), partition);
            }
            if (next.equals(partitions)) break;
            partitions = Map.copyOf(next);
        }

        var representativeByPartition = new LinkedHashMap<Integer, String>();
        for (BusinessLogicGraph.Node node : graph.nodes()) {
            representativeByPartition.putIfAbsent(partitions.get(node.nodeId()), node.nodeId());
        }
        var mapping = new LinkedHashMap<String, String>();
        for (BusinessLogicGraph.Node node : graph.nodes()) {
            mapping.put(node.nodeId(), representativeByPartition.get(partitions.get(node.nodeId())));
        }
        return rewrite(graph, mapping);
    }

    private static Map<String, Integer> initialPartitions(BusinessLogicGraph graph) {
        var signatures = new LinkedHashMap<String, Integer>();
        var partitions = new LinkedHashMap<String, Integer>();
        for (BusinessLogicGraph.Node node : graph.nodes()) {
            String signature = node.kind() + "\u0000" + node.label();
            int partition = signatures.computeIfAbsent(signature, ignored -> signatures.size());
            partitions.put(node.nodeId(), partition);
        }
        return Map.copyOf(partitions);
    }

    private static String behaviorSignature(
            BusinessLogicGraph graph,
            BusinessLogicGraph.Node node,
            Map<String, Integer> partitions) {
        List<String> outgoing = graph.edges().stream()
                .filter(edge -> edge.fromNodeId().equals(node.nodeId()))
                .map(edge -> edge.outcome() + "\u0000" + partitions.get(edge.toNodeId()))
                .sorted().toList();
        return node.kind() + "\u0000" + node.label() + "\u0000" + String.join("\u0001", outgoing);
    }

    private static Rewrite rewrite(
            BusinessLogicGraph graph, Map<String, String> representativeByNodeId) {
        boolean unchanged = representativeByNodeId.entrySet().stream()
                .allMatch(entry -> entry.getKey().equals(entry.getValue()));
        if (unchanged) return new Rewrite(graph, Map.copyOf(representativeByNodeId));

        var retainedNodeIds = new LinkedHashSet<>(representativeByNodeId.values());
        List<BusinessLogicGraph.Node> nodes = graph.nodes().stream()
                .filter(node -> retainedNodeIds.contains(node.nodeId())).toList();
        var entries = new LinkedHashSet<String>();
        graph.entryNodeIds().forEach(id -> entries.add(representativeByNodeId.get(id)));

        var edgeKeys = new HashSet<String>();
        var edges = new ArrayList<BusinessLogicGraph.Edge>();
        for (BusinessLogicGraph.Edge edge : graph.edges()) {
            String from = representativeByNodeId.get(edge.fromNodeId());
            String to = representativeByNodeId.get(edge.toNodeId());
            if (from.equals(to)) continue;
            String key = from + '\u0000' + to + '\u0000' + edge.outcome();
            if (edgeKeys.add(key)) {
                edges.add(new BusinessLogicGraph.Edge(edge.edgeId(), from, to, edge.outcome()));
            }
        }
        BusinessLogicGraph rewritten = new BusinessLogicGraph(
                graph.graphId(), graph.version(), graph.decisionLabel(), List.copyOf(entries),
                nodes, List.copyOf(edges), graph.completeness());
        return new Rewrite(rewritten, Map.copyOf(representativeByNodeId));
    }

    private static LinkedHashMap<String, String> identityMapping(BusinessLogicGraph graph) {
        var mapping = new LinkedHashMap<String, String>();
        graph.nodes().forEach(node -> mapping.put(node.nodeId(), node.nodeId()));
        return mapping;
    }

    private static String find(Map<String, String> parents, String id) {
        String parent = parents.get(id);
        while (!parent.equals(parents.get(parent))) parent = parents.get(parent);
        String root = parent;
        for (String current = id; !parents.get(current).equals(root); ) {
            String next = parents.get(current);
            parents.put(current, root);
            current = next;
        }
        return root;
    }

    private static void union(Map<String, String> parents, String first, String second) {
        String firstRoot = find(parents, first);
        String secondRoot = find(parents, second);
        if (!firstRoot.equals(secondRoot)) parents.put(secondRoot, firstRoot);
    }

    /** One summarized graph plus original-to-final node identity. */
    public record Summary(
            BusinessLogicGraph graph,
            Map<String, String> finalNodeIdsByInputNodeId) {
        /** Creates one immutable summary result. */
        public Summary {
            graph = Objects.requireNonNull(graph, "graph");
            finalNodeIdsByInputNodeId = Map.copyOf(Objects.requireNonNull(
                    finalNodeIdsByInputNodeId, "finalNodeIdsByInputNodeId"));
            var finalNodeIds = new HashSet<String>();
            graph.nodes().forEach(node -> finalNodeIds.add(node.nodeId()));
            if (!finalNodeIds.containsAll(finalNodeIdsByInputNodeId.values())) {
                throw new IllegalArgumentException("summary mapping must reference final graph nodes");
            }
        }
    }

    private record Rewrite(
            BusinessLogicGraph graph,
            Map<String, String> representativeNodeIds) { }
}
