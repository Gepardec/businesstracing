package at.gepardec.fachtracing.business;

import at.gepardec.fachtracing.diagram.ExecutionPathResolver;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.BusinessLogicGraph;
import at.gepardec.fachtracing.model.DecisionExecution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Selects the generated business flow proved by one exact execution. */
public final class BusinessExecutionGraphProjector {
    private static final String GAP_LABEL = "analysis could not determine a required rule";
    private static final String UNKNOWN_RESULT = "result could not be determined";
    private final BusinessGraphProjector projector = new BusinessGraphProjector();
    private final BusinessGraphSummarizer summarizer = new BusinessGraphSummarizer();

    /** Projects one call without changing its exact graph or execution. */
    public BusinessLogicGraph project(BusinessDecisionGraph exact, DecisionExecution execution) {
        Objects.requireNonNull(exact, "exact");
        Objects.requireNonNull(execution, "execution");
        if (!exact.graphId().equals(execution.graphId()) || exact.version() != execution.graphVersion()) {
            throw new IllegalArgumentException("graph and execution versions do not match");
        }

        BusinessGraphProjection projection = projector.projectTraceable(exact);
        BusinessLogicGraph business = projection.graph();
        Set<String> visitedExactEdgeIds = ExecutionPathResolver.visitedEdges(exact, execution);
        var selectedBusinessEdgeIds = new LinkedHashSet<String>();
        projection.exactEdgePathsByBusinessEdgeId().forEach((businessEdgeId, alternatives) -> {
            if (alternatives.stream().anyMatch(visitedExactEdgeIds::containsAll)) {
                selectedBusinessEdgeIds.add(businessEdgeId);
            }
        });

        var selectedNodeIds = new LinkedHashSet<String>();
        business.edges().stream().filter(edge -> selectedBusinessEdgeIds.contains(edge.edgeId()))
                .forEach(edge -> {
                    selectedNodeIds.add(edge.fromNodeId());
                    selectedNodeIds.add(edge.toNodeId());
                });
        execution.observations().forEach(observation -> {
            String businessNodeId = projection.businessNodeIdsByExactNodeId().get(observation.nodeId());
            if (businessNodeId != null) selectedNodeIds.add(businessNodeId);
        });
        visitedExactEdgeIds.forEach(edgeId -> {
            String resultNodeId = projection.businessResultNodeIdsByExactEdgeId().get(edgeId);
            if (resultNodeId != null) selectedNodeIds.add(resultNodeId);
        });

        var nodes = new ArrayList<>(business.nodes().stream()
                .filter(node -> selectedNodeIds.contains(node.nodeId())).toList());
        var edges = new ArrayList<>(business.edges().stream()
                .filter(edge -> selectedBusinessEdgeIds.contains(edge.edgeId()))
                .filter(edge -> selectedNodeIds.contains(edge.fromNodeId())
                        && selectedNodeIds.contains(edge.toNodeId()))
                .toList());

        boolean unknownObservation = execution.observations().stream().anyMatch(observation ->
                exact.nodes().stream().noneMatch(node -> node.nodeId().equals(observation.nodeId())));
        boolean runtimeGap = hasRuntimeOnlyGap(exact, execution);
        long resultCount = nodes.stream().filter(node -> node.kind() == BusinessLogicGraph.NodeKind.RESULT).count();
        boolean missingTerminalProof = resultCount != 1;
        if (missingTerminalProof) replaceResultsWithUnknown(nodes, edges, business.graphId());

        boolean selectedGap = nodes.stream().anyMatch(node -> node.kind() == BusinessLogicGraph.NodeKind.GAP);
        if ((unknownObservation || runtimeGap) && !selectedGap) {
            insertSafeGap(nodes, edges, business.graphId());
            selectedGap = true;
        }
        BusinessLogicGraph.Completeness completeness = selectedGap || missingTerminalProof
                ? BusinessLogicGraph.Completeness.INCOMPLETE
                : BusinessLogicGraph.Completeness.COMPLETE;
        List<String> entries = entryNodeIds(nodes, edges);
        BusinessLogicGraph selected = new BusinessLogicGraph(
                business.graphId(), business.version(), business.decisionLabel(), entries,
                List.copyOf(nodes), List.copyOf(edges), completeness);
        return summarizer.summarize(selected);
    }

    private static boolean hasRuntimeOnlyGap(
            BusinessDecisionGraph graph, DecisionExecution execution) {
        if (execution.completeness() == BusinessDecisionGraph.Completeness.COMPLETE) return false;
        var unexplained = new ArrayList<>(execution.coverageGaps());
        graph.coverageGaps().forEach(gap -> unexplained.remove(gap.description()));
        return graph.completeness() == BusinessDecisionGraph.Completeness.COMPLETE
                || graph.coverageGaps().isEmpty()
                || !unexplained.isEmpty();
    }

