package at.gepardec.fachtracing.jakartaee;

import at.gepardec.fachtracing.analysis.SourceSemanticProvider;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reports source-visible Jakarta EE behavior that needs a container. */
public final class JakartaEeSourceSemanticProvider implements SourceSemanticProvider {
    private static final Map<String, String> DIRECT_GAPS = Map.ofEntries(
            Map.entry("jakarta.transaction.Transactional", "transaction interceptor behavior cannot be reconstructed"),
            Map.entry("jakarta.interceptor.Interceptors", "interceptor execution cannot be reconstructed"),
            Map.entry("jakarta.interceptor.AroundInvoke", "interceptor callback execution cannot be reconstructed"),
            Map.entry("jakarta.interceptor.AroundConstruct", "constructor interceptor execution cannot be reconstructed"),
            Map.entry("jakarta.annotation.security.RolesAllowed", "container security enforcement cannot be reconstructed"),
            Map.entry("jakarta.annotation.security.PermitAll", "container security enforcement cannot be reconstructed"),
            Map.entry("jakarta.annotation.security.DenyAll", "container security enforcement cannot be reconstructed"),
            Map.entry("jakarta.annotation.security.RunAs", "container caller identity changes cannot be reconstructed"),
            Map.entry("jakarta.annotation.security.DeclareRoles", "container security role declarations cannot be reconstructed"),
            Map.entry("jakarta.ejb.Asynchronous", "asynchronous EJB execution cannot be reconstructed"),
            Map.entry("jakarta.ejb.TransactionAttribute", "EJB transaction interceptor behavior cannot be reconstructed"),
            Map.entry("jakarta.ejb.Lock", "EJB container concurrency behavior cannot be reconstructed"),
            Map.entry("jakarta.ejb.Schedule", "EJB timer invocation cannot be reconstructed"),
            Map.entry("jakarta.ejb.Schedules", "EJB timer invocation cannot be reconstructed"),
            Map.entry("jakarta.ejb.Timeout", "EJB timer invocation cannot be reconstructed"),
            Map.entry("jakarta.ejb.MessageDriven", "message-driven invocation cannot be reconstructed"),
            Map.entry("jakarta.enterprise.inject.Produces", "CDI producer resolution cannot be reconstructed"),
            Map.entry("jakarta.enterprise.event.Observes", "CDI event delivery cannot be reconstructed"),
            Map.entry("jakarta.enterprise.event.ObservesAsync", "asynchronous CDI event delivery cannot be reconstructed"),
            Map.entry("jakarta.enterprise.inject.Disposes", "CDI disposer execution cannot be reconstructed"),
            Map.entry("jakarta.decorator.Decorator", "CDI decorator execution cannot be reconstructed"),
            Map.entry("jakarta.decorator.Delegate", "CDI decorator delegation cannot be reconstructed"),
            Map.entry("jakarta.annotation.PostConstruct", "container post-construct callback execution cannot be reconstructed"),
            Map.entry("jakarta.annotation.PreDestroy", "container pre-destroy callback execution cannot be reconstructed"),
            Map.entry("jakarta.persistence.EntityListeners", "JPA entity listener execution cannot be reconstructed"),
            Map.entry("jakarta.persistence.PrePersist", "JPA lifecycle callback execution cannot be reconstructed"),
            Map.entry("jakarta.persistence.PostPersist", "JPA lifecycle callback execution cannot be reconstructed"),
            Map.entry("jakarta.persistence.PreUpdate", "JPA lifecycle callback execution cannot be reconstructed"),
            Map.entry("jakarta.persistence.PostUpdate", "JPA lifecycle callback execution cannot be reconstructed"),
            Map.entry("jakarta.persistence.PreRemove", "JPA lifecycle callback execution cannot be reconstructed"),
            Map.entry("jakarta.persistence.PostRemove", "JPA lifecycle callback execution cannot be reconstructed"),
            Map.entry("jakarta.persistence.PostLoad", "JPA lifecycle callback execution cannot be reconstructed"),
            Map.entry("jakarta.validation.Valid", "cascaded Bean Validation execution cannot be reconstructed"),
            Map.entry("jakarta.ws.rs.Path", "JAX-RS filters, interceptors, exception mappers, and parameter conversion cannot be reconstructed"),
            Map.entry("jakarta.ws.rs.ext.Provider", "JAX-RS provider ordering and callbacks cannot be reconstructed"),
            Map.entry("jakarta.jws.WebService", "SOAP handlers, generated proxies, and remote service behavior cannot be reconstructed"),
            Map.entry("jakarta.websocket.server.ServerEndpoint", "WebSocket peer and endpoint callbacks cannot be reconstructed"),
            Map.entry("jakarta.websocket.ClientEndpoint", "WebSocket peer and endpoint callbacks cannot be reconstructed"),
            Map.entry("jakarta.servlet.annotation.WebServlet", "servlet filters, listeners, and container dispatch cannot be reconstructed"),
            Map.entry("jakarta.servlet.annotation.WebFilter", "servlet filter ordering and callbacks cannot be reconstructed"),
            Map.entry("jakarta.servlet.annotation.WebListener", "servlet listener callbacks cannot be reconstructed"));
    private static final Map<String, String> META_GAPS = Map.of(
            "jakarta.interceptor.InterceptorBinding", "interceptor binding execution cannot be reconstructed",
            "jakarta.validation.Constraint", "Bean Validation constraint execution cannot be reconstructed");

