package agentfixture;

import at.gepardec.fachtracing.api.FachTracing;

public final class StrategyService {
    @FachTracing("proxy decision")
    public boolean decide(DecisionRule rule, int distance) { return rule.accepts(distance); }

    @FachTracing("reflection decision")
    public boolean decideReflectively(DecisionRule rule, int distance) throws Exception {
        var selected = DecisionRule.class.getMethod("accepts", int.class);
        return (boolean) selected.invoke(rule, distance);
    }

    @FachTracing("service loader decision")
    public boolean decideFromServices(int distance) {
        return java.util.ServiceLoader.load(DecisionRule.class).findFirst().orElseThrow().accepts(distance);
    }

    @FachTracing("unknown reflection decision")
    public boolean decideUnknown(java.lang.reflect.Method selected, Object rule, int distance) throws Exception {
        return (boolean) selected.invoke(rule, distance);
    }
}
