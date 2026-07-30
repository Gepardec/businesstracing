package at.gepardec.fachtracing.explain;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.model.DecisionExplanation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Combines one immutable graph version with one ordered execution into what/why/how. */
public final class DecisionExplanationProjector {
    private final BusinessStatementRenderer statements;

    public DecisionExplanationProjector() { this(new BusinessStatementRenderer()); }

    public DecisionExplanationProjector(BusinessStatementRenderer statements) {
        this.statements = Objects.requireNonNull(statements, "statements");
    }

    /** Projects a deterministic business explanation and marks unknown evidence as incomplete. */
    public DecisionExplanation project(BusinessDecisionGraph graph, DecisionExecution execution) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(execution, "execution");
        if (!graph.graphId().equals(execution.graphId()) || graph.version() != execution.graphVersion()) {
            throw new IllegalArgumentException("graph and execution versions do not match");
        }

        var knownNodes = new HashSet<String>();
        graph.nodes().forEach(node -> knownNodes.add(node.nodeId()));
        var steps = new ArrayList<DecisionExplanation.ExplanationStep>();
        var renderedObservations = new HashSet<String>();
        var gaps = new ArrayList<>(execution.coverageGaps());
        for (DecisionExecution.NodeObservation observation : execution.observations().stream()
                .sorted(java.util.Comparator.comparingLong(DecisionExecution.NodeObservation::sequence)).toList()) {
            if (!knownNodes.contains(observation.nodeId())) {
                gaps.add("part of the decision path could not be explained");
                continue;
            }
            var node = graph.node(observation.nodeId());
            if (node.kind() == BusinessDecisionGraph.NodeKind.ENTRY
                    || node.kind() == BusinessDecisionGraph.NodeKind.OUTCOME) continue;
            String observationKey = observation.nodeId() + "\u0000" + observation.outcome()
                    + "\u0000" + observation.evidence() + "\u0000" + observation.selectedEdgeId();
            if (!renderedObservations.add(observationKey)) continue;
            steps.add(new DecisionExplanation.ExplanationStep(
                    observation.sequence(), statements.render(graph, node, observation), observation.outcome()));
        }
        var completeness = graph.completeness() == BusinessDecisionGraph.Completeness.COMPLETE
                && execution.completeness() == BusinessDecisionGraph.Completeness.COMPLETE
                && gaps.isEmpty()
                ? BusinessDecisionGraph.Completeness.COMPLETE
                : BusinessDecisionGraph.Completeness.INCOMPLETE;
        return new DecisionExplanation(graph.decisionLabel(), execution.finalResult(), steps,
                completeness, List.copyOf(gaps));
    }

    /** Renders the projected record as deterministic what/why/how text. */
    public String text(DecisionExplanation explanation) {
        var output = new StringBuilder()
                .append("Decision: ").append(explanation.decisionLabel()).append('\n')
                .append("Result: ").append(explanation.finalDecision().displayValue())
                .append(" [").append(explanation.finalDecision().type()).append("]\n")
                .append("Reasons:\n");
        if (explanation.steps().isEmpty()) output.append("- No evaluated reasons were recorded\n");
        else explanation.steps().forEach(step -> output.append("- ").append(step.statement()).append('\n'));
        output.append("Coverage: ").append(explanation.completeness().name().toLowerCase()).append('\n');
        explanation.coverageGaps().forEach(gap -> output.append("- Gap: ").append(gap).append('\n'));
        return output.toString();
    }
}
