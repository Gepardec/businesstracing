package fixtures.jakartaee;

import at.gepardec.fachtracing.api.FachTracing;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
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
