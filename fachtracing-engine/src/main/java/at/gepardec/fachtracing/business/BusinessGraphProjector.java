package at.gepardec.fachtracing.business;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.BusinessLogicGraph;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Projects an exact analysis graph into a concise build-time business graph. */
public final class BusinessGraphProjector {
    private static final String GAP_LABEL = "analysis could not determine a required rule";

    /** Projects one exact analysis result without changing that result. */
    public BusinessLogicGraph project(AnalysisManifest.AnalysisResult analysis) {
        Objects.requireNonNull(analysis, "analysis");
        BusinessDecisionGraph exact = analysis.graph();

        Map<String, BusinessDecisionGraph.DecisionNode> exactNodes = indexNodes(exact);
        Map<String, List<BusinessDecisionGraph.DecisionEdge>> incoming = incoming(exact);
        Map<String, List<BusinessDecisionGraph.DecisionEdge>> outgoing = outgoing(exact);
        Map<String, LoopProjection> loops = loops(exact, exactNodes, outgoing);
        Set<String> hiddenLoopNodes = loops.values().stream()
                .flatMap(loop -> loop.hiddenNodeIds().stream())
                .collect(java.util.stream.Collectors.toSet());

        var projectedByExactId = new LinkedHashMap<String, BusinessLogicGraph.Node>();
        for (BusinessDecisionGraph.DecisionNode node : exact.nodes()) {
            BusinessLogicGraph.Node projected = projectNode(node, loops.get(node.nodeId()), hiddenLoopNodes);
            if (projected != null) projectedByExactId.put(node.nodeId(), projected);
        }

        var nodes = new ArrayList<>(projectedByExactId.values());
        var targets = new ArrayList<Target>();
        projectedByExactId.forEach((exactId, node) ->
                targets.add(Target.exact(node.nodeId(), exactId)));
        for (BusinessDecisionGraph.DecisionEdge edge : exact.edges()) {
            BusinessDecisionGraph.DecisionNode target = exactNodes.get(edge.toNodeId());
            if (target == null || target.kind() != BusinessDecisionGraph.NodeKind.OUTCOME) continue;
            String id = opaqueId("result", exact.graphId(), edge.edgeId());
            nodes.add(new BusinessLogicGraph.Node(id, BusinessLogicGraph.NodeKind.RESULT,
                    resultLabel(edge.outcome())));
            targets.add(Target.result(id, edge.fromNodeId(), edge.outcome()));
        }

        if (nodes.isEmpty()) {
            String id = opaqueId("result", exact.graphId(), "empty");
            nodes.add(new BusinessLogicGraph.Node(id, BusinessLogicGraph.NodeKind.RESULT, "completed"));
            targets.add(Target.result(id, exact.entryNodeId(), "returns completed"));
        }

        var entries = new LinkedHashSet<String>();
        var edges = new ArrayList<BusinessLogicGraph.Edge>();
        var edgeKeys = new HashSet<String>();
        for (Target target : targets) {
            if (target.result()) {
                walkBackward(exact, target.exactNodeId(), target, List.of(target.initialOutcome()),
                        projectedByExactId, incoming, entries, edges, edgeKeys, new HashSet<>());
            } else {
                List<BusinessDecisionGraph.DecisionEdge> predecessors = incoming
                        .getOrDefault(target.exactNodeId(), List.of());
                if (predecessors.isEmpty() && target.exactNodeId().equals(exact.entryNodeId())) {
                    entries.add(target.businessNodeId());
                }
                for (BusinessDecisionGraph.DecisionEdge predecessor : predecessors) {
                    walkBackward(exact, predecessor.fromNodeId(), target, List.of(predecessor.outcome()),
                            projectedByExactId, incoming, entries, edges, edgeKeys, new HashSet<>());
                }
            }
        }

        removeUnreachableNodes(nodes, edges, entries);
        if (entries.isEmpty() && !nodes.isEmpty()) entries.add(nodes.getFirst().nodeId());

        BusinessLogicGraph graph = new BusinessLogicGraph(
                exact.graphId(), exact.version(), cleanLabel(exact.decisionLabel()), List.copyOf(entries),
                List.copyOf(nodes), List.copyOf(edges),
                exact.completeness() == BusinessDecisionGraph.Completeness.COMPLETE
                        ? BusinessLogicGraph.Completeness.COMPLETE
                        : BusinessLogicGraph.Completeness.INCOMPLETE);
        new BusinessLogicArtifactGuard().requireClean(graph);
        return graph;
    }

