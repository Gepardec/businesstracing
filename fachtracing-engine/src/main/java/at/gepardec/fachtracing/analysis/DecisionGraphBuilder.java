package at.gepardec.fachtracing.analysis;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Builds immutable business graphs from source-derived node and edge drafts. */
public final class DecisionGraphBuilder {
    private final String graphId;
    private final String decisionLabel;
    private final List<BusinessDecisionGraph.DecisionNode> nodes = new ArrayList<>();
    private final List<BusinessDecisionGraph.DecisionEdge> edges = new ArrayList<>();
    private final List<BusinessDecisionGraph.CoverageGap> gaps = new ArrayList<>();
    private final Map<String, AnalysisManifest.SourceMapping> mappings = new LinkedHashMap<>();
    private final List<AnalysisManifest.ProbeSite> probes = new ArrayList<>();
    private final List<AnalysisManifest.DispatchTarget> dispatchTargets = new ArrayList<>();
    private final List<ControlBinding> controlBindings = new ArrayList<>();
    private final List<AnalysisManifest.EvidenceTarget> evidenceTargets = new ArrayList<>();
    private final List<AnalysisManifest.AnalysisDecision> analysisDecisions = new ArrayList<>();
    private final Map<String, List<AnalysisManifest.BranchCompletion>> branchCompletions = new LinkedHashMap<>();
    private int nodeSequence;
    private int edgeSequence;

    /** Creates a builder for one annotated decision method. */
    public DecisionGraphBuilder(String graphId, String decisionLabel) {
        this.graphId = requireText(graphId, "graphId");
        this.decisionLabel = requireText(decisionLabel, "decisionLabel");
    }

    /** Adds a business node plus its separate developer-only provenance. */
    public String addNode(
            BusinessDecisionGraph.NodeKind kind,
            String label,
            Map<String, String> attributes,
            AnalysisManifest.SourceMapping source,
            AnalysisManifest.ProbeKind probeKind,
            String ownerHint,
            String memberHint) {
        return addNode(kind, label, attributes, source, probeKind, ownerHint, memberHint, "");
    }

    /** Adds a business node with an exact developer-only JVM member binding. */
    public String addNode(
            BusinessDecisionGraph.NodeKind kind,
            String label,
            Map<String, String> attributes,
            AnalysisManifest.SourceMapping source,
            AnalysisManifest.ProbeKind probeKind,
            String ownerHint,
            String memberHint,
            String descriptorHint) {
        String businessLabel = BusinessLabelNormalizer.normalize(requireText(label, "label"));
        String nodeId = opaque("node", ++nodeSequence, kind.name(), businessLabel);
        nodes.add(new BusinessDecisionGraph.DecisionNode(nodeId, kind, businessLabel, attributes));
        if (source != null) {
            var resolvedSource = new AnalysisManifest.SourceMapping(
                    nodeId, source.source(), source.line(), source.column(), source.treeKind());
            mappings.put(nodeId, resolvedSource);
            addAnalysisDecision(action(kind), reason(kind), resolvedSource, List.of(nodeId), "");
        }
        if (probeKind != null) {
            probes.add(new AnalysisManifest.ProbeSite(nodeId, probeKind,
                    Objects.requireNonNullElse(ownerHint, ""), Objects.requireNonNullElse(memberHint, ""),
                    Objects.requireNonNullElse(descriptorHint, ""),
                    source == null ? -1 : source.line()));
        }
        return nodeId;
    }

    /** Adds one developer-only explanation for an analysis inclusion or exclusion. */
    public void addAnalysisDecision(
            AnalysisManifest.AnalysisAction action,
            AnalysisManifest.AnalysisReason reason,
            AnalysisManifest.SourceMapping source,
            List<String> nodeIds,
            String subject) {
        Objects.requireNonNull(source, "source");
        analysisDecisions.add(new AnalysisManifest.AnalysisDecision(
                action, reason, source.source(), source.line(), source.column(), source.treeKind(), nodeIds, subject));
    }

    /** Connects two graph nodes. */
    public String addEdge(String from, String to, String outcome) {
        String edgeId = opaque("edge", ++edgeSequence, from, to, Objects.requireNonNullElse(outcome, ""));
        edges.add(new BusinessDecisionGraph.DecisionEdge(edgeId, from, to, Objects.requireNonNullElse(outcome, "")));
        return edgeId;
    }

    /** Adds another bytecode probe that reports to an existing business node. */
    public void addProbe(
            String nodeId,
            AnalysisManifest.ProbeKind probeKind,
            String ownerHint,
            String memberHint) {
        addProbe(nodeId, probeKind, ownerHint, memberHint, "", mappings.get(nodeId));
    }

