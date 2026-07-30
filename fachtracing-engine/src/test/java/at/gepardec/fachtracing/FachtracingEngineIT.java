package at.gepardec.fachtracing;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.analysis.AnalysisRequest;
import at.gepardec.fachtracing.api.DecisionValueRedactor;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.runtime.RuntimeCollector;
import at.gepardec.fachtracing.runtime.TraceRuntime;
import at.gepardec.fachtracing.store.InMemoryDecisionRecordRepository;

import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Executable end-to-end contracts across previously unknown fixture domains. */
public final class FachtracingEngineIT {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path FIXTURES = ROOT.resolve("fachtracing-engine/src/test/resources/fixtures");
    private static final List<Path> ANALYSIS_CLASSPATH = List.of(ROOT.resolve("fachtracing-api/target/classes"));

    private FachtracingEngineIT() { }

    public static void main(String[] args) throws Exception {
        threeDomainsUseTheSameAnalyzeCaptureSaveFlow();
        incompleteAnalysisAndCaptureFailureStayExplicit();
    }

    private static void threeDomainsUseTheSameAnalyzeCaptureSaveFlow() throws Exception {
        var repository = new InMemoryDecisionRecordRepository();
        var engine = new FachtracingEngine(repository);
        var values = new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none());

        var eligibility = engine.analyze(request("eligibility/EligibilityPolicy.java"));
        try (var compiled = compile("eligibility/EligibilityPolicy.java")) {
            Object policy = compiled.loadClass("fixtures.eligibility.EligibilityPolicy").getConstructor().newInstance();
            Object result = policy.getClass().getMethod("decide", int.class, String.class, boolean.class)
                    .invoke(policy, 20, "Vienna", true);
            engine.activate(eligibility, values);
            traceCompleted(eligibility, result, "age and location", "20 and Vienna");
            assertRoundTrip(engine, eligibility, "true");
        }

        var pricing = engine.analyze(request("pricing/PricingPolicy.java"));
        try (var compiled = compile("pricing/PricingPolicy.java")) {
            Class<?> policyType = compiled.loadClass("fixtures.pricing.PricingPolicy");
            Class<?> groupType = compiled.loadClass("fixtures.pricing.PricingPolicy$CustomerGroup");
            Object preferred = enumValue(groupType, "PREFERRED");
            Object result = policyType.getMethod("decide", int.class, int.class, groupType)
                    .invoke(policyType.getConstructor().newInstance(), 2, 100, preferred);
            engine.activate(pricing, values);
            traceCompleted(pricing, result, "customer group", preferred);
            assertRoundTrip(engine, pricing, "185");
        }

