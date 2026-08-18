package at.gepardec.fachtracing.analysis;

/** Classifies when caller source already represents an unavailable call boundary. */
final class SourceUnavailableCallClassifier {
    Representation classify(Evidence evidence) {
        if (evidence.lazyTransformation()) return Representation.CONFIGURED_ACTION;
        if (evidence.callerVisibleCollaborator()) return Representation.CALLER_COLLABORATOR;
        if (evidence.sourceVisibleAction()) return Representation.CALLER_ACTION;
        if (evidence.resultObservedBySourcePredicate()) return Representation.CALLER_PREDICATE;
        if (evidence.explicitCaughtOutcome()) return Representation.CAUGHT_OUTCOME;
        if (evidence.boundaryAlreadyReported()) return Representation.ALREADY_REPORTED;
        return Representation.UNRESOLVED;
    }

    enum Representation {
        CALLER_PREDICATE,
        CALLER_ACTION,
        CALLER_COLLABORATOR,
        CONFIGURED_ACTION,
        CAUGHT_OUTCOME,
        ALREADY_REPORTED,
        UNRESOLVED
    }

    record Evidence(
            boolean resultObservedBySourcePredicate,
            boolean lazyTransformation,
            boolean sourceVisibleAction,
            boolean callerVisibleCollaborator,
            boolean explicitCaughtOutcome,
            boolean boundaryAlreadyReported) { }
}