    @Override public String providerId() { return "jakartaee:source-semantics"; }

    @Override
    public List<String> coverageGaps(ExecutableElement method) {
        var gaps = new LinkedHashSet<String>();
        collect(method, gaps);
        collect(method.getEnclosingElement(), gaps);
        method.getParameters().forEach(parameter -> collect(parameter, gaps));
        method.getEnclosingElement().getEnclosedElements().forEach(element -> {
            collect(element, gaps);
            if (element instanceof ExecutableElement executable) {
                executable.getParameters().forEach(parameter -> collect(parameter, gaps));
            }
        });
        if (hasInjectionPoint(method.getEnclosingElement())) {
            gaps.add("CDI deployment metadata and portable extensions are not statically verified");
        }
        return List.copyOf(gaps);
    }

    private static boolean hasInjectionPoint(Element type) {
        return type.getEnclosedElements().stream().anyMatch(element ->
                hasAnnotation(element, "jakarta.inject.Inject")
                        || hasAnnotation(element, "jakarta.ejb.EJB")
                        || hasAnnotation(element, "jakarta.annotation.Resource")
                        || hasAnnotation(element, "jakarta.persistence.PersistenceContext"));
    }

    private static void collect(Element element, Set<String> gaps) {
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            String type = annotationType(annotation);
            String direct = DIRECT_GAPS.get(type);
            if (direct != null) gaps.add(direct);
            TypeElement annotationElement = (TypeElement) annotation.getAnnotationType().asElement();
            META_GAPS.forEach((meta, gap) -> {
                if (hasMetaAnnotation(annotationElement, meta, new HashSet<>())) gaps.add(gap);
            });
        }
    }

    private static boolean hasAnnotation(Element element, String type) {
        return element.getAnnotationMirrors().stream()
                .anyMatch(annotation -> annotationType(annotation).equals(type));
    }

    private static boolean hasMetaAnnotation(TypeElement annotation, String metaType, Set<String> visited) {
        String type = annotation.getQualifiedName().toString();
        if (!visited.add(type)) return false;
        return annotation.getAnnotationMirrors().stream().anyMatch(meta ->
                annotationType(meta).equals(metaType)
                        || hasMetaAnnotation((TypeElement) meta.getAnnotationType().asElement(), metaType, visited));
    }

    private static String annotationType(AnnotationMirror annotation) {
        return ((TypeElement) annotation.getAnnotationType().asElement()).getQualifiedName().toString();
    }
}
