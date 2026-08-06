package at.gepardec.fachtracing.runtime;

import at.gepardec.fachtracing.api.TraceContextCarrier;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** In-memory, non-blocking collector used by injected probes on application threads. */
public class RuntimeCollector implements TraceContextCarrier {
    private static final String ANY_GRAPH = "*";
    private final Clock clock;
    private final ThreadLocal<ArrayDeque<InvocationContext>> contexts =
            ThreadLocal.withInitial(ArrayDeque::new);
    private final Map<DefinitionKey, Definition> definitions = new ConcurrentHashMap<>();
    private final Map<DispatchKey, String> dispatchEdges = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<DecisionExecution> completed = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<RuntimeDiagnostic> diagnostics = new ConcurrentLinkedQueue<>();
    private final Set<DiagnosticKey> diagnosticKeys = ConcurrentHashMap.newKeySet();
    private final int diagnosticCapacity;
    private final AtomicLong diagnosticOverflow = new AtomicLong();
    private final AtomicLong executionSequence = new AtomicLong();
    private final String executionNamespace;
    private final IdentityHashMap<Future<?>, AsyncReservation> trackedFutures = new IdentityHashMap<>();

    /** Creates a collector using the system clock. */
    public RuntimeCollector() { this(Clock.systemUTC(), 1024); }

    /** Creates a collector with an explicit clock for deterministic tests. */
    public RuntimeCollector(Clock clock) { this(clock, 1024); }

    /** Creates a collector with an explicit bounded developer diagnostic capacity. */
    public RuntimeCollector(Clock clock, int diagnosticCapacity) {
        this.clock = Objects.requireNonNull(clock, "clock");
        if (diagnosticCapacity < 1) throw new IllegalArgumentException("diagnosticCapacity must be positive");
        this.diagnosticCapacity = diagnosticCapacity;
        this.executionNamespace = UUID.randomUUID().toString();
    }

    /** Registers static graph metadata and the value boundary used by future invocations. */
    public void register(BusinessDecisionGraph graph, DecisionExecution.DecisionValueCodec codec) {
        register(graph, codec, "");
    }

