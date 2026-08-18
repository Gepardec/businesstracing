package fixtures.jakartaee;

import at.gepardec.fachtracing.api.FachTracing;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Schedule;
import jakarta.enterprise.event.Observes;
import jakarta.interceptor.InterceptorBinding;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PostLoad;
import jakarta.transaction.Transactional;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.ws.rs.Path;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Tracked
@Path("/container")
final class ContainerWorkflow {
    EntityManager entities;

    @FachTracing("store secured entity")
    @Transactional
    @RolesAllowed("writer")
    boolean store(@BusinessValue Object value) {
        entities.persist(value);
        return true;
    }

    @PostConstruct void initialize() { }
    @PostLoad void loaded() { }
    @Schedule(hour = "*") void scheduled() { }
    @Asynchronous void asynchronous() { }
    void observe(@Observes String event) { }
}

@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@interface Tracked { }

@Constraint(validatedBy = {})
@Retention(RetentionPolicy.RUNTIME)
@interface BusinessValue {
    String message() default "invalid";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
