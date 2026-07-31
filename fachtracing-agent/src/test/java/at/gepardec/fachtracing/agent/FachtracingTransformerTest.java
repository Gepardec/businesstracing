package at.gepardec.fachtracing.agent;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.analysis.AnalysisRequest;
import at.gepardec.fachtracing.analysis.StaticDecisionAnalyzer;
import at.gepardec.fachtracing.api.DecisionValueRedactor;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.runtime.RuntimeCollector;
import at.gepardec.fachtracing.runtime.TraceRuntime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/** Executable transformation and transparency contracts for the Java agent. */
public final class FachtracingTransformerTest {
    private static final String CLASS_NAME = "agentfixture/InstrumentedFixture";

    private FachtracingTransformerTest() { }

    public static void main(String[] args) throws Exception {
        premainAcceptsApplicationConfigurationAfterStartup();
        rejectsFingerprintMismatch();
        transformedMethodPreservesResultsAndCapturesExecution();
        analyzerBindingsCaptureOneCompoundPredicateEdge();
        partialCompoundBindingKeepsLegacyProbes();
        unsupportedCompoundShapeKeepsLegacyProbes();
        ternaryPredicateKeepsLegacyProbes();
        manifestWithoutBranchTargetsKeepsLegacyProbe();
        transformedTargetsRecordTheActualPolymorphicEdge();
    }

