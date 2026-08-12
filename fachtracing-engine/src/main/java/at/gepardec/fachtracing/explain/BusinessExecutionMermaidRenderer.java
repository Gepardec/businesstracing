package at.gepardec.fachtracing.explain;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExplanation;

import java.util.Objects;

/** Renders one evaluated business path as a small Mermaid flow. */
public final class BusinessExecutionMermaidRenderer {
    /** Renders business statements only. */
    public String render(DecisionExplanation explanation) {
        Objects.requireNonNull(explanation, "explanation");
        var output = new StringBuilder("flowchart TD\n")
                .append("  start([\"")
                .append(label("Decision: " + explanation.decisionLabel()))
                .append("\"])\n");
        String previous = "start";
        for (int index = 0; index < explanation.steps().size(); index++) {
            String node = "step" + (index + 1);
            output.append("  ").append(node).append("[\"")
                    .append(label(explanation.steps().get(index).statement()))
                    .append("\"]\n")
                    .append("  ").append(previous).append(" --> ").append(node).append('\n');
            previous = node;
        }
        output.append("  result([\"")
                .append(label("Result: " + explanation.finalDecision().displayValue()))
                .append("\"])\n")
                .append("  ").append(previous).append(" --> result\n");
        if (explanation.completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE) {
            output.append("  coverage[\"Coverage is incomplete\"]\n")
                    .append("  result -.-> coverage\n");
        }
        return output.toString();
    }

    private static String label(String value) {
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace('\n', ' ')
                .replace('\r', ' ');
    }
}
