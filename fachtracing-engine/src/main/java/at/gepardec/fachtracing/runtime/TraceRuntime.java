package at.gepardec.fachtracing.runtime;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Non-throwing static bridge called from injected bytecode. */
public final class TraceRuntime {
    private static volatile RuntimeCollector collector = new RuntimeCollector();
    private static final ConcurrentLinkedQueue<CaptureDiagnostic> diagnostics = new ConcurrentLinkedQueue<>();

    private TraceRuntime() { }

    /** Installs the process collector. */
    public static void configure(RuntimeCollector replacement) {
        collector = Objects.requireNonNull(replacement, "replacement");
    }

    public static void begin(String graphId, long graphVersion) {
        safely("begin", () -> collector.begin(graphId, graphVersion));
    }

    public static void observe(String nodeId, String outcome, Object value) {
        safely("observe", () -> collector.observe(nodeId, outcome, value));
    }

    /** Records evidence only when the named graph is active on this thread. */
    public static void observeFor(String graphId, long graphVersion, String nodeId, String outcome, Object value) {
        safely("observe", () -> {
            if (collector.isActive(graphId, graphVersion)) collector.observe(nodeId, outcome, value);
        });
    }

    public static void edge(String nodeId, String edgeId) {
        safely("edge", () -> collector.edge(nodeId, edgeId));
    }

    /** Records an edge only when the named graph is active on this thread. */
    public static void edgeFor(String graphId, long graphVersion, String nodeId, String edgeId) {
        safely("edge", () -> {
            if (collector.isActive(graphId, graphVersion)) collector.edge(nodeId, edgeId);
        });
    }

    /** Records one exact atomic Boolean path with typed evidence for an active graph. */
    public static void predicateFor(
            String graphId, long graphVersion, String nodeId, String edgeId, boolean value) {
        safely("predicate", () -> {
            if (collector.isActive(graphId, graphVersion)) collector.predicate(nodeId, edgeId, value);
        });
    }

    public static void dispatch(String nodeId, Object target) {
        safely("dispatch", () -> collector.dispatch(nodeId, target));
    }

    public static void selectedEdge(String nodeId, String edgeId) {
        safely("selected edge", () -> collector.selectedEdge(nodeId, edgeId));
    }

    /** Records a selected dispatch edge only for its active graph. */
    public static void selectedEdgeFor(String graphId, long graphVersion, String nodeId, String edgeId) {
        safely("selected edge", () -> {
            if (collector.isActive(graphId, graphVersion)) collector.selectedEdge(nodeId, edgeId);
        });
    }

    public static void expectDispatch(String nodeId) {
        safely("expect dispatch", () -> collector.expectDispatch(nodeId));
    }

    /** Expects dispatch only when the named graph is active on this thread. */
    public static void expectDispatchFor(String graphId, long graphVersion, String nodeId) {
        safely("expect dispatch", () -> {
            if (collector.isActive(graphId, graphVersion)) collector.expectDispatch(nodeId);
        });
    }

    public static void complete(String nodeId, Object result) {
        safely("complete", () -> collector.complete(nodeId, result));
    }

    /** Completes only the named active graph. The result-first order simplifies bytecode injection. */
    public static void completeFor(Object result, String graphId, long graphVersion, String nodeId) {
        safely("complete", () -> {
            if (collector.isActive(graphId, graphVersion)) collector.complete(nodeId, result);
        });
    }

    public static void fail(Throwable failure) {
        safely("fail", () -> collector.fail(failure));
    }

    /** Fails only the named active graph. */
    public static void failFor(Throwable failure, String graphId, long graphVersion) {
        safely("fail", () -> {
            if (collector.isActive(graphId, graphVersion)) collector.fail(failure);
        });
    }

    /** Reports an unsupported asynchronous boundary as incomplete runtime evidence. */
    public static void unsupportedAsyncBoundary(String boundaryKind) {
        safely("unsupported async boundary", () -> collector.unsupportedAsyncBoundary(boundaryKind));
    }

    /** Marks an unavailable exact path only for its active graph. */
    public static void exactPathUnavailableFor(String graphId, long graphVersion, String description) {
        safely("exact path unavailable", () -> {
            if (collector.isActive(graphId, graphVersion)) collector.exactPathUnavailable(description);
        });
    }

    /** Captures the current context for one executor task. */
    public static Runnable wrap(Runnable task) { return collector.wrap(task); }

    /** Captures the current context for one executor task. */
    public static <T> Callable<T> wrap(Callable<T> task) { return collector.wrap(task); }

    /** Returns an executor that restores the submitting thread's current context per task. */
    public static Executor contextExecutor(Executor delegate) {
        Objects.requireNonNull(delegate, "delegate");
        return command -> delegate.execute(collector.wrap(command));
    }

    /** Captures context for a completion-stage function. */
    public static <T, R> Function<T, R> wrapFunction(Function<T, R> function) {
        return collector.wrap(function);
    }

    /** Captures context for a completion-stage consumer. */
    public static <T> Consumer<T> wrapConsumer(Consumer<T> consumer) {
        return collector.wrap(consumer);
    }

    /** Captures context for a two-argument completion-stage function. */
    public static <T, U, R> BiFunction<T, U, R> wrapBiFunction(BiFunction<T, U, R> function) {
        return collector.wrap(function);
    }

    /** Captures context for a two-argument completion-stage consumer. */
    public static <T, U> BiConsumer<T, U> wrapBiConsumer(BiConsumer<T, U> consumer) {
        return collector.wrap(consumer);
    }

    /** Captures context for a completion-stage supplier. */
    public static <T> Supplier<T> wrapSupplier(Supplier<T> supplier) {
        return collector.wrap(supplier);
    }

    /** Returns and removes the next developer-facing capture diagnostic. */
    public static Optional<CaptureDiagnostic> pollDiagnostic() {
        return Optional.ofNullable(diagnostics.poll());
    }

    private static void safely(String operation, Probe probe) {
        try {
            probe.run();
        } catch (Throwable failure) {
            diagnostics.add(new CaptureDiagnostic(operation, failure.getClass().getName(),
                    Objects.requireNonNullElse(failure.getMessage(), "")));
        }
    }

    /** Developer-only diagnostic intentionally excluded from business records. */
    public record CaptureDiagnostic(String operation, String failureType, String message) { }

    @FunctionalInterface private interface Probe { void run() throws Throwable; }
}
