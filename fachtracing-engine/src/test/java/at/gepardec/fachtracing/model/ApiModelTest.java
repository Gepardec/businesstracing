package at.gepardec.fachtracing.model;

import at.gepardec.fachtracing.api.DecisionValueAdapter;
import at.gepardec.fachtracing.api.DecisionValueRedactor;
import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.runtime.RuntimeActivationBundle;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Plain-Java contract tests which avoid a framework dependency. */
public final class ApiModelTest {
    private enum Category { ELIGIBLE }
    private record Money(BigDecimal amount) { }

    private ApiModelTest() { }

    public static void main(String[] args) {
        builtInsRoundTrip();
        customAdapterAndRedaction();
        collectionsPreserveTypedElementsWithoutArbitraryStringification();
        unknownValuesAreRejectedWithoutStringification();
        analysisDecisionAuditIsImmutableAndNotActivated();
        activationBundleRoundTripsWithoutRuntimeSourceAnalysis();
    }

    private static void analysisDecisionAuditIsImmutableAndNotActivated() {
        var nodeIds = new java.util.ArrayList<>(List.of("start"));
        var decisions = new java.util.ArrayList<>(List.of(new AnalysisManifest.AnalysisDecision(
                AnalysisManifest.AnalysisAction.INCLUDED,
                AnalysisManifest.AnalysisReason.ENTRY_POINT,
                Path.of("Policy.java"), 3, 5, "METHOD", nodeIds, "")));
        var mapping = new AnalysisManifest.SourceMapping("start", Path.of("Policy.java"), 3, 5, "METHOD");
        var manifest = new AnalysisManifest("graph", 1, Map.of("start", mapping),
                List.of(), List.of(), List.of(), List.of(), List.of(), decisions,
                Map.of("Policy.java", "0".repeat(64)));
        nodeIds.clear();
        decisions.clear();
        assert manifest.analysisDecisions().size() == 1 : manifest.analysisDecisions();
        assert manifest.analysisDecisions().getFirst().nodeIds().equals(List.of("start"));
        try {
            manifest.analysisDecisions().clear();
            throw new AssertionError("analysis decisions were mutable");
        } catch (UnsupportedOperationException expected) {
            // The manifest is immutable.
        }

        var graph = new BusinessDecisionGraph("graph", 1, "approval", "start",
                List.of(new BusinessDecisionGraph.DecisionNode(
                        "start", BusinessDecisionGraph.NodeKind.ENTRY, "Start", Map.of())),
                List.of(), BusinessDecisionGraph.Completeness.COMPLETE, List.of());
        var bundle = new RuntimeActivationBundle("boundary", "-javaagent:/opt/fachtracing-agent.jar",
                Map.of(), List.of(new RuntimeActivationBundle.DecisionDefinition(graph, manifest)));
        String payload = new String(bundle.toJson(), java.nio.charset.StandardCharsets.UTF_8);
        assert !payload.contains("analysisDecisions") : payload;
        assert !payload.contains("ENTRY_POINT") : payload;
    }

