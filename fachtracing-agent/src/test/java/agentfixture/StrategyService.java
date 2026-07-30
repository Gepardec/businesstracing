package agentfixture;

public final class StrategyService {
    public boolean decide(DecisionRule rule, int distance) { return rule.accepts(distance); }
}