    private static void premainAcceptsApplicationConfigurationAfterStartup() {
        var installed = new java.util.concurrent.atomic.AtomicReference<java.lang.instrument.ClassFileTransformer>();
        var instrumentation = (java.lang.instrument.Instrumentation) java.lang.reflect.Proxy.newProxyInstance(
                FachtracingTransformerTest.class.getClassLoader(),
                new Class<?>[] { java.lang.instrument.Instrumentation.class },
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "addTransformer" -> { installed.set((java.lang.instrument.ClassFileTransformer) arguments[0]); yield null; }
                    case "getAllLoadedClasses", "getInitiatedClasses" -> new Class<?>[0];
                    case "isModifiableClass", "isRetransformClassesSupported", "isRedefineClassesSupported",
                            "isNativeMethodPrefixSupported", "isModifiableModule", "removeTransformer" -> false;
                    case "getObjectSize" -> 0L;
                    default -> null;
                });
        FachtracingAgent.premain("", instrumentation);
        assert installed.get() == null;
        FachtracingAgent.configure(manifest(), Map.of());
        assert installed.get() instanceof FachtracingTransformer;
    }

    private static void transformedTargetsRecordTheActualPolymorphicEdge() throws Exception {
        String serviceName = "agentfixture/StrategyService";
        String localName = "agentfixture/LocalRule";
        String regionalName = "agentfixture/RegionalRule";
        Map<String, byte[]> originals = Map.of(
                serviceName, classBytes(serviceName),
                localName, classBytes(localName),
                regionalName, classBytes(regionalName));
        Map<String, String> fingerprints = originals.entrySet().stream().collect(
                java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> uncheckedSha256(entry.getValue())));
        AnalysisManifest manifest = dispatchManifest();
        FachtracingTransformer transformer = new FachtracingTransformer(manifest, fingerprints);
        var transformed = new java.util.HashMap<String, byte[]>();
        originals.forEach((name, bytes) -> transformed.put(name,
                transformer.transform(null, null, name, null, null, bytes)));
        assert transformed.values().stream().noneMatch(java.util.Objects::isNull);

        RuntimeCollector collector = new RuntimeCollector();
        collector.register(dispatchGraph(), new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        TraceRuntime.configure(collector);
        ClassLoader loader = new MultiClassLoader(transformed);
        Class<?> serviceType = loader.loadClass("agentfixture.StrategyService");
        Class<?> ruleType = Class.forName("agentfixture.DecisionRule");
        Object service = serviceType.getConstructor().newInstance();
        var decide = serviceType.getMethod("decide", ruleType, int.class);
        Object local = loader.loadClass("agentfixture.LocalRule").getConstructor().newInstance();
        Object regional = loader.loadClass("agentfixture.RegionalRule").getConstructor().newInstance();
        assert decide.invoke(service, local, 30).equals(false);
        assert decide.invoke(service, regional, 30).equals(true);

        var localExecution = collector.pollCompleted().orElseThrow();
        var regionalExecution = collector.pollCompleted().orElseThrow();
        assert selectedEdge(localExecution).equals("edge-local") : localExecution.observations();
        assert selectedEdge(regionalExecution).equals("edge-regional") : regionalExecution.observations();
    }

    private static void rejectsFingerprintMismatch() throws Exception {
        byte[] original = fixtureBytes();
        var transformer = new FachtracingTransformer(manifest(), Map.of(CLASS_NAME, "mismatch"));
        assert transformer.transform(null, null, CLASS_NAME, null, null, original) == null;
    }

    private static void transformedMethodPreservesResultsAndCapturesExecution() throws Exception {
        byte[] original = fixtureBytes();
        var transformer = new FachtracingTransformer(manifest(), Map.of(CLASS_NAME, sha256(original)));
        byte[] transformed = transformer.transform(null, null, CLASS_NAME, null, null, original);
        assert transformed != null;
        assert !java.util.Arrays.equals(original, transformed);

        RuntimeCollector collector = new RuntimeCollector();
        collector.register(graph(), new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        TraceRuntime.configure(collector);

        Class<?> fixture = new IsolatedLoader(transformed).loadClass(CLASS_NAME.replace('/', '.'));
        Object instance = fixture.getConstructor().newInstance();
        var decide = fixture.getMethod("decide", int.class);
        assert decide.invoke(instance, 20).equals(true);
        assert decide.invoke(instance, 30).equals(false);

        var first = collector.pollCompleted().orElseThrow();
        var second = collector.pollCompleted().orElseThrow();
        assert first.finalResult().canonicalValue().equals("true");
        assert second.finalResult().canonicalValue().equals("false");
        assert first.observations().stream().anyMatch(observation -> observation.nodeId().equals("entry")
                && observation.evidence().get("value").type().equals("number")
                && observation.evidence().get("value").canonicalValue().equals("20"));
        assert selectedEdge(first).equals("edge-true") : first.observations();
        assert selectedEdge(second).equals("edge-false") : second.observations();
        assert first.observations().stream().allMatch(observation -> !observation.nodeId().contains("agentfixture"));
        Throwable explicitFailure;
        try {
            decide.invoke(instance, -1);
            throw new AssertionError("application exception was not preserved");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            assert expected.getCause() instanceof IllegalArgumentException;
            assert expected.getCause().getMessage().equals("age must not be negative");
            explicitFailure = expected.getCause();
        }
        assert explicitFailure != null;
        var explicitExecution = collector.pollCompleted().orElseThrow();
        assert explicitExecution.terminalStatus() == DecisionExecution.TerminalStatus.FAILED;
        assert explicitExecution.finalResult() == null;
        assert explicitExecution.failure().equals(DecisionExecution.FailureData.genericFailure());

        var throughHelper = fixture.getMethod("decideThroughHelper");
        Throwable expectedFailure = (Throwable) fixture.getMethod("propagatedFailure").invoke(instance);
        try {
            throughHelper.invoke(instance);
            throw new AssertionError("called-method exception was not preserved");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            assert expected.getCause() == expectedFailure : "the failure handler replaced the exception";
        }
        var propagatedExecution = collector.pollCompleted().orElseThrow();
        assert propagatedExecution.terminalStatus() == DecisionExecution.TerminalStatus.FAILED;
        assert propagatedExecution.failure().canonicalValue().equals("FAILED");
        assert collector.pollCompleted().isEmpty();
    }

    private static void manifestWithoutBranchTargetsKeepsLegacyProbe() throws Exception {
        byte[] original = fixtureBytes();
        var transformer = new FachtracingTransformer(legacyManifest(), Map.of(CLASS_NAME, sha256(original)));
        byte[] transformed = transformer.transform(null, null, CLASS_NAME, null, null, original);
        assert transformed != null;

        RuntimeCollector collector = new RuntimeCollector();
        collector.register(graph(), new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        TraceRuntime.configure(collector);
        Class<?> fixture = new IsolatedLoader(transformed).loadClass(CLASS_NAME.replace('/', '.'));
        Object instance = fixture.getConstructor().newInstance();
        assert fixture.getMethod("decide", int.class).invoke(instance, 20).equals(true);
        var execution = collector.pollCompleted().orElseThrow();
        assert execution.observations().stream().anyMatch(observation -> observation.nodeId().equals("predicate")
                && observation.outcome().equals("evaluated") && observation.selectedEdgeId() == null)
                : execution.observations();
    }

    private static void analyzerBindingsCaptureOneCompoundPredicateEdge() throws Exception {
        Path source = Path.of("fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java")
                .toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        var results = new StaticDecisionAnalyzer().analyzeAll(
                AnalysisRequest.of(List.of(source), List.of(apiClasses)));
        verifyCompoundResults(results, "compound conjunction", "decideAnd", List.of(
                new CompoundCase(true, false, "false", 1),
                new CompoundCase(false, true, "false", 0),
                new CompoundCase(true, true, "true", 1)));
        verifyCompoundResults(results, "compound disjunction", "decideOr", List.of(
                new CompoundCase(false, true, "true", 1),
                new CompoundCase(true, false, "true", 0),
                new CompoundCase(false, false, "false", 1)));
    }

    private static void verifyCompoundResults(
            List<AnalysisManifest.AnalysisResult> results,
            String decisionLabel,
            String methodName,
            List<CompoundCase> cases) throws Exception {
        var result = results.stream().filter(item -> item.graph().decisionLabel().equals(decisionLabel))
                .findFirst().orElseThrow();
        assert result.manifest().branchTargets().size() == 2
                : result.manifest().probeSites() + " / " + result.graph().edges();
        assert result.manifest().branchTargets().getFirst().completion()
                != AnalysisManifest.BranchCompletion.BOTH_OUTCOMES : result.manifest().branchTargets();
        assert result.manifest().branchTargets().getLast().completion()
                == AnalysisManifest.BranchCompletion.BOTH_OUTCOMES : result.manifest().branchTargets();

        byte[] original = fixtureBytes();
        var transformer = new FachtracingTransformer(
                result.manifest(), Map.of(CLASS_NAME, sha256(original)));
        byte[] transformed = transformer.transform(null, null, CLASS_NAME, null, null, original);
        assert transformed != null;

        RuntimeCollector collector = new RuntimeCollector();
        collector.register(result.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        TraceRuntime.configure(collector);
        Class<?> fixture = new IsolatedLoader(transformed).loadClass(CLASS_NAME.replace('/', '.'));
        Object instance = fixture.getConstructor().newInstance();
        var method = fixture.getMethod(methodName, boolean.class, boolean.class);
        for (CompoundCase testCase : cases) {
            Object returned = method.invoke(instance, testCase.first(), testCase.second());
            assert returned.toString().equals(testCase.expectedOutcome()) : returned;
            assert fixture.getMethod("secondOperandEvaluations").invoke(instance)
                    .equals(testCase.expectedSecondEvaluations()) : testCase;
            var execution = collector.pollCompleted().orElseThrow();
            var selectedEdges = execution.observations().stream()
                    .filter(observation -> observation.selectedEdgeId() != null).toList();
            assert selectedEdges.size() == 1 : selectedEdges;
            assert selectedEdges.getFirst().outcome().startsWith(testCase.expectedOutcome()) : selectedEdges;
        }
        assert collector.pollCompleted().isEmpty();
    }

    private static void unsupportedCompoundShapeKeepsLegacyProbes() throws Exception {
        Path source = Path.of("fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java")
                .toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        var result = new StaticDecisionAnalyzer().analyzeAll(
                        AnalysisRequest.of(List.of(source), List.of(apiClasses))).stream()
                .filter(item -> item.graph().decisionLabel().equals("mixed compound"))
                .findFirst().orElseThrow();
        assert result.manifest().branchTargets().isEmpty() : result.manifest().branchTargets();

        byte[] original = fixtureBytes();
        var transformer = new FachtracingTransformer(
                result.manifest(), Map.of(CLASS_NAME, sha256(original)));
        byte[] transformed = transformer.transform(null, null, CLASS_NAME, null, null, original);
        assert transformed != null;
        RuntimeCollector collector = new RuntimeCollector();
        collector.register(result.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        TraceRuntime.configure(collector);
        Class<?> fixture = new IsolatedLoader(transformed).loadClass(CLASS_NAME.replace('/', '.'));
        Object instance = fixture.getConstructor().newInstance();
        assert fixture.getMethod("decideMixed", boolean.class, boolean.class, boolean.class)
                .invoke(instance, true, false, false).equals(false);
        var execution = collector.pollCompleted().orElseThrow();
        assert execution.observations().stream().noneMatch(observation -> observation.selectedEdgeId() != null)
                : execution.observations();
        assert execution.observations().stream().anyMatch(observation -> observation.outcome().equals("evaluated"))
                : execution.observations();
    }

    private static void partialCompoundBindingKeepsLegacyProbes() throws Exception {
        Path source = Path.of("fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java")
                .toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        var result = new StaticDecisionAnalyzer().analyzeAll(
                        AnalysisRequest.of(List.of(source), List.of(apiClasses))).stream()
                .filter(item -> item.graph().decisionLabel().equals("compound conjunction"))
                .findFirst().orElseThrow();
        AnalysisManifest original = result.manifest();
        var partial = new AnalysisManifest(
                original.graphId(), original.graphVersion(), original.sourceMappings(), original.probeSites(),
                original.dispatchTargets(), List.of(original.branchTargets().getFirst()),
                original.sourceFingerprints());

        byte[] originalBytes = fixtureBytes();
        var transformer = new FachtracingTransformer(
                partial, Map.of(CLASS_NAME, sha256(originalBytes)));
        byte[] transformed = transformer.transform(null, null, CLASS_NAME, null, null, originalBytes);
        assert transformed != null;
        RuntimeCollector collector = new RuntimeCollector();
        collector.register(result.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        TraceRuntime.configure(collector);
        Class<?> fixture = new IsolatedLoader(transformed).loadClass(CLASS_NAME.replace('/', '.'));
        Object instance = fixture.getConstructor().newInstance();
        assert fixture.getMethod("decideAnd", boolean.class, boolean.class)
                .invoke(instance, true, false).equals(false);
        var execution = collector.pollCompleted().orElseThrow();
        assert execution.observations().stream().noneMatch(observation -> observation.selectedEdgeId() != null)
                : execution.observations();
        assert execution.observations().stream().filter(observation -> observation.outcome().equals("evaluated"))
                .count() == 2 : execution.observations();
    }

    private static void ternaryPredicateKeepsLegacyProbes() throws Exception {
        Path source = Path.of("fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java")
                .toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        var result = new StaticDecisionAnalyzer().analyzeAll(
                        AnalysisRequest.of(List.of(source), List.of(apiClasses))).stream()
                .filter(item -> item.graph().decisionLabel().equals("ternary predicate"))
                .findFirst().orElseThrow();
        String ternaryNode = result.manifest().sourceMappings().values().stream()
                .filter(mapping -> mapping.treeKind().equals("PARENTHESIZED"))
                .map(AnalysisManifest.SourceMapping::nodeId).findFirst()
                .orElseThrow(() -> new AssertionError(result.manifest().sourceMappings()));
        assert result.manifest().branchTargets().stream()
                .noneMatch(target -> target.nodeId().equals(ternaryNode)) : result.manifest().branchTargets();
    }

    private static AnalysisManifest manifest() {
        return new AnalysisManifest("graph", 1, Map.of(), List.of(
                new AnalysisManifest.ProbeSite("entry", AnalysisManifest.ProbeKind.ENTRY,
                        "agentfixture.InstrumentedFixture", "decide"),
                new AnalysisManifest.ProbeSite("predicate", AnalysisManifest.ProbeKind.PREDICATE,
                        "agentfixture.InstrumentedFixture", "decide", 13),
                new AnalysisManifest.ProbeSite("outcome", AnalysisManifest.ProbeKind.OUTCOME,
                        "agentfixture.InstrumentedFixture", "decide"),
                new AnalysisManifest.ProbeSite("outcome", AnalysisManifest.ProbeKind.OUTCOME,
                        "agentfixture.InstrumentedFixture", "decide"),
                new AnalysisManifest.ProbeSite("entry", AnalysisManifest.ProbeKind.ENTRY,
                        "agentfixture.InstrumentedFixture", "decideThroughHelper"),
                new AnalysisManifest.ProbeSite("outcome", AnalysisManifest.ProbeKind.OUTCOME,
                        "agentfixture.InstrumentedFixture", "decideThroughHelper")), List.of(), List.of(
                new AnalysisManifest.BranchTarget("predicate", "edge-true", "edge-false",
                        "agentfixture.InstrumentedFixture", "decide", 13)), Map.of());
    }

    private static AnalysisManifest legacyManifest() {
        return new AnalysisManifest("graph", 1, Map.of(), List.of(
                new AnalysisManifest.ProbeSite("entry", AnalysisManifest.ProbeKind.ENTRY,
                        "agentfixture.InstrumentedFixture", "decide"),
                new AnalysisManifest.ProbeSite("predicate", AnalysisManifest.ProbeKind.PREDICATE,
                        "agentfixture.InstrumentedFixture", "decide", 13),
                new AnalysisManifest.ProbeSite("outcome", AnalysisManifest.ProbeKind.OUTCOME,
                        "agentfixture.InstrumentedFixture", "decide"),
                new AnalysisManifest.ProbeSite("outcome", AnalysisManifest.ProbeKind.OUTCOME,
                        "agentfixture.InstrumentedFixture", "decide")), List.of(), Map.of());
    }

    private static AnalysisManifest dispatchManifest() {
        return new AnalysisManifest("dispatch-graph", 1, Map.of(), List.of(
                new AnalysisManifest.ProbeSite("entry", AnalysisManifest.ProbeKind.ENTRY,
                        "agentfixture.StrategyService", "decide"),
                new AnalysisManifest.ProbeSite("dispatch", AnalysisManifest.ProbeKind.DISPATCH,
                        "agentfixture.StrategyService", "decide"),
                new AnalysisManifest.ProbeSite("outcome", AnalysisManifest.ProbeKind.OUTCOME,
                        "agentfixture.StrategyService", "decide")), List.of(
                new AnalysisManifest.DispatchTarget("dispatch", "edge-local", "agentfixture.LocalRule", "accepts"),
                new AnalysisManifest.DispatchTarget("dispatch", "edge-regional", "agentfixture.RegionalRule", "accepts")),
                Map.of());
    }

    private static BusinessDecisionGraph graph() {
        var nodes = List.of(
                new BusinessDecisionGraph.DecisionNode("entry", BusinessDecisionGraph.NodeKind.ENTRY,
                        "begin decision", Map.of()),
                new BusinessDecisionGraph.DecisionNode("predicate", BusinessDecisionGraph.NodeKind.PREDICATE,
                        "age is below 24", Map.of()),
                new BusinessDecisionGraph.DecisionNode("outcome", BusinessDecisionGraph.NodeKind.OUTCOME,
                        "final decision", Map.of()));
        var edges = List.of(
                new BusinessDecisionGraph.DecisionEdge("edge-true", "predicate", "outcome", "true"),
                new BusinessDecisionGraph.DecisionEdge("edge-false", "predicate", "outcome", "false"));
        return new BusinessDecisionGraph("graph", 1, "eligibility", "entry", nodes, edges,
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
    }

    private static BusinessDecisionGraph dispatchGraph() {
        var nodes = List.of(
                new BusinessDecisionGraph.DecisionNode("entry", BusinessDecisionGraph.NodeKind.ENTRY,
                        "begin decision", Map.of()),
                new BusinessDecisionGraph.DecisionNode("dispatch", BusinessDecisionGraph.NodeKind.DISPATCH,
                        "select applicable decision rule", Map.of()),
                new BusinessDecisionGraph.DecisionNode("local", BusinessDecisionGraph.NodeKind.COMPUTATION,
                        "possible decision rule 1", Map.of()),
                new BusinessDecisionGraph.DecisionNode("regional", BusinessDecisionGraph.NodeKind.COMPUTATION,
                        "possible decision rule 2", Map.of()),
                new BusinessDecisionGraph.DecisionNode("outcome", BusinessDecisionGraph.NodeKind.OUTCOME,
                        "final decision", Map.of()));
        var edges = List.of(
                new BusinessDecisionGraph.DecisionEdge("edge-local", "dispatch", "local", "candidate 1"),
                new BusinessDecisionGraph.DecisionEdge("edge-regional", "dispatch", "regional", "candidate 2"));
        return new BusinessDecisionGraph("dispatch-graph", 1, "delivery eligibility", "entry", nodes, edges,
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
    }

    private static byte[] fixtureBytes() throws Exception {
        return classBytes(CLASS_NAME);
    }

    private static byte[] classBytes(String internalName) throws Exception {
        return Files.readAllBytes(Path.of("fachtracing-agent/target/test-classes", internalName + ".class"));
    }

    private static String sha256(byte[] value) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }

    private static String uncheckedSha256(byte[] value) {
        try { return sha256(value); } catch (Exception error) { throw new IllegalStateException(error); }
    }

    private static String selectedEdge(DecisionExecution execution) {
        return execution.observations().stream().map(DecisionExecution.NodeObservation::selectedEdgeId)
                .filter(java.util.Objects::nonNull).findFirst().orElseThrow();
    }

    private record CompoundCase(
            boolean first,
            boolean second,
            String expectedOutcome,
            int expectedSecondEvaluations) { }

    private static final class IsolatedLoader extends ClassLoader {
        private final byte[] transformed;

        private IsolatedLoader(byte[] transformed) {
            super(FachtracingTransformerTest.class.getClassLoader());
            this.transformed = transformed;
        }

        @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals(CLASS_NAME.replace('/', '.'))) {
                Class<?> defined = findLoadedClass(name);
                if (defined == null) defined = defineClass(name, transformed, 0, transformed.length);
                if (resolve) resolveClass(defined);
                return defined;
            }
            return super.loadClass(name, resolve);
        }
    }

    private static final class MultiClassLoader extends ClassLoader {
        private final Map<String, byte[]> classes;

        private MultiClassLoader(Map<String, byte[]> classes) {
            super(FachtracingTransformerTest.class.getClassLoader());
            this.classes = classes;
        }

        @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            String internalName = name.replace('.', '/');
            byte[] bytes = classes.get(internalName);
            if (bytes == null) return super.loadClass(name, resolve);
            Class<?> defined = findLoadedClass(name);
            if (defined == null) defined = defineClass(name, bytes, 0, bytes.length);
            if (resolve) resolveClass(defined);
            return defined;
        }
    }
}