    private static void replaceResultsWithUnknown(
            List<BusinessLogicGraph.Node> nodes,
            List<BusinessLogicGraph.Edge> edges,
            String graphId) {
        Set<String> removedResultIds = nodes.stream()
                .filter(node -> node.kind() == BusinessLogicGraph.NodeKind.RESULT)
                .map(BusinessLogicGraph.Node::nodeId)
                .collect(java.util.stream.Collectors.toSet());
        nodes.removeIf(node -> removedResultIds.contains(node.nodeId()));
        edges.removeIf(edge -> removedResultIds.contains(edge.fromNodeId())
                || removedResultIds.contains(edge.toNodeId()));
        String gapId = uniqueNodeId(nodes, "call-gap-" + graphId);
        String resultId = uniqueNodeId(nodes, "call-result-" + graphId);
        List<String> leaves = leafNodeIds(nodes, edges);
        nodes.add(new BusinessLogicGraph.Node(gapId, BusinessLogicGraph.NodeKind.GAP, GAP_LABEL));
        nodes.add(new BusinessLogicGraph.Node(resultId, BusinessLogicGraph.NodeKind.RESULT, UNKNOWN_RESULT));
        for (String leaf : leaves) {
            edges.add(new BusinessLogicGraph.Edge(
                    uniqueEdgeId(edges, "call-gap-edge-" + graphId + '-' + edges.size()),
                    leaf, gapId, ""));
        }
        edges.add(new BusinessLogicGraph.Edge(
                uniqueEdgeId(edges, "call-result-edge-" + graphId), gapId, resultId, ""));
    }

    private static void insertSafeGap(
            List<BusinessLogicGraph.Node> nodes,
            List<BusinessLogicGraph.Edge> edges,
            String graphId) {
        String gapId = uniqueNodeId(nodes, "call-gap-" + graphId);
        Set<String> resultIds = nodes.stream()
                .filter(node -> node.kind() == BusinessLogicGraph.NodeKind.RESULT)
                .map(BusinessLogicGraph.Node::nodeId)
                .collect(java.util.stream.Collectors.toSet());
        var incomingToResults = edges.stream().filter(edge -> resultIds.contains(edge.toNodeId())).toList();
        edges.removeAll(incomingToResults);
        nodes.add(new BusinessLogicGraph.Node(gapId, BusinessLogicGraph.NodeKind.GAP, GAP_LABEL));
        if (incomingToResults.isEmpty()) {
            for (String leaf : leafNodeIds(nodes.stream()
                    .filter(node -> !node.nodeId().equals(gapId))
                    .filter(node -> !resultIds.contains(node.nodeId())).toList(), edges)) {
                edges.add(new BusinessLogicGraph.Edge(
                        uniqueEdgeId(edges, "call-gap-edge-" + graphId + '-' + edges.size()),
                        leaf, gapId, ""));
            }
        } else {
            for (BusinessLogicGraph.Edge edge : incomingToResults) {
                edges.add(new BusinessLogicGraph.Edge(edge.edgeId(), edge.fromNodeId(), gapId, edge.outcome()));
            }
        }
        for (String resultId : resultIds) {
            edges.add(new BusinessLogicGraph.Edge(
                    uniqueEdgeId(edges, "call-result-edge-" + graphId + '-' + resultId),
                    gapId, resultId, ""));
        }
    }

    private static List<String> entryNodeIds(
            List<BusinessLogicGraph.Node> nodes, List<BusinessLogicGraph.Edge> edges) {
        var incoming = new HashSet<String>();
        edges.forEach(edge -> incoming.add(edge.toNodeId()));
        List<String> entries = nodes.stream().map(BusinessLogicGraph.Node::nodeId)
                .filter(id -> !incoming.contains(id)).toList();
        if (!entries.isEmpty()) return entries;
        if (nodes.isEmpty()) throw new IllegalArgumentException("execution produced no business nodes");
        return List.of(nodes.getFirst().nodeId());
    }

    private static List<String> leafNodeIds(
            List<BusinessLogicGraph.Node> nodes, List<BusinessLogicGraph.Edge> edges) {
        var sources = new HashSet<String>();
        edges.forEach(edge -> sources.add(edge.fromNodeId()));
        var leaves = new LinkedHashSet<String>();
        nodes.stream().map(BusinessLogicGraph.Node::nodeId)
                .filter(id -> !sources.contains(id)).forEach(leaves::add);
        return List.copyOf(leaves);
    }

    private static String uniqueNodeId(List<BusinessLogicGraph.Node> nodes, String candidate) {
        var ids = new HashSet<String>();
        nodes.forEach(node -> ids.add(node.nodeId()));
        String result = candidate;
        for (int suffix = 2; ids.contains(result); suffix++) result = candidate + '-' + suffix;
        return result;
    }

    private static String uniqueEdgeId(List<BusinessLogicGraph.Edge> edges, String candidate) {
        var ids = new HashSet<String>();
        edges.forEach(edge -> ids.add(edge.edgeId()));
        String result = candidate;
        for (int suffix = 2; ids.contains(result); suffix++) result = candidate + '-' + suffix;
        return result;
    }
}
