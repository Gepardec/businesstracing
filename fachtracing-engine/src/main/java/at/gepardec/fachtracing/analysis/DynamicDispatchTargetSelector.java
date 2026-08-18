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

    /** A selector result. */
    enum Selection { INCLUDE, EXCLUDE, ABSTAIN }
}
