package at.gepardec.fachtracing.explain;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.model.DecisionExplanation;

import java.util.List;

/** Protects automatic business output from developer-only execution details. */
public final class BusinessExplanationProjector {
    private static final String SAFE_COVERAGE_GAP = "some business rules could not be observed";
    private final DecisionExplanationProjector developerProjector = new DecisionExplanationProjector();

    /** Projects one execution without result access or developer diagnostics. */
    public DecisionExplanation project(BusinessDecisionGraph graph, DecisionExecution execution) {
        DecisionExplanation developer = developerProjector.project(graph, execution);
        DecisionExecution.DecisionValue result = developer.finalDecision();
        if (execution.terminalStatus() == DecisionExecution.TerminalStatus.SUCCEEDED
                && result.type().equals("unknown")) {
            result = new DecisionExecution.DecisionValue("status", "COMPLETED", "Completed");
        }
        List<String> gaps = developer.completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE
                ? List.of(SAFE_COVERAGE_GAP)
                : List.of();
        return new DecisionExplanation(
                developer.decisionLabel(), result, developer.steps(), developer.completeness(), gaps);
    }
}
