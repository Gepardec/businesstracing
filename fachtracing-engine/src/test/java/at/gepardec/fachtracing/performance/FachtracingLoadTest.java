package at.gepardec.fachtracing.performance;

import at.gepardec.fachtracing.api.DecisionValueRedactor;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.runtime.RuntimeCollector;
import at.gepardec.fachtracing.runtime.TraceRuntime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/** Rate-controlled enabled/disabled latency and trace-integrity harness. */
public final class FachtracingLoadTest {
    private FachtracingLoadTest() { }

    public static void main(String[] args) throws Exception {
        var settings = Settings.parse(args);
        documentationMatchesImplementation();
        int chunks = settings.enabledSeconds() >= settings.baselineSeconds() * 5
                ? Math.min(10, settings.baselineSeconds()) : 1;
        var baselineChunks = new ArrayList<Result>();
        var enabledChunks = new ArrayList<Result>();
        for (int chunk = 0; chunk < chunks; chunk++) {
            baselineChunks.add(run(false, secondsInChunk(settings.baselineSeconds(), chunks, chunk), settings));
            enabledChunks.add(run(true, secondsInChunk(settings.enabledSeconds(), chunks, chunk), settings));
        }
        Result baseline = Result.combine(baselineChunks);
        Result enabled = Result.combine(enabledChunks);
        double overhead = ((double) enabled.p95Nanos() / baseline.p95Nanos() - 1.0) * 100.0;
        System.out.printf(java.util.Locale.ROOT,
                "PERFORMANCE_RESULT rate=%d baseline_seconds=%d enabled_seconds=%d "
                        + "baseline_p50_us=%.3f baseline_p95_us=%.3f enabled_p50_us=%.3f enabled_p95_us=%.3f "
                        + "p95_overhead_percent=%.3f completed=%d errors=%d mismatches=%d dropped=%d contamination=%d%n",
                settings.rate(), settings.baselineSeconds(), settings.enabledSeconds(),
                baseline.p50Nanos() / 1_000.0, baseline.p95Nanos() / 1_000.0,
                enabled.p50Nanos() / 1_000.0, enabled.p95Nanos() / 1_000.0,
                overhead, enabled.completed(), enabled.errors(), enabled.mismatches(),
                enabled.dropped(), enabled.contamination());
        assert enabled.completed() >= (long) settings.rate() * settings.enabledSeconds();
        assert enabled.errors() == 0;
        assert enabled.mismatches() == 0;
        assert enabled.dropped() == 0;
        assert enabled.contamination() == 0;
        assert overhead <= 10.0 : "p95 overhead was " + overhead + "%";
    }

    private static int secondsInChunk(int total, int chunks, int index) {
        return total / chunks + (index < total % chunks ? 1 : 0);
    }

    private static Result run(boolean enabled, int seconds, Settings settings) throws Exception {
        int expected = Math.multiplyExact(settings.rate(), seconds);
        long[] latencies = new long[expected];
        var completed = new AtomicInteger();
        var errors = new AtomicInteger();
        var mismatches = new AtomicInteger();
        var contamination = new AtomicInteger();
        var drained = new AtomicInteger();
        var stopDrainer = new AtomicBoolean();
        RuntimeCollector collector = enabled ? collector() : new NoopCollector();
        TraceRuntime.configure(collector);

        Thread drainer = Thread.ofPlatform().name("fachtracing-load-drainer").start(() -> {
            while (!stopDrainer.get() || collector.completedCount() > 0) {
                collector.pollCompleted().ifPresentOrElse(execution -> {
                    drained.incrementAndGet();
                    if (execution.observations().size() != 2) contamination.incrementAndGet();
                    else {
                        String evidence = execution.observations().getFirst().evidence().get("value").canonicalValue();
                        if (!evidence.equals(execution.finalResult().canonicalValue())) contamination.incrementAndGet();
                    }
                }, Thread::onSpinWait);
            }
        });

        var finished = new CountDownLatch(expected);
        int requiredConcurrency = Math.max(4,
                (int) Math.ceil(settings.rate() * settings.workMicros() / 1_000_000.0) + 4);
        try (var workers = Executors.newFixedThreadPool(Math.max(requiredConcurrency,
                Runtime.getRuntime().availableProcessors()))) {
            long interval = 1_000_000_000L / settings.rate();
            long next = System.nanoTime();
            for (int sequence = 0; sequence < expected; sequence++) {
                long wait = next - System.nanoTime();
                if (wait > 0) LockSupport.parkNanos(wait);
                int invocation = sequence;
                workers.submit(() -> {
                    long started = System.nanoTime();
                    try {
                        TraceRuntime.begin("load-graph", 1);
                        int result = businessDecision(invocation, settings.workMicros());
                        TraceRuntime.observe("predicate", "evaluated", invocation);
                        TraceRuntime.complete("outcome", result);
                        if (result != invocation) mismatches.incrementAndGet();
                    } catch (Throwable failure) {
                        errors.incrementAndGet();
                    } finally {
                        int index = completed.getAndIncrement();
                        if (index < latencies.length) latencies[index] = System.nanoTime() - started;
                        finished.countDown();
                    }
                });
                next += interval;
            }
            assert finished.await(seconds + 30L, TimeUnit.SECONDS) : "load phase timed out";
            workers.shutdown();
            assert workers.awaitTermination(30, TimeUnit.SECONDS);
        }
        stopDrainer.set(true);
        drainer.join(30_000);
        int captured = enabled ? drained.get() : expected;
        Arrays.sort(latencies);
        return new Result(latencies, completed.get(),
                errors.get(), mismatches.get(), expected - captured, contamination.get());
    }

