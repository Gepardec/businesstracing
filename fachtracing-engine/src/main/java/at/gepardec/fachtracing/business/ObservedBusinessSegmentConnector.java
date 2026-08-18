package at.gepardec.fachtracing.business;

import at.gepardec.fachtracing.model.BusinessLogicGraph;
import at.gepardec.fachtracing.model.DecisionExecution;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Connects ordered runtime observations when an exact hidden edge has no probe evidence. */
final class ObservedBusinessSegmentConnector {
    private static final String GAP_LABEL = "analysis could not determine a required rule";

    Selection connect(
            BusinessLogicGraph completeGraph,
            BusinessGraphProjection projection,
            DecisionExecution execution,
            List<BusinessLogicGraph.Node> selectedNodes,
            List<BusinessLogicGraph.Edge> selectedEdges) {
        var nodes = new ArrayList<>(selectedNodes);
        var edges = new ArrayList<>(selectedEdges);
        Map<String, String> observedOutcomes = observedOutcomes(
                projection.businessNodeIdsByExactNodeId(), execution);
        removeDanglingUnprovedRules(nodes, edges, observedOutcomes.keySet());
        Set<String> selectedNodeIds = new HashSet<>();
        nodes.forEach(node -> selectedNodeIds.add(node.nodeId()));
        List<Anchor> anchors = observedAnchors(
                projection.businessNodeIdsByExactNodeId(), execution, selectedNodeIds);
        int bridge = 0;
        for (int index = 0; index + 1 < anchors.size(); index++) {
            Anchor from = anchors.get(index);
            Anchor to = anchors.get(index + 1);
            if (reachable(from.nodeId(), to.nodeId(), edges)
                    || semanticPathDistance(from.nodeId(), to.nodeId(),
                            completeGraph.nodes(), completeGraph.edges()) < 0) continue;
            if (edges.stream().anyMatch(edge -> edge.fromNodeId().equals(from.nodeId()))) continue;

            String gapId = uniqueNodeId(nodes,
                    "observed-bridge-gap-" + completeGraph.graphId() + '-' + (++bridge));
            nodes.add(new BusinessLogicGraph.Node(
                    gapId, BusinessLogicGraph.NodeKind.GAP, GAP_LABEL));
            String outcome = businessOutcome(from.outcome());
            if (outcome.isBlank()) {
                outcome = firstPathOutcome(
                        from.nodeId(), to.nodeId(), completeGraph.nodes(), completeGraph.edges());
            }
            edges.add(new BusinessLogicGraph.Edge(
                    uniqueEdgeId(edges, "observed-bridge-in-" + completeGraph.graphId() + '-' + bridge),
                    from.nodeId(), gapId, outcome));
            edges.add(new BusinessLogicGraph.Edge(
                    uniqueEdgeId(edges, "observed-bridge-out-" + completeGraph.graphId() + '-' + bridge),
                    gapId, to.nodeId(), ""));
        }
        connectRemainingComponents(completeGraph, nodes, edges, observedOutcomes, bridge);
        return new Selection(List.copyOf(nodes), List.copyOf(edges));
    }

    private static Map<String, String> observedOutcomes(
            Map<String, String> businessNodeByExactNode,
            DecisionExecution execution) {
        var outcomes = new java.util.LinkedHashMap<String, String>();
        for (DecisionExecution.NodeObservation observation : execution.observations()) {
            String nodeId = businessNodeByExactNode.get(observation.nodeId());
            String outcome = businessOutcome(observation.outcome());
            if (nodeId != null && !outcome.isBlank()) outcomes.put(nodeId, outcome);
        }
        return Map.copyOf(outcomes);
    }

    private static void connectRemainingComponents(
            BusinessLogicGraph completeGraph,
            List<BusinessLogicGraph.Node> nodes,
            List<BusinessLogicGraph.Edge> edges,
            Map<String, String> observedOutcomes,
            int existingBridges) {
        int bridge = existingBridges;
        while (entryNodeIds(nodes, edges).size() > 1) {
            BridgeCandidate candidate = bestBridgeCandidate(
                    completeGraph, nodes, edges, observedOutcomes);
            if (candidate == null) return;
            String gapId = uniqueNodeId(nodes,
                    "observed-bridge-gap-" + completeGraph.graphId() + '-' + (++bridge));
            nodes.add(new BusinessLogicGraph.Node(gapId, BusinessLogicGraph.NodeKind.GAP, GAP_LABEL));
            edges.add(new BusinessLogicGraph.Edge(
                    uniqueEdgeId(edges, "observed-bridge-in-" + completeGraph.graphId() + '-' + bridge),
                    candidate.fromNodeId(), gapId, candidate.outcome()));
            edges.add(new BusinessLogicGraph.Edge(
                    uniqueEdgeId(edges, "observed-bridge-out-" + completeGraph.graphId() + '-' + bridge),
                    gapId, candidate.toNodeId(), ""));
        }
    }

