package at.gepardec.fachtracing.runtime;

import at.gepardec.fachtracing.api.DecisionValueRedactor;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Executable contracts for ordered, isolated, failure-safe runtime capture. */
public final class RuntimeCollectorTest {
    private RuntimeCollectorTest() { }

    public static void main(String[] args) throws Exception {
        createsRestartSafeExecutionIds();
        recordsOnlyValidExactEdges();
        queuesGenericFailedExecutions();
        failedChildKeepsParentDispatchState();
        recordsOpaquePolymorphicEdges();
        resolvesAssignableTargetsAndReportsBoundedMismatches();
        boundsConcurrentUniqueDiagnosticsStrictly();
        keepsGraphVersionsAndDispatchMappingsSeparate();
        propagatesAndClearsExplicitAsyncContext();
        implementationEdgesRequireTheExpectedDispatch();
        matchesNestedDispatchExpectationsInStackOrder();
        isolatesThirtyTwoConcurrentInvocations();
        tracingFailuresDoNotEscape();
    }

    private static void createsRestartSafeExecutionIds() throws Exception {
        RuntimeCollector first = collector();
        RuntimeCollector second = collector();
        first.begin("graph", 1);
        first.complete("outcome", true);
        second.begin("graph", 1);
        second.complete("outcome", true);
        String firstId = first.pollCompleted().orElseThrow().executionId();
        String secondId = second.pollCompleted().orElseThrow().executionId();
        assert !firstId.equals(secondId) : firstId;

        RuntimeCollector concurrent = collector();
        int invocations = 64;
        var ready = new CountDownLatch(invocations);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(invocations)) {
            for (int index = 0; index < invocations; index++) {
                executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    concurrent.begin("graph", 1);
                    concurrent.complete("outcome", true);
                    return null;
                });
            }
            assert ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            executor.shutdown();
            assert executor.awaitTermination(10, TimeUnit.SECONDS);
        }
        var ids = new java.util.HashSet<String>();
        while (concurrent.pollCompleted().map(execution -> ids.add(execution.executionId())).orElse(false)) { }
        assert ids.size() == invocations : ids.size();
    }

    private static void recordsOnlyValidExactEdges() {
        RuntimeCollector collector = collector();
        collector.begin("graph", 1);
        collector.edge("predicate", "edge-true");
        collector.edge("predicate", "edge-missing");
        collector.edge("dispatch", "edge-true");
        collector.complete("outcome", true);

        var execution = collector.pollCompleted().orElseThrow();
        var exactEdges = execution.observations().stream()
                .filter(observation -> observation.selectedEdgeId() != null)
                .toList();
        assert exactEdges.size() == 1 : exactEdges;
        assert exactEdges.getFirst().nodeId().equals("predicate");
        assert exactEdges.getFirst().selectedEdgeId().equals("edge-true");
        assert exactEdges.getFirst().outcome().equals("qualified");
    }

    private static void queuesGenericFailedExecutions() {
        RuntimeCollector collector = collector();
        collector.begin("graph", 1);
        collector.observe("predicate", "evaluated", true);
        var applicationFailure = new IllegalStateException("private technical message");
        collector.fail(applicationFailure);

        var execution = collector.pollCompleted().orElseThrow();
        assert execution.terminalStatus() == DecisionExecution.TerminalStatus.FAILED;
        assert execution.finalResult() == null;
        assert execution.failure().equals(DecisionExecution.FailureData.genericFailure());
        assert !execution.toString().contains(applicationFailure.getClass().getName());
        assert !execution.toString().contains(applicationFailure.getMessage());
        assert collector.completedCount() == 0;
    }

    private static void failedChildKeepsParentDispatchState() {
        RuntimeCollector collector = collector();
        collector.begin("graph", 1);
        collector.expectDispatch("dispatch");
        collector.begin("graph", 1);
        collector.expectDispatch("nested-dispatch");
        collector.fail(new IllegalArgumentException("child failure"));
        collector.selectedEdge("dispatch", "edge-dispatch");
        collector.complete("outcome", true);

        var child = collector.pollCompleted().orElseThrow();
        var parent = collector.pollCompleted().orElseThrow();
        assert child.terminalStatus() == DecisionExecution.TerminalStatus.FAILED;
        assert parent.terminalStatus() == DecisionExecution.TerminalStatus.SUCCEEDED;
        assert parent.observations().stream()
                .anyMatch(observation -> "edge-dispatch".equals(observation.selectedEdgeId()))
                : parent.observations();
    }

    private static void recordsOpaquePolymorphicEdges() {
        RuntimeCollector collector = collector();
        collector.registerDispatch("dispatch", LocalRule.class, "edge-local");
        collector.registerDispatch("dispatch", RegionalRule.class, "edge-regional");

        collector.begin("graph", 1);
        collector.dispatch("dispatch", new LocalRule());
        collector.complete("outcome", true);
        collector.begin("graph", 1);
        collector.dispatch("dispatch", new RegionalRule());
        collector.complete("outcome", false);

        var first = collector.pollCompleted().orElseThrow();
        var second = collector.pollCompleted().orElseThrow();
        assert first.observations().getFirst().selectedEdgeId().equals("edge-local");
        assert second.observations().getFirst().selectedEdgeId().equals("edge-regional");
        assert first.observations().stream().noneMatch(observation -> observation.toString().contains("LocalRule"));
    }

    private static void resolvesAssignableTargetsAndReportsBoundedMismatches() {
        RuntimeCollector collector = new RuntimeCollector(java.time.Clock.systemUTC(), 2);
        collector.register(graph(), new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        collector.registerDispatch("graph", 1, "dispatch", BaseRule.class, "edge-dispatch");
        collector.begin("graph", 1);
        collector.dispatch("dispatch", new GeneratedRuleProxy());
        collector.dispatch("dispatch", new UnknownRule());
        collector.dispatch("dispatch", new UnknownRule());
        collector.complete("outcome", true);
        var execution = collector.pollCompleted().orElseThrow();
        assert execution.observations().stream()
                .anyMatch(item -> "edge-dispatch".equals(item.selectedEdgeId())) : execution.observations();
        var diagnostic = collector.pollDiagnostic().orElseThrow();
        assert diagnostic.reason() == RuntimeCollector.DiagnosticReason.UNKNOWN_TARGET : diagnostic;
        assert diagnostic.runtimeTarget().contains("UnknownRule") : diagnostic;
        assert collector.pollDiagnostic().isEmpty() : "duplicate diagnostic was not suppressed";

        collector.registerDispatch("graph", 1, "dispatch", LeftRule.class, "edge-dispatch");
        collector.registerDispatch("graph", 1, "dispatch", RightRule.class, "edge-dispatch");
        collector.begin("graph", 1);
        collector.dispatch("dispatch", new AmbiguousRule());
        collector.dispatch("other", new UnknownRule());
        collector.complete("outcome", true);
        assert collector.pollDiagnostic().orElseThrow().reason()
                == RuntimeCollector.DiagnosticReason.AMBIGUOUS_TARGET;
        assert collector.diagnosticOverflowCount() == 1 : collector.diagnosticOverflowCount();
    }

    private static void keepsGraphVersionsAndDispatchMappingsSeparate() {
        RuntimeCollector collector = new RuntimeCollector();
        var codec = new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none());
        BusinessDecisionGraph first = graph("multi", 1, "edge-one");
        BusinessDecisionGraph second = graph("multi", 2, "edge-two");
        collector.register(first, codec, "class-one");
        collector.register(second, codec, "class-two");
        collector.registerDispatch("multi", 1, "dispatch", LocalRule.class, "edge-one");
        collector.registerDispatch("multi", 2, "dispatch", LocalRule.class, "edge-two");
        collector.begin("multi", 1);
        collector.dispatch("dispatch", new LocalRule());
        collector.complete("outcome", true);
        collector.begin("multi", 2);
        collector.dispatch("dispatch", new LocalRule());
        collector.complete("outcome", true);
        assert collector.pollCompleted().orElseThrow().observations().getFirst().selectedEdgeId().equals("edge-one");
        assert collector.pollCompleted().orElseThrow().observations().getFirst().selectedEdgeId().equals("edge-two");
    }

    private static void boundsConcurrentUniqueDiagnosticsStrictly() throws Exception {
        int capacity = 17;
        int publishers = 128;
        RuntimeCollector collector = new RuntimeCollector(java.time.Clock.systemUTC(), capacity);
        collector.register(graph(), new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(32)) {
            for (int index = 0; index < publishers; index++) {
                int item = index;
                executor.submit(() -> {
                    start.await();
                    collector.begin("graph", 1);
                    collector.dispatch("missing-" + item, new UnknownRule());
                    collector.complete("outcome", true);
                    return null;
                });
            }
            start.countDown();
            executor.shutdown();
            assert executor.awaitTermination(10, TimeUnit.SECONDS);
        }
        assert collector.retainedDiagnosticCount() == capacity : collector.retainedDiagnosticCount();
        assert collector.diagnosticOverflowCount() == publishers - capacity
                : collector.diagnosticOverflowCount();
        int retained = 0;
        while (collector.pollDiagnostic().isPresent()) retained++;
        assert retained == capacity : retained;
    }

    private static void propagatesAndClearsExplicitAsyncContext() throws Exception {
        RuntimeCollector collector = collector();
        collector.begin("graph", 1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(collector.wrap(() -> collector.observe("predicate", "async", 42))).get();
            executor.submit(() -> collector.observe("predicate", "leaked", 99)).get();
            TraceRuntime.configure(collector);
            java.util.concurrent.CompletableFuture.completedFuture("stage")
                    .thenApplyAsync(TraceRuntime.wrapFunction(value -> {
                        collector.observe("predicate", "stage", value);
                        return value;
                    }), executor).get();
        }
        collector.unsupportedAsyncBoundary("reactive-publisher");
        collector.complete("outcome", true);
        var execution = collector.pollCompleted().orElseThrow();
        assert execution.observations().stream().anyMatch(item -> item.outcome().equals("async"));
        assert execution.observations().stream().anyMatch(item -> item.outcome().equals("stage"));
        assert execution.observations().stream().noneMatch(item -> item.outcome().equals("leaked"));
        assert execution.completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE;
        assert execution.coverageGaps().contains("execution crossed an unsupported asynchronous boundary");
        assert collector.pollDiagnostic().orElseThrow().reason()
                == RuntimeCollector.DiagnosticReason.UNSUPPORTED_ASYNC_BOUNDARY;
    }

    private static void implementationEdgesRequireTheExpectedDispatch() {
        var nodes = List.of(
                new BusinessDecisionGraph.DecisionNode("entry", BusinessDecisionGraph.NodeKind.ENTRY,
                        "begin decision", Map.of()),
                new BusinessDecisionGraph.DecisionNode("dispatch", BusinessDecisionGraph.NodeKind.DISPATCH,
                        "select applicable decision rule", Map.of()),
                new BusinessDecisionGraph.DecisionNode("outcome", BusinessDecisionGraph.NodeKind.OUTCOME,
                        "final decision", Map.of()));
        var edges = List.of(new BusinessDecisionGraph.DecisionEdge(
                "selected", "dispatch", "outcome", "candidate 1"));
        var graph = new BusinessDecisionGraph("expected-dispatch", 1, "approval", "entry", nodes, edges,
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
        RuntimeCollector collector = new RuntimeCollector();
        collector.register(graph, new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        collector.begin(graph.graphId(), graph.version());
        collector.selectedEdge("dispatch", "selected");
        collector.expectDispatch("dispatch");
        collector.selectedEdge("other", "selected");
        collector.selectedEdge("dispatch", "selected");
        collector.complete("outcome", true);
        var execution = collector.pollCompleted().orElseThrow();
        assert execution.observations().stream().filter(item -> item.selectedEdgeId() != null).count() == 1
                : execution.observations();
    }

    private static void matchesNestedDispatchExpectationsInStackOrder() {
        RuntimeCollector collector = collector();
        collector.begin("graph", 1);
        collector.expectDispatch("dispatch");

        collector.begin("graph", 1);
        collector.expectDispatch("dispatch");
        collector.expectDispatch("nested-dispatch");
        collector.selectedEdge("dispatch", "edge-dispatch");
        collector.selectedEdge("nested-dispatch", "edge-nested");
        collector.selectedEdge("dispatch", "edge-dispatch");
        collector.complete("outcome", true);

        collector.selectedEdge("dispatch", "edge-dispatch");
        collector.complete("outcome", true);

        var child = collector.pollCompleted().orElseThrow();
        var parent = collector.pollCompleted().orElseThrow();
        var childEdges = child.observations().stream()
                .filter(observation -> observation.selectedEdgeId() != null)
                .map(DecisionExecution.NodeObservation::selectedEdgeId)
                .toList();
        assert childEdges.equals(List.of("edge-nested", "edge-dispatch")) : childEdges;
        assert parent.observations().stream()
                .filter(observation -> observation.selectedEdgeId() != null)
                .map(DecisionExecution.NodeObservation::selectedEdgeId)
                .toList().equals(List.of("edge-dispatch")) : parent.observations();
    }

    private static void isolatesThirtyTwoConcurrentInvocations() throws Exception {
        RuntimeCollector collector = collector();
        int invocations = 32;
        var ready = new CountDownLatch(invocations);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(invocations)) {
            for (int index = 0; index < invocations; index++) {
                String marker = "invocation-" + index;
                executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    collector.begin("graph", 1);
                    collector.observe("predicate", "evaluated", marker);
                    collector.complete("outcome", true);
                    return null;
                });
            }
            assert ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            executor.shutdown();
            assert executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        int records = 0;
        var markers = new java.util.HashSet<String>();
        var executionIds = new java.util.HashSet<String>();
        DecisionExecution execution;
        while ((execution = collector.pollCompleted().orElse(null)) != null) {
            records++;
            executionIds.add(execution.executionId());
            assert execution.observations().size() == 2 : execution.observations();
            markers.add(execution.observations().getFirst().evidence().get("value").canonicalValue());
        }
        assert records == invocations : records;
        assert markers.size() == invocations : markers;
        assert executionIds.size() == invocations : executionIds;
        assert collector.completedCount() == 0;
    }

    private static void tracingFailuresDoNotEscape() {
        RuntimeCollector exploding = new RuntimeCollector() {
            @Override public void begin(String graphId, long graphVersion) { throw new Error("capture failed"); }
            @Override public void complete(String nodeId, Object result) { throw new Error("capture failed"); }
            @Override public void fail(Throwable failure) { throw new Error("capture failed"); }
        };
        TraceRuntime.configure(exploding);
        TraceRuntime.begin("graph", 1);
        int applicationResult = 42;
        TraceRuntime.complete("outcome", applicationResult);
        assert applicationResult == 42;
        var original = new IllegalStateException("application failure");
        TraceRuntime.fail(original);
        assert original.getMessage().equals("application failure");
    }

    private static RuntimeCollector collector() {
        var collector = new RuntimeCollector();
        collector.register(graph(), new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        return collector;
    }

    private static BusinessDecisionGraph graph() {
        var nodes = List.of(
                new BusinessDecisionGraph.DecisionNode("entry", BusinessDecisionGraph.NodeKind.ENTRY,
                        "begin decision", Map.of()),
                new BusinessDecisionGraph.DecisionNode("predicate", BusinessDecisionGraph.NodeKind.PREDICATE,
                        "customer qualifies", Map.of()),
                new BusinessDecisionGraph.DecisionNode("dispatch", BusinessDecisionGraph.NodeKind.DISPATCH,
                        "select applicable decision rule", Map.of()),
                new BusinessDecisionGraph.DecisionNode("nested-dispatch", BusinessDecisionGraph.NodeKind.DISPATCH,
                        "select nested decision rule", Map.of()),
                new BusinessDecisionGraph.DecisionNode("outcome", BusinessDecisionGraph.NodeKind.OUTCOME,
                        "final decision", Map.of()));
        var edges = List.of(
                new BusinessDecisionGraph.DecisionEdge(
                        "edge-true", "predicate", "outcome", "qualified"),
                new BusinessDecisionGraph.DecisionEdge(
                        "edge-dispatch", "dispatch", "outcome", "candidate selected"),
                new BusinessDecisionGraph.DecisionEdge(
                        "edge-nested", "nested-dispatch", "outcome", "nested candidate selected"));
        return new BusinessDecisionGraph("graph", 1, "eligibility", "entry", nodes, edges,
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
    }

    private static BusinessDecisionGraph graph(String id, long version, String dispatchEdge) {
        var base = graph();
        var edges = base.edges().stream().map(edge -> edge.fromNodeId().equals("dispatch")
                ? new BusinessDecisionGraph.DecisionEdge(dispatchEdge, edge.fromNodeId(), edge.toNodeId(), edge.outcome())
                : edge).toList();
        return new BusinessDecisionGraph(id, version, base.decisionLabel(), base.entryNodeId(),
                base.nodes(), edges, base.completeness(), base.coverageGaps());
    }

    private static final class LocalRule { }
    private static final class RegionalRule { }
    private static class BaseRule { }
    private static final class GeneratedRuleProxy extends BaseRule { }
    private static final class UnknownRule { }
    private interface LeftRule { }
    private interface RightRule { }
    private static final class AmbiguousRule implements LeftRule, RightRule { }
}
