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
    private final ThreadLocal<String> expectedDispatch = new ThreadLocal<>();
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
        definitions.put(graph.graphId(), new Definition(graph, codec));
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

    /** Records which opaque static dispatch edge was selected for the target object. */
    public void dispatch(String nodeId, Object target) {
        InvocationContext context = current();
        if (context == null || target == null) return;
        String edge = dispatchEdges.get(new DispatchKey(nodeId, target.getClass()));
        context.observe(nodeId, "selected", Map.of(), edge);
    }

    /** Marks the dispatch node whose implementation entry is expected next on this thread. */
    public void expectDispatch(String nodeId) {
        if (current() != null) expectedDispatch.set(nodeId);
    }

    /** Records a target edge directly from an instrumented implementation entry. */
    public void selectedEdge(String nodeId, String edgeId) {
        InvocationContext context = current();
        if (context == null) return;
        if (!nodeId.equals(expectedDispatch.get())) return;
        boolean belongsToActiveGraph = context.graph().edges().stream()
                .anyMatch(edge -> edge.edgeId().equals(edgeId) && edge.fromNodeId().equals(nodeId));
        if (belongsToActiveGraph) {
            context.observe(nodeId, "selected", Map.of(), edgeId);
            expectedDispatch.remove();
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
            expectedDispatch.remove();
            if (stack.isEmpty()) contexts.remove();
        }
    }

    /** Abandons the current trace while leaving the application exception untouched. */
    public void fail(Throwable ignored) {
        ArrayDeque<InvocationContext> stack = contexts.get();
        if (!stack.isEmpty()) stack.pop();
        expectedDispatch.remove();
        if (stack.isEmpty()) contexts.remove();
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

    private record Definition(BusinessDecisionGraph graph, DecisionExecution.DecisionValueCodec codec) { }
    private record DispatchKey(String nodeId, Class<?> targetType) { }
}
