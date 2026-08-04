package example;

public final class ExternalRuleImplementation implements ExternalRule {
    @Override public boolean accepts(int amount) {
        return amount < 500;
    }
}
