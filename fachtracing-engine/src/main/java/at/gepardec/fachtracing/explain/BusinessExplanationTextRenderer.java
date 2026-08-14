package at.gepardec.fachtracing.explain;

import at.gepardec.fachtracing.model.DecisionExplanation;

import java.util.Objects;

/** Renders an automatic explanation without developer value types. */
public final class BusinessExplanationTextRenderer {
    /** Renders deterministic decision, result, reason, and coverage text. */
    public String render(DecisionExplanation explanation) {
        Objects.requireNonNull(explanation, "explanation");
        var output = new StringBuilder()
                .append("Decision: ").append(explanation.decisionLabel()).append('\n')
                .append("Result: ").append(explanation.finalDecision().displayValue()).append('\n')
                .append("Reasons:\n");
        if (explanation.steps().isEmpty()) {
            output.append("- No evaluated reasons were recorded\n");
        } else {
            explanation.steps().forEach(step -> output.append("- ").append(step.statement()).append('\n'));
        }
        output.append("Coverage: ").append(explanation.completeness().name().toLowerCase()).append('\n');
        explanation.coverageGaps().forEach(gap -> output.append("- Gap: ").append(gap).append('\n'));
        return output.toString();
    }
}
