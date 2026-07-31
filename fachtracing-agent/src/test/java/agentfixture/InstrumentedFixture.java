package agentfixture;

import at.gepardec.fachtracing.api.FachTracing;

public final class InstrumentedFixture {
    private final IllegalStateException propagatedFailure = new IllegalStateException("called method failed");
    private int secondOperandEvaluations;

    public boolean decide(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("age must not be negative");
        }
        if (age < 24) {
            return true;
        }
        return false;
    }

    public boolean decideThroughHelper() {
        failFromHelper();
        return true;
    }

    public Throwable propagatedFailure() {
        return propagatedFailure;
    }

    @FachTracing("compound conjunction")
    public boolean decideAnd(boolean first, boolean second) {
        secondOperandEvaluations = 0;
        if (first
                && evaluateSecondOperand(second)) {
            return true;
        }
        return false;
    }

    @FachTracing("compound disjunction")
    public boolean decideOr(boolean first, boolean second) {
        secondOperandEvaluations = 0;
        if (first
                || evaluateSecondOperand(second)) {
            return true;
        }
        return false;
    }

    @FachTracing("mixed compound")
    public boolean decideMixed(boolean first, boolean second, boolean third) {
        if ((first && second) || third) {
            return true;
        }
        return false;
    }

    @FachTracing("ternary predicate")
    public boolean decideTernary(boolean selector, boolean first, boolean second) {
        if (selector ? first : second) {
            return true;
        }
        return false;
    }

    public int secondOperandEvaluations() {
        return secondOperandEvaluations;
    }

    private boolean evaluateSecondOperand(boolean value) {
        secondOperandEvaluations++;
        return value;
    }

    private void failFromHelper() {
        throw propagatedFailure;
    }
}