    private static void walkBackward(
            BusinessDecisionGraph exact,
            String cursor,
            Target target,
            List<String> outcomes,
            Map<String, BusinessLogicGraph.Node> projectedByExactId,
            Map<String, List<BusinessDecisionGraph.DecisionEdge>> incoming,
            Set<String> entries,
            List<BusinessLogicGraph.Edge> edges,
            Set<String> edgeKeys,
            Set<String> visited) {
        BusinessLogicGraph.Node source = projectedByExactId.get(cursor);
        if (source != null) {
            if (!source.nodeId().equals(target.businessNodeId())) {
                addEdge(exact.graphId(), source, target, outcomes, edges, edgeKeys);
            }
            return;
        }
        if (cursor.equals(exact.entryNodeId())) {
            entries.add(target.businessNodeId());
            return;
        }
        if (!visited.add(cursor)) return;
        List<BusinessDecisionGraph.DecisionEdge> predecessors = incoming.getOrDefault(cursor, List.of());
        if (predecessors.isEmpty()) {
            entries.add(target.businessNodeId());
            return;
        }
        for (BusinessDecisionGraph.DecisionEdge predecessor : predecessors) {
            var path = new ArrayList<String>(outcomes.size() + 1);
            path.add(predecessor.outcome());
            path.addAll(outcomes);
            walkBackward(exact, predecessor.fromNodeId(), target, List.copyOf(path),
                    projectedByExactId, incoming, entries, edges, edgeKeys, new HashSet<>(visited));
        }
    }

    private static void addEdge(
            String graphId,
            BusinessLogicGraph.Node source,
            Target target,
            List<String> outcomes,
            List<BusinessLogicGraph.Edge> edges,
            Set<String> edgeKeys) {
        String outcome = businessOutcome(source.kind(), outcomes);
        String key = source.nodeId() + '\u0000' + target.businessNodeId() + '\u0000' + outcome;
        if (!edgeKeys.add(key)) return;
        String edgeId = opaqueId("edge", graphId, key);
        edges.add(new BusinessLogicGraph.Edge(
                edgeId, source.nodeId(), target.businessNodeId(), outcome));
    }

    private static BusinessLogicGraph.Node projectNode(
            BusinessDecisionGraph.DecisionNode node,
            LoopProjection loop,
            Set<String> hiddenLoopNodes) {
        if (hiddenLoopNodes.contains(node.nodeId()) && !businessEffectInsideLoop(node)) return null;
        if (loop != null) {
            return new BusinessLogicGraph.Node(opaqueId("rule", node.nodeId(), loop.label()),
                    BusinessLogicGraph.NodeKind.RULE, loop.label());
        }
        String label = cleanLabel(node.businessLabel());
        return switch (node.kind()) {
            case ENTRY, OUTCOME -> null;
            case PREDICATE -> new BusinessLogicGraph.Node(
                    opaqueId("rule", node.nodeId(), label), BusinessLogicGraph.NodeKind.RULE, label);
            case CHOICE -> technicalChoice(label) ? null : new BusinessLogicGraph.Node(
                    opaqueId("rule", node.nodeId(), label), BusinessLogicGraph.NodeKind.RULE, label);
            case DISPATCH -> technicalDispatch(label) ? null : new BusinessLogicGraph.Node(
                    opaqueId("rule", node.nodeId(), label), BusinessLogicGraph.NodeKind.RULE, label);
            case COMPUTATION -> technicalCalculation(label) ? null : new BusinessLogicGraph.Node(
                    opaqueId("action", node.nodeId(), label), BusinessLogicGraph.NodeKind.ACTION, label);
            case COVERAGE_GAP -> new BusinessLogicGraph.Node(
                    opaqueId("gap", node.nodeId(), GAP_LABEL), BusinessLogicGraph.NodeKind.GAP, GAP_LABEL);
        };
    }

