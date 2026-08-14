package at.gepardec.fachtracing.maven;

import at.gepardec.fachtracing.api.DecisionValueAdapter;
import at.gepardec.fachtracing.api.DecisionValueRedactor;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.runtime.RuntimeActivationBundle;
import at.gepardec.fachtracing.runtime.RuntimeCollector;
import at.gepardec.fachtracing.runtime.TraceRuntime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Executes the Maven plugin's traced policy through the current Java agent. */
public final class SelfTracingRuntimeTest {
    private static final String DECISION_LABEL = "enable developer graph export";
    private static final String AGENT_CLASS = "at.gepardec.fachtracing.agent.FachtracingAgent";

    private SelfTracingRuntimeTest() { }

    /** Runs the disabled, enabled, and invalid configuration paths. */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("activation bundle path is required");
        }
        Path activation = Path.of(args[0]).toAbsolutePath().normalize();
        var bundle = RuntimeActivationBundle.fromJson(Files.readAllBytes(activation));
        var decision = bundle.decisions().stream()
                .filter(item -> item.graph().decisionLabel().equals(DECISION_LABEL))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("self-tracing decision is absent"));

        var collector = new RuntimeCollector();
        for (var item : bundle.decisions()) {
            var codec = new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none())
                    .register(new OptionalValueAdapter());
            collector.register(item.graph(), codec);
        }
        TraceRuntime.configure(collector);
        var agent = AgentControl.load();
        agent.configure(bundle);

        Path repositoryRoot = Path.of(".").toAbsolutePath().normalize();
        Optional<ProjectGraphGenerator.DeveloperOutput> disabled =
                ProjectGraphGenerator.developerOutput(repositoryRoot, null, null);
        require(disabled.isEmpty(), "disabled configuration returned developer output");
        var disabledExecution = nextExecution(collector);
        verifySuccess(disabledExecution, decision.graph().graphId(), "empty");
        printExecution("disabled", decision.graph(), disabledExecution);

        Optional<ProjectGraphGenerator.DeveloperOutput> enabled = ProjectGraphGenerator.developerOutput(
                repositoryRoot,
                "https://example.invalid/fachtracing",
                "https://example.invalid/fachtracing/blob/{revision}/{path}#L{line}");
        require(enabled.isPresent(), "enabled configuration did not return developer output");
        var enabledExecution = nextExecution(collector);
        verifySuccess(enabledExecution, decision.graph().graphId(), "present");
        printExecution("enabled", decision.graph(), enabledExecution);

        IllegalArgumentException applicationFailure = null;
        try {
            ProjectGraphGenerator.developerOutput(
                    repositoryRoot, "https://example.invalid/fachtracing", null);
        } catch (IllegalArgumentException failure) {
            applicationFailure = failure;
        }
        require(applicationFailure != null, "invalid configuration did not preserve its exception");
        require(applicationFailure.getMessage().contains("must be set together"),
                "invalid configuration changed its exception");
        var failedExecution = nextExecution(collector);
        verifyFailure(failedExecution, decision.graph().graphId());
        printExecution("invalid", decision.graph(), failedExecution);

        require(collector.pollCompleted().isEmpty(), "self trace produced an extra execution");
        Set<String> expectedEvidenceGaps = decision.manifest().evidenceTargets().stream()
                .filter(target -> target.argumentIndex() == -1)
                .map(target -> target.evidenceLabel())
                .collect(Collectors.toUnmodifiableSet());
        verifyRuntimeDiagnostics(collector, decision.graph().graphId(), expectedEvidenceGaps);
        require(collector.diagnosticOverflowCount() == 0, "runtime diagnostics overflowed");
        var agentDiagnostic = agent.pollDiagnostic();
        require(agentDiagnostic.isEmpty(), "agent reported an installation diagnostic: "
                + agentDiagnostic.orElse(null));

        System.out.println("FACHTRACING_SELF_RUNTIME_TRACE_OK");
    }

    private static DecisionExecution nextExecution(RuntimeCollector collector) {
        return collector.pollCompleted().orElseThrow(() ->
                new IllegalStateException("production call did not produce a runtime trace"));
    }

    private static void verifySuccess(DecisionExecution execution, String graphId, String result) {
        verifyPath(execution, graphId);
        require(execution.terminalStatus() == DecisionExecution.TerminalStatus.SUCCEEDED,
                "runtime trace did not succeed");
        require(execution.finalResult() != null, "successful trace has no result");
        require(execution.finalResult().type().equals("optional"),
                "successful trace has an unexpected result type");
        require(execution.finalResult().canonicalValue().equals(result),
                "successful trace has an unexpected result");
        require(execution.failure() == null, "successful trace has failure data");
    }

    private static void verifyFailure(DecisionExecution execution, String graphId) {
        verifyPath(execution, graphId);
        require(execution.terminalStatus() == DecisionExecution.TerminalStatus.FAILED,
                "invalid configuration did not produce a failed trace");
        require(execution.finalResult() == null, "failed trace contains a result");
        require(DecisionExecution.FailureData.genericFailure().equals(execution.failure()),
                "failed trace contains unexpected failure data");
    }

    private static void verifyPath(DecisionExecution execution, String graphId) {
        require(execution.graphId().equals(graphId), "runtime trace belongs to another graph");
        require(!execution.observations().isEmpty(), "runtime trace has no observations");
        require(execution.observations().stream().anyMatch(item -> item.selectedEdgeId() != null),
                "runtime trace has no selected branch edge");
    }

    private static void verifyRuntimeDiagnostics(
            RuntimeCollector collector, String graphId, Set<String> expectedEvidenceGaps) {
        var diagnostics = new ArrayList<RuntimeCollector.RuntimeDiagnostic>();
        for (var next = collector.pollDiagnostic(); next.isPresent();
             next = collector.pollDiagnostic()) {
            diagnostics.add(next.orElseThrow());
        }
        require(diagnostics.stream().allMatch(diagnostic -> diagnostic.graphId().equals(graphId)),
                "runtime diagnostic belongs to another graph");
        require(diagnostics.stream().allMatch(diagnostic ->
                        diagnostic.reason() == RuntimeCollector.DiagnosticReason.EXACT_PATH_UNAVAILABLE),
                "runtime collector reported an unexpected diagnostic: " + diagnostics);
        Set<String> actualEvidenceGaps = diagnostics.stream()
                .map(RuntimeCollector.RuntimeDiagnostic::runtimeTarget)
                .collect(Collectors.toUnmodifiableSet());
        require(actualEvidenceGaps.equals(expectedEvidenceGaps),
                "runtime evidence diagnostics do not match the activation bundle: " + diagnostics);
    }

    private static void printExecution(
            String scenario, BusinessDecisionGraph graph, DecisionExecution execution) {
        var outcomes = execution.observations().stream()
                .map(DecisionExecution.NodeObservation::selectedEdgeId)
                .filter(edgeId -> edgeId != null)
                .map(edgeId -> graph.edges().stream()
                        .filter(edge -> edge.edgeId().equals(edgeId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("runtime edge is absent from its graph"))
                        .outcome())
                .toList();
        String result = execution.terminalStatus() == DecisionExecution.TerminalStatus.SUCCEEDED
                ? execution.finalResult().canonicalValue()
                : execution.failure().canonicalValue();
        System.out.printf("FACHTRACING_SELF_RUNTIME_PATH scenario=%s status=%s result=%s "
                        + "outcomes=%s evidenceGaps=%d%n",
                scenario, execution.terminalStatus(), result, outcomes, execution.coverageGaps().size());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static final class OptionalValueAdapter implements DecisionValueAdapter<Optional<?>> {
        @Override
        @SuppressWarnings("unchecked")
        public Class<Optional<?>> targetType() {
            return (Class<Optional<?>>) (Class<?>) Optional.class;
        }

        @Override
        public AdaptedValue adapt(Optional<?> value) {
            String state = value.isPresent() ? "present" : "empty";
            return new AdaptedValue("optional", state, state);
        }
    }

    private record AgentControl(Method configureMethod, Method pollDiagnosticMethod) {
        private static AgentControl load() throws ReflectiveOperationException {
            Class<?> agentClass = Class.forName(AGENT_CLASS);
            return new AgentControl(
                    agentClass.getMethod("configure", RuntimeActivationBundle.class),
                    agentClass.getMethod("pollDiagnostic"));
        }

        private void configure(RuntimeActivationBundle bundle) throws ReflectiveOperationException {
            invoke(configureMethod, bundle);
        }

        private Optional<?> pollDiagnostic() throws ReflectiveOperationException {
            return (Optional<?>) invoke(pollDiagnosticMethod);
        }

        private static Object invoke(Method method, Object... arguments) throws ReflectiveOperationException {
            try {
                return method.invoke(null, arguments);
            } catch (InvocationTargetException failure) {
                Throwable cause = failure.getCause();
                if (cause instanceof ReflectiveOperationException reflectionFailure) throw reflectionFailure;
                if (cause instanceof RuntimeException runtimeFailure) throw runtimeFailure;
                if (cause instanceof Error error) throw error;
                throw failure;
            }
        }
    }
}
