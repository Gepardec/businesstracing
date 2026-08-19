package at.gepardec.fachtracing.maven;

import at.gepardec.fachtracing.api.DecisionValueRedactor;
import at.gepardec.fachtracing.explain.BusinessExecutionMermaidRenderer;
import at.gepardec.fachtracing.explain.BusinessExplanationProjector;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.model.DecisionRecordEnvelope;
import at.gepardec.fachtracing.runtime.RuntimeActivationBundle;
import at.gepardec.fachtracing.runtime.RuntimeCollector;
import at.gepardec.fachtracing.runtime.TraceRuntime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Executes the two selected production policies through the current Java agent. */
public final class SelfTracingRuntimeTest {
    private static final String NODE_LABEL = "include exact node in business graph";
    private static final String SOURCE_LABEL = "select source inputs for graph analysis";
    private static final String AGENT_CLASS = "at.gepardec.fachtracing.agent.FachtracingAgent";

    private SelfTracingRuntimeTest() { }

    /** Runs two node-policy paths and three source-policy paths. */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("activation bundle and runtime output paths are required");
        }
        Path activation = Path.of(args[0]).toAbsolutePath().normalize();
        Path output = Path.of(args[1]).toAbsolutePath().normalize();
        Files.createDirectories(output);

        var bundle = RuntimeActivationBundle.fromJson(Files.readAllBytes(activation));
        var nodeDecision = decision(bundle, NODE_LABEL);
        var sourceDecision = decision(bundle, SOURCE_LABEL);
        requireSelectedMethod(nodeDecision, "at.gepardec.fachtracing.business.BusinessGraphProjector", "classifyNode");
        requireSelectedMethod(sourceDecision, "at.gepardec.fachtracing.analysis.AnalysisSourceSelector", "selectPlan");

        var collector = new RuntimeCollector();
        var codec = new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none());
        bundle.decisions().forEach(item -> collector.register(item.graph(), codec));
        TraceRuntime.configure(collector);
        var agent = AgentControl.load();
        agent.configure(bundle);

        Class<?> nodeKind = Class.forName(
                "at.gepardec.fachtracing.model.BusinessDecisionGraph$NodeKind");
        Class<?> projector = Class.forName(
                "at.gepardec.fachtracing.business.BusinessGraphProjector");
        Method classifyNode = projector.getDeclaredMethod(
                "classifyNode", nodeKind, boolean.class, boolean.class, boolean.class, boolean.class);
        classifyNode.setAccessible(true);
        Class<?> sourceSelector = Class.forName(
                "at.gepardec.fachtracing.analysis.AnalysisSourceSelector");
        Method selectPlan = sourceSelector.getDeclaredMethod(
                "selectPlan", boolean.class, boolean.class);
        selectPlan.setAccessible(true);

        invokeAndVerify(collector, nodeDecision.graph(), classifyNode,
                new Object[]{enumValue(nodeKind, "PREDICATE"), false, false, false, true},
                "TECHNICAL_PREDICATE", output.resolve("01-node-removed-technical-predicate.mmd"));
        invokeAndVerify(collector, nodeDecision.graph(), classifyNode,
                new Object[]{enumValue(nodeKind, "COMPUTATION"), false, false, false, false},
                "BUSINESS_ACTION", output.resolve("02-node-kept-business-action.mmd"));
        invokeAndVerify(collector, sourceDecision.graph(), selectPlan,
                new Object[]{false, false}, "SKIP_PROJECT_WITH_NO_ENTRY_SOURCE",
                output.resolve("03-source-no-entry.mmd"));
        invokeAndVerify(collector, sourceDecision.graph(), selectPlan,
                new Object[]{true, false},
                "USE_CONNECTED_PROJECT_SOURCES_WITH_EXTERNAL_SOURCES_CLASSPATH_AND_ENTRIES",
                output.resolve("04-source-connected.mmd"));
        invokeAndVerify(collector, sourceDecision.graph(), selectPlan,
                new Object[]{true, true},
                "USE_MODULAR_PROJECT_SOURCES_WITH_EXTERNAL_SOURCES_CLASSPATH_AND_ENTRIES",
                output.resolve("05-source-modular.mmd"));

        require(collector.pollCompleted().isEmpty(), "self trace produced an extra execution");
        require(collector.diagnosticOverflowCount() == 0, "runtime diagnostics overflowed");
        require(collector.pollDiagnostic().isEmpty(), "runtime collector reported a diagnostic");
        require(agent.pollDiagnostic().isEmpty(), "agent reported an installation diagnostic");
        System.out.println("FACHTRACING_SELF_RUNTIME_TRACE_OK");
    }

    private static RuntimeActivationBundle.DecisionDefinition decision(
            RuntimeActivationBundle bundle, String label) {
        return bundle.decisions().stream().filter(item -> item.graph().decisionLabel().equals(label))
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "self-tracing decision is absent: " + label));
    }

    private static void requireSelectedMethod(
            RuntimeActivationBundle.DecisionDefinition decision,
            String owner,
            String method) {
        boolean selected = decision.manifest().probeSites().stream().anyMatch(site ->
                site.kind() == at.gepardec.fachtracing.analysis.AnalysisManifest.ProbeKind.ENTRY
                        && site.ownerHint().equals(owner) && site.memberHint().equals(method));
        require(selected, "activation does not identify " + owner + "#" + method);
    }

    private static void invokeAndVerify(
            RuntimeCollector collector,
            BusinessDecisionGraph graph,
            Method method,
            Object[] arguments,
            String expectedResult,
            Path mermaidFile) throws Exception {
        Object actual = invoke(method, arguments);
        require(((Enum<?>) actual).name().equals(expectedResult),
                "production result changed for " + mermaidFile.getFileName());
        DecisionExecution execution = collector.pollCompleted().orElseThrow(() ->
                new IllegalStateException("production call did not produce a runtime trace"));
        require(execution.graphId().equals(graph.graphId()), "runtime trace belongs to another graph");
        require(execution.terminalStatus() == DecisionExecution.TerminalStatus.SUCCEEDED,
                "runtime trace did not succeed");
        require(execution.completeness() == BusinessDecisionGraph.Completeness.COMPLETE,
                "runtime trace is incomplete: " + execution.coverageGaps());
        require(execution.finalResult() != null
                        && execution.finalResult().canonicalValue().equals(expectedResult),
                "runtime trace result does not match the method result");
        require(execution.observations().stream().anyMatch(item -> item.selectedEdgeId() != null),
                "runtime trace has no selected decision edge");

        var explanation = new BusinessExplanationProjector().project(graph, execution);
        String diagram = new BusinessExecutionMermaidRenderer().render(explanation);
        require(diagram.contains("Result: " + expectedResult),
                "evaluated Mermaid does not contain the method result");
        Files.writeString(mermaidFile, diagram);
        Path decisionFile = mermaidFile.resolveSibling(
                mermaidFile.getFileName().toString().replace(".mmd", ".decision.json"));
        var application = new DecisionExecution.DecisionValue("string", "fachtracing", "Fachtracing");
        var policy = new DecisionExecution.DecisionValue(
                "string", graph.decisionLabel(), graph.decisionLabel());
        var envelope = new DecisionRecordEnvelope(
                "self-" + execution.executionId(), execution,
                "self-tracing:" + graph.graphId() + ":v" + graph.version(),
                Map.of("application", application, "policy", policy), "self-dogfood-v1");
        Files.write(decisionFile, envelope.toJson());
        System.out.printf("FACHTRACING_SELF_RUNTIME_PATH file=%s result=%s observations=%d%n",
                mermaidFile.getFileName(), expectedResult, execution.observations().size());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumValue(Class<?> type, String name) {
        return Enum.valueOf((Class<? extends Enum>) type, name);
    }

    private static Object invoke(Method method, Object... arguments) throws Exception {
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error error) throw error;
            throw failure;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private record AgentControl(Method configureMethod, Method pollDiagnosticMethod) {
        private static AgentControl load() throws ReflectiveOperationException {
            Class<?> agentClass = Class.forName(AGENT_CLASS);
            return new AgentControl(
                    agentClass.getMethod("configure", RuntimeActivationBundle.class),
                    agentClass.getMethod("pollDiagnostic"));
        }

        private void configure(RuntimeActivationBundle bundle) throws Exception {
            invoke(configureMethod, bundle);
        }

        private Optional<?> pollDiagnostic() throws Exception {
            return (Optional<?>) invoke(pollDiagnosticMethod);
        }
    }
}
