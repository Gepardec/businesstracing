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
import java.util.regex.Pattern;

/** Projects an exact analysis graph into a concise build-time business graph. */
public final class BusinessGraphProjector {
    private static final String GAP_LABEL = "analysis could not determine a required rule";
    private final BusinessGraphSummarizer summarizer = new BusinessGraphSummarizer();
    private final BusinessSemanticReducer semanticReducer = new BusinessSemanticReducer();

    /** Projects one exact analysis result without changing that result. */
    public BusinessLogicGraph project(AnalysisManifest.AnalysisResult analysis) {
        return projectWithAudit(analysis).graph();
    }

    /** Projects one exact analysis result and keeps final developer audit decisions. */
    public BusinessGraphAudit projectWithAudit(AnalysisManifest.AnalysisResult analysis) {
        Objects.requireNonNull(analysis, "analysis");
        if (!analysis.graph().graphId().equals(analysis.manifest().graphId())
                || analysis.graph().version() != analysis.manifest().graphVersion()) {
            throw new IllegalArgumentException("graph and analysis manifest versions do not match");
        }
        BusinessGraphProjection raw = projectTraceable(analysis.graph());
        BusinessGraphSummarizer.Summary summary = summarizer.summarizeTraceable(raw.graph());
        List<BusinessGraphProjection.Decision> decisions = raw.decisions().stream()
                .map(decision -> remap(decision, summary.finalNodeIdsByInputNodeId()))
                .toList();
        return new BusinessGraphAudit(summary.graph(), decisions);
    }