    /** Adds a probe for an existing node with provenance specific to this probe site. */
    public void addProbe(
            String nodeId,
            AnalysisManifest.ProbeKind probeKind,
            String ownerHint,
            String memberHint,
            AnalysisManifest.SourceMapping source) {
        addProbe(nodeId, probeKind, ownerHint, memberHint, "", source);
    }

    /** Adds a probe with an exact JVM descriptor. */
    public void addProbe(
            String nodeId,
            AnalysisManifest.ProbeKind probeKind,
            String ownerHint,
            String memberHint,
            String descriptorHint,
            AnalysisManifest.SourceMapping source) {
        probes.add(new AnalysisManifest.ProbeSite(nodeId, probeKind,
                Objects.requireNonNullElse(ownerHint, ""), Objects.requireNonNullElse(memberHint, ""),
                Objects.requireNonNullElse(descriptorHint, ""),
                source == null ? -1 : source.line()));
    }

    /** Adds a developer-only implementation-entry binding for runtime dispatch correlation. */
    public void addDispatchTarget(String dispatchNodeId, String edgeId, String ownerHint, String memberHint) {
        addDispatchTarget(dispatchNodeId, edgeId, ownerHint, memberHint, "");
    }

    /** Adds a dispatch target with an exact JVM descriptor. */
    public void addDispatchTarget(
            String dispatchNodeId,
            String edgeId,
            String ownerHint,
            String memberHint,
            String descriptorHint) {
        dispatchTargets.add(new AnalysisManifest.DispatchTarget(
                dispatchNodeId, edgeId, ownerHint, memberHint, descriptorHint));
    }

    /** Defines how the ordered bytecode jumps can complete one source predicate. */
    public void setBranchCompletions(
            String nodeId, List<AnalysisManifest.BranchCompletion> completions) {
        branchCompletions.put(Objects.requireNonNull(nodeId, "nodeId"), List.copyOf(completions));
    }

    /** Binds one source control-path line to its outgoing business edge. */
    public void addControlTarget(
            String nodeId,
            String outcome,
            String ownerHint,
            String memberHint,
            String descriptorHint,
            long sourceLine) {
        addControlTarget(nodeId, outcome, ownerHint, memberHint, descriptorHint,
                sourceLine, AnalysisManifest.ControlPoint.LINE);
    }

    /** Binds one bytecode control point to its outgoing business edge. */
    public void addControlTarget(
            String nodeId,
            String outcome,
            String ownerHint,
            String memberHint,
            String descriptorHint,
            long sourceLine,
            AnalysisManifest.ControlPoint point) {
        controlBindings.add(new ControlBinding(
                nodeId, outcome, ownerHint, memberHint, descriptorHint, sourceLine, point));
    }

    /** Binds one result-relevant argument value to an existing predicate node. */
    public void addEvidenceTarget(
            String nodeId,
            String ownerHint,
            String memberHint,
            String descriptorHint,
            int argumentIndex,
            String evidenceLabel,
            long sourceLine) {
        var target = new AnalysisManifest.EvidenceTarget(
                nodeId, ownerHint, memberHint, descriptorHint, argumentIndex, evidenceLabel, sourceLine);
        if (!evidenceTargets.contains(target)) evidenceTargets.add(target);
    }

    /** Adds a visible completeness gap linked to its graph node. */
    public void addGap(String nodeId, String description) {
        gaps.add(new BusinessDecisionGraph.CoverageGap(nodeId, description));
    }

    /** Completes the graph, developer manifest, and diagnostics. */
    public BuiltGraph build(
            String entryNodeId,
            Map<String, String> sourceFingerprints,
            List<AnalysisManifest.AnalysisDiagnostic> diagnostics) {
        var completeness = gaps.isEmpty()
                ? BusinessDecisionGraph.Completeness.COMPLETE
                : BusinessDecisionGraph.Completeness.INCOMPLETE;
        var graph = new BusinessDecisionGraph(
                graphId, 1, decisionLabel, entryNodeId, nodes, edges, completeness, gaps);
        var manifest = new AnalysisManifest(
                graphId, 1, mappings, probes, dispatchTargets, branchTargets(), controlTargets(),
                evidenceTargets, analysisDecisions, sourceFingerprints);
        return new BuiltGraph(graph, manifest, List.copyOf(diagnostics));
    }

