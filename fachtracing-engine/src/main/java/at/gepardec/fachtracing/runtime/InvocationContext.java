package at.gepardec.fachtracing.runtime;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Mutable, thread-confined assembly state for one traced invocation. */
public final class InvocationContext {
    private final String executionId;
    private final BusinessDecisionGraph graph;
    private final Instant startedAt;
    private final List<DecisionExecution.NodeObservation> observations = new ArrayList<>();
    private final List<String> runtimeCoverageGaps = new ArrayList<>();
    private final ArrayDeque<String> expectedDispatches = new ArrayDeque<>();
    private long sequence;

    InvocationContext(String executionId, BusinessDecisionGraph graph, Instant startedAt) {
        this.executionId = Objects.requireNonNull(executionId, "executionId");
        this.graph = Objects.requireNonNull(graph, "graph");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
    }

    synchronized void observe(String nodeId, String outcome, java.util.Map<String, DecisionExecution.DecisionValue> evidence,
                 String selectedEdgeId) {
        observations.add(new DecisionExecution.NodeObservation(
                sequence++, nodeId, Objects.requireNonNullElse(outcome, ""), evidence, selectedEdgeId));
    }

    synchronized void observeEdge(BusinessDecisionGraph.DecisionEdge edge) {
        Objects.requireNonNull(edge, "edge");
        observe(edge.fromNodeId(), edge.outcome(), java.util.Map.of(), edge.edgeId());
    }

    synchronized DecisionExecution finish(Instant completedAt, DecisionExecution.DecisionValue result) {
        return new DecisionExecution(executionId, graph.graphId(), graph.version(), startedAt, completedAt,
                observations, result, completeness(), coverageGaps());
    }

    synchronized DecisionExecution fail(Instant completedAt) {
        return new DecisionExecution(executionId, graph.graphId(), graph.version(), startedAt, completedAt,
                observations, DecisionExecution.TerminalStatus.FAILED, null,
                DecisionExecution.FailureData.genericFailure(), completeness(), coverageGaps());
    }

    synchronized void expectDispatch(String nodeId) {
        expectedDispatches.push(Objects.requireNonNull(nodeId, "nodeId"));
    }

    synchronized boolean matchesExpectedDispatch(String nodeId) {
        return Objects.equals(expectedDispatches.peek(), nodeId);
    }

    synchronized void consumeExpectedDispatch() {
        expectedDispatches.pop();
    }

    synchronized void addRuntimeCoverageGap(String description) {
        if (!runtimeCoverageGaps.contains(description)) runtimeCoverageGaps.add(description);
    }

    private BusinessDecisionGraph.Completeness completeness() {
        return runtimeCoverageGaps.isEmpty() ? graph.completeness() : BusinessDecisionGraph.Completeness.INCOMPLETE;
    }

    private List<String> coverageGaps() {
        var gaps = new ArrayList<>(graph.coverageGaps().stream()
                .map(BusinessDecisionGraph.CoverageGap::description).toList());
        gaps.addAll(runtimeCoverageGaps);
        return List.copyOf(gaps);
    }

    /** Opaque invocation identifier. */
    public String executionId() { return executionId; }

    /** Immutable view of observations captured so far. */
    public synchronized List<DecisionExecution.NodeObservation> observations() { return List.copyOf(observations); }

    BusinessDecisionGraph graph() { return graph; }
}
