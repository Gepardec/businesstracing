package at.gepardec.fachtracing.explain;

import at.gepardec.fachtracing.api.DecisionValueRedactor;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Executable business-explanation snapshot and value-boundary contracts. */
public final class DecisionExplanationProjectorTest {
    private DecisionExplanationProjectorTest() { }

    public static void main(String[] args) throws Exception {
        explanationMatchesBusinessSnapshot();
        supportsEveryBuiltInFinalValueKind();
        rendersFailedExecutionWithoutTechnicalData();
        preservesRedactionAndRejectsMissingAdapters();
        unknownEvidenceMakesCoverageExplicit();
        rendersBusinessExecutionMermaid();
    }

    private static void explanationMatchesBusinessSnapshot() throws Exception {
        var projector = new DecisionExplanationProjector();
        String actual = projector.text(projector.project(graph(), execution()));
        String expected = Files.readString(snapshot("eligibility-explanation.txt"));
        assert actual.equals(expected) : "\n" + actual;
        assertNoTechnicalLanguage(actual);
    }

    private static void supportsEveryBuiltInFinalValueKind() {
        var values = List.of(
                DecisionExecution.DecisionValue.of(true),
                DecisionExecution.DecisionValue.of(12.5),
                new DecisionExecution.DecisionValue("category", "APPROVED", "Approved"),
                DecisionExecution.DecisionValue.of("accepted"));
        var projector = new DecisionExplanationProjector();
        for (var value : values) {
            var projected = projector.project(graph(), execution(value, List.of()));
            assert projected.finalDecision().equals(value);
            assert projector.text(projected).contains("[" + value.type() + "]");
        }
    }

    private static void rendersFailedExecutionWithoutTechnicalData() {
        var execution = new DecisionExecution("failed-execution", "eligibility-graph", 1,
                Instant.parse("2026-07-24T08:00:00Z"), Instant.parse("2026-07-24T08:00:01Z"),
                List.of(), DecisionExecution.TerminalStatus.FAILED, null,
                DecisionExecution.FailureData.genericFailure(),
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
        String text = new DecisionExplanationProjector().text(
                new DecisionExplanationProjector().project(graph(), execution));
        assert text.contains("Result: Decision failed [failure]") : text;
        assertNoTechnicalLanguage(text);
    }

    private static void preservesRedactionAndRejectsMissingAdapters() {
        var codec = new DecisionExecution.DecisionValueCodec((value, context) ->
                new at.gepardec.fachtracing.api.DecisionValueAdapter.AdaptedValue(
                        value.type(), value.canonicalValue(), "REDACTED"));
        var redacted = codec.encode("private", "eligibility", "result");
        String text = new DecisionExplanationProjector().text(
                new DecisionExplanationProjector().project(graph(), execution(redacted, List.of())));
        assert text.contains("REDACTED");
        assert !text.contains("private");
        try {
            codec.encode(new Object(), "eligibility", "result");
            throw new AssertionError("missing adapter was accepted");
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage().contains("adapter");
        }
    }

    private static void unknownEvidenceMakesCoverageExplicit() {
        var observations = List.of(new DecisionExecution.NodeObservation(
                0, "unknown", "evaluated", Map.of(), null));
        var explanation = new DecisionExplanationProjector().project(
                graph(), execution(DecisionExecution.DecisionValue.of(true), observations));
        assert explanation.completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE;
        assert !explanation.coverageGaps().isEmpty();
    }

    private static void rendersBusinessExecutionMermaid() {
        var projector = new DecisionExplanationProjector();
        String diagram = new BusinessExecutionMermaidRenderer().render(
                projector.project(graph(), execution()));
        assert diagram.startsWith("flowchart TD\n") : diagram;
        assert diagram.contains("Decision: customer eligibility") : diagram;
        assert diagram.contains("age is below 24 (age was 20)") : diagram;
        assert diagram.contains("Result: true") : diagram;
        assertNoTechnicalLanguage(diagram);
    }

    public static BusinessDecisionGraph graph() {
        var nodes = List.of(
                node("entry", BusinessDecisionGraph.NodeKind.ENTRY, "Start"),
                node("age", BusinessDecisionGraph.NodeKind.PREDICATE, "age is below 24"),
                node("location", BusinessDecisionGraph.NodeKind.PREDICATE, "location equals Vienna"),
                node("outcome", BusinessDecisionGraph.NodeKind.OUTCOME, "Stop"));
        var edges = List.of(
                edge("e1", "entry", "age", "next"),
                edge("e2", "age", "location", "true"),
                edge("e3", "age", "outcome", "false; returns false"),
                edge("e4", "location", "outcome", "true; returns account active"),
                edge("e5", "location", "outcome", "false; returns false"));
        return new BusinessDecisionGraph("eligibility-graph", 1, "customer eligibility", "entry", nodes, edges,
                BusinessDecisionGraph.Completeness.COMPLETE, List.of());
    }

    public static DecisionExecution execution() {
        var observations = List.of(
                new DecisionExecution.NodeObservation(0, "entry", "started", Map.of(), null),
                new DecisionExecution.NodeObservation(1, "age", "true",
                        Map.of("age", DecisionExecution.DecisionValue.of(20)), null),
                new DecisionExecution.NodeObservation(2, "location", "true",
                        Map.of("location", DecisionExecution.DecisionValue.of("Vienna")), null),
                new DecisionExecution.NodeObservation(3, "outcome", "result",
                        Map.of("result", DecisionExecution.DecisionValue.of(true)), null));
        return execution(DecisionExecution.DecisionValue.of(true), observations);
    }

    private static DecisionExecution execution(
            DecisionExecution.DecisionValue result,
            List<DecisionExecution.NodeObservation> observations) {
        return new DecisionExecution("execution", "eligibility-graph", 1,
                Instant.parse("2026-07-24T08:00:00Z"), Instant.parse("2026-07-24T08:00:01Z"),
                observations, result, BusinessDecisionGraph.Completeness.COMPLETE, List.of());
    }

    private static BusinessDecisionGraph.DecisionNode node(
            String id, BusinessDecisionGraph.NodeKind kind, String label) {
        return new BusinessDecisionGraph.DecisionNode(id, kind, label, Map.of());
    }

    private static BusinessDecisionGraph.DecisionEdge edge(String id, String from, String to, String outcome) {
        return new BusinessDecisionGraph.DecisionEdge(id, from, to, outcome);
    }

    private static Path snapshot(String name) {
        return Path.of("fachtracing-engine/src/test/resources/snapshots", name);
    }

    public static void assertNoTechnicalLanguage(String value) {
        String lower = value.toLowerCase();
        for (String prohibited : List.of(".java", "bytecode", "stack frame", "fixtures.", "at.gepardec", "()")) {
            assert !lower.contains(prohibited) : prohibited + " in " + value;
        }
    }
}