    private List<AnalysisManifest.BranchTarget> branchTargets() {
        var targets = new ArrayList<AnalysisManifest.BranchTarget>();
        var methodIndexes = new LinkedHashMap<String, Integer>();
        var nodeIndexes = new LinkedHashMap<String, Integer>();
        for (AnalysisManifest.ProbeSite probe : probes) {
            if (probe.kind() != AnalysisManifest.ProbeKind.PREDICATE) continue;
            String methodKey = probe.ownerHint() + "\u0000" + probe.memberHint()
                    + "\u0000" + probe.descriptorHint();
            int predicateIndex = methodIndexes.getOrDefault(methodKey, 0);
            methodIndexes.put(methodKey, predicateIndex + 1);
            int nodeIndex = nodeIndexes.getOrDefault(probe.nodeId(), 0);
            nodeIndexes.put(probe.nodeId(), nodeIndex + 1);
            List<AnalysisManifest.BranchCompletion> completions = branchCompletions.get(probe.nodeId());
            if (completions == null || nodeIndex >= completions.size()) continue;
            List<BusinessDecisionGraph.DecisionEdge> trueEdges = edges.stream()
                    .filter(edge -> edge.fromNodeId().equals(probe.nodeId()))
                    .filter(edge -> booleanOutcome(edge.outcome(), "true"))
                    .toList();
            List<BusinessDecisionGraph.DecisionEdge> falseEdges = edges.stream()
                    .filter(edge -> edge.fromNodeId().equals(probe.nodeId()))
                    .filter(edge -> booleanOutcome(edge.outcome(), "false"))
                    .toList();
            if (trueEdges.size() == 1 && falseEdges.size() == 1) {
                targets.add(new AnalysisManifest.BranchTarget(
                        probe.nodeId(), trueEdges.getFirst().edgeId(), falseEdges.getFirst().edgeId(),
                        probe.ownerHint(), probe.memberHint(), probe.descriptorHint(),
                        probe.sourceLine(), predicateIndex,
                        completions.get(nodeIndex)));
            }
        }
        return List.copyOf(targets);
    }

    private List<AnalysisManifest.ControlTarget> controlTargets() {
        var targets = new ArrayList<AnalysisManifest.ControlTarget>();
        for (ControlBinding binding : controlBindings) {
            List<BusinessDecisionGraph.DecisionEdge> matches = edges.stream()
                    .filter(edge -> edge.fromNodeId().equals(binding.nodeId()))
                    .filter(edge -> edge.outcome().equals(binding.outcome())
                            || edge.outcome().startsWith(binding.outcome() + ";"))
                    .toList();
            if (matches.size() == 1) {
                targets.add(new AnalysisManifest.ControlTarget(
                        binding.nodeId(), matches.getFirst().edgeId(), binding.ownerHint(), binding.memberHint(),
                        binding.descriptorHint(), binding.sourceLine(), binding.point()));
            }
        }
        return List.copyOf(targets);
    }

    private record ControlBinding(
            String nodeId,
            String outcome,
            String ownerHint,
            String memberHint,
            String descriptorHint,
            long sourceLine,
            AnalysisManifest.ControlPoint point) { }

    private static boolean booleanOutcome(String outcome, String value) {
        return outcome.equals(value) || outcome.startsWith(value + ";");
    }

    private static AnalysisManifest.AnalysisAction action(BusinessDecisionGraph.NodeKind kind) {
        return kind == BusinessDecisionGraph.NodeKind.COVERAGE_GAP
                ? AnalysisManifest.AnalysisAction.GAP
                : AnalysisManifest.AnalysisAction.INCLUDED;
    }

    private static AnalysisManifest.AnalysisReason reason(BusinessDecisionGraph.NodeKind kind) {
        return switch (kind) {
            case ENTRY -> AnalysisManifest.AnalysisReason.ENTRY_POINT;
            case OUTCOME -> AnalysisManifest.AnalysisReason.RETURN_VALUE;
            case PREDICATE, CHOICE, DISPATCH -> AnalysisManifest.AnalysisReason.CONTROL_DEPENDENCY;
            case COMPUTATION -> AnalysisManifest.AnalysisReason.DATA_DEPENDENCY;
            case COVERAGE_GAP -> AnalysisManifest.AnalysisReason.UNRESOLVED_RELEVANCE;
        };
    }

    /** Static graph plus its developer-only artifacts. */
    public record BuiltGraph(
            BusinessDecisionGraph graph,
            AnalysisManifest manifest,
            List<AnalysisManifest.AnalysisDiagnostic> diagnostics) { }

    /** Derives a deterministic opaque identifier. */
    public String opaque(String kind, Object... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(graphId.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(kind.getBytes(StandardCharsets.UTF_8));
            for (Object part : parts) {
                digest.update((byte) 0);
                digest.update(String.valueOf(part).getBytes(StandardCharsets.UTF_8));
            }
            return java.util.HexFormat.of().formatHex(digest.digest(), 0, 12);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
