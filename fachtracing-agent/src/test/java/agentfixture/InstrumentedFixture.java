package agentfixture;

public final class InstrumentedFixture {
    public boolean decide(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("age must not be negative");
        }
        if (age < 24) {
            return true;
        }
        return false;
    }
}
