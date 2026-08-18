package at.gepardec.fachtracing.jakartaee;

import at.gepardec.fachtracing.analysis.DynamicDispatchTargetSelector;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/** Selects CDI injection targets from source-visible implementation candidates. */
public final class CdiDispatchTargetSelector implements DynamicDispatchTargetSelector {
    private static final String INJECT = "jakarta.inject.Inject";
    private static final String ALTERNATIVE = "jakarta.enterprise.inject.Alternative";
    private static final String QUALIFIER = "jakarta.inject.Qualifier";

    @Override
    public Selection select(DispatchTarget target) {
        if (!hasAnnotation(target.receiver(), INJECT)) return Selection.ABSTAIN;
        if (hasAnnotation(target.candidate(), ALTERNATIVE)) return Selection.EXCLUDE;
        Set<String> required = qualifiers(target.receiver());
        if (required.isEmpty()) return Selection.INCLUDE;
        return qualifiers(target.candidate()).containsAll(required) ? Selection.INCLUDE : Selection.EXCLUDE;
    }

    private static boolean hasAnnotation(Element element, String type) {
        return element.getAnnotationMirrors().stream().anyMatch(annotation -> annotationType(annotation).equals(type));
    }

    private static Set<String> qualifiers(Element element) {
        return element.getAnnotationMirrors().stream()
                .filter(annotation -> annotation.getAnnotationType().asElement().getAnnotationMirrors().stream()
                        .anyMatch(meta -> annotationType(meta).equals(QUALIFIER)))
                .map(CdiDispatchTargetSelector::annotationType).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String annotationType(AnnotationMirror annotation) {
        return ((TypeElement) annotation.getAnnotationType().asElement()).getQualifiedName().toString();
    }
}
