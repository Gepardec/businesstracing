package at.gepardec.fachtracing.runtime;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

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
