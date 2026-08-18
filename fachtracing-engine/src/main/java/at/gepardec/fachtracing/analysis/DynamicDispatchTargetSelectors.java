package at.gepardec.fachtracing.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

/** Loads optional framework dispatch selectors from one class loader. */
public final class DynamicDispatchTargetSelectors {
    private DynamicDispatchTargetSelectors() { }

    /** Loads and sorts all selectors visible to the class loader. */
    public static List<DynamicDispatchTargetSelector> load(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        var selectors = new ArrayList<DynamicDispatchTargetSelector>();
        ServiceLoader.load(DynamicDispatchTargetSelector.class, classLoader).forEach(selectors::add);
        selectors.sort(java.util.Comparator.comparing(selector -> selector.getClass().getName()));
        return List.copyOf(selectors);
    }
}
