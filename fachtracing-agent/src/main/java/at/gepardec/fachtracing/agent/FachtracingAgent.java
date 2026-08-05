package at.gepardec.fachtracing.agent;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.runtime.RuntimeActivationBundle;

import java.lang.instrument.Instrumentation;
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
        install(instrumentation);
    }

    public static void agentmain(String arguments, Instrumentation instrumentation) {
        install(instrumentation);
    }

    private static synchronized void install(Instrumentation activeInstrumentation) {
        instrumentation = Objects.requireNonNull(activeInstrumentation, "instrumentation");
        if (configuration != null) installConfiguredTransformer();
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

    private record Configuration(List<AnalysisManifest> manifests, Map<String, String> classFingerprints) {
        private Configuration {
            manifests = List.copyOf(manifests);
            classFingerprints = Map.copyOf(classFingerprints);
        }
    }
}
