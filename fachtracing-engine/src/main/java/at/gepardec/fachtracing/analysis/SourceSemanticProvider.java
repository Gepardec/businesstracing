package at.gepardec.fachtracing.analysis;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

/** Reports framework-managed source semantics for one reachable method. */
public interface SourceSemanticProvider {
    /** Returns one stable provider identifier. */
    String providerId();

    /** Returns incomplete semantic facts for the reachable source method. */
    List<String> coverageGaps(ExecutableElement method);
}
