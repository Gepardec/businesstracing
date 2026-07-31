package at.gepardec.fachtracing.runtime;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

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

    public static void edge(String nodeId, String edgeId) {
        safely("edge", () -> collector.edge(nodeId, edgeId));
    }

    public static void dispatch(String nodeId, Object target) {
        safely("dispatch", () -> collector.dispatch(nodeId, target));
    }

    public static void selectedEdge(String nodeId, String edgeId) {
        safely("selected edge", () -> collector.selectedEdge(nodeId, edgeId));
    }

    public static void expectDispatch(String nodeId) {
        safely("expect dispatch", () -> collector.expectDispatch(nodeId));
    }

    public static void complete(String nodeId, Object result) {
        safely("complete", () -> collector.complete(nodeId, result));
    }

    public static void fail(Throwable failure) {
        safely("fail", () -> collector.fail(failure));
    }

    /** Reports an unsupported asynchronous boundary as incomplete runtime evidence. */
    public static void unsupportedAsyncBoundary(String boundaryKind) {
        safely("unsupported async boundary", () -> collector.unsupportedAsyncBoundary(boundaryKind));
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
        Objects.requireNonNull(function, "function");
        var token = collector.captureContext();
        return value -> {
            try (var ignored = collector.restoreContext(token)) { return function.apply(value); }
        };
    }

    /** Captures context for a completion-stage consumer. */
    public static <T> Consumer<T> wrapConsumer(Consumer<T> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        var token = collector.captureContext();
        return value -> {
            try (var ignored = collector.restoreContext(token)) { consumer.accept(value); }
        };
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