    /** Projects one exact graph and keeps in-memory traceability for runtime selection. */
    public BusinessGraphProjection projectTraceable(BusinessDecisionGraph exact) {
        Objects.requireNonNull(exact, "exact");
        String businessGraphId = businessGraphId(exact);

        Map<String, BusinessDecisionGraph.DecisionNode> exactNodes = indexNodes(exact);
        Map<String, List<BusinessDecisionGraph.DecisionEdge>> incoming = incoming(exact);
        Map<String, List<BusinessDecisionGraph.DecisionEdge>> outgoing = outgoing(exact);
        Map<String, LoopProjection> loops = loops(exact, exactNodes, outgoing);
        Set<String> redundantPredicates = redundantPredicateIds(exact);
        Set<String> duplicateExpandedCalls = duplicateExpandedCallIds(exact);
        Set<String> passThroughCalls = passThroughCallIds(exact);
        Set<String> expandedPredicateDetails = expandedPredicateDetailIds(exact);
        Set<String> aggregateDetails = aggregateDetailIds(exact);
        Set<String> hiddenLoopNodes = loops.values().stream()
                .flatMap(loop -> loop.hiddenNodeIds().stream())
                .collect(java.util.stream.Collectors.toSet());

        var projectionByExactId = new LinkedHashMap<String, NodeProjection>();
        var projectedByExactId = new LinkedHashMap<String, BusinessLogicGraph.Node>();
        for (int index = 0; index < exact.nodes().size(); index++) {
            BusinessDecisionGraph.DecisionNode node = exact.nodes().get(index);
            NodeProjection projection = projectNode(
                    node, loops.get(node.nodeId()), hiddenLoopNodes, redundantPredicates,
                    duplicateExpandedCalls, passThroughCalls,
                    expandedPredicateDetails, aggregateDetails,
                    businessGraphId, index);
            projectionByExactId.put(node.nodeId(), projection);
            if (projection.node() != null) projectedByExactId.put(node.nodeId(), projection.node());
        }

        Set<String> invertedBusinessNodeIds = projectionByExactId.values().stream()
                .filter(NodeProjection::invertOutcome).map(NodeProjection::node).filter(Objects::nonNull)
                .map(BusinessLogicGraph.Node::nodeId).collect(java.util.stream.Collectors.toUnmodifiableSet());

        var nodes = new ArrayList<>(projectedByExactId.values());
        var resultByExactEdgeId = new LinkedHashMap<String, String>();
        Set<String> projectedLoopNodeIds = loops.keySet().stream()
                .map(projectedByExactId::get)
                .filter(Objects::nonNull)
                .map(BusinessLogicGraph.Node::nodeId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        var targets = new ArrayList<Target>();
        projectedByExactId.forEach((exactId, node) ->
                targets.add(Target.exact(node.nodeId(), exactId)));
        for (int index = 0; index < exact.edges().size(); index++) {
            BusinessDecisionGraph.DecisionEdge edge = exact.edges().get(index);
            BusinessDecisionGraph.DecisionNode target = exactNodes.get(edge.toNodeId());
            if (target == null || target.kind() != BusinessDecisionGraph.NodeKind.OUTCOME) continue;
            String id = opaqueId("result", businessGraphId, "terminal:" + index);
            nodes.add(new BusinessLogicGraph.Node(id, BusinessLogicGraph.NodeKind.RESULT,
                    resultLabel(exact, edge, exactNodes, incoming)));
            targets.add(Target.result(id, edge.fromNodeId(), edge.edgeId()));
            resultByExactEdgeId.put(edge.edgeId(), id);
        }

        if (nodes.isEmpty()) {
            String id = opaqueId("result", businessGraphId, "empty");
            nodes.add(new BusinessLogicGraph.Node(id, BusinessLogicGraph.NodeKind.RESULT, "completed"));
            targets.add(Target.result(id, exact.entryNodeId(), null));
        }

        var entries = new LinkedHashSet<String>();
        var edges = new ArrayList<BusinessLogicGraph.Edge>();
        var edgeKeys = new HashSet<String>();
        var exactPathsByBusinessEdgeId = new LinkedHashMap<String, List<List<String>>>();
        for (Target target : targets) {
            if (target.result()) {
                walkBackward(exact, businessGraphId, target.exactNodeId(), target,
                        target.exactEdgeId() == null
                                ? List.of()
                                : List.of(requireExactEdge(exact, target.exactEdgeId())),
                        projectedByExactId, projectedLoopNodeIds, incoming,
                        invertedBusinessNodeIds,
                        entries, edges, edgeKeys, exactPathsByBusinessEdgeId, new HashSet<>());
            } else {
                List<BusinessDecisionGraph.DecisionEdge> predecessors = incoming
                        .getOrDefault(target.exactNodeId(), List.of());
                if (predecessors.isEmpty() && target.exactNodeId().equals(exact.entryNodeId())) {
                    entries.add(target.businessNodeId());
                }
                for (BusinessDecisionGraph.DecisionEdge predecessor : predecessors) {
                    walkBackward(exact, businessGraphId, predecessor.fromNodeId(), target,
                            List.of(predecessor),
                            projectedByExactId, projectedLoopNodeIds, incoming,
                            invertedBusinessNodeIds,
                            entries, edges, edgeKeys, exactPathsByBusinessEdgeId, new HashSet<>());
                }
            }
        }

        removeTransitiveOutcomeEdges(edges);
        removeUnreachableNodes(nodes, edges, entries);
        if (entries.isEmpty() && !nodes.isEmpty()) entries.add(nodes.getFirst().nodeId());

        Set<String> retainedNodeIds = nodes.stream().map(BusinessLogicGraph.Node::nodeId)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> retainedEdgeIds = edges.stream().map(BusinessLogicGraph.Edge::edgeId)
                .collect(java.util.stream.Collectors.toSet());
        projectedByExactId.entrySet().removeIf(entry -> !retainedNodeIds.contains(entry.getValue().nodeId()));
        resultByExactEdgeId.entrySet().removeIf(entry -> !retainedNodeIds.contains(entry.getValue()));
        exactPathsByBusinessEdgeId.entrySet().removeIf(entry -> !retainedEdgeIds.contains(entry.getKey()));

        BusinessLogicGraph graph = new BusinessLogicGraph(
                businessGraphId, exact.version(), cleanLabel(exact.decisionLabel()), List.copyOf(entries),
                List.copyOf(nodes), List.copyOf(edges),
                exact.completeness() == BusinessDecisionGraph.Completeness.COMPLETE
                        ? BusinessLogicGraph.Completeness.COMPLETE
                        : BusinessLogicGraph.Completeness.INCOMPLETE);
        new BusinessLogicArtifactGuard().requireClean(graph);
        var businessNodeIdsByExactNodeId = new LinkedHashMap<String, String>();
        projectedByExactId.forEach((exactId, node) ->
                businessNodeIdsByExactNodeId.put(exactId, node.nodeId()));
        var decisions = new ArrayList<BusinessGraphProjection.Decision>();
        for (BusinessDecisionGraph.DecisionNode exactNode : exact.nodes()) {
            NodeProjection projection = projectionByExactId.get(exactNode.nodeId());
            BusinessLogicGraph.Node retained = projectedByExactId.get(exactNode.nodeId());
            BusinessGraphProjection.Action action;
            BusinessGraphProjection.Reason reason;
            List<String> businessNodeIds;
            if (retained != null) {
                action = BusinessGraphProjection.Action.KEPT;
                reason = projection.reason();
                businessNodeIds = List.of(retained.nodeId());
            } else if (projection.node() != null) {
                action = BusinessGraphProjection.Action.REMOVED;
                reason = BusinessGraphProjection.Reason.UNREACHABLE;
                businessNodeIds = List.of();
            } else {
                action = BusinessGraphProjection.Action.REMOVED;
                reason = projection.reason();
                businessNodeIds = List.of();
            }
            decisions.add(new BusinessGraphProjection.Decision(
                    exactNode.nodeId(), BusinessGraphProjection.SubjectKind.NODE,
                    exactNode.kind().name(), exactNode.businessLabel(), action, reason, businessNodeIds));
        }
        for (BusinessDecisionGraph.DecisionEdge edge : exact.edges()) {
            BusinessDecisionGraph.DecisionNode target = exactNodes.get(edge.toNodeId());
            if (target == null || target.kind() != BusinessDecisionGraph.NodeKind.OUTCOME) continue;
            String resultNodeId = resultByExactEdgeId.get(edge.edgeId());
            if (resultNodeId == null) {
                decisions.add(new BusinessGraphProjection.Decision(
                        edge.edgeId(), BusinessGraphProjection.SubjectKind.TERMINAL_EDGE,
                        "TERMINAL_EDGE", terminalLabel(edge), BusinessGraphProjection.Action.REMOVED,
                        BusinessGraphProjection.Reason.UNREACHABLE, List.of()));
            } else {
                decisions.add(new BusinessGraphProjection.Decision(
                        edge.edgeId(), BusinessGraphProjection.SubjectKind.TERMINAL_EDGE,
                        "TERMINAL_EDGE", terminalLabel(edge), BusinessGraphProjection.Action.REPLACED,
                        BusinessGraphProjection.Reason.TERMINAL_RESULT, List.of(resultNodeId)));
            }
        }
        if (exact.nodes().stream().noneMatch(node ->
                projectionByExactId.get(node.nodeId()).node() != null)
                && resultByExactEdgeId.isEmpty()) {
            decisions.add(new BusinessGraphProjection.Decision(
                    exact.graphId(), BusinessGraphProjection.SubjectKind.GRAPH,
                    "GRAPH", exact.decisionLabel(), BusinessGraphProjection.Action.REPLACED,
                    BusinessGraphProjection.Reason.COMPLETED_FALLBACK,
                    List.of(graph.nodes().getFirst().nodeId())));
        }
        return new BusinessGraphProjection(
                graph,
                businessNodeIdsByExactNodeId,
                resultByExactEdgeId,
                exactPathsByBusinessEdgeId,
                decisions);
    }

    private static BusinessGraphProjection.Decision remap(
            BusinessGraphProjection.Decision decision,
            Map<String, String> finalNodeIdsByInputNodeId) {
        List<String> finalNodeIds = decision.businessNodeIds().stream()
                .map(nodeId -> {
                    String finalNodeId = finalNodeIdsByInputNodeId.get(nodeId);
                    if (finalNodeId == null) {
                        throw new IllegalArgumentException(
                                "projection decision refers to an unknown summary input node: " + nodeId);
                    }
                    return finalNodeId;
                })
                .distinct().toList();
        return new BusinessGraphProjection.Decision(
                decision.subjectId(), decision.subjectKind(), decision.sourceKind(),
                decision.sourceLabel(), decision.action(), decision.reason(), finalNodeIds);
    }

    private static String terminalLabel(BusinessDecisionGraph.DecisionEdge edge) {
        return edge.outcome().isBlank() ? "terminal result" : edge.outcome();
    }

    private static void walkBackward(
            BusinessDecisionGraph exact,
            String businessGraphId,
            String cursor,
            Target target,
            List<BusinessDecisionGraph.DecisionEdge> exactPath,
            Map<String, BusinessLogicGraph.Node> projectedByExactId,
            Set<String> projectedLoopNodeIds,
            Map<String, List<BusinessDecisionGraph.DecisionEdge>> incoming,
            Set<String> invertedBusinessNodeIds,
            Set<String> entries,
            List<BusinessLogicGraph.Edge> edges,
            Set<String> edgeKeys,
            Map<String, List<List<String>>> exactPathsByBusinessEdgeId,
            Set<String> visited) {
        BusinessLogicGraph.Node source = projectedByExactId.get(cursor);
        if (source != null) {
            if (!source.nodeId().equals(target.businessNodeId())) {
                addEdge(businessGraphId, source, projectedLoopNodeIds.contains(source.nodeId()),
                        invertedBusinessNodeIds.contains(source.nodeId()), target, exactPath,
                        edges, edgeKeys, exactPathsByBusinessEdgeId);
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
            var path = new ArrayList<BusinessDecisionGraph.DecisionEdge>(exactPath.size() + 1);
            path.add(predecessor);
            path.addAll(exactPath);
            walkBackward(exact, businessGraphId, predecessor.fromNodeId(), target, List.copyOf(path),
                    projectedByExactId, projectedLoopNodeIds, incoming,
                    invertedBusinessNodeIds,
                    entries, edges, edgeKeys, exactPathsByBusinessEdgeId, new HashSet<>(visited));
        }
    }

    private static void addEdge(
            String graphId,
            BusinessLogicGraph.Node source,
            boolean loopRule,
            boolean invertOutcome,
            Target target,
            List<BusinessDecisionGraph.DecisionEdge> exactPath,
            List<BusinessLogicGraph.Edge> edges,
            Set<String> edgeKeys,
            Map<String, List<List<String>>> exactPathsByBusinessEdgeId) {
        String outcome = businessOutcome(source.kind(), loopRule, exactPath.stream()
                .map(BusinessDecisionGraph.DecisionEdge::outcome).toList());
        if (invertOutcome) outcome = invert(outcome);
        String key = source.nodeId() + '\u0000' + target.businessNodeId() + '\u0000' + outcome;
        String edgeId = opaqueId("edge", graphId, key);
        if (edgeKeys.add(key)) {
            edges.add(new BusinessLogicGraph.Edge(
                    edgeId, source.nodeId(), target.businessNodeId(), outcome));
        }
        List<String> exactEdgeIds = exactPath.stream()
                .map(BusinessDecisionGraph.DecisionEdge::edgeId).toList();
        var alternatives = exactPathsByBusinessEdgeId.computeIfAbsent(edgeId, ignored -> new ArrayList<>());
        if (!alternatives.contains(exactEdgeIds)) alternatives.add(exactEdgeIds);
    }

    private NodeProjection projectNode(
            BusinessDecisionGraph.DecisionNode node,
            LoopProjection loop,
            Set<String> hiddenLoopNodes,
            Set<String> redundantPredicates,
            Set<String> duplicateExpandedCalls,
            Set<String> passThroughCalls,
            Set<String> expandedPredicateDetails,
            Set<String> aggregateDetails,
            String businessGraphId,
            int nodeIndex) {
        boolean redundantRule = redundantPredicates.contains(node.nodeId())
                || duplicateExpandedCalls.contains(node.nodeId())
                || passThroughCalls.contains(node.nodeId())
                || expandedPredicateDetails.contains(node.nodeId());
        redundantRule = redundantRule || aggregateDetails.contains(node.nodeId());
        boolean loopMechanics = hiddenLoopNodes.contains(node.nodeId())
                && !businessEffectInsideLoop(node);
        boolean loopRule = loop != null;
        String label = "";
        boolean technical = false;
        if (!redundantRule && !loopMechanics && !loopRule) {
            BusinessSemanticReducer.Reduction reduction = semanticReducer.reduce(node);
            label = reduction.label();
            technical = reduction.technical() || technical(node.kind(), label);
            if (reduction.promoteRule()) {
                return NodeProjection.kept(new BusinessLogicGraph.Node(
                        opaqueId("rule", businessGraphId, "node:" + nodeIndex),
                        BusinessLogicGraph.NodeKind.RULE, label),
                        BusinessGraphProjection.Reason.BUSINESS_RULE);
            }
            if (reduction.invertOutcome()) {
                return projectedNode(node, businessGraphId, nodeIndex, label, technical, true);
            }
        }
        BusinessGraphProjection.Reason reason = classifyNode(
                node.kind(), redundantRule, loopMechanics, loopRule, technical);
        return switch (reason) {
            case LOOP_RULE -> NodeProjection.kept(new BusinessLogicGraph.Node(
                    opaqueId("rule", businessGraphId, "node:" + nodeIndex),
                    BusinessLogicGraph.NodeKind.RULE, loop.label()), reason);
            case BUSINESS_RULE -> NodeProjection.kept(new BusinessLogicGraph.Node(
                    opaqueId("rule", businessGraphId, "node:" + nodeIndex),
                    BusinessLogicGraph.NodeKind.RULE, label), reason);
            case BUSINESS_ACTION -> NodeProjection.kept(new BusinessLogicGraph.Node(
                    opaqueId("action", businessGraphId, "node:" + nodeIndex),
                    BusinessLogicGraph.NodeKind.ACTION, label), reason);
            case COVERAGE_GAP -> NodeProjection.kept(new BusinessLogicGraph.Node(
                    opaqueId("gap", businessGraphId, "node:" + nodeIndex),
                    BusinessLogicGraph.NodeKind.GAP, GAP_LABEL), reason);
            case STRUCTURAL_ENTRY, STRUCTURAL_OUTCOME, REDUNDANT_RULE, LOOP_MECHANICS,
                    TECHNICAL_PREDICATE, TECHNICAL_CHOICE, TECHNICAL_DISPATCH,
                    TECHNICAL_CALCULATION -> NodeProjection.removed(reason);
            case TERMINAL_RESULT, COMPLETED_FALLBACK, UNREACHABLE ->
                    throw new IllegalStateException("node classifier returned " + reason);
        };
    }

    private static NodeProjection projectedNode(
            BusinessDecisionGraph.DecisionNode node,
            String businessGraphId,
            int nodeIndex,
            String label,
            boolean technical,
            boolean invertOutcome) {
        BusinessGraphProjection.Reason reason = classifyNode(
                node.kind(), false, false, false, technical);
        return switch (reason) {
            case BUSINESS_RULE -> NodeProjection.kept(new BusinessLogicGraph.Node(
                    opaqueId("rule", businessGraphId, "node:" + nodeIndex),
                    BusinessLogicGraph.NodeKind.RULE, label), reason, invertOutcome);
            case TECHNICAL_PREDICATE -> NodeProjection.removed(reason);
            default -> throw new IllegalStateException("inverted semantic reduction requires a predicate");
        };
    }

    /** Returns the final keep or remove reason for one exact node. */
    static BusinessGraphProjection.Reason classifyNode(
            BusinessDecisionGraph.NodeKind nodeKind,
            boolean redundantRule,
            boolean loopMechanics,
            boolean loopRule,
            boolean technical) {
        if (redundantRule) return BusinessGraphProjection.Reason.REDUNDANT_RULE;
        if (loopMechanics) return BusinessGraphProjection.Reason.LOOP_MECHANICS;
        if (loopRule) return BusinessGraphProjection.Reason.LOOP_RULE;
        switch (nodeKind) {
            case ENTRY:
                return BusinessGraphProjection.Reason.STRUCTURAL_ENTRY;
            case OUTCOME:
                return BusinessGraphProjection.Reason.STRUCTURAL_OUTCOME;
            case PREDICATE:
                if (technical) return BusinessGraphProjection.Reason.TECHNICAL_PREDICATE;
                return BusinessGraphProjection.Reason.BUSINESS_RULE;
            case CHOICE:
                if (technical) return BusinessGraphProjection.Reason.TECHNICAL_CHOICE;
                return BusinessGraphProjection.Reason.BUSINESS_RULE;
            case DISPATCH:
                if (technical) return BusinessGraphProjection.Reason.TECHNICAL_DISPATCH;
                return BusinessGraphProjection.Reason.BUSINESS_RULE;
            case COMPUTATION:
                if (technical) return BusinessGraphProjection.Reason.TECHNICAL_CALCULATION;
                return BusinessGraphProjection.Reason.BUSINESS_ACTION;
            case COVERAGE_GAP:
                return BusinessGraphProjection.Reason.COVERAGE_GAP;
            default:
                throw new IllegalStateException("unsupported exact node kind " + nodeKind);
        }
    }

    private static boolean technical(BusinessDecisionGraph.NodeKind nodeKind, String label) {
        return switch (nodeKind) {
            case PREDICATE -> technicalPredicate(label);
            case CHOICE -> technicalChoice(label);
            case DISPATCH -> technicalDispatch(label);
            case COMPUTATION -> technicalCalculation(label);
            default -> false;
        };
    }

    private static Set<String> redundantPredicateIds(BusinessDecisionGraph exact) {
        boolean hasNamedNewRule = exact.nodes().stream().anyMatch(node ->
                node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE
                        && node.businessLabel().toLowerCase(Locale.ROOT).matches(".+ is new"));
        boolean hasDuplicateFailureRule = exact.nodes().stream().anyMatch(node -> {
            String label = node.businessLabel().toLowerCase(Locale.ROOT);
            return node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE
                    && label.contains("duplicate") && label.endsWith(" violation ex");
        });
        Set<String> positiveLabels = exact.nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE)
                .map(node -> cleanLabel(node.businessLabel()).toLowerCase(Locale.ROOT))
                .filter(label -> !label.startsWith("not "))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return exact.nodes().stream().filter(node -> {
            if (node.kind() != BusinessDecisionGraph.NodeKind.PREDICATE) return false;
            String label = cleanLabel(node.businessLabel()).toLowerCase(Locale.ROOT);
            return (hasNamedNewRule && label.equals("value is absent"))
                    || (hasDuplicateFailureRule && (label.equals("message exists")
                    || label.contains(" to lower case contains ")))
                    || (label.startsWith("not ") && positiveLabels.contains(label.substring(4).strip()));
        }).map(BusinessDecisionGraph.DecisionNode::nodeId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Set<String> expandedPredicateDetailIds(BusinessDecisionGraph exact) {
        Set<String> representedMethods = exact.nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE
                        || booleanBusinessCall(node))
                .map(BusinessGraphProjector::calledMethodKey).filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return exact.nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE)
                .filter(node -> representedMethods.contains(enclosingMethodKey(node)))
                .map(BusinessDecisionGraph.DecisionNode::nodeId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Set<String> aggregateDetailIds(BusinessDecisionGraph exact) {
        Set<String> aggregateScopes = exact.nodes().stream()
                .filter(node -> at.gepardec.fachtracing.model.BusinessSemanticAttributes.AGGREGATE
                        .equals(node.attributes().get(
                                at.gepardec.fachtracing.model.BusinessSemanticAttributes.ROLE)))
                .map(node -> node.attributes().getOrDefault(
                        at.gepardec.fachtracing.model.BusinessSemanticAttributes.AGGREGATE_SCOPE, ""))
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return exact.nodes().stream()
                .filter(node -> aggregateScopes.contains(node.attributes().getOrDefault(
                                at.gepardec.fachtracing.model.BusinessSemanticAttributes.PARENT_AGGREGATE_SCOPE, ""))
                        || (!at.gepardec.fachtracing.model.BusinessSemanticAttributes.AGGREGATE
                                .equals(node.attributes().get(
                                        at.gepardec.fachtracing.model.BusinessSemanticAttributes.ROLE))
                        && aggregateScopes.contains(node.attributes().getOrDefault(
                                at.gepardec.fachtracing.model.BusinessSemanticAttributes.AGGREGATE_SCOPE, ""))))
                .map(BusinessDecisionGraph.DecisionNode::nodeId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Set<String> duplicateExpandedCallIds(BusinessDecisionGraph exact) {
        Set<String> predicateCalls = exact.nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE)
                .map(BusinessGraphProjector::calledMethodKey).filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return exact.nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.COMPUTATION)
                .filter(node -> predicateCalls.contains(calledMethodKey(node)))
                .map(BusinessDecisionGraph.DecisionNode::nodeId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Set<String> passThroughCallIds(BusinessDecisionGraph exact) {
        return exact.nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.COMPUTATION)
                .filter(node -> node.attributes().getOrDefault(
                        at.gepardec.fachtracing.model.BusinessSemanticAttributes.CALL_METHOD, "")
                        .equals(node.attributes().getOrDefault(
                                at.gepardec.fachtracing.model.BusinessSemanticAttributes.ENCLOSING_METHOD, "_")))
                .map(BusinessDecisionGraph.DecisionNode::nodeId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static boolean booleanBusinessCall(BusinessDecisionGraph.DecisionNode node) {
        String returnType = node.attributes().getOrDefault(
                at.gepardec.fachtracing.model.BusinessSemanticAttributes.CALL_RETURN_TYPE, "");
        String owner = node.attributes().getOrDefault(
                at.gepardec.fachtracing.model.BusinessSemanticAttributes.CALL_OWNER_TYPE, "");
        return returnType.equals("boolean") && !owner.startsWith("java.")
                && !owner.startsWith("javax.") && !owner.startsWith("jakarta.");
    }

    private static String calledMethodKey(BusinessDecisionGraph.DecisionNode node) {
        String owner = node.attributes().getOrDefault(
                at.gepardec.fachtracing.model.BusinessSemanticAttributes.CALL_OWNER_TYPE, "");
        String method = node.attributes().getOrDefault(
                at.gepardec.fachtracing.model.BusinessSemanticAttributes.CALL_METHOD, "");
        return owner.isBlank() || method.isBlank() ? "" : owner + '#' + method;
    }

    private static String enclosingMethodKey(BusinessDecisionGraph.DecisionNode node) {
        String owner = node.attributes().getOrDefault(
                at.gepardec.fachtracing.model.BusinessSemanticAttributes.OWNER_TYPE, "");
        String method = node.attributes().getOrDefault(
                at.gepardec.fachtracing.model.BusinessSemanticAttributes.ENCLOSING_METHOD, "");
        return owner.isBlank() || method.isBlank() ? "" : owner + '#' + method;
    }

    private static String businessGraphId(BusinessDecisionGraph exact) {
        var indices = new HashMap<String, Integer>();
        var signature = new StringBuilder(exact.decisionLabel()).append('\u0000');
        for (int index = 0; index < exact.nodes().size(); index++) {
            BusinessDecisionGraph.DecisionNode node = exact.nodes().get(index);
            indices.put(node.nodeId(), index);
            signature.append(node.kind()).append(':').append(node.businessLabel()).append('\u0000');
        }
        for (BusinessDecisionGraph.DecisionEdge edge : exact.edges()) {
            signature.append(indices.get(edge.fromNodeId())).append('>')
                    .append(indices.get(edge.toNodeId())).append(':').append(edge.outcome()).append('\u0000');
        }
        signature.append(exact.completeness());
        return opaqueId("business", exact.decisionLabel(), signature.toString());
    }

    private static Map<String, LoopProjection> loops(
            BusinessDecisionGraph graph,
            Map<String, BusinessDecisionGraph.DecisionNode> nodes,
            Map<String, List<BusinessDecisionGraph.DecisionEdge>> outgoing) {
        var loops = new LinkedHashMap<String, LoopProjection>();
        for (BusinessDecisionGraph.DecisionNode node : graph.nodes()) {
            String label = node.businessLabel().toLowerCase(Locale.ROOT);
            if (!label.startsWith("for each ") && !label.startsWith("repeat while ")) continue;
            var hidden = new LinkedHashSet<>(loopBody(node.nodeId(), nodes, outgoing));
            String anchor = loopAnchor(node.nodeId(), nodes, outgoing);
            if (loops.containsKey(anchor)) anchor = node.nodeId();
            if (!anchor.equals(node.nodeId())) hidden.add(node.nodeId());
            loops.put(anchor, new LoopProjection(
                    loopLabel(node.businessLabel(), hidden, nodes), Set.copyOf(hidden)));
        }
        return Map.copyOf(loops);
    }

    private static String loopAnchor(
            String loopNodeId,
            Map<String, BusinessDecisionGraph.DecisionNode> nodes,
            Map<String, List<BusinessDecisionGraph.DecisionEdge>> outgoing) {
        List<String> exitTargets = outgoing.getOrDefault(loopNodeId, List.of()).stream()
                .filter(edge -> edge.outcome().toLowerCase(Locale.ROOT).contains("done"))
                .map(BusinessDecisionGraph.DecisionEdge::toNodeId)
                .distinct().toList();
        if (exitTargets.size() != 1) return loopNodeId;
        var level = new LinkedHashSet<>(exitTargets);
        var visited = new HashSet<String>();
        visited.add(loopNodeId);
        for (int distance = 0; distance < 12 && !level.isEmpty(); distance++) {
            List<String> candidates = level.stream().filter(nodeId -> {
                BusinessDecisionGraph.DecisionNode node = nodes.get(nodeId);
                return node != null && node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE
                        && technicalPredicate(cleanLabel(node.businessLabel()));
            }).toList();
            if (candidates.size() == 1) return candidates.getFirst();
            if (candidates.size() > 1) return loopNodeId;
            var next = new LinkedHashSet<String>();
            for (String nodeId : level) {
                if (!visited.add(nodeId)) continue;
                BusinessDecisionGraph.DecisionNode node = nodes.get(nodeId);
                if (node == null || !canLeadToLoopResult(node)) continue;
                outgoing.getOrDefault(nodeId, List.of()).stream()
                        .map(BusinessDecisionGraph.DecisionEdge::toNodeId)
                        .filter(target -> !visited.contains(target))
                        .forEach(next::add);
            }
            level = next;
        }
        return loopNodeId;
    }

    private static boolean canLeadToLoopResult(BusinessDecisionGraph.DecisionNode node) {
        if (node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE) return true;
        return node.kind() == BusinessDecisionGraph.NodeKind.COMPUTATION
                && technicalCalculation(cleanLabel(node.businessLabel()));
    }

    private static Set<String> loopBody(
            String loopNodeId,
            Map<String, BusinessDecisionGraph.DecisionNode> nodes,
            Map<String, List<BusinessDecisionGraph.DecisionEdge>> outgoing) {
        var hidden = new LinkedHashSet<String>();
        var queue = new ArrayDeque<String>();
        Set<String> exitTargets = outgoing.getOrDefault(loopNodeId, List.of()).stream()
                .filter(edge -> edge.outcome().toLowerCase(Locale.ROOT).contains("done"))
                .map(BusinessDecisionGraph.DecisionEdge::toNodeId)
                .collect(java.util.stream.Collectors.toSet());
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
                String outcome = edge.outcome().toLowerCase(Locale.ROOT);
                if (!edge.toNodeId().equals(loopNodeId) && !exitTargets.contains(edge.toNodeId())
                        && !outcome.contains("returns ")
                        && !outcome.contains("fails")) {
                    queue.add(edge.toNodeId());
                }
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

    private static String resultLabel(
            BusinessDecisionGraph exact,
            BusinessDecisionGraph.DecisionEdge terminal,
            Map<String, BusinessDecisionGraph.DecisionNode> nodes,
            Map<String, List<BusinessDecisionGraph.DecisionEdge>> incoming) {
        String outcome = terminal.outcome();
        String lower = outcome.toLowerCase(Locale.ROOT).trim();
        if (lower.contains("fails")) return hasSemanticEvidence(exact)
                ? cleanLabel(exact.decisionLabel()) + " could not be completed"
                : "operation failed";
        int returns = lower.indexOf("returns ");
        if (returns < 0) return "completed";
        String prefix = lower.substring(0, returns);
        String value = outcome.substring(returns + "returns ".length()).trim()
                .replaceAll("^[\\\"']|[\\\"']$", "");
        String normalized = value.toLowerCase(Locale.ROOT);
        List<PathContext> contexts = pathContexts(terminal, nodes, incoming);
        String selectedTernary = selectedTernaryResult(value, prefix, contexts);
        if (selectedTernary != null) return cleanLabel(selectedTernary);
        if (contexts.stream().anyMatch(context -> context.label().contains("page is empty")
                && affirmative(context.outcome()))) {
            return "no matching records";
        }
        if (contexts.stream().anyMatch(context -> context.label().contains("result count equals 1")
                && affirmative(context.outcome()))) {
            return "one matching record";
        }
        if (contexts.stream().anyMatch(context -> context.label().contains("result count equals 1")
                && negative(context.outcome()))) {
            return "multiple matching records";
        }
        if (normalized.startsWith("whether ") || normalized.startsWith("any of ")
                || normalized.equals("true") || normalized.equals("false")) {
            return prefix.contains("false") ? "condition is not met" : "condition is met";
        }
        if (normalized.equals("absent") || normalized.equals("null") || normalized.equals("empty")) {
            return "no matching result";
        }
        if (technicalResponse(normalized)) {
            if (contexts.stream().anyMatch(context -> context.label().contains("validation has errors")
                    && affirmative(context.outcome()))) {
                return "correction required";
            }
            int correctionDistance = contexts.stream()
                    .filter(context -> context.label().startsWith("record ")
                            && context.label().contains("validation error"))
                    .mapToInt(PathContext::distance).min().orElse(Integer.MAX_VALUE);
            int validDistance = contexts.stream()
                    .filter(context -> context.label().contains("validation has errors")
                            && negative(context.outcome()))
                    .mapToInt(PathContext::distance).min().orElse(Integer.MAX_VALUE);
            if (correctionDistance < validDistance) {
                return "correction required";
            }
            return cleanLabel(exact.decisionLabel()) + " completed";
        }
        if (technicalResultExpression(normalized)) {
            return cleanLabel(exact.decisionLabel()) + " completed";
        }
        if (normalized.matches(".*\\b(?:adapter|controller|port|repository|service)\\b.*")) {
            return cleanLabel(exact.decisionLabel()) + " completed";
        }
        if (normalized.isBlank()) return "completed";
        return cleanLabel(value);
    }

    private static boolean hasSemanticEvidence(BusinessDecisionGraph exact) {
        return exact.nodes().stream().anyMatch(node -> node.attributes().keySet().stream()
                .anyMatch(key -> key.startsWith("semantic.")));
    }

    private static String selectedTernaryResult(
            String value, String outcomePrefix, List<PathContext> contexts) {
        int question = value.indexOf('?');
        int colon = question < 0 ? -1 : value.indexOf(':', question + 1);
        if (question < 0 || colon < 0) return null;
        String whenTrue = value.substring(question + 1, colon).strip();
        String whenFalse = value.substring(colon + 1).strip();
        if (whenTrue.isBlank() || whenFalse.isBlank()) return null;
        String falsePathLabel = "use " + cleanLabel(whenFalse).toLowerCase(Locale.ROOT);
        if (contexts.stream().anyMatch(context -> context.label().equals(falsePathLabel))) return whenFalse;
        String truePathLabel = "use " + cleanLabel(whenTrue).toLowerCase(Locale.ROOT);
        if (contexts.stream().anyMatch(context -> context.label().equals(truePathLabel))) return whenTrue;
        return outcomePrefix.contains("false") ? whenFalse : whenTrue;
    }

    private static boolean technicalResultExpression(String value) {
        long conjunctions = Pattern.compile("\\band\\b").matcher(value).results().count();
        return value.contains(" new ") || value.contains("<>")
                || value.matches(".*\\b(?:true|false)\\b.*")
                || conjunctions >= 3;
    }

    private static boolean technicalResponse(String value) {
        return value.startsWith("redirect:") || value.startsWith("forward:")
                || value.startsWith("views ") || value.startsWith("view ") || value.contains("/");
    }

    private static List<PathContext> pathContexts(
            BusinessDecisionGraph.DecisionEdge terminal,
            Map<String, BusinessDecisionGraph.DecisionNode> nodes,
            Map<String, List<BusinessDecisionGraph.DecisionEdge>> incoming) {
        var contexts = new ArrayList<PathContext>();
        var pending = new ArrayDeque<ContextCursor>();
        pending.add(new ContextCursor(terminal.fromNodeId(), terminal.outcome(), 0, Set.of()));
        while (!pending.isEmpty() && contexts.size() < 64) {
            ContextCursor cursor = pending.removeFirst();
            if (cursor.visited().contains(cursor.nodeId())) continue;
            var visited = new HashSet<>(cursor.visited());
            visited.add(cursor.nodeId());
            BusinessDecisionGraph.DecisionNode node = nodes.get(cursor.nodeId());
            if (node != null && node.kind() != BusinessDecisionGraph.NodeKind.ENTRY
                    && node.kind() != BusinessDecisionGraph.NodeKind.OUTCOME) {
                contexts.add(new PathContext(
                        node.businessLabel().toLowerCase(Locale.ROOT), cursor.outcome(), cursor.distance()));
            }
            for (BusinessDecisionGraph.DecisionEdge predecessor
                    : incoming.getOrDefault(cursor.nodeId(), List.of())) {
                pending.add(new ContextCursor(
                        predecessor.fromNodeId(), predecessor.outcome(), cursor.distance() + 1,
                        Set.copyOf(visited)));
            }
        }
        return List.copyOf(contexts);
    }

    private static boolean affirmative(String outcome) {
        String value = outcome.toLowerCase(Locale.ROOT);
        return value.startsWith("true") || value.equals("yes") || value.equals("item");
    }

    private static boolean negative(String outcome) {
        String value = outcome.toLowerCase(Locale.ROOT);
        return value.startsWith("false") || value.equals("no") || value.equals("done");
    }

    private static String businessOutcome(
            BusinessLogicGraph.NodeKind sourceKind, boolean loopRule, List<String> outcomes) {
        if (sourceKind != BusinessLogicGraph.NodeKind.RULE) return "";
        int start = loopRule ? outcomes.size() - 1 : 0;
        int end = loopRule ? -1 : outcomes.size();
        int step = loopRule ? -1 : 1;
        for (int index = start; index != end; index += step) {
            String raw = outcomes.get(index);
            String first = raw.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            if (first.equals("true") || first.equals("item") || first.equals("match")) return "yes";
            if (first.equals("false") || first.equals("done") || first.equals("no match")) return "no";
        }
        return "";
    }

    private static String invert(String outcome) {
        if (outcome.equals("yes")) return "no";
        if (outcome.equals("no")) return "yes";
        return outcome;
    }

    private static boolean technicalChoice(String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        return lower.contains("decision result path") || lower.startsWith("select ");
    }

    private static boolean technicalPredicate(String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        return lower.matches(".*\\band (?:true|false) exists$");
    }

    private static boolean technicalDispatch(String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        return lower.startsWith("select ") || lower.contains("applicable decision rule");
    }

    private static boolean technicalCalculation(String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        return lower.startsWith("derive ") || lower.startsWith("evaluate ")
                || lower.startsWith("use ") || lower.equals("decision cannot continue")
                || lower.equals("attributes") || lower.startsWith("put attributes with ")
                || lower.startsWith("put all attributes with ")
                || lower.startsWith("initialize ")
                || lower.contains(" != ") || lower.contains(" == ") || lower.contains(" ? ")
                || lower.contains(" new ") || lower.contains("<>") || lower.contains("::")
                || (lower.startsWith("next ") && lower.contains(" iterator"))
                || (lower.startsWith("set ") && (lower.endsWith(" to")
                || lower.contains(" strip") || lower.contains(" iterator")));
    }

    private static boolean businessEffectInsideLoop(BusinessDecisionGraph.DecisionNode node) {
        return node.kind() == BusinessDecisionGraph.NodeKind.COVERAGE_GAP
                || (node.kind() == BusinessDecisionGraph.NodeKind.COMPUTATION
                && !technicalCalculation(cleanLabel(node.businessLabel())));
    }

    private static String cleanLabel(String label) {
        return BusinessLanguageNormalizer.normalize(label);
    }

    private static Map<String, BusinessDecisionGraph.DecisionNode> indexNodes(BusinessDecisionGraph graph) {
        var nodes = new HashMap<String, BusinessDecisionGraph.DecisionNode>();
        graph.nodes().forEach(node -> nodes.put(node.nodeId(), node));
        return Map.copyOf(nodes);
    }

    private static BusinessDecisionGraph.DecisionEdge requireExactEdge(
            BusinessDecisionGraph graph, String edgeId) {
        return graph.edges().stream().filter(edge -> edge.edgeId().equals(edgeId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown exact edge ID"));
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

    private static void removeTransitiveOutcomeEdges(List<BusinessLogicGraph.Edge> edges) {
        List<BusinessLogicGraph.Edge> snapshot = List.copyOf(edges);
        edges.removeIf(direct -> snapshot.stream().anyMatch(first ->
                !first.edgeId().equals(direct.edgeId())
                        && first.fromNodeId().equals(direct.fromNodeId())
                        && first.outcome().equals(direct.outcome())
                        && reaches(first.toNodeId(), direct.toNodeId(), snapshot, Set.of(direct.edgeId()))));
    }

    private static boolean reaches(
            String from, String target, List<BusinessLogicGraph.Edge> edges, Set<String> excluded) {
        if (from.equals(target)) return true;
        var pending = new ArrayDeque<String>();
        var visited = new HashSet<String>();
        pending.add(from);
        while (!pending.isEmpty()) {
            String cursor = pending.removeFirst();
            if (!visited.add(cursor)) continue;
            for (BusinessLogicGraph.Edge edge : edges) {
                if (excluded.contains(edge.edgeId()) || !edge.fromNodeId().equals(cursor)) continue;
                if (edge.toNodeId().equals(target)) return true;
                pending.add(edge.toNodeId());
            }
        }
        return false;
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

    private record NodeProjection(
            BusinessLogicGraph.Node node,
            BusinessGraphProjection.Reason reason,
            boolean invertOutcome) {
        private NodeProjection {
            Objects.requireNonNull(reason, "reason");
        }

        private static NodeProjection kept(
                BusinessLogicGraph.Node node,
                BusinessGraphProjection.Reason reason) {
            return kept(node, reason, false);
        }

        private static NodeProjection kept(
                BusinessLogicGraph.Node node,
                BusinessGraphProjection.Reason reason,
                boolean invertOutcome) {
            return new NodeProjection(Objects.requireNonNull(node, "node"), reason, invertOutcome);
        }

        private static NodeProjection removed(BusinessGraphProjection.Reason reason) {
            return new NodeProjection(null, reason, false);
        }
    }

    private record LoopProjection(String label, Set<String> hiddenNodeIds) { }

    private record Target(
            String businessNodeId,
            String exactNodeId,
            boolean result,
            String exactEdgeId) {
        private static Target exact(String businessId, String exactId) {
            return new Target(businessId, exactId, false, null);
        }
        private static Target result(String businessId, String exactId, String exactEdgeId) {
            return new Target(businessId, exactId, true, exactEdgeId);
        }
    }

    private record PathContext(String label, String outcome, int distance) { }

    private record ContextCursor(String nodeId, String outcome, int distance, Set<String> visited) { }
}
