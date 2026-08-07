package at.gepardec.fachtracing.analysis;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Finds generic Java iteration and dynamic-dispatch mechanics in business graph text. */
public final class BusinessArtifactGuard {
    /** Returns all technical business-artifact violations in deterministic graph order. */
    public List<String> violations(BusinessDecisionGraph graph) {
        var violations = new ArrayList<String>();
        for (BusinessDecisionGraph.DecisionNode node : graph.nodes()) {
            String label = node.businessLabel().toLowerCase(Locale.ROOT);
            if (label.matches("[a-z]")
                    || label.equals("list")
                    || Set.of("evaluate add", "evaluate add all", "evaluate offer", "evaluate set",
                            "evaluate sort").contains(label)
                    || label.matches("derive (?:i|idx|index)(?: as 0)?")
                    || label.matches("repeat while .*\\b(?:index|idx)\\b.*\\bsize\\b.*")
                    || label.matches(".*\\bitem (?:index|idx)(?: plus [0-9]+)?\\b.*")
                    || label.matches(".*\\binitialize\\b.*")
                    || label.matches(".*\\benum\\s+type\\b.*")) {
                violations.add("node " + node.nodeId() + ": " + node.businessLabel());
            }
        }
        for (BusinessDecisionGraph.DecisionEdge edge : graph.edges()) {
            if (edge.outcome().toLowerCase(Locale.ROOT).matches("candidate [0-9]+")) {
                violations.add("edge " + edge.edgeId() + ": " + edge.outcome());
            }
        }
        return List.copyOf(violations);
    }
}
