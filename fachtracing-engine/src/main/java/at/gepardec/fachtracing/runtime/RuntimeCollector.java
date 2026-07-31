package at.gepardec.fachtracing.runtime;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory, non-blocking collector used by injected probes on application threads. */
public class RuntimeCollector {
    private final Clock clock;
    private final ThreadLocal<ArrayDeque<InvocationContext>> contexts =
            ThreadLocal.withInitial(ArrayDeque::new);
    private final Map<String, Definition> definitions = new ConcurrentHashMap<>();
    private final Map<DispatchKey, String> dispatchEdges = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<DecisionExecution> completed = new ConcurrentLinkedQueue<>();
    private final AtomicLong executionSequence = new AtomicLong();

    /** Creates a collector using the system clock. */
    public RuntimeCollector() { this(Clock.systemUTC()); }

    /** Creates a collector with an explicit clock for deterministic tests. */
    public RuntimeCollector(Clock clock) { this.clock = Objects.requireNonNull(clock, "clock"); }

    /** Registers static graph metadata and the value boundary used by future invocations. */
    public void register(BusinessDecisionGraph graph, DecisionExecution.DecisionValueCodec codec) {
        Map<EdgeKey, BusinessDecisionGraph.DecisionEdge> edges = graph.edges().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        edge -> new EdgeKey(edge.fromNodeId(), edge.edgeId()), edge -> edge,
                        (first, ignored) -> first));
        definitions.put(graph.graphId(), new Definition(graph, codec, edges));
    }

    /** Maps a technical target type to an opaque candidate edge outside business records. */
    public void registerDispatch(String dispatchNodeId, Class<?> targetType, String edgeId) {
        dispatchEdges.put(new DispatchKey(dispatchNodeId, targetType), edgeId);
    }

    /** Starts a nested-safe context for a registered graph. */
    public void begin(String graphId, long graphVersion) {
        Definition definition = definitions.get(graphId);
        if (definition == null || definition.graph().version() != graphVersion) return;
        String executionId = Long.toUnsignedString(executionSequence.incrementAndGet(), 36);
        contexts.get().push(new InvocationContext(executionId, definition.graph(), clock.instant()));
    }

    /** Appends one ordered observation without performing external I/O. */
    public void observe(String nodeId, String outcome, Object value) {
        InvocationContext context = current();
        if (context == null) return;
        Map<String, DecisionExecution.DecisionValue> evidence = value == null
                ? Map.of()
                : Map.of("value", definition(context).codec().encode(value,
                        definition(context).graph().decisionLabel(), "value"));
        context.observe(nodeId, outcome, evidence, null);
    }

    /** Appends one exact edge when it leaves the named node in the active graph. */
    public void edge(String nodeId, String edgeId) {
        InvocationContext context = current();
        if (context == null) return;
        BusinessDecisionGraph.DecisionEdge edge = definition(context).edges().get(new EdgeKey(nodeId, edgeId));
        if (edge != null) context.observeEdge(edge);
    }

    /** Records which opaque static dispatch edge was selected for the target object. */
    public void dispatch(String nodeId, Object target) {
        InvocationContext context = current();
        if (context == null || target == null) return;
        String edge = dispatchEdges.get(new DispatchKey(nodeId, target.getClass()));
        context.observe(nodeId, "selected", Map.of(), edge);
    }

    /** Marks the dispatch node whose implementation entry is expected next on this thread. */
    public void expectDispatch(String nodeId) {
        InvocationContext context = current();
        if (context != null) context.expectDispatch(nodeId);
    }

    /** Records a target edge directly from an instrumented implementation entry. */
    public void selectedEdge(String nodeId, String edgeId) {
        InvocationContext context = current();
        if (context == null) return;
        if (!context.matchesExpectedDispatch(nodeId)) return;
        boolean belongsToActiveGraph = definition(context).edges().containsKey(new EdgeKey(nodeId, edgeId));
        if (belongsToActiveGraph) {
            context.observe(nodeId, "selected", Map.of(), edgeId);
            context.consumeExpectedDispatch();
        }
    }

    /** Completes and queues the current record for asynchronous persistence by a consumer. */
    public void complete(String nodeId, Object result) {
        ArrayDeque<InvocationContext> stack = contexts.get();
        InvocationContext context = stack.peek();
        if (context == null) return;
        try {
            Definition definition = definition(context);
            var encoded = definition.codec().encode(result, definition.graph().decisionLabel(), "final decision");
            context.observe(nodeId, "result", Map.of("result", encoded), null);
            completed.add(context.finish(clock.instant(), encoded));
        } finally {
            stack.pop();
            if (stack.isEmpty()) contexts.remove();
        }
    }

    /** Completes one generic failed execution without storing exception details. */
    public void fail(Throwable ignored) {
        ArrayDeque<InvocationContext> stack = contexts.get();
        InvocationContext context = stack.peek();
        if (context == null) return;
        try {
            completed.add(context.fail(clock.instant()));
        } finally {
            stack.pop();
            if (stack.isEmpty()) contexts.remove();
        }
    }

    /** Returns and removes the next completed in-memory record. */
    public Optional<DecisionExecution> pollCompleted() { return Optional.ofNullable(completed.poll()); }

    /** Number of records waiting for an asynchronous consumer. */
    public int completedCount() { return completed.size(); }

    private InvocationContext current() {
        ArrayDeque<InvocationContext> stack = contexts.get();
        return stack.peek();
    }

    private Definition definition(InvocationContext context) {
        return Objects.requireNonNull(definitions.get(context.graph().graphId()), "active graph definition");
    }

    private record Definition(
            BusinessDecisionGraph graph,
            DecisionExecution.DecisionValueCodec codec,
            Map<EdgeKey, BusinessDecisionGraph.DecisionEdge> edges) { }
    private record EdgeKey(String nodeId, String edgeId) { }
    private record DispatchKey(String nodeId, Class<?> targetType) { }
}
