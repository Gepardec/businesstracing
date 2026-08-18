package at.gepardec.fachtracing.jakartaee;

import at.gepardec.fachtracing.analysis.DynamicDispatchTargetSelector;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Selects CDI injection targets from source-visible implementation candidates. */
public final class CdiDispatchTargetSelector implements DynamicDispatchTargetSelector {
    private static final String INJECT = "jakarta.inject.Inject";
    private static final String ALTERNATIVE = "jakarta.enterprise.inject.Alternative";
    private static final String QUALIFIER = "jakarta.inject.Qualifier";
    private static final String DEFAULT = "jakarta.enterprise.inject.Default";
    private static final String ANY = "jakarta.enterprise.inject.Any";
    private static final String NAMED = "jakarta.inject.Named";
    private static final String NONBINDING = "jakarta.enterprise.util.Nonbinding";
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
        List<Element> injectionPoints = target.receiverOrigins().stream()
                .filter(CdiDispatchTargetSelector::isInjectionPoint).toList();
        if (injectionPoints.size() != 1) return Selection.ABSTAIN;
        if (!hasBeanDefiningScope(target.candidate())) return Selection.EXCLUDE;
        if (hasAnnotation(target.candidate(), ALTERNATIVE)) return Selection.EXCLUDE;
        List<AnnotationMirror> declared = qualifiers(injectionPoints.getFirst());
        List<AnnotationMirror> required = declared.stream()
                .filter(qualifier -> !annotationType(qualifier).equals(ANY)).toList();
        boolean matches = required.isEmpty()
                ? declared.stream().anyMatch(qualifier -> annotationType(qualifier).equals(ANY))
                        || hasDefaultQualifier(target.candidate())
                : required.stream().allMatch(qualifier -> candidateHasQualifier(
                        target.candidate(), qualifier));
        return matches ? Selection.INCLUDE : Selection.EXCLUDE;
    }

    private static boolean isInjectionPoint(Element element) {
        if (hasAnnotation(element, INJECT)) return true;
        return element.getKind() == ElementKind.PARAMETER
                && hasAnnotation(element.getEnclosingElement(), INJECT);
    }

    private static boolean hasBeanDefiningScope(Element element) {
        return element.getAnnotationMirrors().stream()
                .map(CdiDispatchTargetSelector::annotationType)
                .anyMatch(BEAN_DEFINING_SCOPES::contains);
    }

    private static boolean hasAnnotation(Element element, String type) {
        return element.getAnnotationMirrors().stream().anyMatch(annotation -> annotationType(annotation).equals(type));
    }

    private static List<AnnotationMirror> qualifiers(Element element) {
        return element.getAnnotationMirrors().stream()
                .filter(annotation -> annotation.getAnnotationType().asElement().getAnnotationMirrors().stream()
                        .anyMatch(meta -> annotationType(meta).equals(QUALIFIER)))
                .map(annotation -> (AnnotationMirror) annotation)
                .toList();
    }

    private static boolean hasDefaultQualifier(Element candidate) {
        List<AnnotationMirror> declared = qualifiers(candidate);
        if (declared.stream().anyMatch(annotation -> annotationType(annotation).equals(DEFAULT))) return true;
        return declared.stream().map(CdiDispatchTargetSelector::annotationType)
                .allMatch(type -> type.equals(ANY) || type.equals(NAMED));
    }

    private static boolean candidateHasQualifier(Element candidate, AnnotationMirror required) {
        String requiredType = annotationType(required);
        if (requiredType.equals(DEFAULT)) return hasDefaultQualifier(candidate);
        return qualifiers(candidate).stream()
                .filter(annotation -> annotationType(annotation).equals(requiredType))
                .anyMatch(annotation -> bindingValues(annotation).equals(bindingValues(required)));
    }

    private static Map<String, String> bindingValues(AnnotationMirror annotation) {
        var explicit = new LinkedHashMap<ExecutableElement, AnnotationValue>();
        explicit.putAll(annotation.getElementValues());
        var values = new LinkedHashMap<String, String>();
        TypeElement type = (TypeElement) annotation.getAnnotationType().asElement();
        for (Element member : type.getEnclosedElements()) {
            if (!(member instanceof ExecutableElement method) || hasAnnotation(method, NONBINDING)) continue;
            AnnotationValue value = explicit.get(method);
            if (value == null) value = method.getDefaultValue();
            if (value != null) values.put(method.getSimpleName().toString(), value.toString());
        }
        return Map.copyOf(values);
    }

    private static String annotationType(AnnotationMirror annotation) {
        return ((TypeElement) annotation.getAnnotationType().asElement()).getQualifiedName().toString();
    }
}
