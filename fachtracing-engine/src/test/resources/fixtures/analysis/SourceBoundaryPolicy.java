package boundary;

import at.gepardec.fachtracing.api.FachTracing;

import java.util.List;
import java.util.Map;

/** Application source that has no access to the binary rule source. */
public final class SourceBoundaryPolicy {
    @FachTracing("caller predicate boundary")
    public boolean callerPredicate(String value) {
        Object match = SourceBoundaryBinaryRules.Nested.find(value);
        return match != null;
    }

    @FachTracing("lazy callback boundary")
    public List<String> lazyCallback(
            List<String> values,
            SourceBoundaryBinaryRules.Rule rule) {
        return values.stream().filter(rule::approve).toList();
    }

    @FachTracing("lazy lambda boundary")
    public List<Object> lazyLambda(List<String> values) {
        return values.stream().map(value -> SourceBoundaryBinaryRules.Nested.find(value)).toList();
    }

    @FachTracing("lazy receiver boundary")
    public List<String> lazyReceiver(SourceBoundaryBinaryRules.StreamSource source) {
        return source.values().map(String::trim).toList();
    }

    @FachTracing("derived collaborator boundary")
    public boolean derivedCollaborator(SourceBoundaryBinaryRules.StateProvider provider) {
        SourceBoundaryBinaryRules.State state = provider.state();
        state.update();
        return state.approved();
    }

    @FachTracing("direct collaborator boundary")
    public Object directCollaborator(SourceBoundaryBinaryRules.StateProvider provider) {
        return provider.state();
    }

    @FachTracing("caller action boundary")
    public Map<String, Object> callerAction(Map<String, Object> target, String value) {
        target.put("match", SourceBoundaryBinaryRules.Nested.find(value));
        return target;
    }

    @FachTracing("caught source boundary")
    public boolean caughtPath(String value) {
        try {
            Object match = SourceBoundaryBinaryRules.Nested.read(value);
            return match != null;
        } catch (RuntimeException failure) {
            return false;
        }
    }

    @FachTracing("nested binary boundary")
    public boolean nestedBinary(int age) {
        return SourceBoundaryBinaryRules.Nested.accepts(age);
    }

    @FachTracing("direct external boundary")
    public boolean directExternal(SourceBoundaryBinaryRules.Rule rule, String value) {
        return rule.approve(value);
    }

    @FachTracing("local external boundary")
    public boolean localExternal(SourceBoundaryBinaryRules.Rule rule, String value) {
        boolean approved = rule.approve(value);
        return approved;
    }

    @FachTracing("guarded external boundary")
    public boolean guardedExternal(SourceBoundaryBinaryRules.Rule rule, String value) {
        if (rule.approve(value)) return true;
        return false;
    }

    @FachTracing("repeated external effect boundary")
    public boolean repeatedExternalEffect(SourceBoundaryBinaryRules.State state) {
        state.update();
        state.update();
        if (state.approved()) return true;
        return false;
    }
}
