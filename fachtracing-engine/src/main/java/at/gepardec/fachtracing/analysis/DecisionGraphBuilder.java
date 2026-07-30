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
        String nodeId = opaque("node", ++nodeSequence, kind.name(), label);
        nodes.add(new BusinessDecisionGraph.DecisionNode(nodeId, kind, requireText(label, "label"), attributes));
        if (source != null) {
            mappings.put(nodeId, new AnalysisManifest.SourceMapping(
                    nodeId, source.source(), source.line(), source.column(), source.treeKind()));
        }
        if (probeKind != null) {
            probes.add(new AnalysisManifest.ProbeSite(nodeId, probeKind,
                    Objects.requireNonNullElse(ownerHint, ""), Objects.requireNonNullElse(memberHint, ""),
                    source == null ? -1 : source.line()));
        }
        return nodeId;
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
        addProbe(nodeId, probeKind, ownerHint, memberHint, mappings.get(nodeId));
    }

    /** Adds a probe for an existing node with provenance specific to this probe site. */
    public void addProbe(
            String nodeId,
            AnalysisManifest.ProbeKind probeKind,
            String ownerHint,
            String memberHint,
            AnalysisManifest.SourceMapping source) {
        probes.add(new AnalysisManifest.ProbeSite(nodeId, probeKind,
                Objects.requireNonNullElse(ownerHint, ""), Objects.requireNonNullElse(memberHint, ""),
                source == null ? -1 : source.line()));
    }

    /** Adds a developer-only implementation-entry binding for runtime dispatch correlation. */
    public void addDispatchTarget(String dispatchNodeId, String edgeId, String ownerHint, String memberHint) {
        dispatchTargets.add(new AnalysisManifest.DispatchTarget(
                dispatchNodeId, edgeId, ownerHint, memberHint));
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
        var manifest = new AnalysisManifest(graphId, 1, mappings, probes, dispatchTargets, sourceFingerprints);
        return new BuiltGraph(graph, manifest, List.copyOf(diagnostics));
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
