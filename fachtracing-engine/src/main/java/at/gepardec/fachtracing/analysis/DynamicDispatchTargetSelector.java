package at.gepardec.fachtracing.analysis;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.List;

/** Selects source-visible dynamic-dispatch candidates from framework metadata. */
@FunctionalInterface
public interface DynamicDispatchTargetSelector {
    /** Returns one candidate decision without changing generic dispatch on abstention. */
    Selection select(DispatchTarget target);

    /** The attributed receiver origins, contract, and one concrete candidate. */
    record DispatchTarget(
            List<Element> receiverOrigins,
            TypeElement contract,
            TypeElement candidate) {
        public DispatchTarget {
            receiverOrigins = List.copyOf(receiverOrigins);
            if (receiverOrigins.isEmpty()) {
                throw new IllegalArgumentException("at least one receiver origin is required");
            }
        }
    }

    /** One framework decision for a source-visible candidate. */
    enum Selection {
        INCLUDE,
        EXCLUDE,
        ABSTAIN,
        UNRESOLVED,
        /** Keep the graph incomplete, but retain this candidate for runtime entry confirmation. */
        RUNTIME_OBSERVABLE
    }
}
