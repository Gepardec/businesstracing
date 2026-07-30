package agentfixture;

public final class LocalRule implements DecisionRule {
    public boolean accepts(int distance) { return distance < 20; }
}