    private static BridgeCandidate bestBridgeCandidate(
            BusinessLogicGraph completeGraph,
            List<BusinessLogicGraph.Node> nodes,
            List<BusinessLogicGraph.Edge> edges,
            Map<String, String> observedOutcomes) {
        Set<String> resultIds = nodes.stream()
                .filter(node -> node.kind() == BusinessLogicGraph.NodeKind.RESULT)
                .map(BusinessLogicGraph.Node::nodeId)
                .collect(java.util.stream.Collectors.toSet());
        List<String> leaves = leafNodeIds(nodes, edges).stream()
                .filter(nodeId -> !resultIds.contains(nodeId)).toList();
        List<String> entries = entryNodeIds(nodes, edges);
        BridgeCandidate best = null;
        for (String from : leaves) {
            for (String to : entries) {
                if (node(nodes, from).kind() == BusinessLogicGraph.NodeKind.RULE
                        && !observedOutcomes.containsKey(from)) continue;
                int distance = semanticPathDistance(
                        from, to, completeGraph.nodes(), completeGraph.edges());
                if (distance < 1) continue;
                String outcome = observedOutcomes.getOrDefault(
                        from, firstPathOutcome(
                                from, to, completeGraph.nodes(), completeGraph.edges()));
                BridgeCandidate candidate = new BridgeCandidate(from, to, outcome, distance);
                if (best == null || candidate.distance() < best.distance()) best = candidate;
            }
        }
        return best;
    }

    private static int semanticPathDistance(
            String start,
            String target,
            List<BusinessLogicGraph.Node> nodes,
            List<BusinessLogicGraph.Edge> edges) {
        BusinessLogicGraph.Node targetNode = node(nodes, target);
        var pending = new ArrayDeque<PathDistance>();
        var seen = new HashSet<String>();
        pending.add(new PathDistance(start, 0));
        while (!pending.isEmpty()) {
            PathDistance current = pending.removeFirst();
            if (!seen.add(current.nodeId())) continue;
            if (sameBusinessNode(node(nodes, current.nodeId()), targetNode)) return current.distance();
            edges.stream().filter(edge -> edge.fromNodeId().equals(current.nodeId()))
                    .map(BusinessLogicGraph.Edge::toNodeId)
                    .forEach(nodeId -> pending.addLast(new PathDistance(nodeId, current.distance() + 1)));
        }
        return -1;
    }

    private static List<String> entryNodeIds(
            List<BusinessLogicGraph.Node> nodes,
            List<BusinessLogicGraph.Edge> edges) {
        Set<String> incoming = new HashSet<>();
        edges.forEach(edge -> incoming.add(edge.toNodeId()));
        return nodes.stream().map(BusinessLogicGraph.Node::nodeId)
                .filter(nodeId -> !incoming.contains(nodeId)).toList();
    }

    private static List<String> leafNodeIds(
            List<BusinessLogicGraph.Node> nodes,
            List<BusinessLogicGraph.Edge> edges) {
        Set<String> outgoing = new HashSet<>();
        edges.forEach(edge -> outgoing.add(edge.fromNodeId()));
        return nodes.stream().map(BusinessLogicGraph.Node::nodeId)
                .filter(nodeId -> !outgoing.contains(nodeId)).toList();
    }

    private static void removeDanglingUnprovedRules(
            List<BusinessLogicGraph.Node> nodes,
            List<BusinessLogicGraph.Edge> edges,
            Set<String> provedOutcomeNodeIds) {
        while (true) {
            Set<String> outgoing = new HashSet<>();
            edges.forEach(edge -> outgoing.add(edge.fromNodeId()));
            Set<String> removed = nodes.stream()
                    .filter(node -> node.kind() == BusinessLogicGraph.NodeKind.RULE)
                    .map(BusinessLogicGraph.Node::nodeId)
                    .filter(nodeId -> !provedOutcomeNodeIds.contains(nodeId))
                    .filter(nodeId -> !outgoing.contains(nodeId))
                    .collect(java.util.stream.Collectors.toSet());
            if (removed.isEmpty()) return;
            nodes.removeIf(node -> removed.contains(node.nodeId()));
            edges.removeIf(edge -> removed.contains(edge.fromNodeId())
                    || removed.contains(edge.toNodeId()));
        }
    }

