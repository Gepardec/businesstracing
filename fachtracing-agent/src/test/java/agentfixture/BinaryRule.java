package agentfixture;

public final class BinaryRule {
    private BinaryRule() { }

    public static boolean accepts(int age) {
        return age >= 24;
    }
}
