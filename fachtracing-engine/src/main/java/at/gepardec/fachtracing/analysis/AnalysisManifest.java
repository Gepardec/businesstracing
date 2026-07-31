package at.gepardec.fachtracing.analysis;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Developer-only source and probe correlation artifact; never embedded in a business record. */
public record AnalysisManifest(
        String graphId,
        long graphVersion,
        Map<String, SourceMapping> sourceMappings,
        List<ProbeSite> probeSites,
        List<DispatchTarget> dispatchTargets,
        List<BranchTarget> branchTargets,
        Map<String, String> sourceFingerprints) {
    /** Compatibility constructor for manifests that have no exact branch bindings. */
    public AnalysisManifest(
            String graphId,
            long graphVersion,
            Map<String, SourceMapping> sourceMappings,
            List<ProbeSite> probeSites,
            List<DispatchTarget> dispatchTargets,
            Map<String, String> sourceFingerprints) {
        this(graphId, graphVersion, sourceMappings, probeSites, dispatchTargets, List.of(), sourceFingerprints);
    }

    /** Creates a defensive manifest snapshot. */
    public AnalysisManifest {
        Objects.requireNonNull(graphId, "graphId");
        sourceMappings = Map.copyOf(sourceMappings);
        probeSites = List.copyOf(probeSites);
        dispatchTargets = List.copyOf(dispatchTargets);
        branchTargets = List.copyOf(branchTargets);
        sourceFingerprints = Map.copyOf(sourceFingerprints);
    }

    /** Technical source provenance keyed by an opaque business node ID. */
    public record SourceMapping(String nodeId, Path source, long line, long column, String treeKind) {
        /** Creates a source mapping. */
        public SourceMapping {
            Objects.requireNonNull(nodeId, "nodeId");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(treeKind, "treeKind");
        }
    }

    /** Requested runtime observation point. */
    public record ProbeSite(String nodeId, ProbeKind kind, String ownerHint, String memberHint, long sourceLine) {
        /** Compatibility constructor for synthetic manifests without source provenance. */
        public ProbeSite(String nodeId, ProbeKind kind, String ownerHint, String memberHint) {
            this(nodeId, kind, ownerHint, memberHint, -1);
        }

        /** Creates a probe site kept outside business output. */
        public ProbeSite {
            Objects.requireNonNull(nodeId, "nodeId");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(ownerHint, "ownerHint");
            Objects.requireNonNull(memberHint, "memberHint");
            if (sourceLine == 0 || sourceLine < -1) {
                throw new IllegalArgumentException("sourceLine must be positive or -1");
            }
        }
    }

    /** Runtime probe kinds. */
    public enum ProbeKind { ENTRY, PREDICATE, DISPATCH, OUTCOME }

    /** Developer-only binding from an implementation entry to an opaque dispatch edge. */
    public record DispatchTarget(String dispatchNodeId, String edgeId, String ownerHint, String memberHint) {
        /** Creates a target binding kept outside the business record. */
        public DispatchTarget {
            Objects.requireNonNull(dispatchNodeId, "dispatchNodeId");
            Objects.requireNonNull(edgeId, "edgeId");
            Objects.requireNonNull(ownerHint, "ownerHint");
            Objects.requireNonNull(memberHint, "memberHint");
        }
    }

    /** Developer-only binding from one predicate to its exact boolean edges. */
    public record BranchTarget(
            String nodeId,
            String trueEdgeId,
            String falseEdgeId,
            String ownerHint,
            String memberHint,
            long sourceLine,
            int predicateIndex,
            BranchCompletion completion) {
        /** Compatibility constructor for one complete, simple predicate. */
        public BranchTarget(
                String nodeId,
                String trueEdgeId,
                String falseEdgeId,
                String ownerHint,
                String memberHint,
                long sourceLine) {
            this(nodeId, trueEdgeId, falseEdgeId, ownerHint, memberHint, sourceLine,
                    0, BranchCompletion.BOTH_OUTCOMES);
        }

        /** Creates a complete boolean branch binding kept outside the business record. */
        public BranchTarget {
            Objects.requireNonNull(nodeId, "nodeId");
            Objects.requireNonNull(trueEdgeId, "trueEdgeId");
            Objects.requireNonNull(falseEdgeId, "falseEdgeId");
            Objects.requireNonNull(ownerHint, "ownerHint");
            Objects.requireNonNull(memberHint, "memberHint");
            if (sourceLine == 0 || sourceLine < -1) {
                throw new IllegalArgumentException("sourceLine must be positive or -1");
            }
            if (predicateIndex < 0) throw new IllegalArgumentException("predicateIndex must be non-negative");
            Objects.requireNonNull(completion, "completion");
        }
    }

    /** Defines which path of one bytecode jump completes the full source predicate. */
    public enum BranchCompletion { BOTH_OUTCOMES, JUMP_TRUE, JUMP_FALSE }

    /** Full static-analysis outcome. */
    public record AnalysisResult(
            BusinessDecisionGraph graph,
            AnalysisManifest manifest,
            List<AnalysisDiagnostic> diagnostics) {
        /** Creates a defensive result snapshot. */
        public AnalysisResult {
            Objects.requireNonNull(graph, "graph");
            Objects.requireNonNull(manifest, "manifest");
            diagnostics = List.copyOf(diagnostics);
        }
    }

    /** Developer-only analysis diagnostic with precise source provenance. */
    public record AnalysisDiagnostic(
            Severity severity,
            Path source,
            long line,
            long column,
            String constructKind,
            String message) {
        /** Creates a developer diagnostic. */
        public AnalysisDiagnostic {
            Objects.requireNonNull(severity, "severity");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(constructKind, "constructKind");
            Objects.requireNonNull(message, "message");
        }
    }

    /** Diagnostic severity. */
    public enum Severity { WARNING, ERROR }
}
