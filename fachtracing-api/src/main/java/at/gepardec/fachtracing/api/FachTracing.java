package at.gepardec.fachtracing.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Java method as a business-decision entry point.
 *
 * <p>The optional label names the decision in business language. Analysis failures are
 * reported separately and never change the annotated method's behavior.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FachTracing {
    /** Returns the business-facing decision label, or an empty string to derive it from source. */
    String value() default "";
}