    private static Map<String, LoopProjection> loops(
            BusinessDecisionGraph graph,
            Map<String, BusinessDecisionGraph.DecisionNode> nodes,
            Map<String, List<BusinessDecisionGraph.DecisionEdge>> outgoing) {
        var loops = new LinkedHashMap<String, LoopProjection>();
        for (BusinessDecisionGraph.DecisionNode node : graph.nodes()) {
            String label = node.businessLabel().toLowerCase(Locale.ROOT);
            if (!label.startsWith("for each ") && !label.startsWith("repeat while ")) continue;
            Set<String> hidden = loopBody(node.nodeId(), nodes, outgoing);
            loops.put(node.nodeId(), new LoopProjection(loopLabel(node.businessLabel(), hidden, nodes), hidden));
        }
        return Map.copyOf(loops);
    }

    private static Set<String> loopBody(
            String loopNodeId,
            Map<String, BusinessDecisionGraph.DecisionNode> nodes,
            Map<String, List<BusinessDecisionGraph.DecisionEdge>> outgoing) {
        var hidden = new LinkedHashSet<String>();
        var queue = new ArrayDeque<String>();
        for (BusinessDecisionGraph.DecisionEdge edge : outgoing.getOrDefault(loopNodeId, List.of())) {
            String outcome = edge.outcome().toLowerCase(Locale.ROOT);
            if (!outcome.contains("done") && !edge.toNodeId().equals(loopNodeId)) queue.add(edge.toNodeId());
        }
        while (!queue.isEmpty()) {
            String nodeId = queue.removeFirst();
            if (nodeId.equals(loopNodeId) || !hidden.add(nodeId)) continue;
            BusinessDecisionGraph.DecisionNode node = nodes.get(nodeId);
            if (node == null || node.kind() == BusinessDecisionGraph.NodeKind.OUTCOME) {
                hidden.remove(nodeId);
                continue;
            }
            for (BusinessDecisionGraph.DecisionEdge edge : outgoing.getOrDefault(nodeId, List.of())) {
                if (!edge.toNodeId().equals(loopNodeId)) queue.add(edge.toNodeId());
            }
        }
        return Set.copyOf(hidden);
    }

    private static String loopLabel(
            String original,
            Set<String> hidden,
            Map<String, BusinessDecisionGraph.DecisionNode> nodes) {
        String item = "item";
        String lower = original.toLowerCase(Locale.ROOT);
        if (lower.startsWith("for each ")) {
            String remainder = original.substring("for each ".length()).trim();
            int in = remainder.toLowerCase(Locale.ROOT).indexOf(" in ");
            if (in > 0) item = cleanLabel(remainder.substring(0, in));
        }
        List<String> labels = hidden.stream().map(nodes::get).filter(Objects::nonNull)
                .map(BusinessDecisionGraph.DecisionNode::businessLabel)
                .map(value -> value.toLowerCase(Locale.ROOT)).toList();
        if (labels.stream().anyMatch(value -> value.contains("name")
                && (value.contains("equal") || value.contains("match")))) {
            return "a " + item + " with this name exists";
        }
        if (labels.stream().anyMatch(value -> value.contains("overlap"))) {
            return "an overlapping " + item + " exists";
        }
        return "a matching " + item + " exists";
    }

