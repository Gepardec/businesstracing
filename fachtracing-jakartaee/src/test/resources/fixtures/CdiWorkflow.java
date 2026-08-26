package fixtures.jakartaee;

import at.gepardec.fachtracing.api.FachTracing;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.util.Nonbinding;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class CdiWorkflow {
    @Inject
    @Urgent
    Rule rule;

    @FachTracing("apply CDI rule")
    public boolean apply(int value) {
        return rule.accepts(value);
    }
}

final class DefaultCdiWorkflow {
    @Inject
    Rule rule;

    @FachTracing("apply default CDI rule")
    boolean apply(int value) {
        return rule.accepts(value);
    }
}

final class ConstructorCdiWorkflow {
    private final Rule rule;

    @Inject
    ConstructorCdiWorkflow(@Region(value = "EU", note = "injection point") Rule rule) {
        this.rule = rule;
    }

    @FachTracing("apply constructor CDI rule")
    boolean apply(int value) {
        return rule.accepts(value);
    }
}

final class CustomScopeCdiWorkflow {
    @Inject @Custom Rule rule;

    @FachTracing("apply custom-scope CDI rule")
    boolean apply(int value) { return rule.accepts(value); }
}

final class StereotypeCdiWorkflow {
    @Inject @Stereotyped Rule rule;

    @FachTracing("apply stereotype CDI rule")
    boolean apply(int value) { return rule.accepts(value); }
}

final class PriorityAlternativeCdiWorkflow {
    @Inject @PrioritySelected Rule rule;

    @FachTracing("apply priority alternative CDI rule")
    boolean apply(int value) { return rule.accepts(value); }
}

final class XmlAlternativeCdiWorkflow {
    @Inject @XmlSelected Rule rule;

    @FachTracing("apply XML alternative CDI rule")
    boolean apply(int value) { return rule.accepts(value); }
}

final class DynamicLookupCdiWorkflow {
    @Inject Instance<Rule> rules;

    @FachTracing("apply dynamic CDI rule")
    boolean apply(int value) { return rules.get().accepts(value); }
}

interface Rule {
    boolean accepts(int value);
}

@ApplicationScoped
@Urgent
final class ScopedRule implements Rule {
    public boolean accepts(int value) {
        return value > 0;
    }
}

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@interface Urgent { }

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@interface Region {
    String value();

    @Nonbinding
    String note() default "";
}

@Qualifier @Retention(RetentionPolicy.RUNTIME) @interface Custom { }
@Qualifier @Retention(RetentionPolicy.RUNTIME) @interface Stereotyped { }
@Qualifier @Retention(RetentionPolicy.RUNTIME) @interface PrioritySelected { }
@Qualifier @Retention(RetentionPolicy.RUNTIME) @interface XmlSelected { }

@NormalScope(passivating = false)
@Retention(RetentionPolicy.RUNTIME)
@interface CustomScoped { }

@Stereotype
@ApplicationScoped
@Retention(RetentionPolicy.RUNTIME)
@interface BusinessScoped { }

final class PlainRule implements Rule {
    public boolean accepts(int value) {
        return value >= 0;
    }
}

@ApplicationScoped
@Alternative
final class AlternativeRule implements Rule {
    public boolean accepts(int value) {
        return value < 0;
    }
}

@ApplicationScoped
final class OtherScopedRule implements Rule {
    public boolean accepts(int value) {
        return value != 0;
    }
}

@ApplicationScoped
@Region(value = "EU", note = "candidate")
final class EuropeanRule implements Rule {
    public boolean accepts(int value) {
        return value > 10;
    }
}

@ApplicationScoped
@Region("US")
final class AmericanRule implements Rule {
    public boolean accepts(int value) {
        return value > 20;
    }
}

@CustomScoped @Custom
final class CustomScopedRule implements Rule {
    public boolean accepts(int value) { return value > 30; }
}

@BusinessScoped @Stereotyped
final class StereotypeRule implements Rule {
    public boolean accepts(int value) { return value > 40; }
}

@ApplicationScoped @Alternative @Priority(10) @PrioritySelected
final class PriorityAlternativeRule implements Rule {
    public boolean accepts(int value) { return value > 50; }
}

@ApplicationScoped @Alternative @XmlSelected
final class XmlAlternativeRule implements Rule {
    public boolean accepts(int value) { return value > 60; }
}