    private static void collectionsPreserveTypedElementsWithoutArbitraryStringification() {
        var codec = new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none());
        var empty = codec.encode(List.of(), "warnings", "result");
        assert empty.type().equals("collection");
        assert empty.canonicalValue().equals("[]");
        var values = codec.encode(List.of(true, 12, Category.ELIGIBLE), "mixed", "result");
        assert values.canonicalValue().equals("[\"true\",\"12\",\"ELIGIBLE\"]") : values;
    }

    private static void builtInsRoundTrip() {
        var codec = new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none());
        assert codec.encode(true, "eligibility", "result").equals(new DecisionExecution.DecisionValue("boolean", "true", "true"));
        assert codec.encode(new BigDecimal("12.50"), "price", "result").canonicalValue().equals("12.5");
        assert codec.encode(Category.ELIGIBLE, "eligibility", "result").type().equals("category");
        assert codec.encode("accepted", "eligibility", "result").type().equals("string");
    }

    private static void customAdapterAndRedaction() {
        DecisionValueAdapter<Money> adapter = new DecisionValueAdapter<>() {
            public Class<Money> targetType() { return Money.class; }
            public AdaptedValue adapt(Money value) {
                var canonical = value.amount().toPlainString();
                return new AdaptedValue("money", canonical, canonical + " EUR");
            }
        };
        DecisionValueRedactor redactor = (value, context) -> new DecisionValueAdapter.AdaptedValue(
                value.type(), value.canonicalValue(), "REDACTED");
        var encoded = new DecisionExecution.DecisionValueCodec(redactor).register(adapter)
                .encode(new Money(new BigDecimal("9.95")), "price", "result");
        assert encoded.type().equals("money");
        assert encoded.canonicalValue().equals("9.95");
        assert encoded.displayValue().equals("REDACTED");
    }

    private static void unknownValuesAreRejectedWithoutStringification() {
        final class Unknown {
            boolean stringified;
            @Override public String toString() { stringified = true; return "unsafe"; }
        }
        var unknown = new Unknown();
        try {
            new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none())
                    .encode(unknown, "decision", "result");
            throw new AssertionError("unknown value was accepted");
        } catch (IllegalArgumentException expected) {
            assert !unknown.stringified;
        }
    }

    private static void activationBundleRoundTripsWithoutRuntimeSourceAnalysis() {
        var graph = new BusinessDecisionGraph("graph", 1, "approval", "start",
                List.of(
                        new BusinessDecisionGraph.DecisionNode("start", BusinessDecisionGraph.NodeKind.ENTRY,
                                "Start", Map.of()),
                        new BusinessDecisionGraph.DecisionNode("stop", BusinessDecisionGraph.NodeKind.OUTCOME,
                                "Stop", Map.of())),
                List.of(new BusinessDecisionGraph.DecisionEdge("edge", "start", "stop", "returns approved")),
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
        var mapping = new AnalysisManifest.SourceMapping("start", Path.of("Policy.java"), 3, 5, "METHOD");
        var manifest = new AnalysisManifest("graph", 1, Map.of("start", mapping),
                List.of(new AnalysisManifest.ProbeSite("start", AnalysisManifest.ProbeKind.ENTRY,
                        "example.Policy", "approve", "(I)Z", 3)),
                List.of(new AnalysisManifest.DispatchTarget(
                        "start", "edge", "example.Policy", "approve", "(I)Z")),
                List.of(new AnalysisManifest.BranchTarget(
                        "start", "edge", "edge", "example.Policy", "approve", "(I)Z", 3, 0,
                        AnalysisManifest.BranchCompletion.BOTH_OUTCOMES)),
                List.of(new AnalysisManifest.ControlTarget(
                        "start", "edge", "example.Policy", "approve", "(I)Z", 3)),
                List.of(new AnalysisManifest.EvidenceTarget(
                        "start", "example.Policy", "approve", "(I)Z", 0, "age", 3)),
                Map.of("Policy.java", "0".repeat(64)));
        var bundle = new RuntimeActivationBundle("boundary", "-javaagent:/opt/fachtracing-agent.jar",
                Map.of("example/Policy", "1".repeat(64)),
                List.of(new RuntimeActivationBundle.DecisionDefinition(graph, manifest)));
        byte[] encoded = bundle.toJson();
        var decoded = RuntimeActivationBundle.fromJson(encoded);
        assert decoded.equals(bundle) : new String(encoded, java.nio.charset.StandardCharsets.UTF_8);
        assert java.util.Arrays.equals(encoded, decoded.toJson());
        assert new String(encoded, java.nio.charset.StandardCharsets.UTF_8)
                .contains("\"schema\":\"fachtracing-activation/v3\"");
        assert decoded.decisions().getFirst().manifest().probeSites().getFirst()
                .descriptorHint().equals("(I)Z");
        assert decoded.decisions().getFirst().manifest().dispatchTargets().getFirst()
                .descriptorHint().equals("(I)Z");
        assert decoded.decisions().getFirst().manifest().branchTargets().getFirst()
                .descriptorHint().equals("(I)Z");
        assert decoded.decisions().getFirst().manifest().evidenceTargets().getFirst()
                .evidenceLabel().equals("age");

        String legacyJson = new String(encoded, java.nio.charset.StandardCharsets.UTF_8)
                .replace("fachtracing-activation/v3", "fachtracing-activation/v2")
                .replaceAll(",\\\"descriptorHint\\\":\\\"[^\\\"]*\\\"", "");
        var legacy = RuntimeActivationBundle.fromJson(
                legacyJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assert legacy.decisions().getFirst().manifest().probeSites().getFirst()
                .descriptorHint().isEmpty();
        assert legacy.decisions().getFirst().manifest().dispatchTargets().getFirst()
                .descriptorHint().isEmpty();
        assert legacy.decisions().getFirst().manifest().branchTargets().getFirst()
                .descriptorHint().isEmpty();
        assert legacy.decisions().getFirst().manifest().evidenceTargets().getFirst()
                .descriptorHint().isEmpty();
    }
}