    private static List<Anchor> observedAnchors(
            Map<String, String> businessNodeByExactNode,
            DecisionExecution execution,
            Set<String> selectedNodeIds) {
        var anchors = new ArrayList<Anchor>();
        for (DecisionExecution.NodeObservation observation : execution.observations()) {
            String nodeId = businessNodeByExactNode.get(observation.nodeId());
            if (nodeId == null || !selectedNodeIds.contains(nodeId)) continue;
            if (!anchors.isEmpty() && anchors.getLast().nodeId().equals(nodeId)) continue;
            anchors.add(new Anchor(nodeId, observation.outcome()));
        }
        return List.copyOf(anchors);
    }

    private static boolean reachable(
            String start,
            String target,
            List<BusinessLogicGraph.Edge> edges) {
        if (start.equals(target)) return true;
        var pending = new ArrayDeque<String>();
        var seen = new HashSet<String>();
        pending.add(start);
        while (!pending.isEmpty()) {
            String current = pending.removeFirst();
            if (!seen.add(current)) continue;
            for (BusinessLogicGraph.Edge edge : edges) {
                if (!edge.fromNodeId().equals(current)) continue;
                if (edge.toNodeId().equals(target)) return true;
                pending.addLast(edge.toNodeId());
            }
        }
        return false;
    }

    private static String firstPathOutcome(
            String start,
            String target,
            List<BusinessLogicGraph.Node> nodes,
            List<BusinessLogicGraph.Edge> edges) {
        BusinessLogicGraph.Node targetNode = node(nodes, target);
        var pending = new ArrayDeque<PathStep>();
        var seen = new LinkedHashSet<String>();
        pending.add(new PathStep(start, ""));
        while (!pending.isEmpty()) {
            PathStep current = pending.removeFirst();
            if (!seen.add(current.nodeId())) continue;
            for (BusinessLogicGraph.Edge edge : edges) {
                if (!edge.fromNodeId().equals(current.nodeId())) continue;
                String firstOutcome = current.firstOutcome().isBlank()
                        ? edge.outcome() : current.firstOutcome();
                if (sameBusinessNode(node(nodes, edge.toNodeId()), targetNode)) return firstOutcome;
                pending.addLast(new PathStep(edge.toNodeId(), firstOutcome));
            }
        }
        return "";
    }

    private static BusinessLogicGraph.Node node(
            List<BusinessLogicGraph.Node> nodes,
            String nodeId) {
        return nodes.stream().filter(node -> node.nodeId().equals(nodeId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "business graph does not contain node " + nodeId));
    }

    private static boolean sameBusinessNode(
            BusinessLogicGraph.Node first,
            BusinessLogicGraph.Node second) {
        return first.kind() == second.kind() && first.label().equals(second.label());
    }

    private static String businessOutcome(String outcome) {
        String normalized = outcome == null ? "" : outcome.toLowerCase(java.util.Locale.ROOT).trim();
        if (normalized.equals("true") || normalized.startsWith("true;")) return "yes";
        if (normalized.equals("false") || normalized.startsWith("false;")) return "no";
        if (normalized.equals("yes") || normalized.equals("no")) return normalized;
        return "";
    }

    private static String uniqueNodeId(List<BusinessLogicGraph.Node> nodes, String candidate) {
        Set<String> ids = new HashSet<>();
        nodes.forEach(node -> ids.add(node.nodeId()));
        String result = candidate;
        for (int suffix = 2; ids.contains(result); suffix++) result = candidate + '-' + suffix;
        return result;
    }

    private static String uniqueEdgeId(List<BusinessLogicGraph.Edge> edges, String candidate) {
        Set<String> ids = new HashSet<>();
        edges.forEach(edge -> ids.add(edge.edgeId()));
        String result = candidate;
        for (int suffix = 2; ids.contains(result); suffix++) result = candidate + '-' + suffix;
        return result;
    }

    record Selection(
            List<BusinessLogicGraph.Node> nodes,
            List<BusinessLogicGraph.Edge> edges) { }

    private record Anchor(String nodeId, String outcome) { }
    private record PathStep(String nodeId, String firstOutcome) { }
    private record PathDistance(String nodeId, int distance) { }
    private record BridgeCandidate(
            String fromNodeId,
            String toNodeId,
            String outcome,
            int distance) { }
}
