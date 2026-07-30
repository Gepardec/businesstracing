package agentfixture;

public final class RegionalRule implements DecisionRule {
    public boolean accepts(int distance) { return distance < 100; }
}
