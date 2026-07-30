package at.gepardec.fachtracing.agent;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
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
        assert first.observations().stream().anyMatch(observation -> observation.nodeId().equals("predicate"));
        assert first.observations().stream().allMatch(observation -> !observation.nodeId().contains("agentfixture"));
        try {
            decide.invoke(instance, -1);
            throw new AssertionError("application exception was not preserved");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            assert expected.getCause() instanceof IllegalArgumentException;
            assert expected.getCause().getMessage().equals("age must not be negative");
        }
        assert collector.pollCompleted().isEmpty();
    }

    private static AnalysisManifest manifest() {
        return new AnalysisManifest("graph", 1, Map.of(), List.of(
                new AnalysisManifest.ProbeSite("entry", AnalysisManifest.ProbeKind.ENTRY,
                        "agentfixture.InstrumentedFixture", "decide"),
                new AnalysisManifest.ProbeSite("predicate", AnalysisManifest.ProbeKind.PREDICATE,
                        "agentfixture.InstrumentedFixture", "decide", 8),
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
        return new BusinessDecisionGraph("graph", 1, "eligibility", "entry", nodes, List.of(),
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
