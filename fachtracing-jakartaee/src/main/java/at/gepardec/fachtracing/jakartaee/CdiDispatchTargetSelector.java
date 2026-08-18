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
    private static final Set<String> DEFAULT_QUALIFIERS = Set.of(
            "jakarta.inject.Default", "jakarta.inject.Any");
    private static final Set<String> BEAN_DEFINING_SCOPES = Set.of(
            "jakarta.enterprise.context.ApplicationScoped",
            "jakarta.enterprise.context.RequestScoped",
            "jakarta.enterprise.context.SessionScoped",
            "jakarta.enterprise.context.ConversationScoped",
            "jakarta.enterprise.context.Dependent",
            "jakarta.inject.Singleton",
            "jakarta.ejb.Stateless",
            "jakarta.ejb.Stateful",
            "jakarta.ejb.Singleton");

    @Override
    public Selection select(DispatchTarget target) {
        if (!hasAnnotation(target.receiver(), INJECT)) return Selection.ABSTAIN;
        if (!hasBeanDefiningScope(target.candidate())) return Selection.EXCLUDE;
        if (hasAnnotation(target.candidate(), ALTERNATIVE)) return Selection.EXCLUDE;
        Set<String> required = qualifiers(target.receiver()).stream()
                .filter(qualifier -> !DEFAULT_QUALIFIERS.contains(qualifier))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (required.isEmpty()) return Selection.INCLUDE;
        return qualifiers(target.candidate()).containsAll(required) ? Selection.INCLUDE : Selection.EXCLUDE;
    }

    private static boolean hasBeanDefiningScope(Element element) {
        return element.getAnnotationMirrors().stream()
                .map(CdiDispatchTargetSelector::annotationType)
                .anyMatch(BEAN_DEFINING_SCOPES::contains);
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
