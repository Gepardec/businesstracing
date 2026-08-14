package at.gepardec.fachtracing.business;

import at.gepardec.fachtracing.model.BusinessLogicGraph;

import java.util.List;
import java.util.Objects;

/** Holds a business graph and the developer-only decisions that produced it. */
public record BusinessGraphProjection(
        BusinessLogicGraph graph,
        List<Decision> decisions) {
    /** Creates one immutable projection result. */
    public BusinessGraphProjection {
        Objects.requireNonNull(graph, "graph");
        decisions = List.copyOf(decisions);
    }

    /** Identifies the exact input type for one projection decision. */
    public enum SubjectKind { NODE, TERMINAL_EDGE, GRAPH }

    /** States how the projection treated one exact input. */
    public enum Action { KEPT, REMOVED, REPLACED }

    /** Gives the stable rule that caused one projection action. */
    public enum Reason {
        STRUCTURAL_ENTRY,
        STRUCTURAL_OUTCOME,
        REDUNDANT_RULE,
        LOOP_MECHANICS,
        LOOP_RULE,
        TECHNICAL_PREDICATE,
        TECHNICAL_CHOICE,
        TECHNICAL_DISPATCH,
        TECHNICAL_CALCULATION,
        BUSINESS_RULE,
        BUSINESS_ACTION,
        COVERAGE_GAP,
        TERMINAL_RESULT,
        COMPLETED_FALLBACK,
        UNREACHABLE
    }

    /** Explains one final exact-to-business projection decision. */
    public record Decision(
            String subjectId,
            SubjectKind subjectKind,
            String sourceKind,
            String sourceLabel,
            Action action,
            Reason reason,
            List<String> businessNodeIds) {
        /** Creates one immutable developer-only projection decision. */
        public Decision {
            subjectId = requireText(subjectId, "subjectId");
            Objects.requireNonNull(subjectKind, "subjectKind");
            sourceKind = requireText(sourceKind, "sourceKind");
            sourceLabel = requireText(sourceLabel, "sourceLabel");
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(reason, "reason");
            businessNodeIds = List.copyOf(businessNodeIds);
            if (action == Action.REMOVED && !businessNodeIds.isEmpty()) {
                throw new IllegalArgumentException("removed decisions cannot refer to business nodes");
            }
            if (action != Action.REMOVED && businessNodeIds.isEmpty()) {
                throw new IllegalArgumentException("kept and replaced decisions must refer to business nodes");
            }
        }

        private static String requireText(String value, String name) {
            Objects.requireNonNull(value, name);
            if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
            return value;
        }
    }
}
