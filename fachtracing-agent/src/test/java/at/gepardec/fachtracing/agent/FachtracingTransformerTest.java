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
        partialCompoundBindingCreatesRuntimeGap();
        mixedCompoundRecordsExactAtomicPaths();
        negatedCompoundRecordsExactAtomicPaths();
        ternaryPredicateRecordsExactPaths();
        switchExpressionsRecordExactCasePaths();
        patternSwitchRecordsGuardAndExactCase();
        exceptionResourceAndFinallyPathsAreExact();
        finallyReturnPathsOverrideExactly();
        standardAsyncBoundariesPropagateAutomatically();
        unsupportedAsyncBoundaryCreatesExecutionGap();
        controlledBinaryFallbackRecordsExactRuntimePath();
        proxiesServiceLoaderAndConstantReflectionSelectProvenCandidates();
        manifestWithoutBranchTargetsCreatesRuntimeGap();
        transformedTargetsRecordTheActualPolymorphicEdge();
        activationTransformerInstallsMultipleDisjointGraphs();
        analyzerAndTransformerSeparateOverloadsAndTheirLambdas();
    }

    private static void analyzerAndTransformerSeparateOverloadsAndTheirLambdas() throws Exception {
        Path source = Path.of("fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java")
                .toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        var selected = new StaticDecisionAnalyzer().analyzeAll(
                        AnalysisRequest.of(List.of(source), List.of(apiClasses))).stream()
                .filter(item -> item.graph().decisionLabel().startsWith("overload "))
                .toList();
        assert selected.size() == 4 : selected;
        var entryDescriptors = selected.stream()
                .flatMap(item -> item.manifest().probeSites().stream())
                .filter(site -> site.kind() == AnalysisManifest.ProbeKind.ENTRY)
                .map(AnalysisManifest.ProbeSite::descriptorHint)
                .collect(java.util.stream.Collectors.toSet());
        assert entryDescriptors.equals(java.util.Set.of("(I)Z", "(Ljava/lang/String;)Z"))
                : entryDescriptors;
        selected.stream().flatMap(item -> item.manifest().probeSites().stream())
                .forEach(site -> { assert !site.descriptorHint().isBlank() : site; });

        byte[] original = fixtureBytes();
        var transformer = new FachtracingTransformer(
                selected.stream().map(AnalysisManifest.AnalysisResult::manifest).toList(),
                Map.of(CLASS_NAME, sha256(original)));
        byte[] transformed = transformer.transform(null, null, CLASS_NAME, null, null, original);
        assert transformed != null;

        RuntimeCollector collector = new RuntimeCollector();
        for (var result : selected) collector.register(result.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        TraceRuntime.configure(collector);
        Class<?> fixture = new IsolatedLoader(transformed).loadClass(CLASS_NAME.replace('/', '.'));
        Object instance = fixture.getConstructor().newInstance();
        assert fixture.getMethod("overloaded", int.class).invoke(instance, 20).equals(true);
        assert fixture.getMethod("overloaded", String.class).invoke(instance, "Vienna").equals(true);
        assert fixture.getMethod("lambdaOverload", int.class).invoke(instance, 20).equals(true);
        assert fixture.getMethod("lambdaOverload", String.class).invoke(instance, "Vienna").equals(true);

        var byGraph = selected.stream().collect(java.util.stream.Collectors.toMap(
                item -> item.graph().graphId(), AnalysisManifest.AnalysisResult::graph));
        var executions = new java.util.ArrayList<DecisionExecution>();
        collector.pollCompleted().ifPresent(executions::add);
        collector.pollCompleted().ifPresent(executions::add);
        collector.pollCompleted().ifPresent(executions::add);
        collector.pollCompleted().ifPresent(executions::add);
        assert executions.size() == 4 : executions;
        assert executions.stream().map(DecisionExecution::graphId).collect(java.util.stream.Collectors.toSet())
                .equals(byGraph.keySet()) : executions;
        for (DecisionExecution execution : executions) {
            var nodeIds = byGraph.get(execution.graphId()).nodes().stream()
                    .map(BusinessDecisionGraph.DecisionNode::nodeId).collect(java.util.stream.Collectors.toSet());
            assert execution.observations().stream().allMatch(item -> nodeIds.contains(item.nodeId()))
                    : execution;
        }
        assert collector.pollCompleted().isEmpty();
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

    private static void activationTransformerInstallsMultipleDisjointGraphs() throws Exception {
        Path source = Path.of("fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java")
                .toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        var selected = new StaticDecisionAnalyzer().analyzeAll(
                        AnalysisRequest.of(List.of(source), List.of(apiClasses))).stream()
                .filter(item -> item.graph().decisionLabel().equals("compound conjunction")
                        || item.graph().decisionLabel().equals("compound disjunction"))
                .toList();
        assert selected.size() == 2 : selected;
        byte[] original = fixtureBytes();
        var transformer = new FachtracingTransformer(
                selected.stream().map(AnalysisManifest.AnalysisResult::manifest).toList(),
                Map.of(CLASS_NAME, sha256(original)));
        byte[] transformed = transformer.transform(null, null, CLASS_NAME, null, null, original);
        assert transformed != null;

        RuntimeCollector collector = new RuntimeCollector();
        for (var result : selected) collector.register(result.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        TraceRuntime.configure(collector);
        Class<?> fixture = new IsolatedLoader(transformed).loadClass(CLASS_NAME.replace('/', '.'));
        Object instance = fixture.getConstructor().newInstance();
        assert fixture.getMethod("decideAnd", boolean.class, boolean.class).invoke(instance, true, true).equals(true);
        assert fixture.getMethod("decideOr", boolean.class, boolean.class).invoke(instance, false, true).equals(true);
        var executions = List.of(collector.pollCompleted().orElseThrow(), collector.pollCompleted().orElseThrow());
        var graphIds = executions.stream().map(DecisionExecution::graphId).toList();
        assert graphIds.containsAll(selected.stream().map(item -> item.graph().graphId()).toList()) : graphIds;
        for (var execution : executions) {
            var graph = selected.stream().filter(item -> item.graph().graphId().equals(execution.graphId()))
                    .findFirst().orElseThrow().graph();
            var nodeIds = graph.nodes().stream().map(BusinessDecisionGraph.DecisionNode::nodeId).toList();
            assert execution.observations().stream().allMatch(item -> nodeIds.contains(item.nodeId()))
                    : execution.observations();
        }
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

    private static void manifestWithoutBranchTargetsCreatesRuntimeGap() throws Exception {
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
        assert execution.completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE : execution;
        assert execution.coverageGaps().stream().anyMatch(gap ->
                gap.contains("exact Boolean path correlation is unavailable")) : execution.coverageGaps();
        assert execution.observations().stream().noneMatch(observation ->
                observation.nodeId().equals("predicate")) : execution.observations();
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
            assert selectedEdges.size() == 1 + testCase.expectedSecondEvaluations() : selectedEdges;
            assert selectedEdges.getLast().outcome().startsWith(testCase.expectedOutcome()) : selectedEdges;
            assert selectedEdges.stream().allMatch(observation ->
                    observation.evidence().get("value") != null
                            && observation.evidence().get("value").type().equals("boolean"))
                    : selectedEdges;
        }
        assert collector.pollCompleted().isEmpty();
    }

    private static void mixedCompoundRecordsExactAtomicPaths() throws Exception {
        Path source = Path.of("fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java")
                .toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        var result = new StaticDecisionAnalyzer().analyzeAll(
                        AnalysisRequest.of(List.of(source), List.of(apiClasses))).stream()
                .filter(item -> item.graph().decisionLabel().equals("mixed compound"))
                .findFirst().orElseThrow();
        assert result.manifest().branchTargets().size() == 3 : result.manifest().branchTargets();

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
        var method = fixture.getMethod("decideMixed", boolean.class, boolean.class, boolean.class);
        assertAtomicPath(method.invoke(instance, false, true, false), false,
                collector.pollCompleted().orElseThrow(), 2);
        assertAtomicPath(method.invoke(instance, true, false, true), true,
                collector.pollCompleted().orElseThrow(), 3);
        assertAtomicPath(method.invoke(instance, true, true, false), true,
                collector.pollCompleted().orElseThrow(), 2);
    }

    private static void negatedCompoundRecordsExactAtomicPaths() throws Exception {
        Path source = Path.of("fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java")
                .toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        var result = new StaticDecisionAnalyzer().analyzeAll(
                        AnalysisRequest.of(List.of(source), List.of(apiClasses))).stream()
                .filter(item -> item.graph().decisionLabel().equals("negated compound"))
                .findFirst().orElseThrow();
        assert result.manifest().branchTargets().size() == 3 : result.manifest().branchTargets();

        byte[] original = fixtureBytes();
        byte[] transformed = new FachtracingTransformer(
                result.manifest(), Map.of(CLASS_NAME, sha256(original)))
                .transform(null, null, CLASS_NAME, null, null, original);
        RuntimeCollector collector = new RuntimeCollector();
        collector.register(result.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        TraceRuntime.configure(collector);
        Class<?> fixture = new IsolatedLoader(transformed).loadClass(CLASS_NAME.replace('/', '.'));
        Object instance = fixture.getConstructor().newInstance();
        var method = fixture.getMethod("decideNegated", boolean.class, boolean.class, boolean.class);
        assertAtomicPath(method.invoke(instance, false, true, true), true,
                collector.pollCompleted().orElseThrow(), 1);
        assertAtomicPath(method.invoke(instance, true, false, false), true,
                collector.pollCompleted().orElseThrow(), 3);
        assertAtomicPath(method.invoke(instance, true, true, false), false,
                collector.pollCompleted().orElseThrow(), 2);
    }

    private static void assertAtomicPath(
            Object returned, boolean expected, DecisionExecution execution, int evaluatedAtoms) {
        assert returned.equals(expected) : returned;
        var selected = execution.observations().stream()
                .filter(observation -> observation.selectedEdgeId() != null).toList();
        assert selected.size() == evaluatedAtoms : selected;
        assert selected.stream().noneMatch(observation -> observation.outcome().equals("evaluated")) : selected;
        assert selected.stream().allMatch(observation ->
                observation.evidence().get("value") != null
                        && observation.evidence().get("value").type().equals("boolean")) : selected;
    }

    private static void partialCompoundBindingCreatesRuntimeGap() throws Exception {
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
        assert execution.observations().stream().noneMatch(observation ->
                observation.selectedEdgeId() != null) : execution.observations();
        assert execution.completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE : execution;
        assert execution.coverageGaps().stream().anyMatch(gap ->
                gap.contains("exact Boolean path correlation is unavailable")) : execution.coverageGaps();
    }

    private static void ternaryPredicateRecordsExactPaths() throws Exception {
        Path source = Path.of("fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java")
                .toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        var result = new StaticDecisionAnalyzer().analyzeAll(
                        AnalysisRequest.of(List.of(source), List.of(apiClasses))).stream()
                .filter(item -> item.graph().decisionLabel().equals("ternary predicate"))
                .findFirst().orElseThrow();
        assert result.manifest().branchTargets().size() == 3 : result.manifest().branchTargets();

        byte[] original = fixtureBytes();
        var transformer = new FachtracingTransformer(
                result.manifest(), Map.of(CLASS_NAME, sha256(original)));
        byte[] transformed = transformer.transform(null, null, CLASS_NAME, null, null, original);
        RuntimeCollector collector = new RuntimeCollector();
        collector.register(result.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        TraceRuntime.configure(collector);
        Class<?> fixture = new IsolatedLoader(transformed).loadClass(CLASS_NAME.replace('/', '.'));
        Object instance = fixture.getConstructor().newInstance();
        var method = fixture.getMethod("decideTernary", boolean.class, boolean.class, boolean.class);
        assertAtomicPath(method.invoke(instance, true, false, true), false,
                collector.pollCompleted().orElseThrow(), 2);
        assertAtomicPath(method.invoke(instance, false, false, true), true,
                collector.pollCompleted().orElseThrow(), 2);
    }

    private static void switchExpressionsRecordExactCasePaths() throws Exception {
        Path source = Path.of("fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java")
                .toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        var results = new StaticDecisionAnalyzer().analyzeAll(
                        AnalysisRequest.of(List.of(source), List.of(apiClasses))).stream()
                .filter(item -> item.graph().decisionLabel().equals("integer switch")
                        || item.graph().decisionLabel().equals("string switch")
                        || item.graph().decisionLabel().equals("enum switch"))
                .toList();
        assert results.size() == 3 : results;
        assert results.stream().allMatch(item -> item.graph().completeness()
                == BusinessDecisionGraph.Completeness.COMPLETE) : results;
        assert results.stream().allMatch(item -> !item.manifest().controlTargets().isEmpty()) : results;

        byte[] original = fixtureBytes();
        byte[] transformed = new FachtracingTransformer(
                results.stream().map(AnalysisManifest.AnalysisResult::manifest).toList(),
                Map.of(CLASS_NAME, sha256(original)))
                .transform(null, null, CLASS_NAME, null, null, original);
        RuntimeCollector collector = new RuntimeCollector();
        results.forEach(result -> collector.register(result.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none())));
        TraceRuntime.configure(collector);
        Class<?> fixture = new IsolatedLoader(transformed).loadClass(CLASS_NAME.replace('/', '.'));
        Object instance = fixture.getConstructor().newInstance();

        assert fixture.getMethod("decideIntegerSwitch", int.class).invoke(instance, 1).equals("low");
        assertOneControlEdge(collector.pollCompleted().orElseThrow(), "1");
        assert fixture.getMethod("decideIntegerSwitch", int.class).invoke(instance, 3).equals("medium");
        assertOneControlEdge(collector.pollCompleted().orElseThrow(), "2 or 3");
        assert fixture.getMethod("decideIntegerSwitch", int.class).invoke(instance, 9).equals("high");
        assertOneControlEdge(collector.pollCompleted().orElseThrow(), "default");

        assert fixture.getMethod("decideStringSwitch", String.class)
                .invoke(instance, "preferred").equals(2);
        assertOneControlEdge(collector.pollCompleted().orElseThrow(), "preferred");
        assert fixture.getMethod("decideStringSwitch", String.class)
                .invoke(instance, "unknown").equals(0);
        assertOneControlEdge(collector.pollCompleted().orElseThrow(), "default");

        Class<?> groupType = fixture.getClassLoader().loadClass("agentfixture.InstrumentedFixture$CustomerGroup");
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object preferred = Enum.valueOf((Class) groupType, "PREFERRED");
        assert fixture.getMethod("decideEnumSwitch", groupType).invoke(instance, preferred).equals(true);
        assertOneControlEdge(collector.pollCompleted().orElseThrow(), "preferred");
    }

    private static void patternSwitchRecordsGuardAndExactCase() throws Exception {
        Path source = Path.of("fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java")
                .toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        var result = new StaticDecisionAnalyzer().analyzeAll(
                        AnalysisRequest.of(List.of(source), List.of(apiClasses))).stream()
                .filter(item -> item.graph().decisionLabel().equals("pattern switch"))
                .findFirst().orElseThrow();
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : result;
        assert result.manifest().controlTargets().size() == 3 : result.manifest().controlTargets();
        assert result.manifest().branchTargets().size() == 1 : result.manifest().branchTargets();

        byte[] original = fixtureBytes();
        byte[] transformed = new FachtracingTransformer(
                result.manifest(), Map.of(CLASS_NAME, sha256(original)))
                .transform(null, null, CLASS_NAME, null, null, original);
        RuntimeCollector collector = new RuntimeCollector();
        collector.register(result.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        TraceRuntime.configure(collector);
        Class<?> fixture = new IsolatedLoader(transformed).loadClass(CLASS_NAME.replace('/', '.'));
        Object instance = fixture.getConstructor().newInstance();
        Class<?> inputType = Class.forName("agentfixture.InstrumentedFixture$DecisionInput");
        Class<?> ageType = Class.forName("agentfixture.InstrumentedFixture$AgeInput");
        Class<?> categoryType = Class.forName("agentfixture.InstrumentedFixture$CategoryInput");
        var method = fixture.getMethod("decidePatternSwitch", inputType);

        assert method.invoke(instance, ageType.getConstructor(int.class).newInstance(30)).equals("adult");
        var adult = collector.pollCompleted().orElseThrow().observations().stream()
                .filter(item -> item.selectedEdgeId() != null).toList();
        assert adult.size() == 2 : adult;
        assert adult.stream().anyMatch(item -> item.outcome().equals("true")) : adult;
        assert method.invoke(instance, ageType.getConstructor(int.class).newInstance(20)).equals("young");
        var young = collector.pollCompleted().orElseThrow().observations().stream()
                .filter(item -> item.selectedEdgeId() != null).toList();
        assert young.size() == 2 : young;
        assert young.stream().anyMatch(item -> item.outcome().equals("false")) : young;
        assert method.invoke(instance, categoryType.getConstructor(String.class).newInstance("gold"))
                .equals("gold");
        var category = collector.pollCompleted().orElseThrow().observations().stream()
                .filter(item -> item.selectedEdgeId() != null).toList();
        assert category.size() == 1 : category;
    }

    private static void assertOneControlEdge(DecisionExecution execution, String expectedOutcome) {
        var selected = execution.observations().stream()
                .filter(observation -> observation.selectedEdgeId() != null).toList();
        assert selected.size() == 1 : selected;
        assert selected.getFirst().outcome().contains(expectedOutcome) : selected;
    }

    private static void exceptionResourceAndFinallyPathsAreExact() throws Exception {
        Path source = Path.of("fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java")
                .toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        var results = new StaticDecisionAnalyzer().analyzeAll(
                        AnalysisRequest.of(List.of(source), List.of(apiClasses))).stream()
                .filter(item -> item.graph().decisionLabel().equals("caught exception decision")
                        || item.graph().decisionLabel().equals("finally decision")
                        || item.graph().decisionLabel().equals("resource decision")
                        || item.graph().decisionLabel().equals("nested exception decision"))
                .toList();
        assert results.size() == 4 : results;
        assert results.stream().allMatch(item -> item.graph().completeness()
                == BusinessDecisionGraph.Completeness.COMPLETE) : results;
        assert results.stream().flatMap(item -> item.graph().nodes().stream())
                .noneMatch(node -> node.businessLabel().contains("catch")
                        || node.businessLabel().contains("throw")
                        || node.businessLabel().contains("resource")) : results;

        byte[] original = fixtureBytes();
        byte[] transformed = new FachtracingTransformer(
                results.stream().map(AnalysisManifest.AnalysisResult::manifest).toList(),
                Map.of(CLASS_NAME, sha256(original)))
                .transform(null, null, CLASS_NAME, null, null, original);
        RuntimeCollector collector = new RuntimeCollector();
        results.forEach(result -> collector.register(result.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none())));
        TraceRuntime.configure(collector);
        Class<?> fixture = new IsolatedLoader(transformed).loadClass(CLASS_NAME.replace('/', '.'));
        Object instance = fixture.getConstructor().newInstance();

        var caught = fixture.getMethod("decideCaughtException", int.class, int.class);
        assert caught.invoke(instance, -1, 10).equals(false);
        var caughtExecution = collector.pollCompleted().orElseThrow();
        assert caughtExecution.observations().stream().filter(item -> item.selectedEdgeId() != null)
                .anyMatch(item -> item.outcome().startsWith("alternative result"))
                : caughtExecution.observations();
        assert caught.invoke(instance, 20, 5).equals(true);
        var normalExecution = collector.pollCompleted().orElseThrow();
        assert normalExecution.observations().stream().filter(item -> item.selectedEdgeId() != null)
                .anyMatch(item -> item.outcome().startsWith("primary result"))
                : normalExecution.observations();

        assert fixture.getMethod("decideFinally", int.class, boolean.class)
                .invoke(instance, 20, true).equals(true);
        assert collector.pollCompleted().orElseThrow().finalResult().canonicalValue().equals("true");
        assert fixture.getMethod("decideResource", String.class)
                .invoke(instance, "gold").equals(true);
        assert collector.pollCompleted().orElseThrow().finalResult().canonicalValue().equals("true");
        var nested = fixture.getMethod("decideNestedException", int.class, boolean.class);
        assert nested.invoke(instance, -1, false).equals(false);
        assert collector.pollCompleted().orElseThrow().finalResult().canonicalValue().equals("false");
        assert nested.invoke(instance, 23, true).equals(true);
        assert collector.pollCompleted().orElseThrow().finalResult().canonicalValue().equals("true");
    }

    private static void finallyReturnPathsOverrideExactly() throws Exception {
        Path source = Path.of("fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java")
                .toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        var results = new StaticDecisionAnalyzer().analyzeAll(
                        AnalysisRequest.of(List.of(source), List.of(apiClasses))).stream()
                .filter(item -> item.graph().decisionLabel().endsWith(" finally return"))
                .toList();
        assert results.size() == 2 : results;
        assert results.stream().allMatch(item -> item.graph().completeness()
                == BusinessDecisionGraph.Completeness.COMPLETE) : results;

        byte[] original = fixtureBytes();
        byte[] transformed = new FachtracingTransformer(
                results.stream().map(AnalysisManifest.AnalysisResult::manifest).toList(),
                Map.of(CLASS_NAME, sha256(original)))
                .transform(null, null, CLASS_NAME, null, null, original);
        RuntimeCollector collector = new RuntimeCollector();
        results.forEach(result -> collector.register(result.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none())));
        TraceRuntime.configure(collector);
        Class<?> fixture = new IsolatedLoader(transformed).loadClass(CLASS_NAME.replace('/', '.'));
        Object instance = fixture.getConstructor().newInstance();

        var conditional = fixture.getMethod(
                "decideConditionalFinallyReturn", boolean.class, boolean.class);
        assert conditional.invoke(instance, true, false).equals(true);
        assert collector.pollCompleted().orElseThrow().finalResult().canonicalValue().equals("true");
        assert conditional.invoke(instance, true, true).equals(false);
        assert collector.pollCompleted().orElseThrow().finalResult().canonicalValue().equals("false");
        assert fixture.getMethod("decideOverridingFinallyReturn", int.class)
                .invoke(instance, 30).equals(false);
        var overridden = collector.pollCompleted().orElseThrow();
        assert overridden.finalResult().canonicalValue().equals("false") : overridden;
        assert overridden.observations().stream().anyMatch(item -> item.selectedEdgeId() != null)
                : overridden.observations();
    }

    private static void standardAsyncBoundariesPropagateAutomatically() throws Exception {
        Path source = Path.of("fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java")
                .toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        var results = new StaticDecisionAnalyzer().analyzeAll(
                        AnalysisRequest.of(List.of(source), List.of(apiClasses))).stream()
                .filter(item -> item.graph().decisionLabel().endsWith(" async"))
                .toList();
        assert results.size() == 4 : results;
        assert results.stream().allMatch(item -> item.graph().completeness()
                == BusinessDecisionGraph.Completeness.COMPLETE) : results;

        byte[] original = fixtureBytes();
        byte[] transformed = new FachtracingTransformer(
                results.stream().map(AnalysisManifest.AnalysisResult::manifest).toList(),
                Map.of(CLASS_NAME, sha256(original)))
                .transform(null, null, CLASS_NAME, null, null, original);
        RuntimeCollector collector = new RuntimeCollector();
        results.forEach(result -> collector.register(result.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none())));
        TraceRuntime.configure(collector);
        Class<?> fixture = new IsolatedLoader(transformed).loadClass(CLASS_NAME.replace('/', '.'));
        Object instance = fixture.getConstructor().newInstance();

        for (String methodName : List.of(
                "decideStageAsync", "decideExecutorAsync", "decidePlatformThread", "decideVirtualThread")) {
            assert fixture.getMethod(methodName, int.class).invoke(instance, 30).equals(true) : methodName;
            var execution = collector.pollCompleted().orElseThrow();
            assert execution.observations().stream().anyMatch(item -> item.selectedEdgeId() != null)
                    : methodName + ": " + execution.observations();
            assert execution.observations().stream().noneMatch(item -> item.outcome().equals("evaluated"))
                    : methodName + ": " + execution.observations();
        }

        var stage = fixture.getMethod("decideStageAsync", int.class);
        try (var callers = java.util.concurrent.Executors.newFixedThreadPool(32)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int index = 0; index < 1_000; index++) {
                int age = index % 48;
                futures.add(callers.submit(() -> {
                    Object returned = stage.invoke(instance, age);
                    assert returned.equals(age >= 24) : returned;
                    return null;
                }));
            }
            for (var future : futures) future.get();
        }
        var executions = new java.util.ArrayList<DecisionExecution>();
        for (int index = 0; index < 1_000; index++) {
            executions.add(collector.pollCompleted().orElseThrow());
        }
        assert executions.size() == 1_000 : executions.size();
        for (DecisionExecution execution : executions) {
            String age = execution.observations().stream()
                    .filter(item -> item.outcome().equals("input 1"))
                    .map(item -> item.evidence().get("value").canonicalValue())
                    .findFirst().orElseThrow();
            assert execution.finalResult().canonicalValue()
                    .equals(Boolean.toString(Integer.parseInt(age) >= 24)) : execution;
        }
    }

    private static void unsupportedAsyncBoundaryCreatesExecutionGap() throws Exception {
        Path source = Path.of("fachtracing-agent/src/test/java/agentfixture/InstrumentedFixture.java")
                .toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        var result = new StaticDecisionAnalyzer().analyzeAll(
                        AnalysisRequest.of(List.of(source), List.of(apiClasses))).stream()
                .filter(item -> item.graph().decisionLabel().equals("scheduled boundary gap"))
                .findFirst().orElseThrow();
        byte[] original = fixtureBytes();
        byte[] transformed = new FachtracingTransformer(
                result.manifest(), Map.of(CLASS_NAME, sha256(original)))
                .transform(null, null, CLASS_NAME, null, null, original);
        RuntimeCollector collector = new RuntimeCollector();
        collector.register(result.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        TraceRuntime.configure(collector);
        Class<?> fixture = new IsolatedLoader(transformed).loadClass(CLASS_NAME.replace('/', '.'));
        Object instance = fixture.getConstructor().newInstance();
        assert fixture.getMethod("decideUnsupportedScheduledBoundary", int.class)
                .invoke(instance, 30).equals(true);
        var execution = collector.pollCompleted().orElseThrow();
        assert execution.completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE : execution;
        assert execution.coverageGaps().contains(
                "execution crossed an unsupported asynchronous boundary") : execution.coverageGaps();
        assert collector.pollDiagnostic().orElseThrow().reason()
                == RuntimeCollector.DiagnosticReason.UNSUPPORTED_ASYNC_BOUNDARY;
    }

    private static void controlledBinaryFallbackRecordsExactRuntimePath() throws Exception {
        String entryName = "agentfixture/BinaryEntryFixture";
        String ruleName = "agentfixture/BinaryRule";
        Path source = Path.of("fachtracing-agent/src/test/java/agentfixture/BinaryEntryFixture.java")
                .toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        Path testClasses = Path.of("fachtracing-agent/target/test-classes").toAbsolutePath().normalize();
        var result = new StaticDecisionAnalyzer().analyze(
                AnalysisRequest.of(List.of(source), List.of(apiClasses, testClasses)));
        assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE : result;
        assert result.manifest().probeSites().stream().anyMatch(site ->
                site.ownerHint().equals("agentfixture.BinaryRule") && site.sourceLine() == -1) : result.manifest();

        Map<String, byte[]> originals = Map.of(
                entryName, classBytes(entryName), ruleName, classBytes(ruleName));
        Map<String, String> fingerprints = originals.entrySet().stream().collect(
                java.util.stream.Collectors.toMap(Map.Entry::getKey,
                        entry -> uncheckedSha256(entry.getValue())));
        var transformer = new FachtracingTransformer(result.manifest(), fingerprints);
        var transformed = new java.util.HashMap<String, byte[]>();
        originals.forEach((name, bytes) -> transformed.put(name,
                transformer.transform(null, null, name, null, null, bytes)));
        assert transformed.values().stream().noneMatch(java.util.Objects::isNull) : transformed;

        RuntimeCollector collector = new RuntimeCollector();
        collector.register(result.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        TraceRuntime.configure(collector);
        Class<?> fixture = new MultiClassLoader(transformed).loadClass("agentfixture.BinaryEntryFixture");
        Object instance = fixture.getConstructor().newInstance();
        assert fixture.getMethod("decide", int.class).invoke(instance, 30).equals(true);
        assert fixture.getMethod("decide", int.class).invoke(instance, 20).equals(false);
        assert collector.pollCompleted().orElseThrow().observations().stream()
                .anyMatch(item -> item.selectedEdgeId() != null && item.outcome().equals("true"));
        assert collector.pollCompleted().orElseThrow().observations().stream()
                .anyMatch(item -> item.selectedEdgeId() != null && item.outcome().equals("false"));
    }

    private static void proxiesServiceLoaderAndConstantReflectionSelectProvenCandidates() throws Exception {
        Path sourceRoot = Path.of("fachtracing-agent/src/test/java/agentfixture").toAbsolutePath().normalize();
        Path apiClasses = Path.of("fachtracing-api/target/classes").toAbsolutePath().normalize();
        var results = new StaticDecisionAnalyzer().analyzeAll(AnalysisRequest.of(List.of(
                sourceRoot.resolve("StrategyService.java"), sourceRoot.resolve("DecisionRule.java"),
                sourceRoot.resolve("LocalRule.java"), sourceRoot.resolve("RegionalRule.java")),
                List.of(apiClasses)));
        assert results.size() == 4 : results;
        var supported = results.stream().filter(item -> !item.graph().decisionLabel()
                .equals("unknown reflection decision")).toList();
        assert supported.stream().allMatch(item -> item.graph().completeness()
                == BusinessDecisionGraph.Completeness.COMPLETE) : supported;
        assert supported.stream().allMatch(item -> item.manifest().dispatchTargets().size() == 2) : supported;
        var unknown = results.stream().filter(item -> item.graph().decisionLabel()
                .equals("unknown reflection decision")).findFirst().orElseThrow();
        assert unknown.graph().completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE : unknown;
        assert unknown.graph().coverageGaps().stream().anyMatch(gap ->
                gap.description().contains("cannot be reconstructed from constants")) : unknown;

        String serviceName = "agentfixture/StrategyService";
        String localName = "agentfixture/LocalRule";
        String regionalName = "agentfixture/RegionalRule";
        Map<String, byte[]> originals = Map.of(
                serviceName, classBytes(serviceName), localName, classBytes(localName),
                regionalName, classBytes(regionalName));
        Map<String, String> fingerprints = originals.entrySet().stream().collect(
                java.util.stream.Collectors.toMap(Map.Entry::getKey,
                        entry -> uncheckedSha256(entry.getValue())));
        var transformer = new FachtracingTransformer(
                supported.stream().map(AnalysisManifest.AnalysisResult::manifest).toList(), fingerprints);
        var transformed = new java.util.HashMap<String, byte[]>();
        originals.forEach((name, bytes) -> transformed.put(name,
                transformer.transform(null, null, name, null, null, bytes)));

        RuntimeCollector collector = new RuntimeCollector();
        supported.forEach(result -> collector.register(result.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none())));
        TraceRuntime.configure(collector);
        ClassLoader loader = new MultiClassLoader(transformed);
        Class<?> serviceType = loader.loadClass("agentfixture.StrategyService");
        Class<?> ruleType = Class.forName("agentfixture.DecisionRule");
        Object service = serviceType.getConstructor().newInstance();
        Object local = loader.loadClass("agentfixture.LocalRule").getConstructor().newInstance();
        Object regional = loader.loadClass("agentfixture.RegionalRule").getConstructor().newInstance();

        Object proxy = java.lang.reflect.Proxy.newProxyInstance(ruleType.getClassLoader(),
                new Class<?>[] { ruleType }, (ignored, method, arguments) -> method.invoke(regional, arguments));
        assert serviceType.getMethod("decide", ruleType, int.class)
                .invoke(service, proxy, 30).equals(true);
        assert selectedEdge(collector.pollCompleted().orElseThrow()).equals(
                expectedDispatchEdge(results, "proxy decision", "agentfixture.RegionalRule"))
                : "proxy did not select its proven delegate";

        assert serviceType.getMethod("decideReflectively", ruleType, int.class)
                .invoke(service, local, 30).equals(false);
        assert selectedEdge(collector.pollCompleted().orElseThrow()).equals(
                expectedDispatchEdge(results, "reflection decision", "agentfixture.LocalRule"))
                : "reflection did not select its proven target";

        Path services = Files.createTempDirectory("fachtracing-services-");
        try {
            Path registration = services.resolve("META-INF/services/agentfixture.DecisionRule");
            Files.createDirectories(registration.getParent());
            Files.writeString(registration, "agentfixture.LocalRule\n");
            ClassLoader previous = Thread.currentThread().getContextClassLoader();
            try (var serviceLoader = new java.net.URLClassLoader(
                    new java.net.URL[] { services.toUri().toURL() }, loader)) {
                Thread.currentThread().setContextClassLoader(serviceLoader);
                assert serviceType.getMethod("decideFromServices", int.class)
                        .invoke(service, 10).equals(true);
            } finally {
                Thread.currentThread().setContextClassLoader(previous);
            }
            assert selectedEdge(collector.pollCompleted().orElseThrow()).equals(
                    expectedDispatchEdge(results, "service loader decision", "agentfixture.LocalRule"))
                    : "ServiceLoader did not select its proven provider";
        } finally {
            deleteTree(services);
        }
    }

    private static String expectedDispatchEdge(
            List<AnalysisManifest.AnalysisResult> results, String label, String owner) {
        return results.stream().filter(item -> item.graph().decisionLabel().equals(label))
                .flatMap(item -> item.manifest().dispatchTargets().stream())
                .filter(target -> target.ownerHint().equals(owner))
                .map(AnalysisManifest.DispatchTarget::edgeId).findFirst().orElseThrow();
    }

    private static void deleteTree(Path root) throws Exception {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
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
