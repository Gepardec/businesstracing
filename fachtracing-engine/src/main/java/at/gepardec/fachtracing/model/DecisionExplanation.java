package at.gepardec.fachtracing.model;

import java.util.List;
import java.util.Objects;

/** Deterministic business-only explanation of what, why, and how. */
public record DecisionExplanation(
        String decisionLabel,
        DecisionExecution.DecisionValue finalDecision,
        List<ExplanationStep> steps,
        BusinessDecisionGraph.Completeness completeness,
        List<String> coverageGaps) {

    /** Creates a defensive explanation snapshot. */
    public DecisionExplanation {
        Objects.requireNonNull(decisionLabel, "decisionLabel");
        Objects.requireNonNull(finalDecision, "finalDecision");
        steps = List.copyOf(steps);
        Objects.requireNonNull(completeness, "completeness");
        coverageGaps = List.copyOf(coverageGaps);
    }

    /** One ordered business reason in the actual execution path. */
    public record ExplanationStep(long sequence, String statement, String outcome) {
        /** Creates a validated explanation step. */
        public ExplanationStep {
            if (sequence < 0) throw new IllegalArgumentException("sequence must be non-negative");
            Objects.requireNonNull(statement, "statement");
            Objects.requireNonNull(outcome, "outcome");
        }
    }
}
