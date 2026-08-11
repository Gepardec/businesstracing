package at.gepardec.fachtracing.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

/** Loads optional external method contract providers from one class loader. */
public final class ExternalMethodContractProviders {
    private ExternalMethodContractProviders() { }

    /** Loads, validates, and sorts all providers visible to the class loader. */
    public static List<ExternalMethodContractProvider> load(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        var providers = new ArrayList<ExternalMethodContractProvider>();
        ServiceLoader.load(ExternalMethodContractProvider.class, classLoader).forEach(providers::add);
        providers.sort(java.util.Comparator.comparing(ExternalMethodContractProvider::providerId));
        var ids = new HashSet<String>();
        for (ExternalMethodContractProvider provider : providers) {
            String id = Objects.requireNonNull(provider.providerId(), "providerId");
            if (id.isBlank()) throw new IllegalArgumentException("providerId must not be blank");
            if (!ids.add(id)) throw new IllegalArgumentException("duplicate external method provider ID: " + id);
        }
        return List.copyOf(providers);
    }
}
