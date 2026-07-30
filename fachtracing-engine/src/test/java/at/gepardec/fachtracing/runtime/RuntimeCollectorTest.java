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
        recordsOpaquePolymorphicEdges();
        implementationEdgesRequireTheExpectedDispatch();
        isolatesThirtyTwoConcurrentInvocations();
        tracingFailuresDoNotEscape();
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
        DecisionExecution execution;
        while ((execution = collector.pollCompleted().orElse(null)) != null) {
            records++;
            assert execution.observations().size() == 2 : execution.observations();
            markers.add(execution.observations().getFirst().evidence().get("value").canonicalValue());
        }
        assert records == invocations : records;
        assert markers.size() == invocations : markers;
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
                new BusinessDecisionGraph.DecisionNode("outcome", BusinessDecisionGraph.NodeKind.OUTCOME,
                        "final decision", Map.of()));
        return new BusinessDecisionGraph("graph", 1, "eligibility", "entry", nodes, List.of(),
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
    }

    private static final class LocalRule { }
    private static final class RegionalRule { }
}
