package at.gepardec.fachtracing.runtime;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Mutable, thread-confined assembly state for one traced invocation. */
public final class InvocationContext {
    private final String executionId;
    private final BusinessDecisionGraph graph;
    private final Instant startedAt;
    private final List<DecisionExecution.NodeObservation> observations = new ArrayList<>();
    private final List<String> runtimeCoverageGaps = new ArrayList<>();
    private final ArrayDeque<String> expectedDispatches = new ArrayDeque<>();
    private final Map<String, LinkedHashMap<String, DecisionExecution.DecisionValue>> pendingEvidence =
            new LinkedHashMap<>();
    private long sequence;
    private int asyncReservations;
    private boolean terminalRequested;
    private boolean failed;
    private boolean published;
    private Instant terminalAt;
    private DecisionExecution.DecisionValue terminalResult;

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

    synchronized void addEvidence(
            String nodeId, String label, DecisionExecution.DecisionValue value) {
        pendingEvidence.computeIfAbsent(nodeId, ignored -> new LinkedHashMap<>()).put(label, value);
    }

    synchronized Map<String, DecisionExecution.DecisionValue> consumeEvidence(String nodeId) {
        Map<String, DecisionExecution.DecisionValue> evidence = pendingEvidence.remove(nodeId);
        return evidence == null ? Map.of() : Map.copyOf(evidence);
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

    synchronized boolean retainAsync() {
        if (published) return false;
        asyncReservations++;
        return true;
    }

    synchronized Optional<DecisionExecution> completeWhenReady(
            Instant completedAt, DecisionExecution.DecisionValue result) {
        terminalRequested = true;
        terminalAt = completedAt;
        terminalResult = result;
        return publishWhenReady();
    }

    synchronized Optional<DecisionExecution> failWhenReady(Instant completedAt) {
        terminalRequested = true;
        failed = true;
        terminalAt = completedAt;
        return publishWhenReady();
    }

    synchronized Optional<DecisionExecution> releaseAsync() {
        if (asyncReservations <= 0) throw new IllegalStateException("asynchronous reservation underflow");
        asyncReservations--;
        return publishWhenReady();
    }

    private Optional<DecisionExecution> publishWhenReady() {
        if (!terminalRequested || asyncReservations != 0 || published) return Optional.empty();
        published = true;
        return Optional.of(failed ? fail(terminalAt) : finish(terminalAt, terminalResult));
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

    synchronized List<String> consumeUnresolvedDispatches() {
        var unresolved = List.copyOf(expectedDispatches);
        expectedDispatches.clear();
        return unresolved;
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