    private static int businessDecision(int invocation, int workMicros) {
        LockSupport.parkNanos(workMicros * 1_000L);
        return invocation;
    }

    private static RuntimeCollector collector() {
        RuntimeCollector collector = new RuntimeCollector();
        collector.register(graph(), new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        return collector;
    }

    private static BusinessDecisionGraph graph() {
        var nodes = List.of(
                new BusinessDecisionGraph.DecisionNode("entry", BusinessDecisionGraph.NodeKind.ENTRY,
                        "begin decision", Map.of()),
                new BusinessDecisionGraph.DecisionNode("predicate", BusinessDecisionGraph.NodeKind.PREDICATE,
                        "request qualifies", Map.of()),
                new BusinessDecisionGraph.DecisionNode("outcome", BusinessDecisionGraph.NodeKind.OUTCOME,
                        "final decision", Map.of()));
        return new BusinessDecisionGraph("load-graph", 1, "load decision", "entry", nodes, List.of(),
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
    }

    private static long percentile(long[] sorted, double percentile) {
        return sorted[Math.min(sorted.length - 1, (int) Math.ceil(sorted.length * percentile) - 1)];
    }

    private static void documentationMatchesImplementation() throws Exception {
        String constructs = Files.readString(Path.of("docs/supported-java-constructs.md"));
        for (String required : List.of("if / else", "comparison", "direct method call",
                "interface or abstract dispatch", "coverage gap", "redaction")) {
            assert constructs.toLowerCase().contains(required) : required;
        }
        for (String diagram : List.of("extraction-flow.puml", "runtime-correlation.puml",
                "explanation-flow.puml", "decision-record-model.puml")) {
            String source = Files.readString(Path.of("docs/plantuml", diagram));
            assert source.startsWith("@startuml") && source.endsWith("@enduml\n") : diagram;
        }
    }

    private record Result(long[] latencies, int completed, int errors,
                          int mismatches, int dropped, int contamination) {
        private long p50Nanos() { return percentile(latencies, 0.50); }
        private long p95Nanos() { return percentile(latencies, 0.95); }

        private static Result combine(List<Result> chunks) {
            int size = chunks.stream().mapToInt(result -> result.latencies.length).sum();
            long[] latencies = new long[size];
            int offset = 0;
            int completed = 0;
            int errors = 0;
            int mismatches = 0;
            int dropped = 0;
            int contamination = 0;
            for (Result chunk : chunks) {
                System.arraycopy(chunk.latencies, 0, latencies, offset, chunk.latencies.length);
                offset += chunk.latencies.length;
                completed += chunk.completed;
                errors += chunk.errors;
                mismatches += chunk.mismatches;
                dropped += chunk.dropped;
                contamination += chunk.contamination;
            }
            Arrays.sort(latencies);
            return new Result(latencies, completed, errors, mismatches, dropped, contamination);
        }
    }

    private record Settings(int rate, int baselineSeconds, int enabledSeconds, int workMicros) {
        private static Settings parse(String[] args) {
            int rate = value(args, "rate", 1_000);
            int baseline = value(args, "baseline-seconds", 60);
            int enabled = value(args, "enabled-seconds", 600);
            int work = value(args, "work-micros", 10_000);
            return new Settings(rate, baseline, enabled, work);
        }

        private static int value(String[] args, String name, int fallback) {
            String prefix = "--" + name + "=";
            return Arrays.stream(args).filter(argument -> argument.startsWith(prefix)).findFirst()
                    .map(argument -> Integer.parseInt(argument.substring(prefix.length()))).orElse(fallback);
        }
    }

    private static final class NoopCollector extends RuntimeCollector {
        @Override public void begin(String graphId, long graphVersion) { }
        @Override public void observe(String nodeId, String outcome, Object value) { }
        @Override public void complete(String nodeId, Object result) { }
    }
}
