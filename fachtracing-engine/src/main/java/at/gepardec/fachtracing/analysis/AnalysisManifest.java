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
        List<ControlTarget> controlTargets,
        List<EvidenceTarget> evidenceTargets,
        Map<String, String> sourceFingerprints) {
    /** Compatibility constructor for manifests that have no exact branch bindings. */
    public AnalysisManifest(
            String graphId,
            long graphVersion,
            Map<String, SourceMapping> sourceMappings,
            List<ProbeSite> probeSites,
            List<DispatchTarget> dispatchTargets,
            Map<String, String> sourceFingerprints) {
        this(graphId, graphVersion, sourceMappings, probeSites, dispatchTargets,
                List.of(), List.of(), List.of(), sourceFingerprints);
    }

    /** Compatibility constructor for manifests that have no exact control-path bindings. */
    public AnalysisManifest(
            String graphId,
            long graphVersion,
            Map<String, SourceMapping> sourceMappings,
            List<ProbeSite> probeSites,
            List<DispatchTarget> dispatchTargets,
            List<BranchTarget> branchTargets,
            Map<String, String> sourceFingerprints) {
        this(graphId, graphVersion, sourceMappings, probeSites, dispatchTargets,
                branchTargets, List.of(), List.of(), sourceFingerprints);
    }

    /** Compatibility constructor for manifests that have no operand evidence bindings. */
    public AnalysisManifest(
            String graphId,
            long graphVersion,
            Map<String, SourceMapping> sourceMappings,
            List<ProbeSite> probeSites,
            List<DispatchTarget> dispatchTargets,
            List<BranchTarget> branchTargets,
            List<ControlTarget> controlTargets,
            Map<String, String> sourceFingerprints) {
        this(graphId, graphVersion, sourceMappings, probeSites, dispatchTargets,
                branchTargets, controlTargets, List.of(), sourceFingerprints);
    }

    /** Creates a defensive manifest snapshot. */
    public AnalysisManifest {
        Objects.requireNonNull(graphId, "graphId");
        sourceMappings = Map.copyOf(sourceMappings);
        probeSites = List.copyOf(probeSites);
        dispatchTargets = List.copyOf(dispatchTargets);
        branchTargets = List.copyOf(branchTargets);
        controlTargets = List.copyOf(controlTargets);
        evidenceTargets = List.copyOf(evidenceTargets);
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
    public record ProbeSite(
            String nodeId,
            ProbeKind kind,
            String ownerHint,
            String memberHint,
            String descriptorHint,
            long sourceLine) {
        /** Compatibility constructor for name-only bindings. */
        public ProbeSite(String nodeId, ProbeKind kind, String ownerHint, String memberHint, long sourceLine) {
            this(nodeId, kind, ownerHint, memberHint, "", sourceLine);
        }

        /** Compatibility constructor for synthetic manifests without source provenance. */
        public ProbeSite(String nodeId, ProbeKind kind, String ownerHint, String memberHint) {
            this(nodeId, kind, ownerHint, memberHint, "", -1);
        }

        /** Creates a probe site kept outside business output. */
        public ProbeSite {
            Objects.requireNonNull(nodeId, "nodeId");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(ownerHint, "ownerHint");
            Objects.requireNonNull(memberHint, "memberHint");
            Objects.requireNonNull(descriptorHint, "descriptorHint");
            if (sourceLine == 0 || sourceLine < -1) {
                throw new IllegalArgumentException("sourceLine must be positive or -1");
            }
        }
    }

    /** Runtime probe kinds. */
    public enum ProbeKind { ENTRY, PREDICATE, DISPATCH, OUTCOME }

    /** Developer-only binding from an implementation entry to an opaque dispatch edge. */
    public record DispatchTarget(
            String dispatchNodeId,
            String edgeId,
            String ownerHint,
            String memberHint,
            String descriptorHint) {
        /** Compatibility constructor for name-only bindings. */
        public DispatchTarget(String dispatchNodeId, String edgeId, String ownerHint, String memberHint) {
            this(dispatchNodeId, edgeId, ownerHint, memberHint, "");
        }

        /** Creates a target binding kept outside the business record. */
        public DispatchTarget {
            Objects.requireNonNull(dispatchNodeId, "dispatchNodeId");
            Objects.requireNonNull(edgeId, "edgeId");
            Objects.requireNonNull(ownerHint, "ownerHint");
            Objects.requireNonNull(memberHint, "memberHint");
            Objects.requireNonNull(descriptorHint, "descriptorHint");
        }
    }

    /** Developer-only binding from one predicate to its exact boolean edges. */
    public record BranchTarget(
            String nodeId,
            String trueEdgeId,
            String falseEdgeId,
            String ownerHint,
            String memberHint,
            String descriptorHint,
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
            this(nodeId, trueEdgeId, falseEdgeId, ownerHint, memberHint, "", sourceLine,
                    0, BranchCompletion.BOTH_OUTCOMES);
        }

        /** Compatibility constructor for exact name-only branch plans. */
        public BranchTarget(
                String nodeId,
                String trueEdgeId,
                String falseEdgeId,
                String ownerHint,
                String memberHint,
                long sourceLine,
                int predicateIndex,
                BranchCompletion completion) {
            this(nodeId, trueEdgeId, falseEdgeId, ownerHint, memberHint, "", sourceLine,
                    predicateIndex, completion);
        }

        /** Creates a complete boolean branch binding kept outside the business record. */
        public BranchTarget {
            Objects.requireNonNull(nodeId, "nodeId");
            Objects.requireNonNull(trueEdgeId, "trueEdgeId");
            Objects.requireNonNull(falseEdgeId, "falseEdgeId");
            Objects.requireNonNull(ownerHint, "ownerHint");
            Objects.requireNonNull(memberHint, "memberHint");
            Objects.requireNonNull(descriptorHint, "descriptorHint");
            if (sourceLine == 0 || sourceLine < -1) {
                throw new IllegalArgumentException("sourceLine must be positive or -1");
            }
            if (predicateIndex < 0) throw new IllegalArgumentException("predicateIndex must be non-negative");
            Objects.requireNonNull(completion, "completion");
        }
    }

    /** Defines which path of one bytecode jump completes the full source predicate. */
    public enum BranchCompletion { BOTH_OUTCOMES, BOTH_OUTCOMES_REVERSED, JUMP_TRUE, JUMP_FALSE }

    /** Developer-only binding from one source control path to its exact graph edge. */
    public record ControlTarget(
            String nodeId,
            String edgeId,
            String ownerHint,
            String memberHint,
            String descriptorHint,
            long sourceLine,
            ControlPoint point) {
        /** Compatibility constructor for a source-line path binding. */
        public ControlTarget(
                String nodeId,
                String edgeId,
                String ownerHint,
                String memberHint,
                String descriptorHint,
                long sourceLine) {
            this(nodeId, edgeId, ownerHint, memberHint, descriptorHint, sourceLine, ControlPoint.LINE);
        }

        /** Creates a control-path binding kept outside the business record. */
        public ControlTarget {
            Objects.requireNonNull(nodeId, "nodeId");
            Objects.requireNonNull(edgeId, "edgeId");
            Objects.requireNonNull(ownerHint, "ownerHint");
            Objects.requireNonNull(memberHint, "memberHint");
            Objects.requireNonNull(descriptorHint, "descriptorHint");
            if (sourceLine <= 0) throw new IllegalArgumentException("sourceLine must be positive");
            Objects.requireNonNull(point, "point");
        }
    }

    /** Bytecode point that proves a source control path was taken. */
    public enum ControlPoint { LINE, RETURN, CASE_EXIT, PREDICATE_TRUE }

    /** Binds one result-relevant method argument, or an unavailable exact operand, to a predicate. */
    public record EvidenceTarget(
            String nodeId,
            String ownerHint,
            String memberHint,
            String descriptorHint,
            int argumentIndex,
            String evidenceLabel,
            long sourceLine) {
        /** Creates an exact, developer-only operand binding. */
        public EvidenceTarget {
            Objects.requireNonNull(nodeId, "nodeId");
            Objects.requireNonNull(ownerHint, "ownerHint");
            Objects.requireNonNull(memberHint, "memberHint");
            Objects.requireNonNull(descriptorHint, "descriptorHint");
            if (argumentIndex < -1) throw new IllegalArgumentException("argumentIndex must be -1 or non-negative");
            if (evidenceLabel == null || evidenceLabel.isBlank()) {
                throw new IllegalArgumentException("evidenceLabel must not be blank");
            }
            if (sourceLine == 0 || sourceLine < -1) {
                throw new IllegalArgumentException("sourceLine must be positive or -1");
            }
        }
    }

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