        var strategy = engine.analyze(request("strategy/StrategyDecisionService.java"));
        try (var compiled = compile("strategy/StrategyDecisionService.java")) {
            Class<?> serviceType = compiled.loadClass("fixtures.strategy.StrategyDecisionService");
            Class<?> ruleType = compiled.loadClass("fixtures.strategy.DecisionRule");
            Class<?> localType = compiled.loadClass("fixtures.strategy.LocalRule");
            var localConstructor = localType.getDeclaredConstructor();
            localConstructor.setAccessible(true);
            Object local = localConstructor.newInstance();
            var method = serviceType.getMethod("decide", ruleType, int.class);
            Object result = method.invoke(serviceType.getConstructor().newInstance(), local, 10);
            engine.activate(strategy, values);
            TraceRuntime.begin(strategy.graph().graphId(), strategy.graph().version());
            var target = strategy.manifest().dispatchTargets().stream()
                    .filter(candidate -> candidate.ownerHint().endsWith("LocalRule"))
                    .findFirst().orElseThrow();
            TraceRuntime.expectDispatch(target.dispatchNodeId());
            TraceRuntime.selectedEdge(target.dispatchNodeId(), target.edgeId());
            TraceRuntime.complete(outcome(strategy.graph()).nodeId(), result);
            var id = engine.saveNext(strategy.graph()).orElseThrow();
            var record = engine.find(id).orElseThrow();
            assert record.explanation().steps().stream()
                    .anyMatch(step -> step.statement().contains("was selected"));
            assert !record.explanation().toString().contains("LocalRule");
        }
        assert repository.size() == 3;
    }

    private static void incompleteAnalysisAndCaptureFailureStayExplicit() {
        var engine = new FachtracingEngine(new InMemoryDecisionRecordRepository());
        var gaps = engine.analyze(request("gaps/UnsupportedPolicy.java"));
        engine.activate(gaps, new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        TraceRuntime.begin(gaps.graph().graphId(), gaps.graph().version());
        TraceRuntime.complete(outcome(gaps.graph()).nodeId(), true);
        var record = engine.find(engine.saveNext(gaps.graph()).orElseThrow()).orElseThrow();
        assert record.explanation().completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE;
        assert record.structurePlantUml().contains("decision graph is incomplete");
        assert record.structureMermaid().contains("Incomplete analysis");

        RuntimeCollector failing = new RuntimeCollector() {
            @Override public void complete(String nodeId, Object result) { throw new IllegalStateException("offline"); }
        };
        TraceRuntime.configure(failing);
        Object applicationResult = new Object();
        TraceRuntime.complete("outcome", applicationResult);
        assert applicationResult != null;
        assert TraceRuntime.pollDiagnostic().orElseThrow().operation().equals("complete");
    }

    private static void traceCompleted(
            AnalysisManifest.AnalysisResult analysis, Object result, String evidenceLabel, Object evidence) {
        TraceRuntime.begin(analysis.graph().graphId(), analysis.graph().version());
        analysis.graph().nodes().stream()
                .filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.PREDICATE)
                .findFirst().ifPresent(node -> TraceRuntime.observe(node.nodeId(), "true", evidence));
        TraceRuntime.complete(outcome(analysis.graph()).nodeId(), result);
    }

    private static void assertRoundTrip(
            FachtracingEngine engine, AnalysisManifest.AnalysisResult analysis, String expectedResult) {
        var id = engine.saveNext(analysis.graph()).orElseThrow();
        var first = engine.find(id).orElseThrow();
        var second = engine.find(id).orElseThrow();
        assert first.equals(second);
        assert first.execution().finalResult().canonicalValue().equals(expectedResult);
        assert first.explanation().equals(second.explanation());
        assert first.executionPlantUml().equals(second.executionPlantUml());
        assert first.structureMermaid().startsWith("flowchart LR\n");
        assert first.executionMermaid().equals(second.executionMermaid());
    }

    private static BusinessDecisionGraph.DecisionNode outcome(BusinessDecisionGraph graph) {
        return graph.nodes().stream().filter(node -> node.kind() == BusinessDecisionGraph.NodeKind.OUTCOME)
                .findFirst().orElseThrow();
    }

    private static AnalysisRequest request(String fixture) {
        return AnalysisRequest.of(List.of(FIXTURES.resolve(fixture)), ANALYSIS_CLASSPATH);
    }

    private static CompiledFixture compile(String fixture) throws Exception {
        Path output = Files.createTempDirectory("fachtracing-fixture-");
        int exit = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "--release", "21", "-proc:none", "-classpath", ANALYSIS_CLASSPATH.getFirst().toString(),
                "-d", output.toString(), FIXTURES.resolve(fixture).toString());
        if (exit != 0) throw new IllegalStateException("fixture compilation failed: " + fixture);
        return new CompiledFixture(output);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Object enumValue(Class<?> type, String name) {
        return Enum.valueOf((Class<? extends Enum>) type, name);
    }

    private static final class CompiledFixture extends URLClassLoader {
        private CompiledFixture(Path output) throws Exception {
            super(new java.net.URL[] { output.toUri().toURL() }, FachtracingEngineIT.class.getClassLoader());
        }
    }
}
