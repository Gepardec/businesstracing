package at.gepardec.fachtracing.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

/** Loads optional source-semantic providers from one class loader. */
public final class SourceSemanticProviders {
    private SourceSemanticProviders() { }

    /** Loads, validates, and sorts all providers visible to the class loader. */
    public static List<SourceSemanticProvider> load(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        var providers = new ArrayList<SourceSemanticProvider>();
        ServiceLoader.load(SourceSemanticProvider.class, classLoader).forEach(providers::add);
        providers.sort(java.util.Comparator.comparing(SourceSemanticProvider::providerId));
        var ids = new HashSet<String>();
        for (SourceSemanticProvider provider : providers) {
            String id = Objects.requireNonNull(provider.providerId(), "providerId");
            if (id.isBlank()) throw new IllegalArgumentException("providerId must not be blank");
            if (!ids.add(id)) throw new IllegalArgumentException("duplicate source semantic provider ID: " + id);
        }
        return List.copyOf(providers);
    }
}
