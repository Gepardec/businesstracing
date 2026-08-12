package at.gepardec.fachtracing.agent;

import at.gepardec.fachtracing.explain.BusinessExecutionMermaidRenderer;
import at.gepardec.fachtracing.explain.DecisionExplanationProjector;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.runtime.RuntimeCollector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Writes completed business traces from one daemon thread. */
final class BusinessTraceFileSink implements AutoCloseable {
    private final RuntimeCollector collector;
    private final Map<GraphKey, BusinessDecisionGraph> graphs;
    private final Path outputDirectory;
    private final Consumer<String> diagnosticReporter;
    private final DecisionExplanationProjector projector = new DecisionExplanationProjector();
    private final BusinessExecutionMermaidRenderer mermaid = new BusinessExecutionMermaidRenderer();
    private final AtomicBoolean running = new AtomicBoolean();
    private Thread worker;

    BusinessTraceFileSink(
            RuntimeCollector collector,
            List<BusinessDecisionGraph> graphs,
            Path outputDirectory,
            Consumer<String> diagnosticReporter) {
        this.collector = Objects.requireNonNull(collector, "collector");
        this.graphs = List.copyOf(graphs).stream().collect(Collectors.toUnmodifiableMap(
                graph -> new GraphKey(graph.graphId(), graph.version()), graph -> graph));
        this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory");
        this.diagnosticReporter = Objects.requireNonNull(diagnosticReporter, "diagnosticReporter");
    }

    /** Creates the output directory and starts the daemon consumer. */
    synchronized void start() throws IOException {
        if (!running.compareAndSet(false, true)) throw new IllegalStateException("file sink is already running");
        Files.createDirectories(outputDirectory);
        worker = Thread.ofPlatform().daemon().name("fachtracing-business-output").unstarted(this::drain);
        worker.start();
    }

    private void drain() {
        while (running.get() || collector.completedCount() > 0) {
            DecisionExecution execution = collector.pollCompleted().orElse(null);
            if (execution == null) {
                LockSupport.parkNanos(10_000_000L);
                continue;
            }
            try {
                write(execution);
            } catch (Throwable failure) {
                diagnosticReporter.accept("business output failed for " + execution.executionId()
                        + ": " + failure.getClass().getName() + ": "
                        + Objects.requireNonNullElse(failure.getMessage(), ""));
            }
        }
    }

    private void write(DecisionExecution execution) throws IOException {
        BusinessDecisionGraph graph = graphs.get(new GraphKey(execution.graphId(), execution.graphVersion()));
        if (graph == null) {
            throw new IllegalStateException("completed execution has no activated graph");
        }
        var explanation = projector.project(graph, execution);
        String base = slug(explanation.decisionLabel()) + "-" + safeId(execution.executionId());
        writeAtomically(outputDirectory.resolve(base + ".txt"), projector.text(explanation));
        writeAtomically(outputDirectory.resolve(base + ".mmd"), mermaid.render(explanation));
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        Path temporary = Files.createTempFile(target.getParent(), ".fachtracing-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String slug(String value) {
        String slug = value.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", "-")
                .replaceAll("^-|-$", "");
        return slug.isBlank() ? "decision" : slug;
    }

    private static String safeId(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "-");
    }

    /** Stops after all queued executions are written. */
    @Override public synchronized void close() {
        if (!running.compareAndSet(true, false)) return;
        LockSupport.unpark(worker);
        try {
            worker.join(5_000L);
            if (worker.isAlive()) diagnosticReporter.accept("business output did not stop within five seconds");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            diagnosticReporter.accept("business output shutdown was interrupted");
        }
    }

    private record GraphKey(String graphId, long graphVersion) { }
}
