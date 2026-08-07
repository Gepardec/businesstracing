package example.rules;

public final class ExternalRules {
    private ExternalRules() { }

    public static boolean accepts(int amount) {
        return amount < 500;
    }
}