    /** Registers a graph version and its optional instrumented-class fingerprint. */
    public void register(
            BusinessDecisionGraph graph,
            DecisionExecution.DecisionValueCodec codec,
            String classFingerprint) {
        Map<EdgeKey, BusinessDecisionGraph.DecisionEdge> edges = graph.edges().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        edge -> new EdgeKey(edge.fromNodeId(), edge.edgeId()), edge -> edge,
                        (first, ignored) -> first));
        definitions.put(new DefinitionKey(graph.graphId(), graph.version()),
                new Definition(graph, codec, edges, Objects.requireNonNull(classFingerprint, "classFingerprint")));
    }

    /** Maps a technical target type to an opaque candidate edge outside business records. */
    public void registerDispatch(String dispatchNodeId, Class<?> targetType, String edgeId) {
        dispatchEdges.put(new DispatchKey(ANY_GRAPH, -1, dispatchNodeId, targetType), edgeId);
    }

    /** Maps a target type for one graph version to an opaque static candidate edge. */
    public void registerDispatch(
            String graphId, long graphVersion, String dispatchNodeId, Class<?> targetType, String edgeId) {
        dispatchEdges.put(new DispatchKey(graphId, graphVersion, dispatchNodeId, targetType), edgeId);
    }

    /** Starts a nested-safe context for a registered graph. */
    public void begin(String graphId, long graphVersion) {
        Definition definition = definitions.get(new DefinitionKey(graphId, graphVersion));
        if (definition == null) return;
        String executionId = executionNamespace + "-"
                + Long.toUnsignedString(executionSequence.incrementAndGet(), 36);
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

    /** Stages one typed business fact for the next exact observation of its predicate. */
    public void observeEvidence(String nodeId, String evidenceLabel, Object value) {
        InvocationContext context = current();
        if (context == null) return;
        Definition definition = definition(context);
        if (value == null) {
            context.addRuntimeCoverageGap("required predicate evidence '" + evidenceLabel
                    + "' was absent and could not be encoded");
            return;
        }
        try {
            context.addEvidence(nodeId, evidenceLabel, definition.codec().encode(
                    value, definition.graph().decisionLabel(), evidenceLabel));
        } catch (RuntimeException unsupported) {
            context.addRuntimeCoverageGap("required predicate evidence '" + evidenceLabel
                    + "' has no safe value adapter");
        }
    }

    /** Appends one exact edge when it leaves the named node in the active graph. */
    public void edge(String nodeId, String edgeId) {
        InvocationContext context = current();
        if (context == null) return;
        BusinessDecisionGraph.DecisionEdge edge = definition(context).edges().get(new EdgeKey(nodeId, edgeId));
        if (edge != null) context.observeEdge(edge);
    }

    /** Appends one exact Boolean edge with typed evidence for its atomic predicate. */
    public void predicate(String nodeId, String edgeId, boolean value) {
        InvocationContext context = current();
        if (context == null) return;
        Definition definition = definition(context);
        BusinessDecisionGraph.DecisionEdge edge = definition.edges().get(new EdgeKey(nodeId, edgeId));
        if (edge == null) return;
        Map<String, DecisionExecution.DecisionValue> evidence = context.consumeEvidence(nodeId);
        if (evidence.isEmpty()) {
            evidence = Map.of("value", definition.codec().encode(
                    value, definition.graph().decisionLabel(), "value"));
        }
        context.observe(nodeId, value ? "true" : "false", evidence, edgeId);
    }

    /** Records which opaque static dispatch edge was selected for the target object. */
    public void dispatch(String nodeId, Object target) {
        InvocationContext context = current();
        if (context == null || target == null) return;
        Class<?> runtimeType = target.getClass();
        String edge = resolveDispatch(context, nodeId, runtimeType);
        if (edge != null) {
            context.observe(nodeId, "selected", Map.of(), edge);
        }
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
            markUnresolvedDispatches(context);
            Definition definition = definition(context);
            var encoded = definition.codec().encode(result, definition.graph().decisionLabel(), "final decision");
            context.observe(nodeId, "result", Map.of("result", encoded), null);
            context.completeWhenReady(clock.instant(), encoded).ifPresent(completed::add);
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
            markUnresolvedDispatches(context);
            context.failWhenReady(clock.instant()).ifPresent(completed::add);
        } finally {
            stack.pop();
            if (stack.isEmpty()) contexts.remove();
        }
    }

    private void markUnresolvedDispatches(InvocationContext context) {
        for (String dispatchNodeId : context.consumeUnresolvedDispatches()) {
            context.addRuntimeCoverageGap("runtime decision implementation did not match a proven candidate");
            publish(context, dispatchNodeId, "", DiagnosticReason.UNKNOWN_TARGET);
        }
    }

    /** Returns and removes the next completed in-memory record. */
    public Optional<DecisionExecution> pollCompleted() { return Optional.ofNullable(completed.poll()); }

    /** Number of records waiting for an asynchronous consumer. */
    public int completedCount() { return completed.size(); }

    /** Returns and removes the next developer-only runtime diagnostic. */
    public Optional<RuntimeDiagnostic> pollDiagnostic() {
        return Optional.ofNullable(diagnostics.poll());
    }

    /** Number of unique diagnostics rejected after the fixed capacity was reached. */
    public long diagnosticOverflowCount() { return diagnosticOverflow.get(); }

    /** Number of retained unique diagnostic keys. */
    public int retainedDiagnosticCount() {
        synchronized (diagnosticKeys) { return diagnosticKeys.size(); }
    }

    /** Reports whether this thread currently executes the named graph version. */
    public boolean isActive(String graphId, long graphVersion) {
        InvocationContext context = current();
        return context != null && context.graph().graphId().equals(graphId)
                && context.graph().version() == graphVersion;
    }

    /** Marks an unsupported asynchronous boundary without joining later unrelated work. */
    public void unsupportedAsyncBoundary(String boundaryKind) {
        InvocationContext context = current();
        if (context == null) return;
        context.addRuntimeCoverageGap("execution crossed an unsupported asynchronous boundary");
        publish(context, "", Objects.requireNonNullElse(boundaryKind, ""),
                DiagnosticReason.UNSUPPORTED_ASYNC_BOUNDARY);
    }

    /** Marks one runtime path whose exact static-to-bytecode correlation is unavailable. */
    public void exactPathUnavailable(String description) {
        InvocationContext context = current();
        if (context == null) return;
        String detail = Objects.requireNonNullElse(description, "exact path correlation is unavailable");
        context.addRuntimeCoverageGap(detail);
        publish(context, "", detail, DiagnosticReason.EXACT_PATH_UNAVAILABLE);
    }

    @Override public ContextToken captureContext() {
        return new CapturedContext(current());
    }

    @Override public ContextScope restoreContext(TraceContextCarrier.ContextToken token) {
        if (!(token instanceof CapturedContext captured)) {
            throw new IllegalArgumentException("context token was created by another carrier");
        }
        if (captured.context() == null) return () -> { };
        ArrayDeque<InvocationContext> stack = contexts.get();
        stack.push(captured.context());
        return () -> {
            ArrayDeque<InvocationContext> active = contexts.get();
            if (active.peek() != captured.context()) {
                publish(captured.context(), "", "", DiagnosticReason.CONTEXT_SCOPE_MISMATCH);
                return;
            }
            active.pop();
            if (active.isEmpty()) contexts.remove();
        };
    }

    /** Captures the current context and restores it only while the task runs. */
    public Runnable wrap(Runnable task) {
        Objects.requireNonNull(task, "task");
        InvocationContext context = current();
        if (context == null || task instanceof ContextWrapped || !context.retainAsync()) return task;
        return new WrappedRunnable(this, new AsyncReservation(this, context), task);
    }

    /** Captures the current context and restores it only while the task runs. */
    public <T> Callable<T> wrap(Callable<T> task) {
        Objects.requireNonNull(task, "task");
        InvocationContext context = current();
        if (context == null || task instanceof ContextWrapped || !context.retainAsync()) return task;
        return new WrappedCallable<>(this, new AsyncReservation(this, context), task);
    }

    /** Captures context for one function callback. */
    public <T, R> Function<T, R> wrap(Function<T, R> task) {
        Objects.requireNonNull(task, "task");
        InvocationContext context = current();
        if (context == null || task instanceof ContextWrapped || !context.retainAsync()) return task;
        return new WrappedFunction<>(this, new AsyncReservation(this, context), task);
    }

    /** Captures context for one consumer callback. */
    public <T> Consumer<T> wrap(Consumer<T> task) {
        Objects.requireNonNull(task, "task");
        InvocationContext context = current();
        if (context == null || task instanceof ContextWrapped || !context.retainAsync()) return task;
        return new WrappedConsumer<>(this, new AsyncReservation(this, context), task);
    }

    /** Captures context for one two-argument function callback. */
    public <T, U, R> BiFunction<T, U, R> wrap(BiFunction<T, U, R> task) {
        Objects.requireNonNull(task, "task");
        InvocationContext context = current();
        if (context == null || task instanceof ContextWrapped || !context.retainAsync()) return task;
        return new WrappedBiFunction<>(this, new AsyncReservation(this, context), task);
    }

    /** Captures context for one two-argument consumer callback. */
    public <T, U> BiConsumer<T, U> wrap(BiConsumer<T, U> task) {
        Objects.requireNonNull(task, "task");
        InvocationContext context = current();
        if (context == null || task instanceof ContextWrapped || !context.retainAsync()) return task;
        return new WrappedBiConsumer<>(this, new AsyncReservation(this, context), task);
    }

    /** Captures context for one supplier callback. */
    public <T> Supplier<T> wrap(Supplier<T> task) {
        Objects.requireNonNull(task, "task");
        InvocationContext context = current();
        if (context == null || task instanceof ContextWrapped || !context.retainAsync()) return task;
        return new WrappedSupplier<>(this, new AsyncReservation(this, context), task);
    }

    private void releaseAsync(InvocationContext context) {
        context.releaseAsync().ifPresent(completed::add);
    }

    private void runAsync(AsyncReservation reservation, Runnable task) {
        if (!reservation.start()) {
            task.run();
            return;
        }
        try (ContextScope ignored = restoreContext(new CapturedContext(reservation.context()))) {
            task.run();
        } finally {
            reservation.finish();
        }
    }

    private <T> T callAsync(AsyncReservation reservation, Callable<T> task) throws Exception {
        if (!reservation.start()) return task.call();
        try (ContextScope ignored = restoreContext(new CapturedContext(reservation.context()))) {
            return task.call();
        } finally {
            reservation.finish();
        }
    }

    /** Releases a prepared callback that was rejected or cancelled before it ran. */
    public void cancelPreparedAsync(Object callback) {
        if (callback instanceof ContextWrapped wrapped) wrapped.reservation().cancelBeforeStart();
    }

    /** Reports whether a callback owns an automatic async reservation. */
    public boolean isPreparedAsync(Object callback) { return callback instanceof ContextWrapped; }

    /** Reports whether a callback reservation belongs to the named active graph. */
    public boolean preparedAsyncBelongsTo(Object callback, String graphId, long graphVersion) {
        if (!(callback instanceof ContextWrapped wrapped)) return false;
        BusinessDecisionGraph graph = wrapped.reservation().context().graph();
        return graph.graphId().equals(graphId) && graph.version() == graphVersion;
    }

    /** Registers the original future for successful pre-start cancellation. */
    public void trackFuture(Future<?> future, Object callback) {
        Objects.requireNonNull(future, "future");
        if (callback instanceof ContextWrapped wrapped) {
            AsyncReservation reservation = wrapped.reservation();
            synchronized (trackedFutures) {
                if (reservation.attachFuture(future)) trackedFutures.put(future, reservation);
            }
        }
    }

    /** Releases the reservation for an original future that was cancelled before start. */
    public void cancelTrackedFuture(Future<?> future) {
        AsyncReservation reservation;
        synchronized (trackedFutures) { reservation = trackedFutures.remove(future); }
        if (reservation != null) reservation.cancelBeforeStart();
    }

    private void unregisterFuture(Future<?> future, AsyncReservation reservation) {
        synchronized (trackedFutures) {
            if (trackedFutures.get(future) == reservation) trackedFutures.remove(future);
        }
    }

    private String resolveDispatch(InvocationContext context, String nodeId, Class<?> runtimeType) {
        String exact = dispatchEdges.get(new DispatchKey(
                context.graph().graphId(), context.graph().version(), nodeId, runtimeType));
        if (exact == null) exact = dispatchEdges.get(new DispatchKey(ANY_GRAPH, -1, nodeId, runtimeType));
        if (exact != null) return exact;
        List<Map.Entry<DispatchKey, String>> assignable = dispatchEdges.entrySet().stream()
                .filter(entry -> entry.getKey().matches(context.graph(), nodeId))
                .filter(entry -> entry.getKey().targetType().isAssignableFrom(runtimeType)).toList();
        List<Map.Entry<DispatchKey, String>> mostSpecific = assignable.stream()
                .filter(candidate -> assignable.stream().noneMatch(other -> candidate != other
                        && candidate.getKey().targetType().isAssignableFrom(other.getKey().targetType())
                        && !candidate.getKey().targetType().equals(other.getKey().targetType())))
                .toList();
        if (mostSpecific.size() == 1) return mostSpecific.getFirst().getValue();
        publish(context, nodeId, runtimeType.getName(), mostSpecific.isEmpty()
                ? DiagnosticReason.UNKNOWN_TARGET : DiagnosticReason.AMBIGUOUS_TARGET);
        return null;
    }

    private void publish(
            InvocationContext context, String nodeId, String runtimeTarget, DiagnosticReason reason) {
        var key = new DiagnosticKey(context.graph().graphId(), context.graph().version(),
                nodeId, runtimeTarget, reason);
        synchronized (diagnosticKeys) {
            if (diagnosticKeys.contains(key)) return;
            if (diagnosticKeys.size() >= diagnosticCapacity) {
                diagnosticOverflow.incrementAndGet();
                return;
            }
            diagnosticKeys.add(key);
            diagnostics.add(new RuntimeDiagnostic(
                    key.graphId(), key.graphVersion(), nodeId, runtimeTarget, reason));
        }
    }

    private InvocationContext current() {
        ArrayDeque<InvocationContext> stack = contexts.get();
        return stack.peek();
    }

    private Definition definition(InvocationContext context) {
        return Objects.requireNonNull(definitions.get(new DefinitionKey(
                context.graph().graphId(), context.graph().version())), "active graph definition");
    }

    private interface ContextWrapped {
        AsyncReservation reservation();
    }

    private record WrappedRunnable(
            RuntimeCollector collector, AsyncReservation reservation, Runnable delegate)
            implements Runnable, ContextWrapped {
        @Override public void run() { collector.runAsync(reservation, delegate); }
    }

    private record WrappedCallable<T>(
            RuntimeCollector collector, AsyncReservation reservation, Callable<T> delegate)
            implements Callable<T>, ContextWrapped {
        @Override public T call() throws Exception { return collector.callAsync(reservation, delegate); }
    }

    private record WrappedFunction<T, R>(
            RuntimeCollector collector, AsyncReservation reservation, Function<T, R> delegate)
            implements Function<T, R>, ContextWrapped {
        @Override public R apply(T value) {
            try {
                return collector.callAsync(reservation, () -> delegate.apply(value));
            } catch (RuntimeException | Error failure) {
                throw failure;
            } catch (Exception impossible) {
                throw new IllegalStateException(impossible);
            }
        }
    }

    private record WrappedConsumer<T>(
            RuntimeCollector collector, AsyncReservation reservation, Consumer<T> delegate)
            implements Consumer<T>, ContextWrapped {
        @Override public void accept(T value) { collector.runAsync(reservation, () -> delegate.accept(value)); }
    }

    private record WrappedBiFunction<T, U, R>(
            RuntimeCollector collector, AsyncReservation reservation, BiFunction<T, U, R> delegate)
            implements BiFunction<T, U, R>, ContextWrapped {
        @Override public R apply(T first, U second) {
            try {
                return collector.callAsync(reservation, () -> delegate.apply(first, second));
            } catch (RuntimeException | Error failure) {
                throw failure;
            } catch (Exception impossible) {
                throw new IllegalStateException(impossible);
            }
        }
    }

    private record WrappedBiConsumer<T, U>(
            RuntimeCollector collector, AsyncReservation reservation, BiConsumer<T, U> delegate)
            implements BiConsumer<T, U>, ContextWrapped {
        @Override public void accept(T first, U second) {
            collector.runAsync(reservation, () -> delegate.accept(first, second));
        }
    }

    private record WrappedSupplier<T>(
            RuntimeCollector collector, AsyncReservation reservation, Supplier<T> delegate)
            implements Supplier<T>, ContextWrapped {
        @Override public T get() {
            try {
                return collector.callAsync(reservation, delegate::get);
            } catch (RuntimeException | Error failure) {
                throw failure;
            } catch (Exception impossible) {
                throw new IllegalStateException(impossible);
            }
        }
    }

    private static final class AsyncReservation {
        private final RuntimeCollector collector;
        private final InvocationContext context;
        private final AtomicReference<AsyncState> state = new AtomicReference<>(AsyncState.RESERVED);
        private volatile Future<?> future;

        private AsyncReservation(RuntimeCollector collector, InvocationContext context) {
            this.collector = collector;
            this.context = context;
        }

        private InvocationContext context() { return context; }

        private boolean start() {
            boolean started = state.compareAndSet(AsyncState.RESERVED, AsyncState.RUNNING);
            if (started) detachFuture();
            return started;
        }

        private boolean attachFuture(Future<?> value) {
            if (state.get() != AsyncState.RESERVED) return false;
            future = value;
            if (state.get() == AsyncState.RESERVED) return true;
            future = null;
            return false;
        }

        private void detachFuture() {
            Future<?> attached = future;
            future = null;
            if (attached != null) collector.unregisterFuture(attached, this);
        }

        private void finish() {
            if (state.compareAndSet(AsyncState.RUNNING, AsyncState.RELEASED)) collector.releaseAsync(context);
        }

        private void cancelBeforeStart() {
            if (state.compareAndSet(AsyncState.RESERVED, AsyncState.RELEASED)) {
                detachFuture();
                collector.releaseAsync(context);
            }
        }
    }

    private enum AsyncState { RESERVED, RUNNING, RELEASED }

    private record Definition(
            BusinessDecisionGraph graph,
            DecisionExecution.DecisionValueCodec codec,
            Map<EdgeKey, BusinessDecisionGraph.DecisionEdge> edges,
            String classFingerprint) { }
    private record DefinitionKey(String graphId, long graphVersion) { }
    private record EdgeKey(String nodeId, String edgeId) { }
    private record DispatchKey(String graphId, long graphVersion, String nodeId, Class<?> targetType) {
        private boolean matches(BusinessDecisionGraph graph, String node) {
            return nodeId.equals(node) && (graphId.equals(ANY_GRAPH)
                    || graphId.equals(graph.graphId()) && graphVersion == graph.version());
        }
    }
    private record CapturedContext(InvocationContext context) implements ContextToken { }
    private record DiagnosticKey(
            String graphId, long graphVersion, String nodeId, String runtimeTarget,
            DiagnosticReason reason) { }

    /** Stable reason codes for developer-only runtime mismatch evidence. */
    public enum DiagnosticReason {
        UNKNOWN_TARGET, AMBIGUOUS_TARGET, CONTEXT_SCOPE_MISMATCH, UNSUPPORTED_ASYNC_BOUNDARY,
        EXACT_PATH_UNAVAILABLE
    }

    /** Technical runtime evidence that never enters business records or diagrams. */
    public record RuntimeDiagnostic(
            String graphId,
            long graphVersion,
            String dispatchNodeId,
            String runtimeTarget,
            DiagnosticReason reason) { }
}
