package at.gepardec.fachtracing.explain;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

/** Deterministically turns visited graph nodes into plain business statements. */
public final class BusinessStatementRenderer {
    /** Renders one observed reason without consulting Java provenance. */
    public String render(
            BusinessDecisionGraph graph,
            BusinessDecisionGraph.DecisionNode node,
            DecisionExecution.NodeObservation observation) {
        String evidence = evidence(observation.evidence());
        return switch (node.kind()) {
            case PREDICATE, CHOICE -> node.businessLabel()
                    + evidence
                    + outcome(observation.outcome());
            case DISPATCH -> selectedAlternative(graph, observation);
            case COMPUTATION -> node.businessLabel() + evidence;
            case COVERAGE_GAP -> node.businessLabel();
            case ENTRY -> "decision started";
            case OUTCOME -> outcomeEvidence(observation.evidence());
        };
    }

    private static String outcomeEvidence(Map<String, DecisionExecution.DecisionValue> evidence) {
        String facts = evidence.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("result"))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> friendly(entry.getKey()) + " was " + entry.getValue().displayValue())
                .collect(Collectors.joining(", "));
        return facts.isBlank() ? "decision produced " + evidenceValue(evidence, "result") : facts;
    }

    private static String selectedAlternative(
            BusinessDecisionGraph graph,
            DecisionExecution.NodeObservation observation) {
        if (observation.selectedEdgeId() == null) return "an applicable decision rule was evaluated";
        return graph.edges().stream()
                .filter(edge -> edge.edgeId().equals(observation.selectedEdgeId()))
                .findFirst()
                .map(edge -> graph.node(edge.toNodeId()).businessLabel() + " was selected")
                .orElse("an applicable decision rule was selected");
    }

    private static String evidence(Map<String, DecisionExecution.DecisionValue> evidence) {
        if (evidence.isEmpty()) return "";
        String values = evidence.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> friendly(entry.getKey()) + " was " + entry.getValue().displayValue())
                .collect(Collectors.joining(", "));
        return " (" + values + ")";
    }

    private static String evidenceValue(
            Map<String, DecisionExecution.DecisionValue> evidence, String preferredKey) {
        DecisionExecution.DecisionValue preferred = evidence.get(preferredKey);
        if (preferred != null) return preferred.displayValue();
        return evidence.values().stream().findFirst().map(DecisionExecution.DecisionValue::displayValue)
                .orElse("a result");
    }

    private static String friendly(String label) {
        return label.replace('_', ' ').replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase();
    }

    private static String outcome(String outcome) {
        return outcome == null || outcome.isBlank() || outcome.equals("evaluated")
                ? ""
                : " — " + outcome;
    }
}
