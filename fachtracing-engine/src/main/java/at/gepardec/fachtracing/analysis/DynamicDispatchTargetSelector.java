package at.gepardec.fachtracing.analysis;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/** Selects source-visible dynamic-dispatch candidates from framework metadata. */
@FunctionalInterface
public interface DynamicDispatchTargetSelector {
    /** Returns one candidate decision without changing generic dispatch on abstention. */
    Selection select(DispatchTarget target);

    /** The attributed receiver, contract, and one concrete candidate. */
    record DispatchTarget(Element receiver, TypeElement contract, TypeElement candidate) { }

    /** A selector result. */
    enum Selection { INCLUDE, EXCLUDE, ABSTAIN }
}
