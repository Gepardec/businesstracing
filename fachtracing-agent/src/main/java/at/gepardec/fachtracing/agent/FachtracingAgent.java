package at.gepardec.fachtracing.agent;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.api.DecisionValueAdapter;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.runtime.RuntimeActivationBundle;
import at.gepardec.fachtracing.runtime.RuntimeCollector;
import at.gepardec.fachtracing.runtime.TraceRuntime;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Java-agent entry point. The embedding application supplies its build-time manifest. */
public final class FachtracingAgent {
    private static volatile Configuration configuration;
    private static volatile Instrumentation instrumentation;
    private static volatile FachtracingTransformer transformer;
    private static volatile BusinessTraceFileSink fileSink;
    private static volatile Thread shutdownHook;
    private static final ConcurrentLinkedQueue<String> diagnostics = new ConcurrentLinkedQueue<>();

    private FachtracingAgent() { }

    /** Supplies the analyzed manifest and SHA-256 class fingerprints before installation. */
    public static synchronized void configure(AnalysisManifest manifest, Map<String, String> classFingerprints) {
        configuration = new Configuration(List.of(Objects.requireNonNull(manifest, "manifest")),
                Map.copyOf(classFingerprints));
        if (instrumentation != null) installConfiguredTransformer();
    }

    /** Configures all graphs from one build-generated activation bundle. */
    public static synchronized void configure(RuntimeActivationBundle bundle) {
        Objects.requireNonNull(bundle, "bundle");
        configuration = new Configuration(bundle.decisions().stream()
                .map(RuntimeActivationBundle.DecisionDefinition::manifest).toList(),
                bundle.classFingerprints());
        if (instrumentation != null) installConfiguredTransformer();
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        install(arguments, instrumentation);
    }

    public static void agentmain(String arguments, Instrumentation instrumentation) {
        install(arguments, instrumentation);
    }

    private static synchronized void install(String arguments, Instrumentation activeInstrumentation) {
        AgentOptions.parse(arguments).ifPresent(FachtracingAgent::activateAutomaticOutput);
        instrumentation = Objects.requireNonNull(activeInstrumentation, "instrumentation");
        if (configuration != null) installConfiguredTransformer();
    }

    private static void activateAutomaticOutput(AgentOptions options) {
        RuntimeActivationBundle bundle;
        try {
            bundle = RuntimeActivationBundle.fromJson(Files.readAllBytes(options.activation()));
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Could not read Fachtracing activation file: " + options.activation(), failure);
        }
        var collector = new RuntimeCollector();
        var codec = automaticCodec();
        bundle.decisions().forEach(decision -> collector.register(decision.graph(), codec));
        var newSink = new BusinessTraceFileSink(
                collector,
                bundle.decisions().stream().map(RuntimeActivationBundle.DecisionDefinition::graph).toList(),
                options.output(),
                FachtracingAgent::reportDiagnostic);
        try {
            newSink.start();
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Could not start Fachtracing business output: " + options.output(), failure);
        }
        BusinessTraceFileSink previous = fileSink;
        fileSink = newSink;
        if (previous != null) previous.close();
        TraceRuntime.configure(collector);
        configuration = configuration(bundle);
        installShutdownHook();
    }

    static DecisionExecution.DecisionValueCodec automaticCodec() {
        return new DecisionExecution.DecisionValueCodec((value, context) ->
                new DecisionValueAdapter.AdaptedValue("redacted", "REDACTED", "REDACTED"));
    }

    private static Configuration configuration(RuntimeActivationBundle bundle) {
        return new Configuration(bundle.decisions().stream()
                .map(RuntimeActivationBundle.DecisionDefinition::manifest).toList(),
                bundle.classFingerprints());
    }

    private static void installShutdownHook() {
        if (shutdownHook != null) return;
        shutdownHook = Thread.ofPlatform().name("fachtracing-business-output-shutdown").unstarted(() -> {
            BusinessTraceFileSink active = fileSink;
            if (active != null) active.close();
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private static void installConfiguredTransformer() {
        Configuration active = configuration;
        if (active == null) return;
        if (transformer != null) instrumentation.removeTransformer(transformer);
        transformer = new FachtracingTransformer(active.manifests(), active.classFingerprints());
        instrumentation.addTransformer(transformer, true);
        for (Class<?> loaded : instrumentation.getAllLoadedClasses()) {
            if (!transformer.selects(loaded.getName()) || !instrumentation.isModifiableClass(loaded)) continue;
            try {
                instrumentation.retransformClasses(loaded);
            } catch (Throwable failure) {
                diagnostics.add(loaded.getName() + ": " + Objects.requireNonNullElse(failure.getMessage(), ""));
            }
        }
    }

    /** Returns and removes one developer-only installation/retransformation diagnostic. */
    public static Optional<String> pollDiagnostic() { return Optional.ofNullable(diagnostics.poll()); }

    static void reportDiagnostic(String diagnostic) {
        diagnostics.add(Objects.requireNonNull(diagnostic, "diagnostic"));
    }

    private record Configuration(List<AnalysisManifest> manifests, Map<String, String> classFingerprints) {
        private Configuration {
            manifests = List.copyOf(manifests);
            classFingerprints = Map.copyOf(classFingerprints);
        }
    }
}