    private static String resultLabel(String outcome) {
        String lower = outcome.toLowerCase(Locale.ROOT).trim();
        if (lower.contains("fails")) return "operation failed";
        int returns = lower.indexOf("returns ");
        if (returns < 0) return "completed";
        String prefix = lower.substring(0, returns);
        String value = outcome.substring(returns + "returns ".length()).trim()
                .replaceAll("^[\\\"']|[\\\"']$", "");
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("whether ") || normalized.equals("true") || normalized.equals("false")) {
            return prefix.contains("false") ? "condition is not met" : "condition is met";
        }
        if (normalized.equals("absent") || normalized.equals("null") || normalized.equals("empty")) {
            return "no matching result";
        }
        if (normalized.startsWith("redirect:") || normalized.startsWith("forward:")
                || normalized.startsWith("views ") || normalized.startsWith("view ")) {
            return "response is ready";
        }
        if (normalized.isBlank()) return "completed";
        return cleanLabel(value);
    }

    private static String businessOutcome(BusinessLogicGraph.NodeKind sourceKind, List<String> outcomes) {
        if (sourceKind != BusinessLogicGraph.NodeKind.RULE) return "";
        for (String raw : outcomes) {
            String first = raw.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            if (first.equals("true") || first.equals("item") || first.equals("match")) return "yes";
            if (first.equals("false") || first.equals("done") || first.equals("no match")) return "no";
        }
        return "";
    }

    private static boolean technicalChoice(String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        return lower.contains("decision result path") || lower.startsWith("select ");
    }

    private static boolean technicalDispatch(String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        return lower.startsWith("select ") || lower.contains("applicable decision rule");
    }

    private static boolean technicalCalculation(String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        return lower.startsWith("derive ") || lower.startsWith("evaluate ")
                || lower.startsWith("use ") || lower.equals("decision cannot continue")
                || lower.startsWith("initialize ");
    }

    private static boolean businessEffectInsideLoop(BusinessDecisionGraph.DecisionNode node) {
        return node.kind() == BusinessDecisionGraph.NodeKind.COVERAGE_GAP
                || (node.kind() == BusinessDecisionGraph.NodeKind.COMPUTATION
                && !technicalCalculation(cleanLabel(node.businessLabel())));
    }

    private static String cleanLabel(String label) {
        String value = Objects.requireNonNull(label, "label").strip()
                .replaceAll("(?i)^analysis incomplete:\\s*", "")
                .replaceAll("(?i)\\bcomp(?:arison)?\\s+", "")
                .replaceAll("\\s+", " ");
        if (value.isBlank()) return "business condition";
        return value;
    }

    private static Map<String, BusinessDecisionGraph.DecisionNode> indexNodes(BusinessDecisionGraph graph) {
        var nodes = new HashMap<String, BusinessDecisionGraph.DecisionNode>();
        graph.nodes().forEach(node -> nodes.put(node.nodeId(), node));
        return Map.copyOf(nodes);
    }

    private static Map<String, List<BusinessDecisionGraph.DecisionEdge>> incoming(BusinessDecisionGraph graph) {
        var result = new HashMap<String, List<BusinessDecisionGraph.DecisionEdge>>();
        for (BusinessDecisionGraph.DecisionEdge edge : graph.edges()) {
            result.computeIfAbsent(edge.toNodeId(), ignored -> new ArrayList<>()).add(edge);
        }
        return immutableLists(result);
    }

    private static Map<String, List<BusinessDecisionGraph.DecisionEdge>> outgoing(BusinessDecisionGraph graph) {
        var result = new HashMap<String, List<BusinessDecisionGraph.DecisionEdge>>();
        for (BusinessDecisionGraph.DecisionEdge edge : graph.edges()) {
            result.computeIfAbsent(edge.fromNodeId(), ignored -> new ArrayList<>()).add(edge);
        }
        return immutableLists(result);
    }

    private static Map<String, List<BusinessDecisionGraph.DecisionEdge>> immutableLists(
            Map<String, List<BusinessDecisionGraph.DecisionEdge>> source) {
        var result = new HashMap<String, List<BusinessDecisionGraph.DecisionEdge>>();
        source.forEach((key, value) -> result.put(key, List.copyOf(value)));
        return Map.copyOf(result);
    }

    private static void removeUnreachableNodes(
            List<BusinessLogicGraph.Node> nodes,
            List<BusinessLogicGraph.Edge> edges,
            Set<String> entries) {
        if (entries.isEmpty()) return;
        var reachable = new HashSet<String>(entries);
        boolean changed;
        do {
            changed = false;
            for (BusinessLogicGraph.Edge edge : edges) {
                if (reachable.contains(edge.fromNodeId()) && reachable.add(edge.toNodeId())) changed = true;
            }
        } while (changed);
        nodes.removeIf(node -> !reachable.contains(node.nodeId()));
        edges.removeIf(edge -> !reachable.contains(edge.fromNodeId()) || !reachable.contains(edge.toNodeId()));
    }

    private static String opaqueId(String kind, String graphId, String seed) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((kind + '\u0000' + graphId + '\u0000' + seed)
                            .getBytes(StandardCharsets.UTF_8));
            return kind + '-' + java.util.HexFormat.of().formatHex(digest, 0, 10);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private record LoopProjection(String label, Set<String> hiddenNodeIds) { }

    private record Target(
            String businessNodeId,
            String exactNodeId,
            boolean result,
            String initialOutcome) {
        private static Target exact(String businessId, String exactId) {
            return new Target(businessId, exactId, false, "");
        }
        private static Target result(String businessId, String exactId, String outcome) {
            return new Target(businessId, exactId, true, outcome);
        }
    }
}
