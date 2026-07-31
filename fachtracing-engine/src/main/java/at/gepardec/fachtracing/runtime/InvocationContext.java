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
    private final ArrayDeque<String> expectedDispatches = new ArrayDeque<>();
    private long sequence;

    InvocationContext(String executionId, BusinessDecisionGraph graph, Instant startedAt) {
        this.executionId = Objects.requireNonNull(executionId, "executionId");
        this.graph = Objects.requireNonNull(graph, "graph");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
    }

    void observe(String nodeId, String outcome, java.util.Map<String, DecisionExecution.DecisionValue> evidence,
                 String selectedEdgeId) {
        observations.add(new DecisionExecution.NodeObservation(
                sequence++, nodeId, Objects.requireNonNullElse(outcome, ""), evidence, selectedEdgeId));
    }

    void observeEdge(BusinessDecisionGraph.DecisionEdge edge) {
        Objects.requireNonNull(edge, "edge");
        observe(edge.fromNodeId(), edge.outcome(), java.util.Map.of(), edge.edgeId());
    }

    DecisionExecution finish(Instant completedAt, DecisionExecution.DecisionValue result) {
        return new DecisionExecution(executionId, graph.graphId(), graph.version(), startedAt, completedAt,
                observations, result, graph.completeness(),
                graph.coverageGaps().stream().map(BusinessDecisionGraph.CoverageGap::description).toList());
    }

    DecisionExecution fail(Instant completedAt) {
        return new DecisionExecution(executionId, graph.graphId(), graph.version(), startedAt, completedAt,
                observations, DecisionExecution.TerminalStatus.FAILED, null,
                DecisionExecution.FailureData.genericFailure(), graph.completeness(),
                graph.coverageGaps().stream().map(BusinessDecisionGraph.CoverageGap::description).toList());
    }

    void expectDispatch(String nodeId) {
        expectedDispatches.push(Objects.requireNonNull(nodeId, "nodeId"));
    }

    boolean matchesExpectedDispatch(String nodeId) {
        return Objects.equals(expectedDispatches.peek(), nodeId);
    }

    void consumeExpectedDispatch() {
        expectedDispatches.pop();
    }

    /** Opaque invocation identifier. */
    public String executionId() { return executionId; }

    /** Immutable view of observations captured so far. */
    public List<DecisionExecution.NodeObservation> observations() { return List.copyOf(observations); }

    BusinessDecisionGraph graph() { return graph; }
}
